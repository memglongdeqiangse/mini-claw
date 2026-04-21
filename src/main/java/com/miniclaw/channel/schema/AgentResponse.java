package com.miniclaw.channel.schema;

import com.miniclaw.channel.ChannelType;
import io.agentscope.core.message.Msg;

import java.util.Map;

/**
 * AgentResponse - Agent 响应结构
 *
 * @author lei
 */
public record AgentResponse(String sessionId, String userId, Msg output, ChannelType channelType, Map<String, Object> channelMeta) {}