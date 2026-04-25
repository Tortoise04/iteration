package com.iterate.digitalwellness.controller;

import com.iterate.digitalwellness.ai.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 服务管理接口
 * 用于查看 AI 配置状态和切换提供商
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Autowired
    private AIService aiService;

    /**
     * 获取 AI 服务状态
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("currentProvider", aiService.getCurrentProvider().getName());
        status.put("availableProviders", aiService.getAvailableProviders());
        status.put("isAvailable", aiService.getCurrentProvider().isAvailable());
        return status;
    }

    /**
     * 切换 AI 提供商
     */
    @PostMapping("/provider/{providerName}")
    public Map<String, Object> switchProvider(@PathVariable String providerName) {
        Map<String, Object> result = new HashMap<>();
        try {
            aiService.setCurrentProvider(providerName);
            result.put("success", true);
            result.put("message", "已切换到: " + providerName);
            result.put("currentProvider", aiService.getCurrentProvider().getName());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 测试 AI 生成
     */
    @GetMapping("/test")
    public Map<String, Object> testGenerate(@RequestParam(defaultValue = "你好") String prompt) {
        Map<String, Object> result = new HashMap<>();
        result.put("provider", aiService.getCurrentProvider().getName());
        result.put("prompt", prompt);
        result.put("response", aiService.generate("你是一个友好的助手", prompt));
        return result;
    }
}
