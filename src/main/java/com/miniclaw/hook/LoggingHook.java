package com.miniclaw.hook;

import io.agentscope.core.hook.*;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * @description: 日志钩子
 * @author: zhanglei
 * @date: 2026/3/13
 */
@Slf4j
public class LoggingHook implements Hook {

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {

        // 看到发往 LLM 的完整消息
        if (event instanceof PreReasoningEvent e) {
            log.info("=== 发给 LLM 的消息 ===");
            e.getInputMessages().forEach(msg -> {
                log.info("role:{},content:{}", msg.getRole(), msg.getTextContent());
            });
        }

        // 看到 LLM 返回的内容
        if (event instanceof PostReasoningEvent e) {
            log.info("=== LLM 返回 ===");
            log.info("{}", e.getReasoningMessage().getTextContent());
        }

        // 看到工具调用
        if (event instanceof PreActingEvent e) {
            log.info("=== 调用工具 ===");
            log.info("{}({})", e.getToolUse().getName(), e.getToolUse().getInput());
        }
        return Mono.just(event);
    }
}
