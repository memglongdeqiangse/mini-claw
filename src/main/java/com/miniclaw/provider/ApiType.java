package com.miniclaw.provider;

import java.util.regex.Pattern;

/**
 * API类型枚举：区分OpenAI API和Anthropic API
 */
public enum ApiType {
    /**
     * OpenAI API（OpenAPI风格）
     * 特征：域名包含api.openai.com，路径含/v1/（如/v1/chat/completions）
     */
    OPENAI(
            "openai",
            Pattern.compile(".*api\\.openai\\.com.*\\/v1\\/.*", Pattern.CASE_INSENSITIVE)
    ),
    /**
     * Anthropic API（Claude系列）
     * 特征：域名包含api.anthropic.com，路径含/v1/（如/v1/messages）
     */
    ANTHROPIC(
            "anthropic",
            Pattern.compile(".*api\\.anthropic\\.com.*\\/v1\\/.*", Pattern.CASE_INSENSITIVE)
    ),
    /**
     * 未知API类型
     */
    UNKNOWN("unknown", null);

    // API类型名称
    private final String typeName;
    // URL匹配正则（忽略大小写）
    private final Pattern urlPattern;

    ApiType(String typeName, Pattern urlPattern) {
        this.typeName = typeName;
        this.urlPattern = urlPattern;
    }

    public String getTypeName() {
        return typeName;
    }

    public Pattern getUrlPattern() {
        return urlPattern;
    }

    /**
     * 根据URL推断API类型
     * @param apiUrl API的完整URL（如https://api.openai.com/v1/chat/completions）
     * @return 匹配的API类型，无匹配返回UNKNOWN
     */
    public static ApiType getType(String apiUrl) {
        // 1. 空值/空字符串校验
        if (apiUrl == null || apiUrl.trim().isEmpty()) {
            return ApiType.UNKNOWN;
        }

        // 2. 遍历枚举，匹配URL特征
        for (ApiType apiType : ApiType.values()) {
            if (apiType == ApiType.UNKNOWN) {
                continue;
            }
            // 正则匹配URL
            if (apiType.getUrlPattern().matcher(apiUrl.trim()).matches()) {
                return apiType;
            }
        }

        // 3. 无匹配返回未知
        return ApiType.UNKNOWN;
    }
    public static ApiType getByType(String typeName) {
        // 1. 空值/空字符串校验
        if (typeName == null || typeName.trim().isEmpty()) {
            return ApiType.UNKNOWN;
        }
        // 2. 遍历枚举，匹配类型名称
        for (ApiType apiType : ApiType.values()) {
            if (apiType == ApiType.UNKNOWN) {
                continue;
            }
            if (apiType.getTypeName().equalsIgnoreCase(typeName.trim())) {
                return apiType;
            }
        }
        // 3. 无匹配返回未知
        return ApiType.UNKNOWN;
    }

}
