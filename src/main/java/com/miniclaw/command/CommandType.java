package com.miniclaw.command;

import com.miniclaw.channel.schema.AgentRequest;

import java.util.function.Function;

/**
 * @description:
 * @author: zhanglei
 * @date: 2026/3/23
 */
public enum CommandType {
    HELP("/help", "显示帮助", (agentRequest) -> {
        return "显示帮助";
    }),
    COMPACT("/compact", "压缩", (agentRequest) -> {
        return "compact：TODO";
    }),
    NEW("/new", "开始新对话", (agentRequest) -> {
        return "new";
    }),
    CLEAR("/clear", "清空会话内容", (agentRequest) -> {
        return "clear";
    });

    private final String command;
    private final String description;
    private final Function<AgentRequest,String> handler;

    CommandType(String command, String description, Function<AgentRequest,String> handler) {
        this.command = command;
        this.description = description;
        this.handler = handler;
    }

    /**
     * 根据文本获取命令类型
     * @param text
     * @return
     */
    public static CommandType fromText(String text) {
        if (text == null) return null;
        for (CommandType commandType : CommandType.values()) {
            if (commandType.command.equals(text)) {
                return commandType;
            }
        }
        return null;
    }


    public String getCommand() {
        return command;
    }

    public String getDescription() {
        return description;
    }

    public Function<AgentRequest,String> getHandler() {
        return handler;
    }

    /**
     * 判断是否为系统命令
     */
    public static boolean isCommand(String content) {
        if (content == null) return false;
        String lowerCase = content.split(" ")[0].toLowerCase();
        return content.startsWith("/") && CommandType.fromText(lowerCase) != null;
    }

}
