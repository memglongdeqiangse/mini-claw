package com.miniclaw.channel.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miniclaw.bus.AgentMessageQueue;
import com.miniclaw.channel.BaseChannel;
import com.miniclaw.channel.ChannelType;
import com.miniclaw.channel.config.WebChannelConfig;
import com.miniclaw.channel.schema.AgentResponse;
import io.agentscope.core.message.Msg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.RouterFunctions;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Web 渠道实现
 * 提供 HTTP REST API 和 SSE (Server-Sent Events) 推送
 *
 * @author zhanglei
 * @date 2026/4/15
 */
@Slf4j
public class WebChannel extends BaseChannel {

    private final WebChannelConfig webChannelConfig;
    private final SseSessionManager sseSessionManager;
    private final ObjectMapper objectMapper;
    private DisposableServer server;

    public WebChannel(WebChannelConfig config, AgentMessageQueue agentMessageQueue) {
        super(ChannelType.WEB, config, agentMessageQueue);
        this.webChannelConfig = config;
        this.sseSessionManager = new SseSessionManager(config.getSseHeartbeatInterval());
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 是否允许用户发送消息
     *
     * @param userId 用户ID
     * @return 是否允许
     */
    @Override
    public boolean isAllowed(String userId) {
        // 如果白名单为空，允许所有用户
        if (allowFrom == null || allowFrom.isEmpty()) {
            return true;
        }
        return allowFrom.contains(userId);
    }

    /**
     * 发送响应给用户
     * 通过 SSE 推送消息到前端
     *
     * @param response 响应
     * @return 异步结果
     */
    @Override
    public CompletableFuture<Void> send(AgentResponse response) {
        return CompletableFuture.runAsync(() -> {
            String sessionId = response.sessionId();
            Msg output = response.output();

            if (output == null) {
                log.warn("响应消息为空: sessionId={}", sessionId);
                return;
            }

            try {
                // 构建响应消息
                Map<String, Object> responseMessage = Map.of(
                        "sessionId", sessionId,
                        "userId", response.userId(),
                        "content", output.getTextContent(),
                        "channelType", response.channelType().getChannelName()
                );

                String jsonMessage = objectMapper.writeValueAsString(responseMessage);

                // 通过 SSE 推送
                boolean sent = sseSessionManager.send(sessionId, jsonMessage);
                if (!sent) {
                    log.warn("消息推送失败，会话可能已断开: sessionId={}", sessionId);
                }
            } catch (Exception e) {
                log.error("发送响应失败: sessionId={}", sessionId, e);
            }
        });
    }

    /**
     * 启动 Web 服务器
     */
    @Override
    public void start() {
        log.info("Starting WebChannel on port {}...", webChannelConfig.getPort());

        try {
            // 创建 HTTP Handler
            WebChannelHandler handler = new WebChannelHandler(this, sseSessionManager, webChannelConfig.getBasePath());
            HttpHandler httpHandler = RouterFunctions.toHttpHandler(handler.buildRouter());

            // 创建 Reactor Netty 服务器
            ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);
            HttpServer httpServer = HttpServer.create()
                    .port(webChannelConfig.getPort())
                    .handle(adapter);

            // 启动服务器（非阻塞），bindNow 返回 DisposableServer
            server = httpServer.bindNow();

            log.info("WebChannel started successfully on port {}", webChannelConfig.getPort());
            log.info("Web UI: http://localhost:{}/", webChannelConfig.getPort());
            log.info("API endpoints:");
            log.info("  POST http://localhost:{}{}/chat", webChannelConfig.getPort(), webChannelConfig.getBasePath());
            log.info("  POST http://localhost:{}{}/chat/stream", webChannelConfig.getPort(), webChannelConfig.getBasePath());
            log.info("  GET  http://localhost:{}{}/sse/{{sessionId}}", webChannelConfig.getPort(), webChannelConfig.getBasePath());
            log.info("  GET  http://localhost:{}{}/health", webChannelConfig.getPort(), webChannelConfig.getBasePath());

        } catch (Exception e) {
            log.error("Failed to start WebChannel", e);
            throw new RuntimeException("Failed to start WebChannel", e);
        }
    }

    /**
     * 停止 Web 服务器
     */
    @Override
    public void stop() {
        log.info("Stopping WebChannel...");

        if (server != null) {
            server.dispose();
            log.info("WebChannel stopped.");
        }
    }
}
