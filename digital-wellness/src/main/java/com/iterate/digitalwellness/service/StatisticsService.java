package com.iterate.digitalwellness.service;

import java.time.LocalDate;
import java.util.Map;

public interface StatisticsService {
    Map<String, Object> getWeeklyStatistics(LocalDate startDate, LocalDate endDate);
    Map<String, Object> getMonthlyStatistics(LocalDate startDate, LocalDate endDate);
    Map<String, Object> getDashboardStatistics();
}