package com.miniclaw.channel.stdio;

import com.miniclaw.bus.AgentMessageQueue;
import com.miniclaw.channel.BaseChannel;
import com.miniclaw.channel.ChannelType;
import com.miniclaw.channel.config.BaseChannelConfig;
import com.miniclaw.channel.schema.AgentResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

/**
 * @description:
 * @author: lei
 * @date: 2026/3/20
 */
@Slf4j
public class StdChannel extends BaseChannel {
    public StdChannel(BaseChannelConfig config, AgentMessageQueue agentMessageQueue) {
        super(ChannelType.STD, config, agentMessageQueue);
    }


    /**
     * 是否允许用户发送消息
     *
     * @param userId
     * @return
     */
    @Override
    public boolean isAllowed(String userId) {
        return true;
    }


    /**
     * 不同渠道发送给用户文本 (抽象方法，子类实现)
     *
     * @param response 响应
     */
    @Override
    public CompletableFuture<Void> send(AgentResponse response) {
        System.out.println("Sending text: " + response.output().getTextContent());
        return null;
    }

    Scanner scanner;

    /**
     * 启动
     */
    @Override
    public void start() {
        // 模拟用户输入
        // 模拟用户ID和会话ID
        String userId = "user01";
        String sessionId = "session1";
        // 创建Scanner对象用于用户输入
        scanner = new Scanner(System.in);
        System.out.print("请输入：");
        while (true) {
            String input = scanner.nextLine();
            try {
                this.process(userId, sessionId, input,null);
            } catch (InterruptedException e) {
                log.error("Error processing input:", e);
            }
        }
    }

    /**
     * 停止
     */
    @Override
    public void stop() {
        scanner.close();
    }

}
