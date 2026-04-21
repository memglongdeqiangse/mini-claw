package com.miniclaw.config;

import lombok.Data;

import java.util.List;

/**
 * @description:
 * @author: zhanglei
 * @date: 2026/4/13
 */
@Data
public class CustomerAgentConfig {
    private String description;
    private String name;
    private String prompt;
    private List<String> tools;
    private List<String> skillPath;
}
