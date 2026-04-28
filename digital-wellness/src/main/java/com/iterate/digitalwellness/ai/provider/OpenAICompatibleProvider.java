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
    private static final int CONNECT_TIMEOUT = 60000;  // 连接超时 60秒
    private static final int READ_TIMEOUT = 300000;    // 读取超时 300秒

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

    private static final int MAX_TOKENS = 4096;
    private static final int MAX_CONTINUATIONS = 3;

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (!isAvailable()) {
            logger.warn("AI 服务未配置: {}", name);
            return "AI 服务未配置：请检查 " + name + " API Key 和 URL 配置";
        }

        logger.info("开始调用 {} API, model: {}", name, config.getModel());
        long startTime = System.currentTimeMillis();

        try {
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

            StringBuilder fullContent = new StringBuilder();

            for (int round = 0; round <= MAX_CONTINUATIONS; round++) {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", config.getModel());
                requestBody.put("messages", messages);
                requestBody.put("max_tokens", MAX_TOKENS);
                requestBody.put("temperature", 0.7);

                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + config.getApiKey());
                headers.set("Content-Type", "application/json");

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                logger.debug("发送请求到: {} (第{}轮)", config.getApiUrl(), round + 1);

                ResponseEntity<Map> response = restTemplate.postForEntity(config.getApiUrl(), entity, Map.class);

                if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                    if (round == 0) {
                        return "AI 调用失败，HTTP 状态码: " + response.getStatusCode();
                    }
                    break;
                }

                Map<String, Object> responseBody = response.getBody();

                if (responseBody.containsKey("error")) {
                    Object error = responseBody.get("error");
                    logger.error("API 返回错误: {}", error);
                    if (round == 0) {
                        return "AI 服务错误: " + error;
                    }
                    break;
                }

                if (!responseBody.containsKey("choices")) {
                    if (round == 0) {
                        return "AI 响应格式异常，请检查 API 返回";
                    }
                    break;
                }

                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (choices.isEmpty()) {
                    if (round == 0) {
                        return "AI 响应格式异常，请检查 API 返回";
                    }
                    break;
                }

                Map<String, Object> choice = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                if (message == null || !message.containsKey("content")) {
                    if (round == 0) {
                        return "AI 响应格式异常，请检查 API 返回";
                    }
                    break;
                }

                String content = (String) message.get("content");
                fullContent.append(content);

                String finishReason = choice.get("finish_reason") != null ? String.valueOf(choice.get("finish_reason")) : "";
                logger.info("第{}轮完成, finish_reason={}, 内容长度={}", round + 1, finishReason, content.length());

                if (!"length".equals(finishReason)) {
                    break;
                }

                logger.warn("AI 响应因 max_tokens 被截断 (finish_reason=length)，开始续写第{}轮...", round + 2);

                Map<String, String> assistantMessage = new HashMap<>();
                assistantMessage.put("role", "assistant");
                assistantMessage.put("content", content);
                messages.add(assistantMessage);

                Map<String, String> continueMessage = new HashMap<>();
                continueMessage.put("role", "user");
                continueMessage.put("content", "请继续生成，从你上次中断的地方继续。");
                messages.add(continueMessage);
            }

            long elapsed = System.currentTimeMillis() - startTime;
            logger.info("AI 调用完成, 总耗时: {}ms, 总内容长度: {}", elapsed, fullContent.length());

            return fullContent.toString();

        } catch (org.springframework.web.client.ResourceAccessException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            logger.error("AI 调用超时, 耗时: {}ms, 错误: {}", elapsed, e.getMessage());
            return "AI 调用超时，可能是网络问题或 AI 服务响应缓慢，请稍后重试";
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
