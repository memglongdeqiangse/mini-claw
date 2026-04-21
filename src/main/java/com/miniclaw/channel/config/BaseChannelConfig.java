package com.miniclaw.channel.config;

/**
 * @description:
 * @author: zhanglei
 * @date: 2026/3/19
 */
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.miniclaw.channel.ChannelType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息渠道配置基类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseChannelConfig {

    /**
     * 渠道类型（不序列化到JSON，由子类默认值或反序列化时从map key推断）
     */
    @JsonIgnore
    private ChannelType channelType;

    /**
     * 是否启用该渠道
     */
    private boolean enabled = false;

    /**
     * 机器人触发前缀
     */
    private String botPrefix = "";

    /**
     * 是否过滤工具消息
     */
    private boolean filterToolMessages = false;

    /**
     * 是否过滤思考过程
     */
    private boolean filterThinking = false;

    /**
     * 允许的用户/群组白名单
     */
    private List<String> allowFrom = new ArrayList<>();

    /**
     * 拒绝时的提示消息
     */
    private String denyMessage = "";

    /**
     * 校验配置是否有效
     * @return 校验通过返回 true，否则返回 false
     */
    public abstract boolean validate();


}

