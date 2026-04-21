package com.miniclaw.channel.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 会话管理器
 * 管理用户与服务端的 SSE 连接，支持向指定会话推送消息
 *
 * @author zhanglei
 * @date 2026/4/15
 */
@Slf4j
public class SseSessionManager {

    /**
     * 会话ID -> SSE Sink 映射
     */
    private final Map<String, Many<String>> sessionSinks = new ConcurrentHashMap<>();

    /**
     * 心跳间隔（秒）
     */
    private final int heartbeatInterval;

    public SseSessionManager(int heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    /**
     * 注册一个 SSE 会话，返回消息流
     *
     * @param sessionId 会话ID
     * @return SSE 消息流
     */
    public Flux<ServerSentEvent<String>> register(String sessionId) {
        Many<String> sink = sessionSinks.computeIfAbsent(sessionId, id -> {
            // 使用 multicast 支持多个订阅者
            Many<String> newSink = Sinks.many().multicast().onBackpressureBuffer();
            log.info("SSE 会话已注册: {}", id);
            return newSink;
        });

        // 连接建立后立即发送一个 ready 事件
        ServerSentEvent<String> readyEvent = ServerSentEvent.<String>builder()
                .event("ready")
                .data("connected")
                .build();

        // 构建心跳消息
        Flux<ServerSentEvent<String>> heartbeat = Flux.interval(Duration.ofSeconds(heartbeatInterval))
                .map(seq -> ServerSentEvent.<String>builder()
                        .event("heartbeat")
                        .data("ping")
                        .build());

        // 合并：ready 事件 + 实际消息 + 心跳
        return Flux.just(readyEvent)
                .concatWith(sink.asFlux()
                        .map(data -> ServerSentEvent.<String>builder()
                                .event("message")
                                .data(data)
                                .build()))
                .mergeWith(heartbeat)
                .doOnCancel(() -> {
                    log.info("SSE 会话已断开: {}", sessionId);
                    unregister(sessionId);
                })
                .doOnTerminate(() -> unregister(sessionId));
    }

    /**
     * 取消注册 SSE 会话
     *
     * @param sessionId 会话ID
     */
    public void unregister(String sessionId) {
        Many<String> sink = sessionSinks.remove(sessionId);
        if (sink != null) {
            sink.tryEmitComplete();
            log.info("SSE 会话已注销: {}", sessionId);
        }
    }

    /**
     * 向指定会话发送消息
     *
     * @param sessionId 会话ID
     * @param message   消息内容
     * @return 是否发送成功
     */
    public boolean send(String sessionId, String message) {
        Many<String> sink = sessionSinks.get(sessionId);
        if (sink == null) {
            log.warn("会话不存在，无法发送消息: {}", sessionId);
            return false;
        }
        Sinks.EmitResult result = sink.tryEmitNext(message);
        if (result.isFailure()) {
            log.warn("发送消息失败: sessionId={}, result={}", sessionId, result);
            return false;
        }
        log.debug("消息已发送: sessionId={}", sessionId);
        return true;
    }

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话ID
     * @return 是否存在
     */
    public boolean hasSession(String sessionId) {
        return sessionSinks.containsKey(sessionId);
    }

    /**
     * 获取活跃会话数量
     *
     * @return 活跃会话数量
     */
    public int getActiveSessionCount() {
        return sessionSinks.size();
    }
}
