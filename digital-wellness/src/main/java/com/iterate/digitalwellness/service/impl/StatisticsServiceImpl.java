package com.iterate.digitalwellness.service.impl;

import com.iterate.digitalwellness.entity.PhoneUsage;
import com.iterate.digitalwellness.repository.GoalRepository;
import com.iterate.digitalwellness.repository.PhoneUsageRepository;
import com.iterate.digitalwellness.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatisticsServiceImpl implements StatisticsService {
    @Autowired
    private PhoneUsageRepository phoneUsageRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Override
    public Map<String, Object> getWeeklyStatistics(LocalDate startDate, LocalDate endDate) {
        List<PhoneUsage> phoneUsages = phoneUsageRepository.findByDateBetween(startDate, endDate);
        return calculateStatistics(phoneUsages, "周");
    }

    @Override
    public Map<String, Object> getMonthlyStatistics(LocalDate startDate, LocalDate endDate) {
        List<PhoneUsage> phoneUsages = phoneUsageRepository.findByDateBetween(startDate, endDate);
        return calculateStatistics(phoneUsages, "月");
    }

    @Override
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> dashboard = new HashMap<>();

        // 今日手机使用
        LocalDate today = LocalDate.now();
        List<PhoneUsage> todayUsage = phoneUsageRepository.findByDateBetween(today, today);
        long todayTotal = todayUsage.stream().mapToLong(PhoneUsage::getUsageTime).sum();
        dashboard.put("todayPhoneUsage", todayTotal);

        // 本周手机使用
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        List<PhoneUsage> weekUsage = phoneUsageRepository.findByDateBetween(weekStart, today);
        long weekTotal = weekUsage.stream().mapToLong(PhoneUsage::getUsageTime).sum();
        dashboard.put("weekPhoneUsage", weekTotal);

        // 进行中目标数
        long inProgressGoals = goalRepository.findAll().stream()
                .filter(g -> "IN_PROGRESS".equals(g.getStatus()))
                .count();
        dashboard.put("inProgressGoals", inProgressGoals);

        // 已完成目标数
        long completedGoals = goalRepository.findAll().stream()
                .filter(g -> "COMPLETED".equals(g.getStatus()))
                .count();
        dashboard.put("completedGoals", completedGoals);

        return dashboard;
    }

    private Map<String, Object> calculateStatistics(List<PhoneUsage> phoneUsages, String period) {
        Map<String, Object> statistics = new HashMap<>();
        if (phoneUsages.isEmpty()) {
            statistics.put(period + "总使用时间", 0);
            statistics.put(period + "平均使用时间", 0);
            statistics.put(period + "最高使用时间", 0);
            statistics.put(period + "最低使用时间", 0);
            return statistics;
        }

        long totalUsageTime = 0;
        long maxUsageTime = 0;
        long minUsageTime = Long.MAX_VALUE;

        for (PhoneUsage phoneUsage : phoneUsages) {
            long usageTime = phoneUsage.getUsageTime();
            totalUsageTime += usageTime;
            if (usageTime > maxUsageTime) {
                maxUsageTime = usageTime;
            }
            if (usageTime < minUsageTime) {
                minUsageTime = usageTime;
            }
        }

        long averageUsageTime = totalUsageTime / phoneUsages.size();

        statistics.put(period + "总使用时间", totalUsageTime);
        statistics.put(period + "平均使用时间", averageUsageTime);
        statistics.put(period + "最高使用时间", maxUsageTime);
        statistics.put(period + "最低使用时间", minUsageTime);
        statistics.put("数据天数", phoneUsages.size());

        return statistics;
    }
}