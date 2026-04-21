package com.miniclaw.cron.schedule.hook;

import com.miniclaw.agent.UserContext;
import com.miniclaw.bus.AgentMessageQueue;
import com.miniclaw.channel.schema.AgentResponse;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostCallEvent;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * @description: 将结果交付给用户hook
 * @author: zhanglei
 * @date: 2026/4/12
 */
@Slf4j
public class DeliverToUserHook implements Hook {
    /**
     * queue
     */
    private AgentMessageQueue agentMessageQueue;
    /**
     * user context
     */
    private UserContext userContext;
    /**
     * 是否交付给用户
     */
    private boolean deliverToUser;

    public DeliverToUserHook(AgentMessageQueue agentMessageQueue, UserContext userContext, boolean deliverToUser) {
        this.agentMessageQueue = agentMessageQueue;
        this.userContext = userContext;
        this.deliverToUser = deliverToUser;
    }

    /**
     * Handle a hook event.
     *
     * @param event The hook event
     * @return Mono containing the potentially modified event
     */
    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (deliverToUser) {
            if (event instanceof PostCallEvent postCallEvent) {
                log.info("智能体完成: {}", postCallEvent.getFinalMessage().getTextContent());
                AgentResponse agentResponse = new AgentResponse(userContext.getSessionId(),
                        userContext.getUserId(), postCallEvent.getFinalMessage(),
                        userContext.getChannelType(),  userContext.getMetadata());
                try {
                    agentMessageQueue.putResponse(agentResponse);
                } catch (Exception e) {
                    log.error("put response error", e);
                }
            }
        }
        return Mono.just(event);
    }
}
