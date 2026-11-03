package com.smartschedule.service;

import com.smartschedule.dto.AiScheduleResponse;
import com.smartschedule.dto.ChatResponse;
import com.smartschedule.entity.*;
import com.smartschedule.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * AI 服务 -- 封装 DeepSeek（文本）和智谱（图片识别）API 调用。
 */
@Slf4j
@Service
public class AiService {

    private final RestTemplate deepseekRestTemplate;
    private final RestTemplate zhipuRestTemplate;
    private final AiConversationRepository conversationRepo;
    private final CourseRepository courseRepo;
    private final GoalRepository goalRepo;
    private final DailyPlanRepository planRepo;
    private final DailyTaskRepository taskRepo;
    private final ObjectMapper objectMapper;

    // ── DeepSeek 配置 ──
    @Value("${app.ai.deepseek.base-url}")
    private String dsBaseUrl;

    @Value("${app.ai.deepseek.model:deepseek-chat}")
    private String dsModel;

    @Value("${app.ai.deepseek.max-tokens:4096}")
    private int dsMaxTokens;

    @Value("${app.ai.deepseek.temperature:0.7}")
    private double dsTemperature;

    // ── 任务书生成专用配置（契约 §2：默认 deepseek-chat，避免推理模型的 reasoning 吃掉 token）──
    @Value("${app.ai.deepseek.taskbook-model:deepseek-chat}")
    private String dsTaskbookModel;

    @Value("${app.ai.deepseek.taskbook-max-tokens:8192}")
    private int dsTaskbookMaxTokens;

    @Value("${app.ai.deepseek.taskbook-temperature:0.4}")
    private double dsTaskbookTemperature;

    // ── 智谱 配置 ──
    @Value("${app.ai.zhipu.base-url}")
    private String zhipuBaseUrl;

    @Value("${app.ai.zhipu.model:glm-4v}")
    private String zhipuModel;

    @Value("${app.ai.zhipu.max-tokens:4096}")
    private int zhipuMaxTokens;

    @Value("${app.ai.zhipu.temperature:0.3}")
    private double zhipuTemperature;

    public AiService(@Qualifier("deepseekRestTemplate") RestTemplate deepseekRestTemplate,
                     @Qualifier("zhipuRestTemplate") RestTemplate zhipuRestTemplate,
                     AiConversationRepository conversationRepo,
                     CourseRepository courseRepo,
                     GoalRepository goalRepo,
                     DailyPlanRepository planRepo,
                     DailyTaskRepository taskRepo,
                     ObjectMapper objectMapper) {
        this.deepseekRestTemplate = deepseekRestTemplate;
        this.zhipuRestTemplate = zhipuRestTemplate;
        this.conversationRepo = conversationRepo;
        this.courseRepo = courseRepo;
        this.goalRepo = goalRepo;
        this.planRepo = planRepo;
        this.taskRepo = taskRepo;
        this.objectMapper = objectMapper;
    }

    /**
     * 通用 AI 调用（纯文本 → DeepSeek）
     */
    public String generate(String systemPrompt, String userMessage) {
        return callDeepseek(systemPrompt, userMessage);
    }

    /**
     * 带图片的 AI 调用（OCR / 视觉识别 → 智谱 GLM-4V）
     */
    public String generateWithImage(String systemPrompt, String textPrompt,
                                     String base64Image, String mimeType) {
        return callZhipu(systemPrompt, textPrompt, base64Image, mimeType);
    }

    // ═══════════════════════════════════════════
    //  DeepSeek（纯文本）
    // ═══════════════════════════════════════════
    private String callDeepseek(String systemPrompt, String userText) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", dsModel);
        body.put("max_tokens", dsMaxTokens);
        body.put("temperature", dsTemperature);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(msg("system", systemPrompt));
        messages.add(msg("user", userText));
        body.put("messages", messages);

