package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.entity.DailyActivity;
import com.iterate.digitalwellness.service.DailyActivityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/daily-activities")
public class DailyActivityController {

    private static final Logger logger = LoggerFactory.getLogger(DailyActivityController.class);

    @Autowired
    private DailyActivityService dailyActivityService;

    @PostMapping
    public ResponseEntity<?> save(@RequestBody DailyActivity dailyActivity) {
        try {
            logger.info("创建每日活动: {}", dailyActivity.getActivity());
            DailyActivity saved = dailyActivityService.save(dailyActivity);
            logger.info("活动创建成功, ID: {}", saved.getId());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("创建活动失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "创建失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @GetMapping
    public ResponseEntity<?> findAll() {
        try {
            logger.info("查询所有活动");
            List<DailyActivity> activities = dailyActivityService.findAll();
            logger.info("查询到 {} 条活动", activities.size());
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            logger.error("查询活动失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable Long id) {
        try {
            DailyActivity activity = dailyActivityService.findById(id);
            if (activity == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "活动不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response);
            }
            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            logger.error("查询活动失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody DailyActivity update) {
        try {
            logger.info("更新活动 ID: {}", id);
            DailyActivity existing = dailyActivityService.findById(id);
            if (existing == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "活动不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response);
            }
            if (update.getDate() != null) existing.setDate(update.getDate());
            if (update.getActivity() != null) existing.setActivity(update.getActivity());
            if (update.getDuration() != null) existing.setDuration(update.getDuration());
            if (update.getLocation() != null) existing.setLocation(update.getLocation());
            DailyActivity saved = dailyActivityService.save(existing);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("更新活动失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "更新失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteById(@PathVariable Long id) {
        try {
            logger.info("删除活动 ID: {}", id);
            dailyActivityService.deleteById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除活动失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "删除失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @GetMapping("/date-range")
    public ResponseEntity<?> findByDateBetween(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        try {
            List<DailyActivity> activities = dailyActivityService.findByDateBetween(startDate, endDate);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            logger.error("按日期查询活动失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }
}
