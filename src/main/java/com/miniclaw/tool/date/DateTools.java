package com.miniclaw.tool.date;

import io.agentscope.core.tool.Tool;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类
 * 供 Agent 调用，用于获取当前时间
 * @author lei
 */
public class DateTools {


    /**
     * 获取当前时间
     * @return 格式化后的当前时间字符串
     */
    @Tool(description = "获取当前时间,返回格式化后的当前时间字符串,格式:yyyy-MM-dd HH:mm:ss:SSS")
    public String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");
        return now.format(formatter);
    }
}
