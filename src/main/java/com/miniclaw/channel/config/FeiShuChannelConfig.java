package com.miniclaw.channel.config;

/**
 * @description:
 * @author: zhanglei
 * @date: 2026/3/19
 */
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * 飞书渠道配置
 * @author lei
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeiShuChannelConfig extends BaseChannelConfig {

    /**
     * 飞书 机器人 App ID
     */
    private String appId;

    /**
     * 飞书 机器人客户端密钥
     */
    private String appSecret;
    /**
     * 飞书 机器人 openid用于群里被@
     */
    private String botOpenId;

    /**
     * 是否启用 Markdown 渲染
     */
    private boolean markdownEnabled = true;

    @Override
    public boolean validate() {
        if (!isEnabled()) {
            return false;
        }
        // 必须提供 appId 和 appSecret
        return appId != null && !appId.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }

}
