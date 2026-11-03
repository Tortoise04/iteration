package com.smartschedule.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import java.time.Duration;

@Configuration
public class AiConfig {

    // ── DeepSeek ──
    @Value("${app.ai.deepseek.api-key}")
    private String deepseekApiKey;

    @Bean
    public RestTemplate deepseekRestTemplate(RestTemplateBuilder builder) {
        return builder
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + deepseekApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(300))
                .build();
    }

    // ── 智谱 GLM-4V（OCR / 图片识别）──
    @Value("${app.ai.zhipu.api-key}")
    private String zhipuApiKey;

    @Bean
    public RestTemplate zhipuRestTemplate(RestTemplateBuilder builder) {
        return builder
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + zhipuApiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(300))
                .build();
    }
}
