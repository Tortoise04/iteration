package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.service.StatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsController.class);

    @Autowired
    private StatisticsService statisticsService;

    @GetMapping("/weekly")
    public ResponseEntity<?> getWeeklyStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            logger.info("获取周统计: {} 至 {}", startDate, endDate);
            Map<String, Object> stats = statisticsService.getWeeklyStatistics(startDate, endDate);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取周统计失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "获取统计失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlyStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        try {
            logger.info("获取月统计: {} 至 {}", startDate, endDate);
            Map<String, Object> stats = statisticsService.getMonthlyStatistics(startDate, endDate);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取月统计失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "获取统计失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStatistics() {
        try {
            logger.info("获取仪表盘统计");
            Map<String, Object> stats = statisticsService.getDashboardStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取仪表盘统计失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "获取统计失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }
}
