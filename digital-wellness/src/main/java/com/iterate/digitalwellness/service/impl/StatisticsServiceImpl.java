package com.iterate.digitalwellness.service.impl;

import com.iterate.digitalwellness.entity.PhoneUsage;
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