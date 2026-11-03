package com.smartschedule.controller;

import com.smartschedule.entity.User;
import com.smartschedule.exception.BusinessException;
import com.smartschedule.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * OCR 课表识别接口。
 * 上传课表图片，通过智谱 GLM-4V 视觉识别提取课程信息。
 */
@Slf4j
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final AiService aiService;

    @PostMapping("/parse-course-table")
    public ResponseEntity<Map<String, Object>> parseCourseTable(@RequestParam("image") MultipartFile image) {
        // 认证
        User currentUser = (User) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Long userId = currentUser.getId();

        // 校验
        if (image.isEmpty()) {
            throw new BusinessException("请选择一张图片");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("只支持图片文件（JPEG/PNG/WebP）");
        }

        // 读取为 Base64
        byte[] bytes;
        try {
            bytes = image.getBytes();
        } catch (IOException e) {
            throw new BusinessException("读取图片失败: " + e.getMessage());
        }
        String base64 = Base64.getEncoder().encodeToString(bytes);
        String mimeType = contentType;

        log.info("OCR: user={}, file={}, size={}KB, type={}",
                userId, image.getOriginalFilename(), bytes.length / 1024, mimeType);

        // 调用 AI 视觉识别
        String systemPrompt = buildSystemPrompt();
        String userPrompt = "请识别这张课程表中的所有课程信息，按照上述 JSON 格式返回。";
        String aiResult = aiService.generateWithImage(systemPrompt, userPrompt, base64, mimeType);

        log.info("OCR result (first 400 chars): {}",
                aiResult.substring(0, Math.min(400, aiResult.length())));

        // 解析 JSON
        List<Map<String, Object>> courses = parseAiResult(aiResult);

        // 响应
        Map<String, Object> resp = new HashMap<>();
        resp.put("courses", courses);
        resp.put("rawText", aiResult);
        return ResponseEntity.ok(resp);
    }

    private String buildSystemPrompt() {
        return "你是一个专业的课程表 OCR 解析器。用户会上传一张课程表图片，请仔细识别图片中的所有课程信息，"
             + "并以 JSON 数组格式返回。每条课程包含以下字段：\n\n"
             + "- name: 课程名称（字符串）\n"
             + "- dayOfWeek: 星期几，数字 1-7（1=周一，2=周二，...，7=周日）\n"
             + "- startTime: 开始时间，24小时制格式 HH:mm（如上午课 \"08:00\"，下午课 \"14:00\"，晚上课 \"19:00\"）\n"
             + "- endTime: 结束时间，24小时制格式 HH:mm\n"
             + "- location: 上课地点（字符串，如果没有地点信息则返回空字符串 \"\"）\n\n"
             + "重要：时间必须是严格的24小时制！上午8点是08:00，下午2点是14:00，晚上7点是19:00。"
             + "如果你在图片中看到类似\"1:00\"或\"01:00\"的时间，请根据上下文判断是凌晨1点(01:00)还是下午1点(13:00)。"
             + "大学课程通常在08:00-21:00之间，请据此修正不合理的时间。\n\n"
             + "要求：\n"
             + "1. 只返回 JSON 数组，不要包含任何其他文字说明\n"
             + "2. 数组格式示例：\n"
             + "[{\"name\":\"高等数学\",\"dayOfWeek\":1,\"startTime\":\"08:00\",\"endTime\":\"09:30\",\"location\":\"教A201\"}]\n"
             + "3. 如果图片中某个字段内容不清晰，请根据上下文合理推测\n"
             + "4. 如果图片完全无法识别或不是课表，返回空数组 []\n"
             + "5. 如果一张图片包含多天的课表（如周一到周五），请逐一识别每一门课程";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseAiResult(String aiResult) {
        try {
            String json = aiResult.trim();

            // 去掉 markdown 代码块标记
            if (json.startsWith("```")) {
                int arrStart = json.indexOf('[');
                int arrEnd = json.lastIndexOf(']');
                if (arrStart >= 0 && arrEnd > arrStart) {
                    json = json.substring(arrStart, arrEnd + 1);
                }
            }

            // 确保是 JSON 数组
            int arrStart = json.indexOf('[');
            int arrEnd = json.lastIndexOf(']');
            if (arrStart < 0 || arrEnd <= arrStart) {
                log.warn("No JSON array found in AI result: {}", aiResult.substring(0, Math.min(200, aiResult.length())));
                return Collections.emptyList();
            }
            json = json.substring(arrStart, arrEnd + 1);

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> list = mapper.readValue(json, List.class);

            // 清洗数据
            for (Map<String, Object> course : list) {
                // dayOfWeek 范围修正
                Object dow = course.get("dayOfWeek");
                if (dow instanceof Integer) {
                    int d = (Integer) dow;
                    if (d < 1 || d > 7) {
                        course.put("dayOfWeek", ((d - 1) % 7) + 1);
                    }
                }
                // 时间智能修正：AI 常见的 AM/PM 混淆
                fixTime(course, "startTime");
                fixTime(course, "endTime");
            }

            return list;
        } catch (Exception e) {
            log.error("Failed to parse AI OCR result", e);
            return Collections.emptyList();
        }
    }

    /**
     * 修正 AI 常见的时间识别错误。
     * 如果时间在 01:00-07:59 且图片是大学课表（通常 08:00-21:00），
     * 大概率是 PM 时间被误识别，自动加 12 小时。
     */
    private void fixTime(Map<String, Object> course, String key) {
        Object val = course.get(key);
        if (!(val instanceof String)) return;
        String time = (String) val;
        if (!time.matches("\\d{2}:\\d{2}")) return;

        try {
            int hour = Integer.parseInt(time.substring(0, 2));
            int minute = Integer.parseInt(time.substring(3, 5));
            if (hour >= 1 && hour <= 7) {
                // 凌晨 1-7 点 → 大概率是下午 13-19 点
                hour += 12;
                course.put(key, String.format("%02d:%02d", hour, minute));
            }
        } catch (NumberFormatException ignored) { }
    }
}
