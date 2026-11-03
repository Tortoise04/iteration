package com.smartschedule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartschedule.entity.TaskBook;
import com.smartschedule.entity.TaskBookItem;
import com.smartschedule.entity.TaskBookWeek;
import com.smartschedule.repository.GoalRepository;
import com.smartschedule.repository.TaskBookItemRepository;
import com.smartschedule.repository.TaskBookRepository;
import com.smartschedule.repository.TaskBookWeekRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 顺延算法测试（契约 §4）。
 * 覆盖：逾期两条、当天已有 3 条（含 DONE 占位）、跨周、endDate 已满、无逾期幂等。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TaskBookServiceTest {

    private static final Long BOOK_ID = 1L;
    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);

    @Mock private TaskBookRepository bookRepo;
    @Mock private TaskBookWeekRepository weekRepo;
    @Mock private TaskBookItemRepository itemRepo;
    @Mock private GoalRepository goalRepo;
    @Mock private AiService aiService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private TaskBookService taskBookService;

    @BeforeEach
    void setUp() {
        when(itemRepo.save(any(TaskBookItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(weekRepo.findByBookIdOrderByWeekIndexAsc(anyLong()))
                .thenReturn(Collections.<TaskBookWeek>emptyList());
    }

    // ── 情况 1：逾期两条都顺延到今天（今天还有空位）──
    @Test
    void testReschedule_TwoOverdueItems_BothMovedToToday() {
        TaskBook book = book(LocalDate.of(2026, 11, 17));
        List<TaskBookItem> items = new ArrayList<>();
        items.add(item(101L, LocalDate.of(2026, 10, 19), "PENDING", 0));
        items.add(item(102L, LocalDate.of(2026, 10, 20), "PENDING", 0));
        when(itemRepo.findByBookIdOrderByPlanDateAscSortOrderAsc(BOOK_ID)).thenReturn(items);

        List<TaskBookService.MovedItem> moved =
                taskBookService.rescheduleBook(book, TODAY, null);

        assertEquals(2, moved.size(), "两条逾期条目都应被顺延");
        assertEquals(TODAY, items.get(0).getPlanDate());
        assertEquals(TODAY, items.get(1).getPlanDate());
        assertEquals(Integer.valueOf(1), items.get(0).getDeferCount());
        assertEquals(Integer.valueOf(1), items.get(1).getDeferCount());
        // origin_date 永不改写
        assertEquals(LocalDate.of(2026, 10, 19), items.get(0).getOriginDate());
        assertEquals(LocalDate.of(2026, 10, 20), items.get(1).getOriginDate());
    }

    // ── 情况 2：当天已有 3 条（含 DONE 占位）→ 顺延到次日 ──
    @Test
    void testReschedule_TodayFullThreeItems_MovesToTomorrow() {
        TaskBook book = book(LocalDate.of(2026, 11, 17));
        List<TaskBookItem> items = new ArrayList<>();
        items.add(item(201L, TODAY, "PENDING", 0));
        items.add(item(202L, TODAY, "PENDING", 1));
        items.add(item(203L, TODAY, "DONE", 2));      // DONE 也占位
        TaskBookItem overdue = item(204L, LocalDate.of(2026, 10, 20), "PENDING", 0);
        items.add(overdue);
        when(itemRepo.findByBookIdOrderByPlanDateAscSortOrderAsc(BOOK_ID)).thenReturn(items);

        List<TaskBookService.MovedItem> moved =
                taskBookService.rescheduleBook(book, TODAY, null);

        assertEquals(1, moved.size());
        assertEquals(TODAY.plusDays(1), overdue.getPlanDate(), "当天已满 3 条，应落到次日");
        assertEquals(Integer.valueOf(1), overdue.getDeferCount());
        assertEquals(LocalDate.of(2026, 10, 20), moved.get(0).from, "from 是条目原 planDate");
        assertEquals(TODAY.plusDays(1), moved.get(0).to);
    }

    // ── 情况 3：本周已排满 → 跨周到下一周，weekId 同步到新周 ──
    @Test
    void testReschedule_CrossWeek_LandsInNextWeek() {
        TaskBook book = book(LocalDate.of(2026, 11, 17));
        TaskBookWeek week1 = TaskBookWeek.builder().id(11L).bookId(BOOK_ID).weekIndex(1)
                .startDate(TODAY).endDate(TODAY.plusDays(6)).build();
        TaskBookWeek week2 = TaskBookWeek.builder().id(12L).bookId(BOOK_ID).weekIndex(2)
                .startDate(TODAY.plusDays(7)).endDate(TODAY.plusDays(13)).build();
        when(weekRepo.findByBookIdOrderByWeekIndexAsc(BOOK_ID))
                .thenReturn(Arrays.asList(week1, week2));

        List<TaskBookItem> items = new ArrayList<>();
        long id = 300L;
        // 今天起连续 7 天，每天排满 3 条
        for (int day = 0; day < 7; day++) {
            for (int slot = 0; slot < TaskBookService.DAILY_CAPACITY; slot++) {
                items.add(item(id++, TODAY.plusDays(day), "PENDING", slot));
            }
        }
        TaskBookItem overdue = item(399L, TODAY.minusDays(1), "PENDING", 0);
        items.add(overdue);
        when(itemRepo.findByBookIdOrderByPlanDateAscSortOrderAsc(BOOK_ID)).thenReturn(items);

        List<TaskBookService.MovedItem> moved =
                taskBookService.rescheduleBook(book, TODAY, null);

        assertEquals(1, moved.size());
        assertEquals(TODAY.plusDays(7), overdue.getPlanDate(), "本周排满后应跨到下一周的第一天");
        assertEquals(Long.valueOf(12L), overdue.getWeekId(), "weekId 应同步到条目所在的新周");
    }

    // ── 情况 4：endDate 之前全满 → 落到 endDate（允许当天超过 3 条）──
    @Test
    void testReschedule_EndDateFull_FallsBackToEndDate() {
        LocalDate end = TODAY.plusDays(2);
        TaskBook book = book(end);
        List<TaskBookItem> items = new ArrayList<>();
        long id = 400L;
        for (int day = 0; day <= 2; day++) {
            for (int slot = 0; slot < TaskBookService.DAILY_CAPACITY; slot++) {
                items.add(item(id++, TODAY.plusDays(day), "PENDING", slot));
            }
        }
        TaskBookItem overdue = item(499L, TODAY.minusDays(3), "PENDING", 0);
        items.add(overdue);
        when(itemRepo.findByBookIdOrderByPlanDateAscSortOrderAsc(BOOK_ID)).thenReturn(items);

        List<TaskBookService.MovedItem> moved =
                taskBookService.rescheduleBook(book, TODAY, null);

        assertEquals(1, moved.size());
        assertEquals(end, overdue.getPlanDate(), "全满时应落到 endDate");
        assertEquals(Integer.valueOf(1), overdue.getDeferCount());
    }

    // ── 情况 5：没有逾期条目 → 幂等，返回空 ──
    @Test
    void testReschedule_NoOverdue_ReturnsEmptyAndKeepsDates() {
        TaskBook book = book(LocalDate.of(2026, 11, 17));
        List<TaskBookItem> items = new ArrayList<>();
        items.add(item(501L, TODAY, "PENDING", 0));
        items.add(item(502L, TODAY.plusDays(1), "PENDING", 0));
        items.add(item(503L, LocalDate.of(2026, 10, 1), "DONE", 0));   // DONE 永不移动
        when(itemRepo.findByBookIdOrderByPlanDateAscSortOrderAsc(BOOK_ID)).thenReturn(items);

        List<TaskBookService.MovedItem> moved =
                taskBookService.rescheduleBook(book, TODAY, null);

        assertTrue(moved.isEmpty(), "没有逾期条目时应返回空 moved");
        assertEquals(TODAY, items.get(0).getPlanDate());
        assertEquals(TODAY.plusDays(1), items.get(1).getPlanDate());
        assertEquals(LocalDate.of(2026, 10, 1), items.get(2).getPlanDate(), "DONE 永不移动");
        assertEquals(Integer.valueOf(0), items.get(2).getDeferCount());
    }

    // ── 情况 6：显式 defer 当天条目 → 移到次日 ──
    @Test
    void testReschedule_ExplicitDefer_TodaysItemMovesToTomorrow() {
        TaskBook book = book(LocalDate.of(2026, 11, 17));
        List<TaskBookItem> items = new ArrayList<>();
        TaskBookItem todayItem = item(601L, TODAY, "PENDING", 0);
        items.add(todayItem);
        when(itemRepo.findByBookIdOrderByPlanDateAscSortOrderAsc(BOOK_ID)).thenReturn(items);

        List<TaskBookService.MovedItem> moved =
                taskBookService.rescheduleBook(book, TODAY, 601L);

        assertEquals(1, moved.size());
        assertEquals(TODAY.plusDays(1), todayItem.getPlanDate());
        assertEquals(Integer.valueOf(1), todayItem.getDeferCount());
    }

    // ── 工具方法 ──

    private TaskBook book(LocalDate endDate) {
        return TaskBook.builder()
                .id(BOOK_ID).userId(USER_ID)
                .title("测试任务书")
                .startDate(TODAY).endDate(endDate)
                .status("CONFIRMED")
                .build();
    }

    private TaskBookItem item(long id, LocalDate planDate, String status, int sortOrder) {
        return TaskBookItem.builder()
                .id(id)
                .bookId(BOOK_ID)
                .planDate(planDate)
                .originDate(planDate)
                .title("条目" + id)
                .learnContent("练习题 20 道")
                .estMinutes(45)
                .doneCriteria("正确率≥70%")
                .category("STUDY")
                .status(status)
                .deferCount(0)
                .sortOrder(sortOrder)
                .build();
    }
}
