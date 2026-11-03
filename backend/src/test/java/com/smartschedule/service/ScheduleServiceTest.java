package com.smartschedule.service;

import com.smartschedule.entity.*;
import com.smartschedule.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ScheduleService 单元测试 - 测试核心排程算法。
 * 覆盖基本路径法和逻辑覆盖法场景。
 */
@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock private DailyPlanRepository planRepo;
    @Mock private DailyTaskRepository taskRepo;
    @Mock private GoalRepository goalRepo;
    @Mock private CourseRepository courseRepo;
    @Mock private UserRepository userRepo;
    @Mock private PointsService pointsService;
    @Mock private AiService aiService;

    @InjectMocks
    private ScheduleService scheduleService;

    private User testUser;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        testUser = User.builder()
                .id(1L).username("test").password("pass")
                .wakeUpTime(LocalTime.of(8, 0))
                .sleepTime(LocalTime.of(23, 0))
                .energyPeak("EVENING")
                .totalPoints(0).level(1)
                .build();
    }

    // ========== 基本路径测试 ==========

    /**
     * 路径1：用户存在 + 无已有计划 + 无课程 + 无目标 → 生成空计划
     * 覆盖: buildTimeSlots → 无课程标记 → 无目标分配 → AI 兜底建议
     */
    @Test
    void testGeneratePlan_NoGoalsNoCourses_ReturnsEmptyPlan() {
        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(planRepo.findByUserIdAndPlanDate(1L, today)).thenReturn(Optional.empty());
        when(courseRepo.findByUserIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());
        when(goalRepo.findByUserIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());
        when(planRepo.save(any(DailyPlan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aiService.generate(anyString(), anyString())).thenReturn("今天放松一下");

        DailyPlan result = scheduleService.generateDailyPlan(1L, today);

        assertNotNull(result);
        assertEquals("CONFIRMED", result.getStatus());
        assertEquals("今天放松一下", result.getAiSuggestion());
        assertEquals(Integer.valueOf(0), result.getEstimatedPoints());
        verify(taskRepo, never()).save(any());
        verify(planRepo, atLeastOnce()).save(any(DailyPlan.class));
    }

    /**
     * 路径2：用户存在 + 无已有计划 + 有一个目标 + 无课程冲突 → 成功分配
     * 覆盖: 贪心分配成功路径
     */
    @Test
    void testGeneratePlan_SingleGoal_AllocatesTask() {
        Goal goal = Goal.builder()
                .id(1L).user(testUser).name("学英语")
                .category("STUDY").energyLevel("MEDIUM")
                .priority(8).defaultDuration(60).isActive(true)
                .build();

        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(planRepo.findByUserIdAndPlanDate(1L, today)).thenReturn(Optional.empty());
        when(courseRepo.findByUserIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());
        when(goalRepo.findByUserIdAndIsActiveTrue(1L)).thenReturn(Collections.singletonList(goal));
        when(planRepo.save(any(DailyPlan.class))).thenAnswer(inv -> inv.getArgument(0));
        // 贪心按 30 分钟 slot 切分，实际传的是 allocMinutes=30，不能用固定 60 严格匹配
        when(pointsService.calculateTaskPoints(eq("STUDY"), anyInt())).thenReturn(9);
        when(taskRepo.save(any(DailyTask.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aiService.generate(anyString(), anyString())).thenReturn("加油");

        DailyPlan result = scheduleService.generateDailyPlan(1L, today);

        assertNotNull(result);
        // 默认时长 60 分钟的目标会被贪心切成 2 个 30 分钟 slot，每 slot 计 9 分 → 合计 18
        assertEquals(Integer.valueOf(18), result.getEstimatedPoints());
        verify(taskRepo, atLeastOnce()).save(any(DailyTask.class));
    }

    /**
     * 路径3：用户存在 + 已有今日计划 → 删除旧计划并重建
     * 覆盖: 已有计划的分支
     */
    @Test
    void testGeneratePlan_ExistingPlan_DeletesAndRebuilds() {
        DailyPlan oldPlan = DailyPlan.builder().id(10L).user(testUser).planDate(today).build();
        DailyTask oldTask = DailyTask.builder().id(100L).dailyPlan(oldPlan).name("旧任务").build();

        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(planRepo.findByUserIdAndPlanDate(1L, today)).thenReturn(Optional.of(oldPlan));
        when(taskRepo.findByDailyPlanIdOrderBySortOrderAsc(10L))
                .thenReturn(Collections.singletonList(oldTask));
        when(courseRepo.findByUserIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());
        when(goalRepo.findByUserIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());
        when(planRepo.save(any(DailyPlan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aiService.generate(anyString(), anyString())).thenReturn("新的开始");

        DailyPlan result = scheduleService.generateDailyPlan(1L, today);

        assertNotNull(result);
        verify(taskRepo).deleteAll(anyList());
        verify(planRepo).delete(oldPlan);
    }

    // ========== 边界值测试 ==========

    /**
     * 边界条件：目标时长为 0 → 不分配任务
     */
    @Test
    void testGeneratePlan_ZeroDurationGoal_NoTaskCreated() {
        Goal zeroGoal = Goal.builder()
                .id(2L).user(testUser).name("休息")
                .category("OTHER").energyLevel("LOW")
                .priority(1).defaultDuration(0).isActive(true)
                .build();

        when(userRepo.findById(1L)).thenReturn(Optional.of(testUser));
        when(planRepo.findByUserIdAndPlanDate(1L, today)).thenReturn(Optional.empty());
        when(courseRepo.findByUserIdAndIsActiveTrue(1L)).thenReturn(Collections.emptyList());
        when(goalRepo.findByUserIdAndIsActiveTrue(1L)).thenReturn(Collections.singletonList(zeroGoal));
        when(planRepo.save(any(DailyPlan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(aiService.generate(anyString(), anyString())).thenReturn("OK");

        DailyPlan result = scheduleService.generateDailyPlan(1L, today);

        assertEquals(Integer.valueOf(0), result.getEstimatedPoints());
        verify(taskRepo, never()).save(any());
    }

    /**
     * 边界条件：用户不存在 → 抛出异常
     */
    @Test
    void testGeneratePlan_UserNotFound_ThrowsException() {
        when(userRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                scheduleService.generateDailyPlan(999L, today));
    }

    // ========== 逻辑覆盖测试（条件覆盖） ==========

    /**
     * 条件覆盖：COMPLETED状态 + 关联Goal → 发放积分
     */
    @Test
    void testUpdateTaskStatus_CompletedWithGoal_AwardsPoints() {
        Goal goal = Goal.builder().id(1L).name("跑步").category("EXERCISE").build();
        DailyPlan plan = DailyPlan.builder().id(1L).user(testUser).build();
        DailyTask task = DailyTask.builder()
                .id(1L).dailyPlan(plan).goal(goal)
                .name("跑步").durationMinutes(30)
                .status("PENDING").build();

        when(taskRepo.findById(1L)).thenReturn(Optional.of(task));
        when(pointsService.calculateTaskPoints("EXERCISE", 30)).thenReturn(6);
        when(taskRepo.save(any(DailyTask.class))).thenReturn(task);

        DailyTask result = scheduleService.updateTaskStatus(1L, "COMPLETED");

        assertEquals("COMPLETED", result.getStatus());
        verify(pointsService).awardPoints(eq(testUser.getId()), eq(1L), eq(6), anyString());
    }

    /**
     * 条件覆盖：SKIPPED状态 + 无Goal → 不发放积分
     */
    @Test
    void testUpdateTaskStatus_Skipped_NoPoints() {
        DailyPlan plan = DailyPlan.builder().id(1L).user(testUser).build();
        DailyTask task = DailyTask.builder()
                .id(2L).dailyPlan(plan).goal(null)
                .name("休息").status("PENDING").build();

        when(taskRepo.findById(2L)).thenReturn(Optional.of(task));
        when(taskRepo.save(any(DailyTask.class))).thenReturn(task);

        DailyTask result = scheduleService.updateTaskStatus(2L, "SKIPPED");

        assertEquals("SKIPPED", result.getStatus());
        verify(pointsService, never()).awardPoints(anyLong(), anyLong(), anyInt(), anyString());
    }

    /**
     * 获取今日计划 - 无计划时返回 null
     */
    @Test
    void testGetTodayPlan_NoPlan_ReturnsNull() {
        when(planRepo.findByUserIdAndPlanDate(1L, today)).thenReturn(Optional.empty());

        DailyPlan result = scheduleService.getTodayPlan(1L);

        assertNull(result);
    }
}
