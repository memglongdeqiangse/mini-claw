package com.miniclaw.channel.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Web渠道配置
 *
 * @author zhanglei
 * @date 2026/4/11
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WebChannelConfig extends BaseChannelConfig {

    /**
     * 服务端口
     */
    private int port = 8080;

    /**
     * API 基础路径前缀
     */
    private String basePath = "/api";

    /**
     * 是否启用 CORS
     */
    private boolean enableCors = true;

    /**
     * SSE 心跳间隔（秒）
     */
    private int sseHeartbeatInterval = 30;


    @Override
    public boolean validate() {
        if (port <= 0 || port > 65535) {
            return false;
        }
        if (basePath == null || basePath.isBlank()) {
            return false;
        }
        return true;
    }
}
