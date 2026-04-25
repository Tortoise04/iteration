package com.iterate.digitalwellness.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 阿里云百炼 AI 提供商
 * API 文档: https://help.aliyun.com/document_detail/2712195.html
 */
public class AlibabaBailingProvider implements AIProvider {

    private static final Logger logger = LoggerFactory.getLogger(AlibabaBailingProvider.class);
    private static final String NAME = "alibaba-bailing";
    private static final int CONNECT_TIMEOUT = 30000;  // 连接超时 30秒
    private static final int READ_TIMEOUT = 120000;    // 读取超时 120秒（AI 生成可能较慢）

    private final ProviderConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AlibabaBailingProvider(ProviderConfig config) {
        this.config = config;
        this.restTemplate = createRestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return new RestTemplate(factory);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            logger.warn("AI 服务未配置");
            return "AI 服务未配置：请检查阿里云百炼 API Key 和 URL 配置";
        }

        logger.info("开始调用阿里云百炼 API, model: {}", config.getModel());
        long startTime = System.currentTimeMillis();

        try {
            // 构建请求体 - 阿里云百炼 API 格式
            Map<String, Object> requestBody = new HashMap<>();

            // 模型参数
            Map<String, Object> input = new HashMap<>();
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

            input.put("messages", messages);

            // 参数配置
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("max_tokens", 2000);
            parameters.put("temperature", 0.7);
            parameters.put("result_format", "message");

            requestBody.put("model", config.getModel());
            requestBody.put("input", input);
            requestBody.put("parameters", parameters);

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
                logger.debug("响应内容: {}", responseBody);

                // 阿里云百炼返回格式: {"output": {"choices": [{"message": {"content": "..."}}]}}
                if (responseBody.containsKey("output")) {
                    Map<String, Object> output = (Map<String, Object>) responseBody.get("output");
                    if (output.containsKey("choices")) {
                        List<Map<String, Object>> choices = (List<Map<String, Object>>) output.get("choices");
                        if (!choices.isEmpty()) {
                            Map<String, Object> choice = choices.get(0);
                            Map<String, Object> message = (Map<String, Object>) choice.get("message");
                            if (message != null && message.containsKey("content")) {
                                return (String) message.get("content");
                            }
                        }
                    }
                    // 兼容旧格式: {"output": {"text": "..."}}
                    if (output.containsKey("text")) {
                        return (String) output.get("text");
                    }
                }

                // 检查错误信息
                if (responseBody.containsKey("code")) {
                    String errorCode = String.valueOf(responseBody.get("code"));
                    String errorMsg = String.valueOf(responseBody.get("message"));
                    logger.error("API 返回错误: code={}, message={}", errorCode, errorMsg);
                    return "AI 服务错误: " + errorMsg;
                }

                logger.error("未知的响应格式: {}", responseBody);
                return "AI 响应格式异常，请检查 API 返回";
            }

            return "AI 调用失败，HTTP 状态码: " + response.getStatusCode();

        } catch (org.springframework.web.client.ResourceAccessException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            logger.error("AI 调用超时, 耗时: {}ms, 错误: {}", elapsed, e.getMessage());
            return "AI 调用超时，请稍后重试（当前超时设置: " + (READ_TIMEOUT / 1000) + "秒）";
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
