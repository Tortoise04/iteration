package com.iterate.digitalwellness.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.iterate.digitalwellness.ai.provider.AlibabaBailingProvider;
import com.iterate.digitalwellness.ai.provider.OpenAICompatibleProvider;
import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 服务门面类
 * 根据配置自动选择 AI 提供商，支持运行时切换
 */
@Service
public class AIService {

    @Autowired
    private AIConfig aiConfig;

    private final Map<String, AIProvider> providers = new HashMap<>();
    private AIProvider currentProvider;

    @PostConstruct
    public void init() {
        // 注册所有支持的提供商
        registerProvider(new OpenAICompatibleProvider("zhipu", aiConfig.getZhipu()));
        registerProvider(new AlibabaBailingProvider(aiConfig.getAlibabaBailing()));
        registerProvider(new OpenAICompatibleProvider("volc-engine", aiConfig.getVolcEngine()));
        registerProvider(new OpenAICompatibleProvider("openai", aiConfig.getOpenai()));

        // 设置当前提供商
        setCurrentProvider(aiConfig.getProvider());
    }

    /**
     * 注册 AI 提供商
     */
    public void registerProvider(AIProvider provider) {
        providers.put(provider.getName(), provider);
    }

    /**
     * 设置当前使用的提供商
     */
    public void setCurrentProvider(String providerName) {
        AIProvider provider = providers.get(providerName.toLowerCase());
        if (provider != null) {
            this.currentProvider = provider;
        } else {
            throw new IllegalArgumentException("未知的 AI 提供商: " + providerName +
                    "，支持的提供商: " + String.join(", ", providers.keySet()));
        }
    }

    /**
     * 获取当前提供商
     */
    public AIProvider getCurrentProvider() {
        return currentProvider;
    }

    /**
     * 生成文本（使用当前提供商）
     */
    public String generate(String systemPrompt, String userPrompt) {
        if (currentProvider == null) {
            return "AI 服务未初始化，请检查配置";
        }
        return currentProvider.generate(systemPrompt, userPrompt);
    }

    /**
     * 生成周期总结
     */
    public String generateSummary(String data) {
        String systemPrompt = "你是一个数字健康助手，负责根据用户的健康数据生成详细的周期总结。" +
                "请用中文回答，输出格式清晰，包含以下内容：" +
                "1. 数据分析：详细分析用户的目标完成情况、手机使用情况、日常活动等数据" +
                "2. 问题识别：指出存在的问题和改进空间" +
                "3. 解决办法：针对问题提供具体的解决方法" +
                "4. 落地方案：提供可操作的具体计划和步骤" +
                "5. 目标建议：基于当前情况，提出合理的下一步目标" +
                "请确保内容具体、实用，能够真正帮助用户改善数字健康状况。";

        String userPrompt = "请根据以下数字健康数据生成一份详细的周期总结报告：\n\n" + data;

        return generate(systemPrompt, userPrompt);
    }

    /**
     * 获取所有可用的提供商
     */
    public Map<String, Boolean> getAvailableProviders() {
        Map<String, Boolean> result = new HashMap<>();
        providers.forEach((name, provider) -> result.put(name, provider.isAvailable()));
        return result;
    }
}
