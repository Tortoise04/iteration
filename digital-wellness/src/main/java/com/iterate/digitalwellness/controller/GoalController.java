package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.entity.Goal;
import com.iterate.digitalwellness.entity.User;
import com.iterate.digitalwellness.repository.UserRepository;
import com.iterate.digitalwellness.service.GoalService;
import com.iterate.digitalwellness.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private static final Logger logger = LoggerFactory.getLogger(GoalController.class);

    @Autowired
    private GoalService goalService;

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

    @PostMapping
    public ResponseEntity<?> save(@RequestBody Goal goal,
                                   @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            logger.info("创建目标: {}", goal.getGoal());
            Long userId = extractUserId(authHeader);
            if (userId != null) {
                goal.setUserId(userId);
                logger.info("关联用户ID: {}", userId);
            }
            Goal saved = goalService.save(goal);
            logger.info("目标创建成功, ID: {}", saved.getId());
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            logger.error("创建目标失败: ", e);
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
            logger.info("查询所有目标");
            List<Goal> goals = goalService.findAll();
            logger.info("查询到 {} 条目标", goals.size());
            return ResponseEntity.ok(goals);
        } catch (Exception e) {
            logger.error("查询目标失败: ", e);
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
            logger.info("查询目标 ID: {}", id);
            Goal goal = goalService.findById(id);
            if (goal == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "目标不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response);
            }
            return ResponseEntity.ok(goal);
        } catch (Exception e) {
            logger.error("查询目标失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "查询失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Goal goalUpdate,
                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            logger.info("更新目标 ID: {}", id);

            // 1. 查询 Goal 是否存在
            Goal existingGoal = goalService.findById(id);
            if (existingGoal == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "目标不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 2. 提取当前用户 ID
            Long currentUserId = extractUserId(authHeader);
            if (currentUserId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "未授权");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // 3. 安全校验：检查是否是本人的数据
            Long goalUserId = existingGoal.getUserId();
            if (goalUserId == null || !goalUserId.equals(currentUserId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "无权修改此目标");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // 4. 更新字段
            if (goalUpdate.getGoal() != null) existingGoal.setGoal(goalUpdate.getGoal());
            if (goalUpdate.getDescription() != null) existingGoal.setDescription(goalUpdate.getDescription());
            if (goalUpdate.getStartTime() != null) existingGoal.setStartTime(goalUpdate.getStartTime());
            if (goalUpdate.getEndTime() != null) existingGoal.setEndTime(goalUpdate.getEndTime());
            if (goalUpdate.getStatus() != null) existingGoal.setStatus(goalUpdate.getStatus());

            // 5. 保存
            Goal savedGoal = goalService.save(existingGoal);
            logger.info("目标更新成功");
            return ResponseEntity.ok(savedGoal);
        } catch (Exception e) {
            logger.error("更新目标失败: ", e);
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
            logger.info("删除目标 ID: {}", id);
            goalService.deleteById(id);
            logger.info("目标删除成功");
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除目标失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "删除失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }
}
