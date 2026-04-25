package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.entity.PhoneUsage;
import com.iterate.digitalwellness.service.PhoneUsageService;
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
@RequestMapping("/api/phone-usage")
public class PhoneUsageController {

    private static final Logger logger = LoggerFactory.getLogger(PhoneUsageController.class);

    @Autowired
    private PhoneUsageService phoneUsageService;

    @PostMapping
    public ResponseEntity<?> save(@RequestBody PhoneUsage phoneUsage) {
        try {
            logger.info("创建手机使用记录: {}", phoneUsage.getDate());
            PhoneUsage saved = phoneUsageService.save(phoneUsage);
            logger.info("记录创建成功, ID: {}", saved.getId());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("创建手机使用记录失败: ", e);
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
            logger.info("查询所有手机使用记录");
            List<PhoneUsage> records = phoneUsageService.findAll();
            logger.info("查询到 {} 条记录", records.size());
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            logger.error("查询手机使用记录失败: ", e);
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
            PhoneUsage record = phoneUsageService.findById(id);
            if (record == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "记录不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response);
            }
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            logger.error("查询手机使用记录失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PhoneUsage update) {
        try {
            logger.info("更新手机使用记录 ID: {}", id);
            PhoneUsage existing = phoneUsageService.findById(id);
            if (existing == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "记录不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response);
            }
            if (update.getDate() != null) existing.setDate(update.getDate());
            if (update.getUsageTime() != null) existing.setUsageTime(update.getUsageTime());
            PhoneUsage saved = phoneUsageService.save(existing);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("更新手机使用记录失败: ", e);
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
            logger.info("删除手机使用记录 ID: {}", id);
            phoneUsageService.deleteById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除手机使用记录失败: ", e);
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
            List<PhoneUsage> records = phoneUsageService.findByDateBetween(startDate, endDate);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            logger.error("按日期查询手机使用记录失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }
}
