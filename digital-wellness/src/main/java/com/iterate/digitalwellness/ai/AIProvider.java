package com.iterate.digitalwellness.ai;

/**
 * AI 模型提供商接口
 * 支持多厂商扩展，如阿里云百炼、火山引擎、OpenAI 等
 */
public interface AIProvider {

    /**
     * 获取提供商名称
     */
    String getName();

    /**
     * 生成文本内容
     * @param systemPrompt 系统提示词
     * @param userPrompt 用户提示词
     * @return 生成的文本内容
     */
    String generate(String systemPrompt, String userPrompt);

    /**
     * 检查该提供商是否可用
     */
    boolean isAvailable();
}
