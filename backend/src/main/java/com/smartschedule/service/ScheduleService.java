package com.smartschedule.service;

import com.smartschedule.dto.AiScheduleResponse;
import com.smartschedule.entity.*;
import com.smartschedule.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 日程引擎 -- 核心排程算法。
 * 继承 Eve 的精力模型和贪心匹配算法。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final DailyPlanRepository planRepo;
    private final DailyTaskRepository taskRepo;
    private final GoalRepository goalRepo;
    private final CourseRepository courseRepo;
    private final UserRepository userRepo;
    private final PointsService pointsService;
    private final AiService aiService;

    @Transactional
    public DailyPlan generateDailyPlanGreedy(Long userId, LocalDate date) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 1. 检查是否已有今日计划
        Optional<DailyPlan> existingPlan = planRepo.findByUserIdAndPlanDate(userId, date);
        if (existingPlan.isPresent()) {
            List<DailyTask> oldTasks = taskRepo.findByDailyPlanIdOrderBySortOrderAsc(existingPlan.get().getId());
            taskRepo.deleteAll(oldTasks);
            planRepo.delete(existingPlan.get());
            planRepo.flush(); // 确保 delete 先写入 DB，避免后续 insert 触发唯一键冲突
        }

        // 2. 创建新计划
        DailyPlan plan = DailyPlan.builder()
                .user(user).planDate(date).status("DRAFT")
                .estimatedPoints(0).build();
        plan = planRepo.save(plan);

        // 3. 获取课程
        List<Course> courses = courseRepo.findByUserIdAndIsActiveTrue(userId);
        int dayOfWeek = date.getDayOfWeek().getValue();
        List<Course> todayCourses = new ArrayList<>();
        for (Course c : courses) {
            if (c.getDayOfWeek() == dayOfWeek) {
                todayCourses.add(c);
            }
        }

        // 4. 获取目标
        List<Goal> goals = goalRepo.findByUserIdAndIsActiveTrue(userId);
        Collections.sort(goals, new Comparator<Goal>() {
            public int compare(Goal a, Goal b) {
                return b.getPriority().compareTo(a.getPriority());
            }
        });

        // 5. 构建时间槽
        List<TimeSlot> slots = buildTimeSlots(user.getWakeUpTime(), user.getSleepTime(), date, user.getEnergyPeak());

        // 6. 标记课程
        for (Course course : todayCourses) {
            markOccupied(slots, course.getStartTime(), course.getEndTime());
        }

        // 7. 贪心分配
        int sortOrder = 0;
        int totalPoints = 0;
        for (Goal goal : goals) {
            int remaining = goal.getDefaultDuration();
            while (remaining > 0) {
                TimeSlot best = findBestSlot(slots, goal.getEnergyLevel(), remaining);
                if (best == null) break;

                int allocMinutes = Math.min(remaining, best.availableMinutes());
                DailyTask task = DailyTask.builder()
                        .dailyPlan(plan).goal(goal)
                        .name(goal.getName())
                        .taskType("GOAL")
                        .scheduledStart(best.start)
                        .scheduledEnd(best.start.plusMinutes(allocMinutes))
                        .durationMinutes(allocMinutes)
                        .energyLevel(goal.getEnergyLevel())
                        .priority(goal.getPriority())
                        .sortOrder(sortOrder++)
                        .status("PENDING")
                        .build();
                taskRepo.save(task);
                totalPoints += pointsService.calculateTaskPoints(goal.getCategory(), allocMinutes);

                best.start = best.start.plusMinutes(allocMinutes + 10);
                remaining -= allocMinutes;
            }
        }

        // 8. 插入课程
        for (Course course : todayCourses) {
            DailyTask task = DailyTask.builder()
                    .dailyPlan(plan).course(course)
                    .name("\uD83D\uDCD8 " + course.getName())
                    .taskType("COURSE")
                    .scheduledStart(LocalDateTime.of(date, course.getStartTime()))
                    .scheduledEnd(LocalDateTime.of(date, course.getEndTime()))
                    .durationMinutes((int) Duration.between(course.getStartTime(), course.getEndTime()).toMinutes())
                    .sortOrder(sortOrder++)
                    .status("PENDING")
                    .build();
            taskRepo.save(task);
        }

        // 9. AI 建议
        try {
            String aiSuggestion = aiService.generate(
                "你是一个日程规划专家。根据用户的任务列表给出1-2条简短建议（50字以内）。",
                "今日目标:" + goals.size() + "个, 课程:" + todayCourses.size() + "个");
            plan.setAiSuggestion(aiSuggestion);
        } catch (Exception e) {
            plan.setAiSuggestion("今天的日程已就绪，加油！");
        }

        plan.setEstimatedPoints(totalPoints);
        plan.setStatus("CONFIRMED");
        return planRepo.save(plan);
    }

    // ═══════════════════════════════════════════
    //  AI 生成日程（主入口：AI 优先，贪心兜底）
    // ═══════════════════════════════════════════

    @Transactional
    public DailyPlan generateDailyPlan(Long userId, LocalDate date) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 尝试 AI 生成
        try {
            AiScheduleResponse aiResponse = aiService.generateScheduleJson(user, date);
            if (aiResponse != null && aiResponse.getTasks() != null
                    && !aiResponse.getTasks().isEmpty()) {
                log.info("Using AI-generated schedule for user {} on {}", userId, date);
                return createPlanFromAiTasks(user, date, aiResponse);
            }
        } catch (Exception e) {
            log.warn("AI schedule generation failed, falling back to greedy: {}", e.getMessage());
        }

        // 回退到贪心算法
        log.info("Using greedy fallback for user {} on {}", userId, date);
        return generateDailyPlanGreedy(userId, date);
    }

    private DailyPlan createPlanFromAiTasks(User user, LocalDate date,
                                             AiScheduleResponse aiResponse) {
        // 1. 删除旧计划
        planRepo.findByUserIdAndPlanDate(user.getId(), date).ifPresent(oldPlan -> {
            List<DailyTask> oldTasks = taskRepo.findByDailyPlanIdOrderBySortOrderAsc(oldPlan.getId());
            taskRepo.deleteAll(oldTasks);
            planRepo.delete(oldPlan);
            planRepo.flush();
        });

        // 2. 创建新计划
        DailyPlan plan = DailyPlan.builder()
                .user(user).planDate(date).status("DRAFT")
                .estimatedPoints(0).build();
        plan = planRepo.save(plan);

        // 3. 加载目标和课程用于匹配
        List<Goal> goals = goalRepo.findByUserIdAndIsActiveTrue(user.getId());
        List<Course> allCourses = courseRepo.findByUserIdAndIsActiveTrue(user.getId());
        int dayOfWeek = date.getDayOfWeek().getValue();
        List<Course> todayCourses = allCourses.stream()
                .filter(c -> c.getDayOfWeek() == dayOfWeek)
                .collect(Collectors.toList());

        // 4. 从 AI 任务列表创建 DailyTask
        int sortOrder = 0;
        int totalPoints = 0;

        for (AiScheduleResponse.AiScheduleTask aiTask : aiResponse.getTasks()) {
            LocalTime startTime = parseTimeOrNull(aiTask.getStartTime());
            LocalTime endTime = parseTimeOrNull(aiTask.getEndTime());
            Integer duration = aiTask.getDurationMinutes();

            if (duration == null && startTime != null && endTime != null) {
                duration = (int) Duration.between(startTime, endTime).toMinutes();
            }
            if (duration == null) duration = 30;

            // 匹配 Goal 实体
            Goal matchedGoal = matchGoalByName(goals, aiTask.getName(), aiTask.getGoalName());
            // 匹配 Course 实体
            Course matchedCourse = matchCourseByName(todayCourses, aiTask.getCourseName());
            // 规范化 taskType
            String taskType = normalizeTaskType(aiTask.getTaskType());

            // 契约 §5：AI 返回的休息块直接丢弃，不再写成待办
            if ("BREAK".equals(taskType)) continue;

            DailyTask task = DailyTask.builder()
                    .dailyPlan(plan)
                    .goal(matchedGoal)
                    .course(matchedCourse)
                    .name(aiTask.getName() != null ? aiTask.getName() : "未命名任务")
                    .taskType(taskType)
                    .scheduledStart(startTime != null ? LocalDateTime.of(date, startTime) : null)
                    .scheduledEnd(endTime != null ? LocalDateTime.of(date, endTime) : null)
                    .durationMinutes(duration)
                    .energyLevel(aiTask.getEnergyLevel() != null ? aiTask.getEnergyLevel() : "MEDIUM")
                    .priority(aiTask.getPriority() != null ? aiTask.getPriority() : 5)
                    .sortOrder(sortOrder++)
                    .status("PENDING")
                    .build();

            taskRepo.save(task);

            if (matchedGoal != null) {
                totalPoints += pointsService.calculateTaskPoints(
                        matchedGoal.getCategory(), duration);
            }
        }

        // 5. AI 建议 & 积分
        plan.setAiSuggestion(
                aiResponse.getSuggestion() != null ? aiResponse.getSuggestion() : "今日日程已就绪，加油！");
        plan.setEstimatedPoints(totalPoints);
        plan.setStatus("CONFIRMED");
        return planRepo.save(plan);
    }

    // ── 辅助方法 ──

    private Goal matchGoalByName(List<Goal> goals, String taskName, String goalName) {
        String search = (goalName != null && !goalName.trim().isEmpty()) ? goalName.trim() : taskName;
        if (search == null || search.trim().isEmpty()) return null;

        // 1) 精确匹配（忽略大小写）
        for (Goal g : goals) {
            if (g.getName().equals(search) || g.getName().equalsIgnoreCase(search)) {
                return g;
            }
        }
        // 2) 包含匹配
        for (Goal g : goals) {
            if (search.contains(g.getName()) || g.getName().contains(search)) {
                return g;
            }
        }
        // 3) 前缀重叠 ≥ 3 字符且覆盖 ≥ 50%
        for (Goal g : goals) {
            String gn = g.getName();
            int prefixLen = longestCommonPrefix(search, gn);
            if (prefixLen >= 3 && (double) prefixLen / Math.max(search.length(), gn.length()) >= 0.5) {
                return g;
            }
        }
        return null;
    }

    private int longestCommonPrefix(String a, String b) {
        int i = 0;
        int max = Math.min(a.length(), b.length());
        while (i < max && a.charAt(i) == b.charAt(i)) i++;
        return i;
    }

    private Course matchCourseByName(List<Course> todayCourses, String courseName) {
        if (courseName == null || courseName.trim().isEmpty()) return null;
        String search = courseName.trim();
        for (Course c : todayCourses) {
            if (c.getName().equals(search) || c.getName().contains(search)
                    || search.contains(c.getName())) {
                return c;
            }
        }
        return null;
    }

    private String normalizeTaskType(String type) {
        if (type == null) return "CUSTOM";
        switch (type.toUpperCase()) {
            case "GOAL":   return "GOAL";
            case "BREAK":  return "BREAK";
            case "COURSE": return "COURSE";
            default:       return "CUSTOM";
        }
    }

    private LocalTime parseTimeOrNull(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) return null;
        try {
            return LocalTime.parse(timeStr.trim());
        } catch (DateTimeParseException e) {
            log.debug("Failed to parse time '{}': {}", timeStr, e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════

    public DailyPlan getTodayPlan(Long userId) {
        return getPlanByDate(userId, LocalDate.now());
    }

    public DailyPlan getPlanByDate(Long userId, LocalDate date) {
        Optional<DailyPlan> plan = planRepo.findByUserIdAndPlanDate(userId, date);
        return plan.orElse(null);
    }

    @Transactional
    public DailyTask updateTaskStatus(Long taskId, String status) {
        DailyTask task = taskRepo.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        task.setStatus(status);

        if ("COMPLETED".equals(status) && task.getGoal() != null && task.getDailyPlan() != null) {
            pointsService.awardPoints(task.getDailyPlan().getUser().getId(),
                    taskId,
                    pointsService.calculateTaskPoints(
                            task.getGoal().getCategory(), task.getDurationMinutes()),
                    "完成: " + task.getName());
        }
        return taskRepo.save(task);
    }

    // ---- 内部 ----

    private List<TimeSlot> buildTimeSlots(LocalTime wake, LocalTime sleep, LocalDate date, String peak) {
        List<TimeSlot> slots = new ArrayList<>();
        LocalDateTime cursor = LocalDateTime.of(date, wake);
        LocalDateTime end;
        if (sleep.isAfter(wake)) {
            end = LocalDateTime.of(date, sleep);
        } else {
            end = LocalDateTime.of(date.plusDays(1), sleep);
        }

        while (cursor.plusMinutes(30).isBefore(end) || cursor.plusMinutes(30).equals(end)) {
            int hour = cursor.getHour();
            int energy = 50;
            if ("MORNING".equals(peak) && hour >= 6 && hour < 12) energy = 85;
            else if ("AFTERNOON".equals(peak) && hour >= 12 && hour < 18) energy = 85;
            else if ("EVENING".equals(peak) && hour >= 18 && hour < 23) energy = 85;
            else if ("NIGHT".equals(peak) && (hour >= 23 || hour < 6)) energy = 85;

            slots.add(new TimeSlot(cursor, cursor.plusMinutes(30), energy));
            cursor = cursor.plusMinutes(30);
        }
        return slots;
    }

    private void markOccupied(List<TimeSlot> slots, LocalTime start, LocalTime end) {
        LocalDate date = slots.isEmpty() ? LocalDate.now() : slots.get(0).start.toLocalDate();
        LocalDateTime s = LocalDateTime.of(date, start);
        LocalDateTime e = LocalDateTime.of(date, end);
        for (TimeSlot slot : slots) {
            // 槽与课程 [s, e) 有重叠即为占用
            if (slot.start.isBefore(e) && slot.end.isAfter(s)) {
                slot.occupied = true;
            }
        }
    }

    private TimeSlot findBestSlot(List<TimeSlot> slots, String energyLevel, int minMinutes) {
        int targetEnergy;
        switch (energyLevel) {
            case "HIGH": targetEnergy = 85; break;
            case "MEDIUM": targetEnergy = 50; break;
            default: targetEnergy = 30; break;
        }
        // 优先匹配精力（每个槽固定 30 分钟，目标可能跨多个槽，
        // 只需找到有剩余时间的槽即可，由 while 循环负责跨槽分配）
        for (TimeSlot s : slots) {
            if (!s.occupied && s.energy >= targetEnergy && s.availableMinutes() > 0) {
                return s;
            }
        }
        // 降级：任意空闲槽
        for (TimeSlot s : slots) {
            if (!s.occupied && s.availableMinutes() > 0) {
                return s;
            }
        }
        return null;
    }

    static class TimeSlot {
        LocalDateTime start;
        LocalDateTime end;
        int energy;
        boolean occupied;

        TimeSlot(LocalDateTime start, LocalDateTime end, int energy) {
            this.start = start;
            this.end = end;
            this.energy = energy;
        }

        boolean contains(LocalDateTime time) {
            return !start.isAfter(time) && end.isAfter(time);
        }

        int availableMinutes() {
            return (int) Duration.between(start, end).toMinutes();
        }
    }
}
