package com.miniclaw.channel.schema;

import com.miniclaw.channel.ChannelType;
import io.agentscope.core.message.Msg;

import java.util.List;
import java.util.Map;

/**
 * AgentRequest - Agent 请求结构
 * @author lei
 */
public record AgentRequest(
        String sessionId,
        String userId,
        List<Msg> input,
        ChannelType channelType,
        Map<String, Object> channelMeta) {
    public AgentRequest {
        if (input == null) {
            input = List.of();
        }
        if (channelMeta == null) {
            channelMeta = Map.of();
        }
    }

    /**
     * 创建带更新内容的请求副本
     */
    public AgentRequest withInput(List<Msg> newInput) {
        return new AgentRequest(sessionId, userId, newInput, channelType, channelMeta);
    }

    /**
     * 创建带更新元数据的请求副本
     */
    public AgentRequest withChannelMeta(Map<String, Object> newMeta) {
        return new AgentRequest(sessionId, userId, input, channelType, newMeta);
    }
}