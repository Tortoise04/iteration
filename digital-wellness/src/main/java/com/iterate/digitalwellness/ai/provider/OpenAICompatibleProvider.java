package com.iterate.digitalwellness.ai.provider;

import com.iterate.digitalwellness.ai.AIProvider;
import com.iterate.digitalwellness.ai.AIConfig.ProviderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * OpenAI 兼容格式 AI 提供商
 * 支持: OpenAI、字节跳动火山引擎、DeepSeek 等兼容 OpenAI API 格式的服务
 */
public class OpenAICompatibleProvider implements AIProvider {

    private static final Logger logger = LoggerFactory.getLogger(OpenAICompatibleProvider.class);
    private static final int CONNECT_TIMEOUT = 30000;  // 连接超时 30秒
    private static final int READ_TIMEOUT = 120000;    // 读取超时 120秒

    private final String name;
    private final ProviderConfig config;
    private final RestTemplate restTemplate;

    public OpenAICompatibleProvider(String name, ProviderConfig config) {
        this.name = name;
        this.config = config;
        this.restTemplate = createRestTemplate();
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return new RestTemplate(factory);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            logger.warn("AI 服务未配置: {}", name);
            return "AI 服务未配置：请检查 " + name + " API Key 和 URL 配置";
        }

        logger.info("开始调用 {} API, model: {}", name, config.getModel());
        long startTime = System.currentTimeMillis();

        try {
            // 构建 OpenAI 兼容格式请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel());

            List<Map<String, String>> messages = new ArrayList<>();

            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                Map<String, String> systemMessage = new HashMap<>();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemPrompt);
                messages.add(systemMessage);
            }

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messages.add(userMessage);

            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 2000);
            requestBody.put("temperature", 0.7);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + config.getApiKey());
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            logger.debug("发送请求到: {}", config.getApiUrl());

            // 发送请求
            ResponseEntity<Map> response = restTemplate.postForEntity(config.getApiUrl(), entity, Map.class);

            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("AI 调用完成, 耗时: {}ms", elapsed);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();

                // OpenAI 格式: {"choices": [{"message": {"content": "..."}}]}
                if (responseBody.containsKey("choices")) {
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> choice = choices.get(0);
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        if (message != null && message.containsKey("content")) {
                            return (String) message.get("content");
                        }
                    }
                }

                // 检查错误信息
                if (responseBody.containsKey("error")) {
                    Object error = responseBody.get("error");
                    logger.error("API 返回错误: {}", error);
                    return "AI 服务错误: " + error;
                }

                return "AI 响应格式异常，请检查 API 返回";
            }

            return "AI 调用失败，HTTP 状态码: " + response.getStatusCode();

        } catch (org.springframework.web.client.ResourceAccessException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            logger.error("AI 调用超时, 耗时: {}ms, 错误: {}", elapsed, e.getMessage());
            return "AI 调用超时，请稍后重试";
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            logger.error("AI 调用异常, 耗时: {}ms", elapsed, e);
            return "AI 调用异常: " + e.getMessage();
        }
    }

    @Override
    public boolean isAvailable() {
        return config != null && config.isValid();
    }
}
