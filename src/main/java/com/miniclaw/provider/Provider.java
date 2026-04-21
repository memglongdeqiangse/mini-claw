package com.miniclaw.provider;

import lombok.Data;

/**
 * @description: llm提供者配置
 * @author: lei
 * @date: 2026/3/20
 */
@Data
public class Provider {
    /**
     * API密钥
     */
    private String apiKey;
    /**
     * API类型：
     * 目前只支持openai、anthropic
     */
    private String apiType;
    /**
     * 基础URL
     */
    private String baseUrl;
    /**
     * 模型名称
     */
    private String modelName;
}
