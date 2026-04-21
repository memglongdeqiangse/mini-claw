package com.miniclaw.channel.config;

import com.miniclaw.bus.AgentMessageQueue;
import com.miniclaw.channel.BaseChannel;
import com.miniclaw.channel.schema.AgentRequest;
import com.miniclaw.channel.schema.AgentResponse;
import com.miniclaw.channel.ChannelType;
import com.miniclaw.config.Config;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @description: channel管理器，启动和停止所有channel，路由消息给请求的用户
 * @author: lei
 * @date: 2026/3/20
 */
@Slf4j
public class ChannelManager {

    private volatile boolean running = true;
    private final Map<ChannelType, BaseChannel> channels = new HashMap<>();
    AgentMessageQueue agentMessageQueue;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public ChannelManager(Config config, AgentMessageQueue agentMessageQueue) {
        this.agentMessageQueue = agentMessageQueue;
        // 校验并获取有效的 channel 配置
        List<BaseChannelConfig> validConfigs = config.validateChannels();
        if (validConfigs.isEmpty()) {
            throw new IllegalArgumentException("没有有效的 channel 配置");
        }

        // 根据 channel 类型创建对应的实例
        for (BaseChannelConfig channelConfig : validConfigs) {
            if (channelConfig.validate()) {
                ChannelType channelType = channelConfig.getChannelType();
                Class<? extends BaseChannel> channelClazz = channelType.getChannelClass();
                BaseChannel channel = null;
                try {
                    channel = channelClazz.getDeclaredConstructor(channelConfig.getClass(), AgentMessageQueue.class)
                            .newInstance(channelConfig, agentMessageQueue);
                    channels.put(channelType, channel);
                } catch (Exception e) {
                    log.error("Failed to create channel instance", e);
                }
            }
        }
    }

    public void start() {
        // 启动所有 channel
        channels.values().forEach(channel -> executor.submit(channel::start));
        // 启动回复
        executor.submit(this::dispatchOutbound);
    }

    public void stop() {
        running = false;
        for (BaseChannel ch : channels.values()) {
            ch.stop();
        }
        executor.shutdown();
    }

    /**
     * 从队列取请求
     *
     * @return
     * @throws InterruptedException
     */
    public AgentRequest takeRequest() throws InterruptedException {
        return agentMessageQueue.takeRequest();
    }

    /**
     * 将响应放入队列
     *
     * @param agentResponse
     * @throws InterruptedException
     */
    public void putResponse(AgentResponse agentResponse) throws InterruptedException {
        agentMessageQueue.putResponse(agentResponse);
    }

    /**
     * 从 bus 取 outbound 分发给已注册 channelType
     */
    private void dispatchOutbound() {
        while (running && agentMessageQueue.isRunning()) {
            try {
                AgentResponse agentResponse = agentMessageQueue.pollResponse(2, TimeUnit.SECONDS);
                if (agentResponse == null) {
                    continue;
                }
                BaseChannel channel = channels.get(agentResponse.channelType());
                if (channel != null) {
                    channel.send(agentResponse);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }


}
