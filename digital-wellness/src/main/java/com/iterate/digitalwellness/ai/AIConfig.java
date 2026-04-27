package com.iterate.digitalwellness.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 配置类
 * 从 application.yml 加载多厂商配置
 */
@Configuration
@ConfigurationProperties(prefix = "ai")
public class AIConfig {

    /**
     * 当前使用的提供商
     */
    private String provider = "zhipu";

    /**
     * 智谱AI配置
     */
    private ProviderConfig zhipu = new ProviderConfig();

    /**
     * 阿里云百炼配置
     */
    private ProviderConfig alibabaBailing = new ProviderConfig();

    /**
     * 字节跳动火山引擎配置
     */
    private ProviderConfig volcEngine = new ProviderConfig();

    /**
     * OpenAI 配置
     */
    private ProviderConfig openai = new ProviderConfig();

    // Getters and Setters
    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public ProviderConfig getZhipu() {
        return zhipu;
    }

    public void setZhipu(ProviderConfig zhipu) {
        this.zhipu = zhipu;
    }

    public ProviderConfig getAlibabaBailing() {
        return alibabaBailing;
    }

    public void setAlibabaBailing(ProviderConfig alibabaBailing) {
        this.alibabaBailing = alibabaBailing;
    }

    public ProviderConfig getVolcEngine() {
        return volcEngine;
    }

    public void setVolcEngine(ProviderConfig volcEngine) {
        this.volcEngine = volcEngine;
    }

    public ProviderConfig getOpenai() {
        return openai;
    }

    public void setOpenai(ProviderConfig openai) {
        this.openai = openai;
    }

    /**
     * 根据提供商名称获取配置
     */
    public ProviderConfig getConfigByProvider(String providerName) {
        switch (providerName.toLowerCase()) {
            case "zhipu":
            case "zhipuai":
            case "glm":
                return zhipu;
            case "alibaba-bailing":
            case "alibaba":
            case "bailing":
                return alibabaBailing;
            case "volc-engine":
            case "volcengine":
            case "volc":
                return volcEngine;
            case "openai":
            case "open-ai":
                return openai;
            default:
                return zhipu;
        }
    }

    /**
     * 单个提供商配置
     */
    public static class ProviderConfig {
        private String apiKey;
        private String model;
        private String apiUrl;

        public ProviderConfig() {
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiUrl() {
            return apiUrl;
        }

        public void setApiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
        }

        public boolean isValid() {
            return apiKey != null && !apiKey.isEmpty()
                    && apiUrl != null && !apiUrl.isEmpty();
        }
    }
}
