package com.miniclaw.channel;

import com.miniclaw.bus.AgentMessageQueue;
import com.miniclaw.channel.config.BaseChannelConfig;
import com.miniclaw.channel.schema.*;
import com.miniclaw.command.CommandType;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;

import java.util.*;
import java.util.concurrent.*;

/**
 * BaseChannel
 * - 所有渠道的抽象基类
 * - sealed classes 用于内容类型
 * - records 用于不可变数据
 * - 虚拟线程用于异步处理
 * - 模式匹配用于类型处理
 */
public abstract class BaseChannel {
    protected final ChannelType channelType;
    protected AgentMessageQueue agentMessageQueue;
    protected boolean showToolDetails = true;
    protected boolean filterToolMessages = false;
    protected boolean filterThinking = false;
    protected Set<String> allowFrom = new HashSet<>();
    protected String denyMessage = "";

    protected BaseChannel(ChannelType channelType, BaseChannelConfig config, AgentMessageQueue agentMessageQueue) {
        this.channelType = channelType;
        if (config == null) {
            return;
        }
        this.agentMessageQueue = agentMessageQueue;
        this.filterToolMessages = config.isFilterToolMessages();
        this.filterThinking = config.isFilterThinking();
        this.allowFrom = new HashSet<>(config.getAllowFrom());
        this.denyMessage = config.getDenyMessage();
    }

    /**
     * 获取渠道类型
     */
    public ChannelType getChannelType() {
        return channelType;
    }

    /**
     * 处理用户输入
     * @param userId
     * @param sessionId
     * @param text
     */
    public void process(String userId, String sessionId, String text, Map<String, Object> channelMeta) throws InterruptedException {
        //处理是系统命令还是用户消息
        text = text.trim();
        boolean isCommand = CommandType.isCommand(text);
        Msg msg = buildAgentRequestFromUserContent(text, channelMeta);
        AgentRequest request = new AgentRequest(sessionId, userId, List.of(msg), channelType, channelMeta);
        if (isCommand) {
            CommandType commandType = CommandType.fromText(text);
            String result = commandType.getHandler().apply(request);
            Msg systemMsg = Msg.builder()
                    .name("assistant")
                    .role(MsgRole.ASSISTANT)
                    .content(TextBlock.builder().text(result).build())
                    .metadata(channelMeta)
                    .build();
            AgentResponse response = new AgentResponse(sessionId, userId, systemMsg, channelType, channelMeta);
            agentMessageQueue.putResponse(response);
        } else {
            agentMessageQueue.offerRequest(request);
        }
    }

    /**
     * 是否允许用户发送消息
     *
     * @param userId
     * @return
     */
    public abstract boolean isAllowed(String userId);

    /**
     * 从用户内容构建 AgentRequest
     */
    protected Msg buildAgentRequestFromUserContent(String text, Map<String, Object> channelMeta) {
        Msg msg = Msg.builder().name("user").textContent(text).metadata(channelMeta).role(MsgRole.USER).build();
        return msg;
    }


    /**
     * 消费单个消息
     * 核心处理流程
     */
    public boolean offer(AgentRequest request) {
        return agentMessageQueue.offerRequest(request);
    }


    /**
     * 不同渠道发送给用户文本 (抽象方法，子类实现)
     * @param response 响应
     */
    public abstract CompletableFuture<Void> send(AgentResponse response);


    /**
     * 启动
     */
    public abstract void start();

    /**
     * 停止
     */
    public abstract void stop();


}