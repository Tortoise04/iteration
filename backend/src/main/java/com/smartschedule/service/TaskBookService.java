package com.smartschedule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartschedule.dto.*;
import com.smartschedule.entity.*;
import com.smartschedule.exception.BusinessException;
import com.smartschedule.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 任务书服务 —— AI 生成 → 人工编辑 → 确认 → 逐日执行 → 逾期顺延。
 * 顺延算法严格按契约 §4：每日容量 {@value #DAILY_CAPACITY} 条（DONE 也占位），
 * 候选集 = plan_date &lt; today 的 PENDING 条目 + 被显式 defer 的那一条，
 * 按 (origin_date, sort_order) 升序从 max(today, plan_date + 1) 找第一个未满日期，
 * 一直找不到就落到 book.end_date，origin_date 永不改写。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskBookService {

    /** 同一本任务书同一天最多容纳的条目数（DONE 也占位）。 */
    public static final int DAILY_CAPACITY = 3;

    /** generate 未指定 endDate 时的默认跨度：4 周。 */
    private static final int DEFAULT_SPAN_DAYS = 27;

    private static final String[] THEME_CYCLE = {"打底", "推进", "强化", "冲刺", "复盘"};
    private static final String[] CATEGORY_CYCLE = {"STUDY", "PRACTICE", "REVIEW"};

    /** 契约 §2/§5：不允许出现的生活类条目关键词。 */
    private static final String[] FORBIDDEN_TITLE_WORDS = {
            "休息", "午休", "小憩", "吃饭", "早饭", "午饭", "晚饭", "早餐", "午餐", "晚餐", "用餐",
            "睡觉", "睡眠", "洗澡", "通勤", "路上"
    };

    /** 空话 learnContent 黑名单。 */
    private static final String[] VAGUE_CONTENT = {
            "学习", "学习英语", "复习", "复习一下", "练习", "看书", "努力", "好好学习"
    };

    private final TaskBookRepository bookRepo;
    private final TaskBookWeekRepository weekRepo;
    private final TaskBookItemRepository itemRepo;
    private final GoalRepository goalRepo;
    private final AiService aiService;
    private final ObjectMapper objectMapper;

    // ═══════════════════════════════════════════
    //  生成
    // ═══════════════════════════════════════════

    @Transactional
    public TaskBookDetail generate(Long userId, Long goalId, LocalDate startDate, LocalDate endDate) {
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : start.plusDays(DEFAULT_SPAN_DAYS);
        if (end.isBefore(start)) {
            throw new BusinessException("endDate 不能早于 startDate");
        }

        Goal goal = resolveGoal(userId, goalId);
        AiTaskBookResponse ai = generateFromAi(goal, start, end);
        boolean aiGenerated = ai != null;
        AiTaskBookResponse plan = aiGenerated ? ai : buildFallbackPlan(goal, start, end);

        String goalName = resolveGoalName(goal);
        String title = isBlank(plan.getTitle()) ? goalName + "任务书" : trim(plan.getTitle(), 120);
        String purpose = isBlank(plan.getPurpose()) ? defaultPurpose(goalName) : plan.getPurpose();
        String requirements = isBlank(plan.getRequirements())
                ? defaultRequirements(goalName) : plan.getRequirements();

        TaskBook book = bookRepo.save(TaskBook.builder()
                .userId(userId)
                .goalId(goal != null ? goal.getId() : null)
                .title(title)
                .purpose(purpose)
                .requirements(requirements)
                .startDate(start)
                .endDate(end)
                .status("DRAFT")
                .build());

        int fallbackIndex = 0;
        for (AiTaskBookResponse.AiWeek aw : plan.getWeeks()) {
            if (aw == null) continue;
            fallbackIndex++;
            Integer weekIndex = aw.getWeekIndex() != null && aw.getWeekIndex() > 0
                    ? aw.getWeekIndex() : fallbackIndex;
            LocalDate weekStart = clamp(parseDate(aw.getStartDate(), start), start, end);
            LocalDate weekEnd = clamp(parseDate(aw.getEndDate(), end), weekStart, end);

            TaskBookWeek week = weekRepo.save(TaskBookWeek.builder()
                    .bookId(book.getId())
                    .weekIndex(weekIndex)
                    .theme(isBlank(aw.getTheme()) ? "第" + weekIndex + "周" : trim(aw.getTheme(), 120))
                    .focus(isBlank(aw.getFocus()) ? goalName + "：第 " + weekIndex + " 周推进" : trim(aw.getFocus(), 255))
                    .startDate(weekStart)
                    .endDate(weekEnd)
                    .build());

            if (aw.getItems() == null) continue;
            Map<LocalDate, Integer> sortPerDay = new HashMap<LocalDate, Integer>();
            for (AiTaskBookResponse.AiItem item : aw.getItems()) {
                if (item == null || isBlank(item.getTitle())) continue;
                LocalDate planDate = clamp(parseDate(item.getPlanDate(), weekStart), start, end);
                int sortOrder = sortPerDay.containsKey(planDate) ? sortPerDay.get(planDate) : 0;
                sortPerDay.put(planDate, sortOrder + 1);
                int minutes = item.getEstMinutes() != null ? Math.max(5, Math.min(240, item.getEstMinutes())) : 40;

                itemRepo.save(TaskBookItem.builder()
                        .bookId(book.getId())
                        .weekId(week.getId())
                        .planDate(planDate)
                        .originDate(planDate)
                        .title(trim(item.getTitle(), 200))
                        .learnContent(item.getLearnContent())
                        .estMinutes(minutes)
                        .doneCriteria(isBlank(item.getDoneCriteria())
                                ? "完成本条内容并整理笔记" : trim(item.getDoneCriteria(), 255))
                        .category(normalizeCategory(item.getCategory()))
                        .status("PENDING")
                        .deferCount(0)
                        .sortOrder(sortOrder)
                        .build());
            }
        }

        log.info("Task book generated for user {}: bookId={}, aiGenerated={}", userId, book.getId(), aiGenerated);
        return buildDetail(book, Boolean.valueOf(aiGenerated));
    }

    public List<TaskBookSummary> list(Long userId) {
        List<TaskBook> books = bookRepo.findByUserIdOrderByCreatedAtDesc(userId);
        if (books.isEmpty()) return Collections.emptyList();

        List<Long> bookIds = new ArrayList<Long>();
        for (TaskBook book : books) bookIds.add(book.getId());
        Map<Long, int[]> stats = new HashMap<Long, int[]>();
        for (TaskBookItem item : itemRepo.findByBookIdIn(bookIds)) {
            if ("DROPPED".equals(item.getStatus())) continue;
            int[] counter = stats.get(item.getBookId());
            if (counter == null) {
                counter = new int[2];
                stats.put(item.getBookId(), counter);
            }
            counter[0]++;
            if ("DONE".equals(item.getStatus())) counter[1]++;
        }

        Map<Long, String> goalNames = loadGoalNames(books);
        List<TaskBookSummary> rows = new ArrayList<TaskBookSummary>();
        for (TaskBook book : books) {
            int[] counter = stats.get(book.getId());
            int total = counter != null ? counter[0] : 0;
            int done = counter != null ? counter[1] : 0;
            rows.add(TaskBookSummary.builder()
                    .id(book.getId())
                    .title(book.getTitle())
                    .goalId(book.getGoalId())
                    .goalName(book.getGoalId() == null ? null : goalNames.get(book.getGoalId()))
                    .startDate(book.getStartDate())
                    .endDate(book.getEndDate())
                    .status(book.getStatus())
                    .totalItems(Integer.valueOf(total))
                    .doneItems(Integer.valueOf(done))
                    .build());
        }
        return rows;
    }

    public TaskBookDetail detail(Long userId, Long bookId) {
        return buildDetail(requireBook(userId, bookId), null);
    }

    @Transactional
    public TaskBookDetail update(Long userId, Long bookId, String title, String purpose, String requirements) {
        TaskBook book = requireBook(userId, bookId);
        if (title != null) book.setTitle(trim(title, 120));
        if (purpose != null) book.setPurpose(purpose);
        if (requirements != null) book.setRequirements(requirements);
        bookRepo.save(book);
        return buildDetail(book, null);
    }

    @Transactional
    public TaskBookDetail updateStatus(Long userId, Long bookId, String status) {
        String normalized = normalizeBookStatus(status);
        TaskBook book = requireBook(userId, bookId);
        book.setStatus(normalized);
        bookRepo.save(book);

        List<MovedItem> moved = Collections.emptyList();
        if ("CONFIRMED".equals(normalized)) {
            moved = rescheduleBook(book, LocalDate.now(), null);
        }
        if (!moved.isEmpty()) {
            log.info("Task book {} confirmed: {} overdue item(s) rescheduled", bookId, moved.size());
        }
        return buildDetail(book, null);
    }

    @Transactional
    public TaskBookItem updateItem(Long userId, Long itemId, TaskBookItemUpdateRequest req) {
        TaskBookItem item = requireItem(userId, itemId);
        if (req != null) {
            if (req.getTitle() != null) item.setTitle(trim(req.getTitle(), 200));
            if (req.getLearnContent() != null) item.setLearnContent(req.getLearnContent());
            if (req.getEstMinutes() != null) {
                item.setEstMinutes(Integer.valueOf(Math.max(5, Math.min(240, req.getEstMinutes().intValue()))));
            }
            if (req.getDoneCriteria() != null) item.setDoneCriteria(trim(req.getDoneCriteria(), 255));
            if (req.getPlanDate() != null && !req.getPlanDate().trim().isEmpty()) {
                LocalDate planDate = parseDate(req.getPlanDate().trim(), null);
                if (planDate == null) {
                    throw new BusinessException("日期格式不正确，请使用 yyyy-MM-dd 格式");
                }
                item.setPlanDate(planDate);
                item.setWeekId(resolveWeekId(item.getBookId(), planDate, item.getWeekId()));
            }
        }
        return itemRepo.save(item);
    }

    @Transactional
    public Map<String, Object> updateItemStatus(Long userId, Long itemId, String status) {
        String normalized = normalizeItemStatus(status);
        TaskBookItem item = requireItem(userId, itemId);
        item.setStatus(normalized);
        itemRepo.save(item);

        List<MovedItem> moved = Collections.emptyList();
        if ("PENDING".equals(normalized)) {
            TaskBook book = bookRepo.findById(item.getBookId()).orElse(null);
            if (book != null) moved = rescheduleBook(book, LocalDate.now(), null);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("item", itemRepo.findById(itemId).orElse(item));
        result.put("rescheduled", toRescheduled(moved));
        return result;
    }

    @Transactional
    public Map<String, Object> deferItem(Long userId, Long itemId) {
        TaskBookItem item = requireItem(userId, itemId);
        TaskBook book = bookRepo.findById(item.getBookId())
                .orElseThrow(() -> new EntityNotFoundException("任务书不存在"));
        List<MovedItem> moved = rescheduleBook(book, LocalDate.now(), itemId);

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("item", itemRepo.findById(itemId).orElse(item));
        result.put("rescheduled", toRescheduled(moved));
        return result;
    }

    @Transactional
    public TaskBookItem createItem(Long userId, Long bookId, TaskBookItemCreateRequest req) {
        TaskBook book = requireBook(userId, bookId);
        if (req == null || isBlank(req.getTitle())) {
            throw new BusinessException("条目标题不能为空");
        }
        String rawDate = req.getPlanDate() == null ? null : req.getPlanDate().trim();
        LocalDate planDate = parseDate(rawDate, LocalDate.now());
        if (planDate == null) {
            throw new BusinessException("日期格式不正确，请使用 yyyy-MM-dd 格式");
        }
        planDate = clamp(planDate, book.getStartDate(), book.getEndDate());

        int estMinutes = req.getEstMinutes() == null ? 30
                : Math.max(5, Math.min(240, req.getEstMinutes().intValue()));
        TaskBookItem item = TaskBookItem.builder()
                .bookId(book.getId())
                .weekId(resolveWeekId(book.getId(), planDate, req.getWeekId()))
                .planDate(planDate)
                .originDate(planDate)
                .title(trim(req.getTitle(), 200))
                .learnContent(req.getLearnContent() == null ? "" : req.getLearnContent())
                .estMinutes(Integer.valueOf(estMinutes))
                .doneCriteria(req.getDoneCriteria() == null ? "" : trim(req.getDoneCriteria(), 255))
                .category(normalizeCategory(req.getCategory()))
                .status("PENDING")
                .deferCount(Integer.valueOf(0))
                .sortOrder(Integer.valueOf(nextSortOrder(book.getId(), planDate)))
                .build();
        return itemRepo.save(item);
    }

    @Transactional
    public Map<String, Object> deleteItem(Long userId, Long itemId) {
        TaskBookItem item = requireItem(userId, itemId);
        itemRepo.delete(item);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("deleted", Boolean.TRUE);
        return result;
    }

    private int nextSortOrder(Long bookId, LocalDate planDate) {
        List<TaskBookItem> all = itemRepo.findByBookIdOrderByPlanDateAscSortOrderAsc(bookId);
        int max = -1;
        if (all != null) {
            for (TaskBookItem existing : all) {
                if (planDate.equals(existing.getPlanDate()) && existing.getSortOrder() != null
                        && existing.getSortOrder().intValue() > max) {
                    max = existing.getSortOrder().intValue();
                }
            }
        }
        return max + 1;
    }

    @Transactional
    public Map<String, Object> rescheduleAll(Long userId) {
        LocalDate today = LocalDate.now();
        List<Map<String, Object>> moved = new ArrayList<Map<String, Object>>();
        for (TaskBook book : bookRepo.findByUserIdOrderByCreatedAtDesc(userId)) {
            moved.addAll(toMoved(rescheduleBook(book, today, null)));
        }
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("moved", moved);
        return result;
    }

    // ═══════════════════════════════════════════
    //  顺延算法（契约 §4）
    // ═══════════════════════════════════════════

    /**
     * 把一本任务书里逾期的 PENDING 条目顺延重排。
     *
     * @param explicitItemId 被显式 defer 的那一条（即使没逾期也要移动），可空
     * @return 真正发生了日期变化的条目
     */
    public List<MovedItem> rescheduleBook(TaskBook book, LocalDate today, Long explicitItemId) {
        List<MovedItem> moved = new ArrayList<MovedItem>();
        if (book == null || book.getId() == null) return moved;

        List<TaskBookItem> all = itemRepo.findByBookIdOrderByPlanDateAscSortOrderAsc(book.getId());
        if (all.isEmpty()) return moved;

        // 每日占位（DONE 也占位；DROPPED 不占位）
        Map<LocalDate, Integer> load = new HashMap<LocalDate, Integer>();
        for (TaskBookItem item : all) {
            if ("DROPPED".equals(item.getStatus())) continue;
            LocalDate key = item.getPlanDate();
            Integer current = load.get(key);
            load.put(key, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
        }

        List<TaskBookItem> candidates = new ArrayList<TaskBookItem>();
        for (TaskBookItem item : all) {
            if (!"PENDING".equals(item.getStatus())) continue;
            boolean overdue = item.getPlanDate() != null && item.getPlanDate().isBefore(today);
            boolean explicit = explicitItemId != null && explicitItemId.equals(item.getId());
            if (overdue || explicit) candidates.add(item);
        }
        if (candidates.isEmpty()) return moved;

        Collections.sort(candidates, new Comparator<TaskBookItem>() {
            public int compare(TaskBookItem a, TaskBookItem b) {
                LocalDate da = a.getOriginDate() != null ? a.getOriginDate() : a.getPlanDate();
                LocalDate db = b.getOriginDate() != null ? b.getOriginDate() : b.getPlanDate();
                int cmp = da.compareTo(db);
                if (cmp != 0) return cmp;
                int sa = a.getSortOrder() != null ? a.getSortOrder().intValue() : 0;
                int sb = b.getSortOrder() != null ? b.getSortOrder().intValue() : 0;
                return Integer.compare(sa, sb);
            }
        });

        LocalDate bookEnd = book.getEndDate() != null ? book.getEndDate() : today;
        List<TaskBookWeek> weeks = weekRepo.findByBookIdOrderByWeekIndexAsc(book.getId());

        for (TaskBookItem item : candidates) {
            LocalDate from = item.getPlanDate() != null ? item.getPlanDate() : today;
            LocalDate next = from.plusDays(1);
            LocalDate cursor = next.isAfter(today) ? next : today;

            LocalDate target = null;
            LocalDate probe = cursor;
            while (!probe.isAfter(bookEnd)) {
                Integer used = load.get(probe);
                if (used == null || used.intValue() < DAILY_CAPACITY) {
                    target = probe;
                    break;
                }
                probe = probe.plusDays(1);
            }
            // 兜底：区间全满 → 落到 end_date（允许当天超容量）
            if (target == null) target = bookEnd;

            item.setPlanDate(target);
            item.setDeferCount(Integer.valueOf(
                    (item.getDeferCount() == null ? 0 : item.getDeferCount().intValue()) + 1));
            item.setWeekId(resolveWeekId(weeks, target, item.getWeekId()));
            itemRepo.save(item);

            Integer fromLoad = load.get(from);
            if (fromLoad != null) load.put(from, Integer.valueOf(Math.max(0, fromLoad.intValue() - 1)));
            Integer toLoad = load.get(target);
            load.put(target, Integer.valueOf(toLoad == null ? 1 : toLoad.intValue() + 1));

            if (!target.equals(from)) {
                moved.add(new MovedItem(item.getId(), from, target));
            }
        }
        return moved;
    }

    /** 顺延结果条目。 */
    public static class MovedItem {
        public final Long itemId;
        public final LocalDate from;
        public final LocalDate to;

        public MovedItem(Long itemId, LocalDate from, LocalDate to) {
            this.itemId = itemId;
            this.from = from;
            this.to = to;
        }
    }

    // ═══════════════════════════════════════════
    //  今天（契约 §3.1，只读）
    // ═══════════════════════════════════════════

    public Map<String, Object> today(Long userId, LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        List<TaskBook> books = bookRepo.findByUserIdAndStatusInOrderByCreatedAtDesc(
                userId, Collections.singletonList("CONFIRMED"));

        List<Long> bookIds = new ArrayList<Long>();
        Map<Long, TaskBook> bookById = new LinkedHashMap<Long, TaskBook>();
        for (TaskBook book : books) {
            bookIds.add(book.getId());
            bookById.put(book.getId(), book);
        }

        List<Map<String, Object>> bookViews = new ArrayList<Map<String, Object>>();
        int done = 0;
        int total = 0;

        for (TaskBook book : books) {
            List<TaskBookItem> items = itemRepo.findByBookIdOrderByPlanDateAscSortOrderAsc(book.getId());
            List<TaskBookItem> dayItems = new ArrayList<TaskBookItem>();
            for (TaskBookItem item : items) {
                if ("DROPPED".equals(item.getStatus())) continue;
                total++;
                if ("DONE".equals(item.getStatus())) done++;
                if (target.equals(item.getPlanDate())) dayItems.add(item);
            }
            if (dayItems.isEmpty()) continue;

            Map<String, Object> view = new LinkedHashMap<String, Object>();
            view.put("bookId", book.getId());
            view.put("title", book.getTitle());
            view.put("theme", weekTheme(book.getId(), target));
            view.put("items", dayItems);
            bookViews.add(view);
        }

        List<Map<String, Object>> behindItems = new ArrayList<Map<String, Object>>();
        if (!bookIds.isEmpty()) {
            List<TaskBookItem> overdue = itemRepo.findByBookIdInAndStatusAndPlanDateLessThan(
                    bookIds, "PENDING", target);
            Collections.sort(overdue, new Comparator<TaskBookItem>() {
                public int compare(TaskBookItem a, TaskBookItem b) {
                    int cmp = a.getPlanDate().compareTo(b.getPlanDate());
                    if (cmp != 0) return cmp;
                    Long ba = a.getBookId() == null ? 0L : a.getBookId();
                    Long bb = b.getBookId() == null ? 0L : b.getBookId();
                    return ba.compareTo(bb);
                }
            });
            for (TaskBookItem item : overdue) {
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("itemId", item.getId());
                row.put("bookId", item.getBookId());
                row.put("title", item.getTitle());
                row.put("originDate", item.getOriginDate() == null ? null : item.getOriginDate().toString());
                row.put("planDate", item.getPlanDate() == null ? null : item.getPlanDate().toString());
                row.put("deferCount", item.getDeferCount());
                behindItems.add(row);
            }
        }

        Map<String, Object> behind = new LinkedHashMap<String, Object>();
        behind.put("count", Integer.valueOf(behindItems.size()));
        behind.put("items", behindItems);

        Map<String, Object> progress = new LinkedHashMap<String, Object>();
        progress.put("done", Integer.valueOf(done));
        progress.put("total", Integer.valueOf(total));

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("date", target.toString());
        result.put("books", bookViews);
        result.put("behind", behind);
        result.put("progress", progress);
        return result;
    }

    // ═══════════════════════════════════════════
    //  AI 生成 + 校验（契约 §2）
    // ═══════════════════════════════════════════

    private AiTaskBookResponse generateFromAi(Goal goal, LocalDate start, LocalDate end) {
        try {
            String raw = aiService.generateTaskBookJson(goal, start, end, buildExtraContext(goal));
            String json = stripToJson(raw);
            if (json == null) return null;
            AiTaskBookResponse parsed = objectMapper.readValue(json, AiTaskBookResponse.class);
            AiTaskBookResponse clean = sanitizeAiResponse(parsed, goal, start, end);
            if (clean == null) {
                log.warn("AI task book JSON failed validation, falling back to deterministic generation");
            }
            return clean;
        } catch (Exception e) {
            log.warn("AI task book generation failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildExtraContext(Goal goal) {
        if (goal == null) return null;
        StringBuilder sb = new StringBuilder();
        sb.append("目标分类: ").append(goal.getCategory());
        sb.append("; 每次建议时长: ").append(goal.getDefaultDuration()).append(" 分钟");
        sb.append("; 每周频次: ").append(goal.getWeeklyFrequency()).append(" 次");
        if (!isBlank(goal.getDescription())) {
            sb.append("; 目标描述: ").append(goal.getDescription());
        }
        return sb.toString();
    }

    private String stripToJson(String raw) {
        if (raw == null) return null;
        String text = raw.trim();
        if (text.isEmpty()) return null;
        if (text.startsWith("```")) {
            int firstLine = text.indexOf('\n');
            if (firstLine > 0) {
                text = text.substring(firstLine + 1);
            }
            int fence = text.lastIndexOf("```");
            if (fence >= 0) text = text.substring(0, fence);
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        return text.substring(start, end + 1).trim();
    }

    /** 逐条校验 AI 结果；不合格整体返回 null（走确定性兜底）。 */
    private AiTaskBookResponse sanitizeAiResponse(AiTaskBookResponse raw, Goal goal,
                                                  LocalDate start, LocalDate end) {
        if (raw == null || raw.getWeeks() == null || raw.getWeeks().isEmpty()) return null;
        String goalName = resolveGoalName(goal);

        List<AiTaskBookResponse.AiWeek> cleanWeeks = new ArrayList<AiTaskBookResponse.AiWeek>();
        int fallbackIndex = 0;
        int totalItems = 0;

        for (AiTaskBookResponse.AiWeek rawWeek : raw.getWeeks()) {
            if (rawWeek == null) continue;
            fallbackIndex++;
            int weekIndex = rawWeek.getWeekIndex() != null && rawWeek.getWeekIndex() > 0
                    ? rawWeek.getWeekIndex() : fallbackIndex;
            LocalDate weekStart = parseDate(rawWeek.getStartDate(), null);
            LocalDate weekEnd = parseDate(rawWeek.getEndDate(), null);
            if (weekStart == null) weekStart = start.plusDays((long) (weekIndex - 1) * 7);
            if (weekEnd == null) weekEnd = weekStart.plusDays(6);
            // 越界日期必须裁掉（契约 §2 日期不得越界）
            if (weekStart.isBefore(start)) weekStart = start;
            if (weekEnd.isAfter(end)) weekEnd = end;
            if (weekEnd.isBefore(weekStart) || weekStart.isAfter(end)) continue;

            List<AiTaskBookResponse.AiItem> cleanItems = new ArrayList<AiTaskBookResponse.AiItem>();
            Map<LocalDate, Integer> perDay = new HashMap<LocalDate, Integer>();

            if (rawWeek.getItems() != null) {
                for (AiTaskBookResponse.AiItem rawItem : rawWeek.getItems()) {
                    if (rawItem == null || isBlank(rawItem.getTitle())) continue;
                    if (containsForbiddenWord(rawItem.getTitle())) continue;

                    LocalDate planDate = parseDate(rawItem.getPlanDate(), null);
                    if (planDate == null || planDate.isBefore(start) || planDate.isAfter(end)) continue;
                    Integer used = perDay.get(planDate);
                    if (used != null && used.intValue() >= 3) continue;
                    perDay.put(planDate, Integer.valueOf(used == null ? 1 : used.intValue() + 1));

                    AiTaskBookResponse.AiItem clean = new AiTaskBookResponse.AiItem();
                    clean.setPlanDate(planDate.toString());
                    clean.setTitle(trim(rawItem.getTitle(), 200));
                    clean.setLearnContent(sanitizeLearnContent(
                            rawItem.getLearnContent(), goalName, weekIndex, planDate));
                    int minutes = rawItem.getEstMinutes() != null ? rawItem.getEstMinutes().intValue() : 40;
                    clean.setEstMinutes(Integer.valueOf(Math.max(5, Math.min(240, minutes))));
                    clean.setDoneCriteria(isBlank(rawItem.getDoneCriteria())
                            ? "完成本条内容并整理笔记" : trim(rawItem.getDoneCriteria(), 255));
                    clean.setCategory(normalizeCategory(rawItem.getCategory()));
                    cleanItems.add(clean);
                    totalItems++;
                }
            }

            AiTaskBookResponse.AiWeek cleanWeek = new AiTaskBookResponse.AiWeek();
            cleanWeek.setWeekIndex(Integer.valueOf(weekIndex));
            cleanWeek.setTheme(isBlank(rawWeek.getTheme()) ? "第" + weekIndex + "周" : trim(rawWeek.getTheme(), 120));
            cleanWeek.setFocus(isBlank(rawWeek.getFocus())
                    ? goalName + "：第 " + weekIndex + " 周推进" : trim(rawWeek.getFocus(), 255));
            cleanWeek.setStartDate(weekStart.toString());
            cleanWeek.setEndDate(weekEnd.toString());
            cleanWeek.setItems(cleanItems);
            cleanWeeks.add(cleanWeek);
        }

        if (cleanWeeks.isEmpty() || totalItems == 0) return null;
        // 契约 §2：周必须覆盖 startDate..endDate 整段
        if (!coversRange(cleanWeeks, start, end)) return null;

        AiTaskBookResponse result = new AiTaskBookResponse();
        result.setTitle(isBlank(raw.getTitle()) ? goalName + "任务书" : trim(raw.getTitle(), 120));
        result.setPurpose(isBlank(raw.getPurpose()) ? defaultPurpose(goalName) : raw.getPurpose());
        result.setRequirements(isBlank(raw.getRequirements())
                ? defaultRequirements(goalName) : raw.getRequirements());
        result.setWeeks(cleanWeeks);
        return result;
    }

    private boolean coversRange(List<AiTaskBookResponse.AiWeek> weeks, LocalDate start, LocalDate end) {
        List<AiTaskBookResponse.AiWeek> sorted = new ArrayList<AiTaskBookResponse.AiWeek>(weeks);
        Collections.sort(sorted, new Comparator<AiTaskBookResponse.AiWeek>() {
            public int compare(AiTaskBookResponse.AiWeek a, AiTaskBookResponse.AiWeek b) {
                return parseDate(a.getStartDate(), LocalDate.MIN)
                        .compareTo(parseDate(b.getStartDate(), LocalDate.MIN));
            }
        });
        LocalDate cursor = start;
        for (AiTaskBookResponse.AiWeek week : sorted) {
            LocalDate ws = parseDate(week.getStartDate(), start);
            LocalDate we = parseDate(week.getEndDate(), start);
            if (ws.isAfter(cursor)) return false;
            if (!we.isBefore(cursor)) cursor = we.plusDays(1);
        }
        return cursor.isAfter(end);
    }

    /** learnContent 校验：空话/过短按空内容处理，补一条带具体量的模板。 */
    private String sanitizeLearnContent(String content, String goalName, int weekIndex, LocalDate planDate) {
        if (content != null) {
            String text = content.trim();
            if (text.length() >= 6 && !isVague(text) && hasConcreteDetail(text)) {
                return text;
            }
        }
        return templateLearnContent(goalName, weekIndex, planDate);
    }

    private boolean isVague(String text) {
        for (String vague : VAGUE_CONTENT) {
            if (text.equals(vague)) return true;
        }
        return false;
    }

    /** 具体性判断：出现数字（题量/词数/页码）或足够长的描述。 */
    private boolean hasConcreteDetail(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isDigit(text.charAt(i))) return true;
        }
        return text.length() >= 12;
    }

    private String templateLearnContent(String goalName, int weekIndex, LocalDate planDate) {
        return "「" + goalName + "」第 " + weekIndex + " 周（" + planDate + "）：完成 1 个单元材料，"
                + "练习题 20 道并订正错题 5 道，整理笔记 3 条，生词/要点 15 个";
    }

    private boolean containsForbiddenWord(String title) {
        for (String word : FORBIDDEN_TITLE_WORDS) {
            if (title.contains(word)) return true;
        }
        return false;
    }

    // ═══════════════════════════════════════════
    //  确定性兜底生成
    // ═══════════════════════════════════════════

    private AiTaskBookResponse buildFallbackPlan(Goal goal, LocalDate start, LocalDate end) {
        String goalName = resolveGoalName(goal);
        String desc = goal != null && !isBlank(goal.getDescription()) ? trim(goal.getDescription(), 40) : null;
        int weekCount = (int) ((end.toEpochDay() - start.toEpochDay()) / 7) + 1;

        AiTaskBookResponse plan = new AiTaskBookResponse();
        plan.setTitle(goalName + "·" + weekCount + "周任务书");
        plan.setPurpose(defaultPurpose(goalName));
        plan.setRequirements(defaultRequirements(goalName));

        List<AiTaskBookResponse.AiWeek> weeks = new ArrayList<AiTaskBookResponse.AiWeek>();
        for (int w = 1; w <= weekCount; w++) {
            LocalDate weekStart = start.plusDays((long) (w - 1) * 7);
            if (weekStart.isAfter(end)) break;
            LocalDate weekEnd = weekStart.plusDays(6);
            if (weekEnd.isAfter(end)) weekEnd = end;

            AiTaskBookResponse.AiWeek week = new AiTaskBookResponse.AiWeek();
            week.setWeekIndex(Integer.valueOf(w));
            week.setTheme("第" + w + "周·" + THEME_CYCLE[(w - 1) % THEME_CYCLE.length]);
            week.setFocus(goalName + "：每天 1-2 条，推进到第 " + w + " 阶段");
            week.setStartDate(weekStart.toString());
            week.setEndDate(weekEnd.toString());

            List<AiTaskBookResponse.AiItem> items = new ArrayList<AiTaskBookResponse.AiItem>();
            LocalDate day = weekStart;
            while (!day.isAfter(weekEnd)) {
                int dow = day.getDayOfWeek().getValue();
                boolean busy = (dow == 1 || dow == 3 || dow == 5) && !day.equals(weekEnd);
                int itemCount = busy ? 2 : 1;
                for (int i = 0; i < itemCount; i++) {
                    int qty = 20 + w * 5 + i * 10;
                    AiTaskBookResponse.AiItem item = new AiTaskBookResponse.AiItem();
                    item.setPlanDate(day.toString());
                    item.setTitle(goalName + "·第" + w + "周 " + (i == 0 ? "推进" : "巩固"));
                    item.setLearnContent("「" + goalName + "」第 " + w + " 周 " + day + "：完成 "
                            + (i == 0 ? "新内容 1 个单元" : "巩固材料 1 个单元")
                            + "，练习题 " + qty + " 道，订正错题 5 道，整理笔记 3 条"
                            + (desc != null ? "（结合目标：" + desc + "）" : ""));
                    item.setEstMinutes(Integer.valueOf(i == 0 ? 45 : 30));
                    item.setDoneCriteria("完成上述材料与 " + qty + " 道练习题，正确率≥70%，笔记已整理");
                    item.setCategory(CATEGORY_CYCLE[(w + i) % CATEGORY_CYCLE.length]);
                    items.add(item);
                }
                day = day.plusDays(1);
            }
            week.setItems(items);
            weeks.add(week);
        }
        plan.setWeeks(weeks);
        return plan;
    }

    private String defaultPurpose(String goalName) {
        return "本任务书把「" + goalName + "」拆成按周推进的每日条目："
                + "先打底，再推进，最后强化与复盘。每天 1-2 条、每条写清做什么/学什么/多久/怎么算完成，"
                + "保证节奏不断，逾期条目会自动顺延重排。";
    }

    private String defaultRequirements(String goalName) {
        return String.join(System.lineSeparator(),
                "1. 每天按当日条目执行，完成后立刻勾选；",
                "2. 未完成的条目点「未完成」，系统自动顺延，不手动改日期；",
                "3. 每条结束后写 3 条笔记，错题当天订正；",
                "4. 每天最多 3 条，做不完优先保证第一条；",
                "5. 每周末回看本周完成度，再进入下一周。");
    }

    // ═══════════════════════════════════════════
    //  详情组装
    // ═══════════════════════════════════════════

    private TaskBookDetail buildDetail(TaskBook book, Boolean aiGenerated) {
        List<TaskBookWeek> weeks = weekRepo.findByBookIdOrderByWeekIndexAsc(book.getId());
        List<TaskBookItem> items = itemRepo.findByBookIdOrderByPlanDateAscSortOrderAsc(book.getId());

        Map<Long, List<TaskBookItem>> byWeek = new LinkedHashMap<Long, List<TaskBookItem>>();
        for (TaskBookWeek week : weeks) {
            byWeek.put(week.getId(), new ArrayList<TaskBookItem>());
        }
        List<TaskBookItem> orphans = new ArrayList<TaskBookItem>();
        for (TaskBookItem item : items) {
            List<TaskBookItem> bucket = item.getWeekId() == null ? null : byWeek.get(item.getWeekId());
            if (bucket != null) bucket.add(item);
            else orphans.add(item);
        }
        for (TaskBookItem orphan : orphans) {
            TaskBookWeek week = findWeek(weeks, orphan.getPlanDate());
            if (week != null) byWeek.get(week.getId()).add(orphan);
        }

        int done = 0;
        int total = 0;
        for (TaskBookItem item : items) {
            if ("DROPPED".equals(item.getStatus())) continue;
            total++;
            if ("DONE".equals(item.getStatus())) done++;
        }

        List<TaskBookDetail.WeekView> views = new ArrayList<TaskBookDetail.WeekView>();
        for (TaskBookWeek week : weeks) {
            views.add(TaskBookDetail.WeekView.builder()
                    .id(week.getId())
                    .weekIndex(week.getWeekIndex())
                    .theme(week.getTheme())
                    .focus(week.getFocus())
                    .startDate(week.getStartDate())
                    .endDate(week.getEndDate())
                    .items(byWeek.get(week.getId()))
                    .build());
        }

        Goal goal = book.getGoalId() == null ? null : goalRepo.findById(book.getGoalId()).orElse(null);
        return TaskBookDetail.builder()
                .id(book.getId())
                .title(book.getTitle())
                .purpose(book.getPurpose())
                .requirements(book.getRequirements())
                .goalId(book.getGoalId())
                .goalName(goal == null ? null : goal.getName())
                .startDate(book.getStartDate())
                .endDate(book.getEndDate())
                .status(book.getStatus())
                .progress(new TaskBookDetail.Progress(done, total))
                .weeks(views)
                .aiGenerated(aiGenerated)
                .build();
    }

    // ═══════════════════════════════════════════
    //  辅助
    // ═══════════════════════════════════════════

    private TaskBook requireBook(Long userId, Long bookId) {
        TaskBook book = bookRepo.findById(bookId)
                .orElseThrow(() -> new EntityNotFoundException("任务书不存在"));
        if (userId == null || !userId.equals(book.getUserId())) {
            throw new EntityNotFoundException("任务书不存在");
        }
        return book;
    }

    private TaskBookItem requireItem(Long userId, Long itemId) {
        TaskBookItem item = itemRepo.findById(itemId)
                .orElseThrow(() -> new EntityNotFoundException("任务条目不存在"));
        TaskBook book = bookRepo.findById(item.getBookId())
                .orElseThrow(() -> new EntityNotFoundException("任务书不存在"));
        if (userId == null || !userId.equals(book.getUserId())) {
            throw new EntityNotFoundException("任务条目不存在");
        }
        return item;
    }

    private Goal resolveGoal(Long userId, Long goalId) {
        if (goalId != null) {
            Goal goal = goalRepo.findById(goalId).orElse(null);
            if (goal != null && goal.getUser() != null && userId.equals(goal.getUser().getId())) {
                return goal;
            }
        }
        List<Goal> goals = goalRepo.findByUserIdAndIsActiveTrue(userId);
        if (goals == null || goals.isEmpty()) return null;
        Collections.sort(goals, new Comparator<Goal>() {
            public int compare(Goal a, Goal b) {
                int pa = a.getPriority() == null ? 5 : a.getPriority().intValue();
                int pb = b.getPriority() == null ? 5 : b.getPriority().intValue();
                return Integer.compare(pb, pa);
            }
        });
        return goals.get(0);
    }

    private String resolveGoalName(Goal goal) {
        if (goal != null && !isBlank(goal.getName())) return goal.getName().trim();
        return "我的目标";
    }

    private Map<Long, String> loadGoalNames(List<TaskBook> books) {
        Map<Long, String> names = new HashMap<Long, String>();
        List<Long> goalIds = new ArrayList<Long>();
        for (TaskBook book : books) {
            if (book.getGoalId() != null && !goalIds.contains(book.getGoalId())) {
                goalIds.add(book.getGoalId());
            }
        }
        if (goalIds.isEmpty()) return names;
        for (Goal goal : goalRepo.findAllById(goalIds)) {
            names.put(goal.getId(), goal.getName());
        }
        return names;
    }

    private String weekTheme(Long bookId, LocalDate date) {
        TaskBookWeek week = findWeek(weekRepo.findByBookIdOrderByWeekIndexAsc(bookId), date);
        return week == null ? null : week.getTheme();
    }

    private TaskBookWeek findWeek(List<TaskBookWeek> weeks, LocalDate date) {
        if (date == null) return null;
        for (TaskBookWeek week : weeks) {
            if (week.getStartDate() == null || week.getEndDate() == null) continue;
            if (!date.isBefore(week.getStartDate()) && !date.isAfter(week.getEndDate())) return week;
        }
        return null;
    }

    private Long resolveWeekId(Long bookId, LocalDate date, Long current) {
        return resolveWeekId(weekRepo.findByBookIdOrderByWeekIndexAsc(bookId), date, current);
    }

    private Long resolveWeekId(List<TaskBookWeek> weeks, LocalDate date, Long current) {
        TaskBookWeek week = findWeek(weeks, date);
        return week == null ? current : week.getId();
    }

    private String normalizeBookStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new BusinessException("status 不能为空");
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"DRAFT".equals(normalized) && !"CONFIRMED".equals(normalized) && !"ARCHIVED".equals(normalized)) {
            throw new BusinessException("status 只能是 DRAFT / CONFIRMED / ARCHIVED");
        }
        return normalized;
    }

    private String normalizeItemStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new BusinessException("status 不能为空");
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!"DONE".equals(normalized) && !"PENDING".equals(normalized) && !"DROPPED".equals(normalized)) {
            throw new BusinessException("status 只能是 DONE / PENDING / DROPPED");
        }
        return normalized;
    }

    private String normalizeCategory(String category) {
        if (category == null) return "STUDY";
        String normalized = category.trim().toUpperCase(Locale.ROOT);
        if ("STUDY".equals(normalized) || "PRACTICE".equals(normalized) || "REVIEW".equals(normalized)) {
            return normalized;
        }
        return "STUDY";
    }

    private List<Map<String, Object>> toRescheduled(List<MovedItem> moved) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (MovedItem item : moved) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("itemId", item.itemId);
            row.put("planDate", item.to.toString());
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, Object>> toMoved(List<MovedItem> moved) {
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        for (MovedItem item : moved) {
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("itemId", item.itemId);
            row.put("from", item.from.toString());
            row.put("to", item.to.toString());
            rows.add(row);
        }
        return rows;
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.trim().isEmpty()) return fallback;
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }

    private LocalDate clamp(LocalDate date, LocalDate min, LocalDate max) {
        if (date == null) return min;
        if (date.isBefore(min)) return min;
        if (max != null && date.isAfter(max)) return max;
        return date;
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        String text = value.trim();
        return text.length() <= max ? text : text.substring(0, max);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
