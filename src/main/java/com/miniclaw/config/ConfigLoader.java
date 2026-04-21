package com.miniclaw.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniclaw.provider.ApiType;
import com.miniclaw.provider.Provider;
import io.agentscope.core.model.AnthropicChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: 配置工具类
 * @author: zhanglei
 * @date: 2026/3/20
 */
public class ConfigLoader {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 环境变量占位符模式: ${ENV_VAR} 或 ${ENV_VAR:default}
     */
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([^}:]+)(?::([^}]*))?}");

    /**
     * 从家目录读取config.json配置文件，支持环境变量替换
     */
    public static Config loadConfig() {
        String homeDir = System.getProperty("user.home");
        Path configPath = Path.of(homeDir, ".mini-claw", "config.json");
        File configFile = configPath.toFile();

        if (!configFile.exists()) {
            throw new IllegalArgumentException("配置文件未找到: " + configPath);
        }

        try {
            String jsonContent = Files.readString(configPath);
            jsonContent = resolveEnvVariables(jsonContent);
            return OBJECT_MAPPER.readValue(jsonContent, Config.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("加载配置文件失败: " + configPath, e);
        }
    }

    /**
     * 解析字符串中的环境变量占位符
     * 支持 ${ENV_VAR} 和 ${ENV_VAR:default} 两种格式
     */
    static String resolveEnvVariables(String content) {
        Matcher matcher = ENV_PATTERN.matcher(content);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String envVar = matcher.group(1);
            String defaultValue = matcher.group(2);
            String envValue = System.getenv(envVar);
            if (envValue == null) {
                if (defaultValue == null) {
                    throw new RuntimeException("环境变量 " + envVar + " 未设置且无默认值");
                }
                envValue = defaultValue;
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(envValue));
        }
        matcher.appendTail(result);
        return result.toString();
    }


    public static Model MODEL = null;

    /**
     * 创建聊天模型
     */
    public static Model getChatModel(Config config) {
        if (MODEL != null) {
            return MODEL;
        }
        Provider provider = config.getProvider();
        String baseUrl = provider.getBaseUrl();
        String apiTypeString = provider.getApiType();
        ApiType apiType = null;
        if (apiTypeString != null && !apiTypeString.isEmpty()) {
            apiType = ApiType.getByType(apiTypeString.toLowerCase());
        }
        if (apiType == null) {
            apiType = ApiType.getType(baseUrl);
        }
        if (apiType == ApiType.ANTHROPIC) {
            MODEL = AnthropicChatModel.builder()
                    .apiKey(provider.getApiKey())
                    .baseUrl(provider.getBaseUrl())
                    .modelName(provider.getModelName())
                    .build();
        } else {
            MODEL = OpenAIChatModel.builder()
                    .apiKey(provider.getApiKey())
                    .baseUrl(provider.getBaseUrl())
                    .modelName(provider.getModelName())
                    .build();
        }
        return MODEL;
    }
}
