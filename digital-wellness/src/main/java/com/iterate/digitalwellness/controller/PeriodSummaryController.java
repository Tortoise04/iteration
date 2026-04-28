package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.entity.PeriodSummary;
import com.iterate.digitalwellness.entity.User;
import com.iterate.digitalwellness.repository.UserRepository;
import com.iterate.digitalwellness.service.PeriodSummaryService;
import com.iterate.digitalwellness.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/period-summaries")
public class PeriodSummaryController {

    private static final Logger logger = LoggerFactory.getLogger(PeriodSummaryController.class);

    @Autowired
    private PeriodSummaryService periodSummaryService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    /**
     * 从 Authorization header 提取用户 ID
     */
    private Long extractUserId(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String username = jwtUtil.getUsernameFromToken(token);
                User user = userRepository.findByUsername(username);
                if (user != null) {
                    return user.getId();
                }
            } catch (Exception e) {
                logger.warn("Token 解析失败: {}", e.getMessage());
            }
        }
        // 开发阶段：如果没有token，返回第一个用户的ID，方便测试
        List<User> users = userRepository.findAll();
        if (!users.isEmpty()) {
            logger.warn("使用默认用户ID: {}", users.get(0).getId());
            return users.get(0).getId();
        }
        return null;
    }

    @PostMapping
    public ResponseEntity<?> save(@RequestBody PeriodSummary periodSummary) {
        try {
            logger.info("创建周期总结: {}", periodSummary.getPeriod());
            PeriodSummary saved = periodSummaryService.save(periodSummary);
            logger.info("总结创建成功, ID: {}", saved.getId());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("创建周期总结失败: ", e);
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
            logger.info("查询所有周期总结");
            List<PeriodSummary> summaries = periodSummaryService.findAll();
            logger.info("查询到 {} 条总结", summaries.size());
            return ResponseEntity.ok(summaries);
        } catch (Exception e) {
            logger.error("查询周期总结失败: ", e);
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
            PeriodSummary summary = periodSummaryService.findById(id);
            if (summary == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "总结不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response);
            }
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("查询周期总结失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PeriodSummary update) {
        try {
            logger.info("更新周期总结 ID: {}", id);
            PeriodSummary existing = periodSummaryService.findById(id);
            if (existing == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "总结不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response);
            }
            if (update.getPeriod() != null) existing.setPeriod(update.getPeriod());
            if (update.getPeriodType() != null) existing.setPeriodType(update.getPeriodType());
            if (update.getStartDate() != null) existing.setStartDate(update.getStartDate());
            if (update.getEndDate() != null) existing.setEndDate(update.getEndDate());
            if (update.getSummary() != null) existing.setSummary(update.getSummary());
            if (update.getHighlights() != null) existing.setHighlights(update.getHighlights());
            if (update.getImprovements() != null) existing.setImprovements(update.getImprovements());
            if (update.getNextPlan() != null) existing.setNextPlan(update.getNextPlan());
            PeriodSummary saved = periodSummaryService.save(existing);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("更新周期总结失败: ", e);
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
            logger.info("删除周期总结 ID: {}", id);
            periodSummaryService.deleteById(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除周期总结失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "删除失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generateSummary(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam String periodType,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = extractUserId(authHeader);
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "未授权");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            logger.info("AI 生成周期总结: {} 至 {}, 类型: {}, userId: {}", startDate, endDate, periodType, userId);
            PeriodSummary summary = periodSummaryService.generateSummary(userId, startDate, endDate, periodType);
            logger.info("AI 生成总结成功");
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            logger.error("AI 生成周期总结失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "AI 生成失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }
}
