package com.smartschedule.service;

import com.smartschedule.entity.PointsRecord;
import com.smartschedule.entity.User;
import com.smartschedule.repository.PointsRecordRepository;
import com.smartschedule.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PointsService 单元测试。
 * 覆盖等价类划分和边界值分析。
 */
@ExtendWith(MockitoExtension.class)
class PointsServiceTest {

    @Mock private PointsRecordRepository pointsRepo;
    @Mock private UserRepository userRepo;

    @InjectMocks
    private PointsService pointsService;

    // ========== 等价类划分测试 ==========

    /**
     * 等价类划分：测试各分类的积分计算
     * STUDY: 0.15/min, WORK: 0.12/min, EXERCISE: 0.20/min,
     * HOBBY: 0.10/min, OTHER/null: 0.08/min
     */
    @ParameterizedTest
    @CsvSource({
            "STUDY, 60, 9",      // 60 * 0.15 = 9
            "WORK, 60, 8",       // 60 * 0.12 = 7.2 → ceil = 8
            "EXERCISE, 60, 12",  // 60 * 0.20 = 12
            "HOBBY, 60, 6",      // 60 * 0.10 = 6
            "OTHER, 60, 5",      // 60 * 0.08 = 4.8 → ceil = 5
            "null, 60, 5",       // null → OTHER → 5
    })
    void testCalculateTaskPoints_AllCategories(String category, int duration, int expected) {
        String cat = "null".equals(category) ? null : category;
        int points = pointsService.calculateTaskPoints(cat, duration);
        assertEquals(expected, points);
    }

    // ========== 边界值测试 ==========

    @Test
    void testCalculateTaskPoints_ZeroDuration_ReturnsOne() {
        // ceil(0.15 * 0) = ceil(0) = 0
        int points = pointsService.calculateTaskPoints("STUDY", 0);
        assertEquals(0, points);
    }

    @Test
    void testCalculateTaskPoints_LargeDuration() {
        // ceil(0.15 * 1000) = ceil(150) = 150
        int points = pointsService.calculateTaskPoints("STUDY", 1000);
        assertEquals(150, points);
    }

    @Test
    void testCalculateTaskPoints_OneMinute() {
        // ceil(0.15 * 1) = ceil(0.15) = 1
        int points = pointsService.calculateTaskPoints("STUDY", 1);
        assertEquals(1, points);
    }

    // ========== 积分发放测试 ==========

    @Test
    void testAwardPoints_IncreasesPointsAndLevel() {
        User user = User.builder().id(1L).username("test")
                .totalPoints(150).level(1).build();

        when(userRepo.findById(1L)).thenReturn(Optional.of(user));

        pointsService.awardPoints(1L, 10L, 60, "完成任务");

        // 150 + 60 = 210, level = 210/200 + 1 = 2
        assertEquals(Integer.valueOf(210), user.getTotalPoints());
        assertEquals(Integer.valueOf(2), user.getLevel());
        verify(userRepo).save(user);
        verify(pointsRepo).save(any(PointsRecord.class));
    }

    @Test
    void testAwardPoints_Level10_CapAt10() {
        User user = User.builder().id(2L).username("pro")
                .totalPoints(2000).level(10).build();

        when(userRepo.findById(2L)).thenReturn(Optional.of(user));

        pointsService.awardPoints(2L, 11L, 500, "大师任务");

        assertEquals(Integer.valueOf(2500), user.getTotalPoints());
        assertEquals(Integer.valueOf(10), user.getLevel()); // 不会超过10
    }
}
