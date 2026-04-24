package com.iterate.digitalwellness.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AIServiceImpl {
    private final String apiKey;
    private final String apiUrl;
    private final String model;
    private final RestTemplate restTemplate;

    public AIServiceImpl(
            @Value("${alibaba.bailing.api-key}") String apiKey,
            @Value("${alibaba.bailing.api-url}") String apiUrl,
            @Value("${alibaba.bailing.model}") String model) {
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
        this.model = model;
        this.restTemplate = new RestTemplate();
    }

    public String generateSummary(String data) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个数字健康助手，负责根据用户的健康数据生成详细的周期总结，包括分析和建议。");
        
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", "请根据以下数字健康数据生成一份详细的周期总结，包括分析和建议：\n" + data);
        
        java.util.List<Map<String, String>> messages = new java.util.ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);
        
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 1000);
        requestBody.put("temperature", 0.7);

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Content-Type", "application/json");

        org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);
        org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                java.util.List<Map<String, Object>> choices = (java.util.List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) choice.get("message");
                    return (String) message.get("content");
                }
            }
        }
        
        // 如果 AI 调用失败，返回默认总结
        return "AI 总结生成失败，请检查 API 密钥是否正确。";
    }
}