package com.miniclaw.channel;

import com.fasterxml.jackson.annotation.JsonValue;
import com.miniclaw.channel.feishu.FeishuChannel;
import com.miniclaw.channel.stdio.StdChannel;
import com.miniclaw.channel.web.WebChannel;

/**
 * ChannelType - 渠道类型枚举
 * @author: lei
 * @date: 2026/3/23
 */
public enum ChannelType {
    /** 标准输入输出渠道 */
    STD("stdio", StdChannel.class),
    /** 飞书渠道 */
    FEISHU("feishu", FeishuChannel.class),
    /** Web渠道 */
    WEB("web", WebChannel.class)
    ;

    private final String channelName;
    private final Class<? extends BaseChannel> channelClass;

    ChannelType(String channelName, Class<? extends BaseChannel> channelClass) {
        this.channelName = channelName;
        this.channelClass = channelClass;
    }

    public String getChannelName() {
        return channelName;
    }

    /**
     * 获取对应的渠道类
     * @return 渠道类，必须是 BaseChannel 的子类
     */
    public Class<? extends BaseChannel> getChannelClass() {
        return channelClass;
    }

    @Override
    public String toString() {
        return channelName;
    }

    /**
     * Jackson 序列化时使用枚举名称
     */
    @JsonValue
    public String getName() {
        return name();
    }

    /**
     * 从字符串值获取对应的渠道类型
     * @param value 渠道类型字符串值
     * @return 对应的 ChannelType，未找到返回 null
     */
    public static ChannelType fromValue(String value) {
        if (value == null) {
            return null;
        }
        // 先尝试按枚举名称匹配
        for (ChannelType type : values()) {
            if (type.name().equals(value)) {
                return type;
            }
        }
        // 再尝试按 channelName 匹配
        for (ChannelType type : values()) {
            if (type.channelName.equals(value)) {
                return type;
            }
        }
        return null;
    }



}