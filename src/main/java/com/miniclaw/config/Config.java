package com.miniclaw.config;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniclaw.channel.ChannelType;
import com.miniclaw.channel.config.BaseChannelConfig;
import com.miniclaw.channel.config.FeiShuChannelConfig;
import com.miniclaw.channel.config.StdChannelConfig;
import com.miniclaw.channel.config.WebChannelConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.miniclaw.provider.Provider;
import lombok.Data;

/**
 * @description:
 * @author: lei
 * @date: 2026/3/20
 */
@Data
public class Config {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * llm提供者配置
     */
    private Provider provider;
    /**
     * 自定义agent
     */
    private List<CustomerAgentConfig> agent;
    /**
     * 工作空间
     */
    private String workspace;
    /**
     * 所有 channel 配置
     * key 为 channel 名称
     * value 为 channel 配置
     */
    private Map<String, BaseChannelConfig> channels = new HashMap<>();

    private List<String> skillPath;

    /**
     * 自定义反序列化 channels，从 map key 推断 channelType
     */
    @JsonSetter("channels")
    @SuppressWarnings("unchecked")
    public void setChannelsFromJson(Map<String, Object> channelsMap) {
        if (channelsMap == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : channelsMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            ChannelType type = ChannelType.fromValue(key);
            if (type == null) {
                continue;
            }
            Class<? extends BaseChannelConfig> configClass = getConfigClass(type);
            if (configClass != null) {
                try {
                    // 将 LinkedHashMap 转换为目标配置类
                    BaseChannelConfig config = MAPPER.convertValue(value, configClass);
                    config.setChannelType(type);
                    channels.put(key, config);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to parse channel config: " + key, e);
                }
            }
        }
    }

    private Class<? extends BaseChannelConfig> getConfigClass(ChannelType type) {
        return switch (type) {
            case FEISHU -> FeiShuChannelConfig.class;
            case WEB -> WebChannelConfig.class;
            case STD -> StdChannelConfig.class;
        };
    }

    /**
     * 校验所有 channel 配置，返回通过校验的配置列表
     * 从 channels 配置中获取所有 channel 配置，并校验其有效性
     * @return 通过校验的 BaseChannelConfig 列表
     */
    public List<BaseChannelConfig> validateChannels() {
        List<BaseChannelConfig> validConfigs = new ArrayList<>();
        for (ChannelType channelType : ChannelType.values()) {
            BaseChannelConfig config = channels.get(channelType.getChannelName());
            if (config != null && config.validate()) {
                validConfigs.add(config);
            }
        }
        return validConfigs;
    }
}
