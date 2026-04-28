package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.entity.User;
import com.iterate.digitalwellness.repository.UserRepository;
import com.iterate.digitalwellness.service.DailySubtaskService;
import com.iterate.digitalwellness.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/daily-subtasks")
public class DailySubtaskController {

    private static final Logger logger = LoggerFactory.getLogger(DailySubtaskController.class);

    @Autowired
    private DailySubtaskService dailySubtaskService;

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
        return null;
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayTasks(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            logger.info("查询今日待办任务");
            Long userId = extractUserId(authHeader);
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "未授权");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            List<Map<String, Object>> tasks = dailySubtaskService.findTodayTasksByUserId(userId);
            logger.info("查询到 {} 个今日待办任务", tasks.size());
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            logger.error("查询今日待办任务失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> completeTask(@PathVariable Long id, 
                                         @RequestParam(required = false) String completionNote, 
                                         @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            logger.info("完成任务 ID: {}", id);
            Long userId = extractUserId(authHeader);
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "未授权");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Object task = dailySubtaskService.completeTask(id, completionNote);
            if (task == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "任务不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            logger.info("任务完成成功");
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            logger.error("完成任务失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "完成失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
