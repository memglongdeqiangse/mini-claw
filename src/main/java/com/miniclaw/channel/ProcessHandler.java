package com.miniclaw.channel;

import com.miniclaw.channel.schema.AgentRequest;

import java.util.concurrent.CompletableFuture;

/**
 * ProcessHandler - 消息处理器函数式接口
 * 处理 AgentRequest 并通过回调返回事件流
 */
@FunctionalInterface
public interface ProcessHandler {

    /**
     * 处理请求，返回事件流
     *
     * @param request Agent 请求
     * @return 完成时返回的 Future
     */
    CompletableFuture<Void> process(AgentRequest request);
}