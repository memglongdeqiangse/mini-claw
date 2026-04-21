package com.miniclaw.agent;

import com.miniclaw.channel.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

/**
 * @description: 用户上下文
 * @author: zhanglei
 * @date: 2026/3/31
 */
@Data
@AllArgsConstructor
public class UserContext {
    /**
     * userId
     */
    private final String userId;
    /**
     * sessionId
     */
    private final String sessionId;
    /**
     * channelType
     */
    private final ChannelType channelType;
    /**
     * chatType
     */
    private final Map<String, Object> metadata;
}
