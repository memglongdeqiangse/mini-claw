package com.miniclaw.channel.config;

import lombok.Data;

/**
 * @description: 标准输入输出渠道配置
 * @author: lei
 * @date: 2026/3/20
 */
@Data
public class StdChannelConfig extends BaseChannelConfig {

    @Override
    public boolean validate() {
        // StdChannel 无特殊必填配置，启用即有效
        return true;
    }
}
