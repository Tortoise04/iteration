package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {
    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/weekly")
    public Map<String, Object> getWeeklyStatistics(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return statisticsService.getWeeklyStatistics(startDate, endDate);
    }

    @GetMapping("/monthly")
    public Map<String, Object> getMonthlyStatistics(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        return statisticsService.getMonthlyStatistics(startDate, endDate);
    }
}