        return doPost(deepseekRestTemplate, dsBaseUrl + "/chat/completions", body);
    }

    // ═══════════════════════════════════════════
    //  智谱 GLM-4V（图片识别）
    // ═══════════════════════════════════════════
    private String callZhipu(String systemPrompt, String userText,
                             String base64Image, String mimeType) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", zhipuModel);
        body.put("max_tokens", zhipuMaxTokens);
        body.put("temperature", zhipuTemperature);

        List<Map<String, Object>> messages = new ArrayList<>();

        // system
        Map<String, Object> sysMsg = new HashMap<>();
        sysMsg.put("role", "system");
        sysMsg.put("content", systemPrompt);
        messages.add(sysMsg);

        // user（文本 + 图片）
        Map<String, Object> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        List<Map<String, Object>> parts = new ArrayList<>();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("type", "text");
        textPart.put("text", userText);
        parts.add(textPart);

        Map<String, Object> imagePart = new HashMap<>();
        imagePart.put("type", "image_url");
        Map<String, String> imageUrl = new HashMap<>();
        imageUrl.put("url", "data:" + mimeType + ";base64," + base64Image);
        imagePart.put("image_url", imageUrl);
        parts.add(imagePart);

        userMsg.put("content", parts);
        messages.add(userMsg);

        body.put("messages", messages);

        return doPost(zhipuRestTemplate, zhipuBaseUrl + "/chat/completions", body);
    }

    // ═══════════════════════════════════════════
    //  通用 HTTP POST
    // ═══════════════════════════════════════════
    @SuppressWarnings("unchecked")
    private String doPost(RestTemplate rt, String url, Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            Map<String, Object> response = rt.postForObject(url, entity, Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
                    return (String) msg.get("content");
                }
            }
            return "AI 服务暂时不可用，请稍后重试。";
        } catch (Exception e) {
            log.error("AI API call failed: {}", url, e);
            return "抱歉，AI 服务调用失败：" + e.getMessage();
        }
    }

    // ═══════════════════════════════════════════
    //  带对话历史的聊天（纯文本 → DeepSeek）
    // ═══════════════════════════════════════════
    @Transactional
    public ChatResponse chat(User user, String sessionId, String userMessage) {
        conversationRepo.save(AiConversation.builder()
                .user(user).sessionId(sessionId).role("user").content(userMessage).build());

        List<AiConversation> history = conversationRepo
                .findByUserIdAndSessionIdOrderByCreatedAtAsc(user.getId(), sessionId);
        int start = Math.max(0, history.size() - 21);
        List<Map<String, String>> messages = new ArrayList<>();

        messages.add(msg("system", getChatSystemPrompt(user.getId())));

        for (int i = start; i < history.size(); i++) {
            messages.add(msg(history.get(i).getRole(), history.get(i).getContent()));
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", dsModel);
        body.put("max_tokens", dsMaxTokens);
        body.put("temperature", dsTemperature);
        body.put("messages", messages);

        String reply = doPost(deepseekRestTemplate, dsBaseUrl + "/chat/completions", body);

        conversationRepo.save(AiConversation.builder()
                .user(user).sessionId(sessionId).role("assistant").content(reply).build());

        // 检测用户意图
        String intent = detectIntent(userMessage);

        return new ChatResponse("assistant", reply, null, new ChatResponse.Action(intent, null));
    }

    /**
     * 简单关键词意图识别
     */
    private String detectIntent(String message) {
        if (message == null) return null;
        String msg = message.toLowerCase();
        boolean schedule = msg.contains("安排") || msg.contains("日程") || msg.contains("生成")
                        || msg.contains("规划") || msg.contains("计划") || msg.contains("课表")
                        || msg.contains("明天") || msg.contains("今天") || msg.contains("今日");
        if (schedule) return "SCHEDULE_GENERATE";
        return null;
    }

    /**
     * DeepSeek 调用（可指定 temperature）
     */
    private String callDeepseek(String systemPrompt, String userText, double temperature) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", dsModel);
        body.put("max_tokens", dsMaxTokens);
        body.put("temperature", temperature);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(msg("system", systemPrompt));
        messages.add(msg("user", userText));
        body.put("messages", messages);

        return doPost(deepseekRestTemplate, dsBaseUrl + "/chat/completions", body);
    }

    // ═══════════════════════════════════════════
    //  AI 结构化日程生成
    // ═══════════════════════════════════════════

    // ═══════════════════════════════════════════
    //  任务书 JSON 生成（契约 §2）
    // ═══════════════════════════════════════════

    /**
     * 调用 DeepSeek 生成整本任务书的 JSON（周分段 + 天拆条）。
     * 返回原始文本；网络/调用失败返回 null，由调用方走确定性兜底生成。
     */
    public String generateTaskBookJson(Goal goal, LocalDate startDate, LocalDate endDate, String extraContext) {
        try {
            String systemPrompt = buildTaskBookSystemPrompt(goal, startDate, endDate, extraContext);
            String userMessage = "请为日期区间 " + startDate + " ~ " + endDate + " 生成任务书 JSON。";

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", dsTaskbookModel);
            body.put("max_tokens", dsTaskbookMaxTokens);
            body.put("temperature", dsTaskbookTemperature);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(msg("system", systemPrompt));
            messages.add(msg("user", userMessage));
            body.put("messages", messages);

            String content = doPost(deepseekRestTemplate, dsBaseUrl + "/chat/completions", body);
            log.info("Task book AI call done (model={}, chars={})",
                    dsTaskbookModel, content == null ? 0 : content.length());
            return content;
        } catch (Exception e) {
            log.warn("AI task book generation failed: {}", e.getMessage());
            return null;
        }
    }

    private String buildTaskBookSystemPrompt(Goal goal, LocalDate startDate, LocalDate endDate,
                                             String extraContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是 SmartSchedule 的任务书生成引擎。你的唯一职责是输出一个合法的 JSON 对象。\n");
        prompt.append("不要输出任何其他文字、解释或 Markdown 标记。只输出纯 JSON。\n\n");

        prompt.append("=== 目标 ===\n");
        if (goal == null) {
            prompt.append("名称: 我的目标\n描述: （用户未提供目标描述）\n");
        } else {
            prompt.append("名称: ").append(goal.getName()).append("\n");
            prompt.append("分类: ").append(goal.getCategory()).append("\n");
            if (goal.getDescription() != null && !goal.getDescription().trim().isEmpty()) {
                prompt.append("描述: ").append(goal.getDescription()).append("\n");
            }
        }
        if (extraContext != null && !extraContext.trim().isEmpty()) {
            prompt.append("补充: ").append(extraContext).append("\n");
        }
        prompt.append("\n");

        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        long weeks = (days + 6) / 7;
        prompt.append("=== 时间范围 ===\n");
        prompt.append("起始: ").append(startDate).append("\n");
        prompt.append("截止: ").append(endDate).append("\n");
        prompt.append("共 ").append(days).append(" 天，约 ").append(weeks).append(" 周\n\n");

        prompt.append("=== JSON 格式（严格遵守） ===\n");
        prompt.append("{\n");
        prompt.append("  \"title\": \"任务书标题（12-20 字，含目标名与周期）\",\n");
        prompt.append("  \"purpose\": \"2-4 句，说明为什么这样排、预期达到什么水平\",\n");
        prompt.append("  \"requirements\": \"总要求，3-6 条，用换行分隔\",\n");
        prompt.append("  \"weeks\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"weekIndex\": 1,\n");
        prompt.append("      \"theme\": \"本周主题，如 听力打底\",\n");
        prompt.append("      \"focus\": \"本周要点\",\n");
        prompt.append("      \"startDate\": \"").append(startDate).append("\",\n");
        prompt.append("      \"endDate\": \"yyyy-MM-dd\",\n");
        prompt.append("      \"items\": [\n");
        prompt.append("        {\n");
        prompt.append("          \"planDate\": \"yyyy-MM-dd\",\n");
        prompt.append("          \"title\": \"做什么（具体动作）\",\n");
        prompt.append("          \"learnContent\": \"学什么：具体到材料名/章节/题量/单词数\",\n");
        prompt.append("          \"estMinutes\": 45,\n");
        prompt.append("          \"doneCriteria\": \"怎么算完成（可验证）\",\n");
        prompt.append("          \"category\": \"STUDY\"\n");
        prompt.append("        }\n");
        prompt.append("      ]\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");

        prompt.append("=== 硬性要求（必须全部满足） ===\n");
        prompt.append("1. weeks 必须连续覆盖 ").append(startDate).append(" 到 ").append(endDate)
                .append(" 整段，最后一周可以不满 7 天；任何日期都不得越界\n");
        prompt.append("2. 每个有安排的日子每天 1-3 条；允许周末空一天不排条目\n");
        prompt.append("3. 禁止生成吃饭、睡觉、休息、通勤类条目\n");
        prompt.append("4. learnContent 必须具体（材料名、章节、题量、单词数），禁止「学习英语」「复习一下」这类空话\n");
        prompt.append("5. category 只能是 STUDY / PRACTICE / REVIEW 之一\n");
        prompt.append("6. planDate 必须落在所属周的 startDate~endDate 之间，且落在总区间内\n");
        prompt.append("7. title 不超过 40 字，doneCriteria 不超过 60 字，写可验证的完成标准\n");
        prompt.append("8. requirements 用 \\n 换行分隔成 3-6 条\n");
        return prompt.toString();
    }

    /**
     * 调用 DeepSeek 生成结构化的 JSON 日程。
     * 失败时返回 null，由调用方回退到贪心算法。
     */
    public AiScheduleResponse generateScheduleJson(User user, LocalDate date) {
        try {
            String systemPrompt = buildScheduleJsonSystemPrompt(user, date);
            String userMessage = "请为 " + date + " 生成日程 JSON。";

            String rawResponse = callDeepseek(systemPrompt, userMessage, 0.2);
            return parseScheduleJsonResponse(rawResponse);
        } catch (Exception e) {
            log.warn("AI schedule JSON generation failed for user {} on {}: {}",
                    user.getId(), date, e.getMessage());
            return null;
        }
    }

    private String buildScheduleJsonSystemPrompt(User user, LocalDate date) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是 SmartSchedule 的日程生成引擎。你的唯一职责是输出一个合法的 JSON 对象。\n");
        prompt.append("不要输出任何其他文字、解释或 Markdown 标记。只输出纯 JSON。\n\n");

        // 用户信息
        prompt.append("=== 用户信息 ===\n");
        prompt.append("起床时间: ").append(user.getWakeUpTime()).append("\n");
        prompt.append("睡觉时间: ").append(user.getSleepTime()).append("\n");
        String peakDesc = user.getEnergyPeak();
        if ("MORNING".equals(peakDesc)) peakDesc = "MORNING（上午6-12点精力最好）";
        else if ("AFTERNOON".equals(peakDesc)) peakDesc = "AFTERNOON（下午12-18点精力最好）";
        else if ("EVENING".equals(peakDesc)) peakDesc = "EVENING（晚上18-23点精力最好）";
        else peakDesc = "NIGHT（深夜23-6点精力最好）";
        prompt.append("精力高峰: ").append(peakDesc).append("\n\n");

        // 当天课程
        try {
            java.time.DayOfWeek dow = date.getDayOfWeek();
            String[] weekNames = {"周一","周二","周三","周四","周五","周六","周日"};
            List<Course> courses = courseRepo.findByUserIdAndIsActiveTrue(user.getId());
            List<Course> todayCourses = new java.util.ArrayList<>();
            for (Course c : courses) {
                if (c.getDayOfWeek() == dow.getValue()) {
                    todayCourses.add(c);
                }
            }
            prompt.append("=== 今日课表（时间不可更改，必须原样放入 tasks 中，taskType=COURSE） ===\n");
            if (todayCourses.isEmpty()) {
                prompt.append("（今天没有课程）\n");
            } else {
                for (Course c : todayCourses) {
                    prompt.append("- ").append(weekNames[dow.getValue()-1])
                        .append(" ").append(c.getStartTime()).append("-").append(c.getEndTime())
                        .append(" ").append(c.getName());
                    if (c.getLocation() != null && !c.getLocation().isEmpty()) {
                        prompt.append(" @").append(c.getLocation());
                    }
                    prompt.append("\n");
                }
            }
            prompt.append("\n");
        } catch (Exception e) {
            log.warn("Failed to load courses for schedule prompt: {}", e.getMessage());
        }

        // 用户目标
        try {
            List<Goal> goals = goalRepo.findByUserIdAndIsActiveTrue(user.getId());
            prompt.append("=== 用户目标（可灵活安排，可拆分成多个30-90分钟的块） ===\n");
            if (goals.isEmpty()) {
                prompt.append("（用户还没有添加目标）\n");
            } else {
                for (Goal g : goals) {
                    prompt.append("- ").append(g.getName())
                        .append("（").append(g.getCategory())
                        .append("，每次").append(g.getDefaultDuration()).append("分钟")
                        .append("，优先级").append(g.getPriority()).append("/10")
                        .append("，需要").append(g.getEnergyLevel()).append("精力");
                    if (g.getDescription() != null && !g.getDescription().isEmpty()) {
                        prompt.append("，备注：").append(g.getDescription());
                    }
                    prompt.append("）\n");
                }
            }
            prompt.append("\n");
        } catch (Exception e) {
            log.warn("Failed to load goals for schedule prompt: {}", e.getMessage());
        }

        // JSON 格式和排程规则
        prompt.append("=== JSON 格式（严格遵守） ===\n");
        prompt.append("{\n");
        prompt.append("  \"tasks\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"name\": \"任务名称\",\n");
        prompt.append("      \"taskType\": \"GOAL\",\n");
        prompt.append("      \"startTime\": \"09:00\",\n");
        prompt.append("      \"endTime\": \"09:50\",\n");
        prompt.append("      \"durationMinutes\": 50,\n");
        prompt.append("      \"energyLevel\": \"HIGH\",\n");
        prompt.append("      \"priority\": 8,\n");
        prompt.append("      \"goalName\": \"对应的目标名称\",\n");
        prompt.append("      \"courseName\": null\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"suggestion\": \"给用户的一句简短鼓励或建议（50字以内）\"\n");
        prompt.append("}\n\n");

        prompt.append("=== 排程规则（必须严格遵守） ===\n");
        prompt.append("1. taskType 必须是以下之一: GOAL(目标), COURSE(课程), CUSTOM(自定义)\n");
        prompt.append("2. 课程必须原样放入 taskType=COURSE，时间和名称不可更改\n");
        prompt.append("3. 每个目标可以拆分成多个 30-90 分钟的块\n");
        prompt.append("4. 禁止生成吃饭、睡觉、休息、通勤类任务（系统已移除 BREAK 休息块）\n");
        prompt.append("5. HIGH 精力目标尽量安排在用户的精力高峰时段\n");
        prompt.append("6. 相邻任务之间自然留出 10-15 分钟空隙即可，不要单独生成休息任务\n");
        prompt.append("7. 所有任务的时间不能重叠\n");
        prompt.append("8. 所有任务必须在起床和睡觉时间范围内\n");
        prompt.append("9. goalName 字段填写原目标名称（用于系统匹配），CUSTOM 类型可以不填\n");
        prompt.append("10. courseName 字段填写原课程名称\n");
        prompt.append("11. 不要输出 ```json 标记，不要输出任何解释文字，只输出纯 JSON\n");
        prompt.append("12. 合理安排任务次序，在精力好的时候做高优先级任务\n");

        return prompt.toString();
    }

    private AiScheduleResponse parseScheduleJsonResponse(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) return null;

        String json = rawResponse.trim();

        // 去掉可能的 markdown 代码块标记
        if (json.startsWith("```")) {
            int start = json.indexOf('\n');
            if (start > 0) {
                json = json.substring(start + 1);
                int end = json.lastIndexOf("```");
                if (end > 0) json = json.substring(0, end);
            }
        }

        // 找到 JSON 对象的起止位置
        int objStart = json.indexOf('{');
        int objEnd = json.lastIndexOf('}');
        if (objStart < 0 || objEnd <= objStart) return null;
        json = json.substring(objStart, objEnd + 1);

        try {
            AiScheduleResponse response = objectMapper.readValue(json, AiScheduleResponse.class);
            if (response.getTasks() == null || response.getTasks().isEmpty()) {
                log.warn("AI returned valid JSON but empty tasks list");
                return null;
            }
            return response;
        } catch (Exception e) {
            log.warn("Failed to parse AI schedule JSON: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, String> msg(String role, String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private String getChatSystemPrompt(Long userId) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是 SmartSchedule 智能日程规划助手的 AI 助手。你的职责是：\n");
        prompt.append("1. 帮助用户规划每日任务和日程安排\n");
        prompt.append("2. 根据用户的目标、课表和精力偏好生成合理的日程计划\n");
        prompt.append("3. 处理临时事件插入和日程调整\n");
        prompt.append("4. 提供时间管理和效率提升建议\n");
        prompt.append("5. 用温暖、鼓励的语气与用户交流\n\n");
        prompt.append("请用简洁、友好的中文回复，像一位贴心的日程管家。\n\n");

        // 注入用户真实数据
        try {
            // 当前日期上下文
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.DayOfWeek dow = today.getDayOfWeek();
            String[] weekNames = {"周一","周二","周三","周四","周五","周六","周日"};
            prompt.append("=== 当前时间 ===\n");
            prompt.append("今天是 ").append(today).append(" ").append(weekNames[dow.getValue()-1]).append("\n\n");

            // 课表
            List<Course> courses = courseRepo.findByUserIdAndIsActiveTrue(userId);
            if (!courses.isEmpty()) {
                prompt.append("=== 用户当前课表 ===\n");
                for (Course c : courses) {
                    prompt.append("- ").append(weekNames[c.getDayOfWeek()-1])
                        .append(" ").append(c.getStartTime()).append("-").append(c.getEndTime())
                        .append(" ").append(c.getName());
                    if (c.getLocation() != null && !c.getLocation().isEmpty()) {
                        prompt.append(" @").append(c.getLocation());
                    }
                    prompt.append("\n");
                }
                prompt.append("\n");
            }

            // 目标
            List<Goal> goals = goalRepo.findByUserIdAndIsActiveTrue(userId);
            if (!goals.isEmpty()) {
                prompt.append("=== 用户当前目标 ===\n");
                for (Goal g : goals) {
                    prompt.append("- ").append(g.getName())
                        .append("（").append(g.getCategory())
                        .append("，每次").append(g.getDefaultDuration()).append("分钟")
                        .append("，优先级").append(g.getPriority()).append("/10");
                    if (g.getDescription() != null && !g.getDescription().isEmpty()) {
                        prompt.append("，").append(g.getDescription());
                    }
                    prompt.append("）\n");
                }
                prompt.append("\n");
            }

            // 今日日程
            planRepo.findByUserIdAndPlanDate(userId, today).ifPresent(plan -> {
                prompt.append("=== 用户今日日程 ===\n");
                prompt.append("状态: ").append(plan.getStatus())
                    .append("，预估积分: ").append(plan.getEstimatedPoints()).append("\n");
                List<DailyTask> tasks = taskRepo.findByDailyPlanIdOrderBySortOrderAsc(plan.getId());
                if (!tasks.isEmpty()) {
                    for (DailyTask t : tasks) {
                        prompt.append("- ").append(t.getStatus())
                            .append(" | ").append(t.getScheduledStart() != null ? t.getScheduledStart().toLocalTime() : "?")
                            .append("-").append(t.getScheduledEnd() != null ? t.getScheduledEnd().toLocalTime() : "?")
                            .append(" | ").append(t.getName())
                            .append("（").append(t.getDurationMinutes()).append("分钟）\n");
                    }
                } else {
                    prompt.append("（今日暂无任务）\n");
                }
                prompt.append("\n");
            });

            if (courses.isEmpty() && goals.isEmpty()) {
                prompt.append("（用户目前还没有添加课表或目标，请引导他们先添加课程和目标以更好地规划日程。）\n");
            }
        } catch (Exception e) {
            log.warn("Failed to build user context for AI prompt: {}", e.getMessage());
        }

        return prompt.toString();
    }
}
