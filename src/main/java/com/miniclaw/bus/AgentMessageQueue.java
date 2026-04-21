package com.miniclaw.bus;

import com.miniclaw.channel.schema.AgentRequest;
import com.miniclaw.channel.schema.AgentResponse;
import lombok.Data;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @description:
 * @author: lei
 * @date: 2026/3/20
 */
@Data
public class AgentMessageQueue {

    private final BlockingQueue<AgentRequest> requestQueue;
    private final BlockingQueue<AgentResponse> responseQueue;
    private AtomicBoolean running = new AtomicBoolean(true);

    public AgentMessageQueue() {
        this.requestQueue = new LinkedBlockingQueue<>();
        this.responseQueue = new LinkedBlockingQueue<>();
    }

    /**
     * 渠道调用：将用户消息入队
     */
    public boolean offerRequest(AgentRequest msg) {
        if (msg != null) {
            return requestQueue.offer(msg);
        }
        return false;
    }

    /**
     * Agent 调用：阻塞获取请求
     */
    public AgentRequest takeRequest() throws InterruptedException {
        return requestQueue.take();
    }

    /**
     * agent 调用：将响应入队
     */
    public void putResponse(AgentResponse msg) throws InterruptedException {
        if (msg != null) {
            responseQueue.put(msg);
        }
    }

    /**
     * 阻塞取一条出站消息
     * 队列为空：立刻返回 null
     * 不为空：返回队首
     */
    public AgentResponse takeResponse() throws InterruptedException {
        return responseQueue.take();
    }

    /**
     * 空队列：最多等指定时间
     * 时间到还空：返回 null
     */
    public AgentResponse pollResponse(long timeout, TimeUnit unit) throws InterruptedException {
        return responseQueue.poll(timeout, unit);
    }

    /**
     * 停止 dispatch 循环（设置 running 标志）
     */
    public void stop() {
        running.set(false);
    }

    public boolean isRunning() {
        return running.get();
    }


}
