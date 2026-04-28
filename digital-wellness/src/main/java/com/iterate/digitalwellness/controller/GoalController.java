package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.ai.AIService;
import com.iterate.digitalwellness.entity.DailySubtask;
import com.iterate.digitalwellness.entity.Goal;
import com.iterate.digitalwellness.entity.User;
import com.iterate.digitalwellness.repository.UserRepository;
import com.iterate.digitalwellness.service.DailySubtaskService;
import com.iterate.digitalwellness.service.GoalService;
import com.iterate.digitalwellness.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
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
    private DailySubtaskService dailySubtaskService;

    @Autowired
    private AIService aiService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

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

    @PostMapping("/{goalId}/ai-breakdown")
    public ResponseEntity<?> aiBreakdown(@PathVariable Long goalId, 
                                       @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            logger.info("AI 目标拆解，目标 ID: {}", goalId);

            // 1. 查询目标信息
            Goal goal = goalService.findById(goalId);
            if (goal == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "目标不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            // 2. 提取用户 ID
            Long userId = extractUserId(authHeader);
            if (userId == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "未授权");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // 安全校验：检查是否是本人的数据
            Long goalUserId = goal.getUserId();
            if (goalUserId == null || !goalUserId.equals(userId)) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "无权操作此目标");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            // 3. 计算时间跨度
            long days = java.time.Duration.between(goal.getStartTime(), goal.getEndTime()).toDays() + 1;
            logger.info("目标时间跨度: {} 天", days);

            // 4. 查询用户的其他目标和已有任务
            List<Goal> otherGoals = goalService.findByUserId(userId);
            List<DailySubtask> existingTasks = dailySubtaskService.findByUserIdAndDateRange(userId, goal.getStartTime().toLocalDate(), goal.getEndTime().toLocalDate());

            // 5. 构造其他目标和已有任务的描述
            StringBuilder otherGoalsDesc = new StringBuilder();
            if (!otherGoals.isEmpty()) {
                otherGoalsDesc.append("用户的其他目标：\n");
                for (Goal g : otherGoals) {
                    if (!g.getId().equals(goalId)) {
                        otherGoalsDesc.append(String.format("- %s (%.1f 天)\n", g.getGoal(), 
                            java.time.Duration.between(g.getStartTime(), g.getEndTime()).toDays() + 1));
                    }
                }
            }

            StringBuilder existingTasksDesc = new StringBuilder();
            if (!existingTasks.isEmpty()) {
                existingTasksDesc.append("用户已有的任务安排：\n");
                for (DailySubtask task : existingTasks) {
                    existingTasksDesc.append(String.format("- %s (日期：%s)\n", 
                        task.getTaskContent(), task.getTargetDate()));
                }
            }

            // 6. 构造 Prompt
            String systemPrompt = "你是一个目标规划专家。请根据以下目标信息、用户的其他目标和已有任务安排，生成合理的每日执行计划。要合理安排任务密度，避免任务冲突。";
            String userPrompt = String.format(
                "目标：%s\n" +
                "目标描述：%s\n" +
                "开始日期：%s\n" +
                "结束日期：%s\n" +
                "总天数：%d天\n" +
                "\n%s" +
                "\n%s" +
                "\n要求：\n" +
                "1. 将目标拆解为 %d 个每日子任务，每天一个任务\n" +
                "2. 每个任务要具体、可执行、有明确的学习/行动内容\n" +
                "3. 任务难度要循序渐进，符合学习曲线\n" +
                "4. 如果目标是学习类，要包含复习和练习安排\n" +
                "5. 要考虑用户的其他目标和已有任务安排，合理分配任务密度\n" +
                "6. 不要与已有的任务安排产生时间冲突\n" +
                "7. 返回严格的 JSON 数组格式，不要有任何 markdown 代码块标记，不要添加任何解释文字\n" +
                "\n返回格式示例：\n" +
                "[\n" +
                "  {\n" +
                "    \"taskContent\": \"学习 Spring Boot 自动配置原理，阅读官方文档第1-2章\",\n" +
                "    \"targetDate\": \"2024-01-01\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"taskContent\": \"动手搭建第一个 Spring Boot 项目，完成 Hello World\",\n" +
                "    \"targetDate\": \"2024-01-02\"\n" +
                "  }\n" +
                "]",
                goal.getGoal(),
                goal.getDescription() != null ? goal.getDescription() : "",
                goal.getStartTime().toLocalDate(),
                goal.getEndTime().toLocalDate(),
                days,
                otherGoalsDesc.toString(),
                existingTasksDesc.toString(),
                days
            );

            // 7. 调用 AI
            logger.info("调用 AI 生成子任务");
            String aiResponse = aiService.generate(systemPrompt, userPrompt);
            logger.info("AI 返回结果: {}", aiResponse);

            // 8. 检查 AI 响应是否为错误信息
            if (aiResponse.startsWith("AI 服务未配置") || aiResponse.startsWith("AI 调用失败") || aiResponse.startsWith("AI 调用异常") || aiResponse.startsWith("AI 服务错误")) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "AI 调用失败");
                response.put("message", aiResponse);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }

            // 9. 解析 JSON
            List<Map<String, Object>> tasks = objectMapper.readValue(aiResponse, List.class);
            List<DailySubtask> subtasks = new ArrayList<>();

            for (Map<String, Object> task : tasks) {
                DailySubtask subtask = new DailySubtask();
                subtask.setGoal(goal);
                subtask.setTaskContent((String) task.get("taskContent"));
                subtask.setTargetDate(java.time.LocalDate.parse((String) task.get("targetDate")));
                subtask.setIsCompleted(false);
                subtasks.add(subtask);
            }

            // 9. 批量插入
            dailySubtaskService.batchSave(subtasks);
            logger.info("生成 {} 个每日子任务", subtasks.size());

            return ResponseEntity.ok(subtasks);
        } catch (Exception e) {
            logger.error("AI 目标拆解失败: ", e);
            Map<String, Object> response = new HashMap<>();
            response.put("error", "拆解失败");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
