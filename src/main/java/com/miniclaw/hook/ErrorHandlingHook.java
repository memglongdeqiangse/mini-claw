package com.miniclaw.hook;

/**
 * @description:
 * @author: zhanglei
 * @date: 2026/3/13
 */
import io.agentscope.core.hook.ErrorEvent;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
@Slf4j
public class ErrorHandlingHook implements Hook {

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {

        if (event instanceof ErrorEvent e) {
            log.error("智能体错误: {}", e.getAgent().getName());
            log.error("错误消息: {}", e.getError().getMessage());
            return Mono.just(event);
        }

        return Mono.just(event);
    }
}
