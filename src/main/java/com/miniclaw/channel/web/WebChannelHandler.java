package com.miniclaw.channel.web;

import com.miniclaw.channel.BaseChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Web Channel HTTP Handler
 * 处理 HTTP 请求和 SSE 推送
 *
 * @author zhanglei
 * @date 2026/4/15
 */
@Slf4j
public class WebChannelHandler {

    private final BaseChannel channel;
    private final SseSessionManager sseSessionManager;
    private final String basePath;

    public WebChannelHandler(BaseChannel channel, SseSessionManager sseSessionManager, String basePath) {
        this.channel = channel;
        this.sseSessionManager = sseSessionManager;
        this.basePath = basePath;
    }

    /**
     * 构建路由
     */
    public RouterFunction<ServerResponse> buildRouter() {
        return RouterFunctions.route()
                // 静态资源 - 首页
                .GET("/", this::handleIndex)
                .GET("/index.html", this::handleIndex)
                // API 接口
                .POST(basePath + "/chat", this::handleChat)
                .POST(basePath + "/chat/stream", this::handleChatStream)
                .GET(basePath + "/sse/{sessionId}", this::handleSse)
                .GET(basePath + "/health", this::handleHealth)
                // 静态资源目录
                .resources("/static/**", new ClassPathResource("static/"))
                .build();
    }

    /**
     * 处理首页请求
     */
    private Mono<ServerResponse> handleIndex(ServerRequest request) {
        ClassPathResource indexResource = new ClassPathResource("static/index.html");
        if (indexResource.exists()) {
            return DataBufferUtils.read(
                            indexResource,
                            new DefaultDataBufferFactory(true),
                            4096)
                    .collectList()
                    .flatMap(buffers -> {
                        DefaultDataBufferFactory factory = new DefaultDataBufferFactory(true);
                        DataBuffer joined = factory.join(buffers);
                        return ServerResponse.ok()
                                .contentType(MediaType.TEXT_HTML)
                                .bodyValue(joined);
                    });
        }
        return ServerResponse.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("error", "页面未找到"));
    }

    /**
     * 处理普通聊天请求
     * POST /api/chat
     */
    private Mono<ServerResponse> handleChat(ServerRequest request) {
        return request.bodyToMono(ChatRequest.class)
                .flatMap(chatRequest -> {
                    String userId = chatRequest.userId() != null ? chatRequest.userId() : "anonymous";
                    String sessionId = chatRequest.sessionId() != null ? chatRequest.sessionId() : UUID.randomUUID().toString();
                    String text = chatRequest.message();

                    if (text == null || text.isBlank()) {
                        return ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("error", "消息不能为空"));
                    }

                    // 检查用户权限
                    if (!channel.isAllowed(userId)) {
                        return ServerResponse.status(HttpStatus.FORBIDDEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("error", "您没有权限使用此服务"));
                    }

                    try {
                        // 处理消息
                        channel.process(userId, sessionId, text, null);
                        return ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of(
                                        "success", true,
                                        "sessionId", sessionId
                                ));
                    } catch (InterruptedException e) {
                        log.error("处理消息失败", e);
                        Thread.currentThread().interrupt();
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("error", "服务器内部错误"));
                    }
                })
                .onErrorResume(e -> {
                    log.error("处理聊天请求异常", e);
                    return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(Map.of("error", "服务器内部错误: " + e.getMessage()));
                });
    }

    /**
     * 处理流式聊天请求
     * POST /api/chat/stream
     */
    private Mono<ServerResponse> handleChatStream(ServerRequest request) {
        return request.bodyToMono(ChatRequest.class)
                .flatMap(chatRequest -> {
                    String userId = chatRequest.userId() != null ? chatRequest.userId() : "anonymous";
                    String sessionId = chatRequest.sessionId() != null ? chatRequest.sessionId() : UUID.randomUUID().toString();
                    String text = chatRequest.message();

                    if (text == null || text.isBlank()) {
                        return ServerResponse.badRequest()
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("error", "消息不能为空"));
                    }

                    // 检查用户权限
                    if (!channel.isAllowed(userId)) {
                        return ServerResponse.status(HttpStatus.FORBIDDEN)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("error", "您没有权限使用此服务"));
                    }

                    try {
                        // 处理消息
                        channel.process(userId, sessionId, text, Map.of("stream", true));

                        // 返回 SSE 流
                        Flux<ServerSentEvent<String>> sseFlux = sseSessionManager.register(sessionId);
                        return ServerResponse.ok()
                                .contentType(MediaType.TEXT_EVENT_STREAM)
                                .body(sseFlux, ServerSentEvent.class);
                    } catch (InterruptedException e) {
                        log.error("处理消息失败", e);
                        Thread.currentThread().interrupt();
                        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(Map.of("error", "服务器内部错误"));
                    }
                });
    }

    /**
     * 处理 SSE 连接
     * GET /api/sse/{sessionId}
     */
    private Mono<ServerResponse> handleSse(ServerRequest request) {
        String sessionId = request.pathVariable("sessionId");

        Flux<ServerSentEvent<String>> sseFlux = sseSessionManager.register(sessionId);
        return ServerResponse.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(sseFlux, ServerSentEvent.class);
    }

    /**
     * 健康检查
     * GET /api/health
     */
    private Mono<ServerResponse> handleHealth(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "status", "UP",
                        "activeSessions", sseSessionManager.getActiveSessionCount()
                ));
    }

    /**
     * 聊天请求体
     */
    public record ChatRequest(String userId, String sessionId, String message) {}
}
