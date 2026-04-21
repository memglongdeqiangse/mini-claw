package com.miniclaw.agent;

import com.miniclaw.config.CustomerAgentConfig;
import com.miniclaw.hook.LoggingHook;
import com.miniclaw.skill.SkillManager;
import com.miniclaw.tool.ToolManager;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.model.Model;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.subagent.SubAgentConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @description:
 * @author: zhanglei
 * @date: 2026/4/15
 */
@Slf4j
public class AgentUtil {
    public static void registerCustomerAgent(Toolkit mainToolkit, CustomerAgentConfig customerAgentConfig, Model model) {
        String description = customerAgentConfig.getDescription();
        String prompt = customerAgentConfig.getPrompt();
        String name = customerAgentConfig.getName();
        List<String> skillPathList = customerAgentConfig.getSkillPath();
        //加载飞书skill
        SkillBox skillBox = null;
        if (skillPathList != null || !skillPathList.isEmpty()) {
            skillBox = SkillManager.registerSkills(skillPathList);
        }
        Toolkit toolkit = new Toolkit();
        ToolManager.registerBasicTool(toolkit, null);
        try {
            ReActAgent customerAgent = ReActAgent.builder()
                    .name(name)
                    .sysPrompt(prompt)
                    .description(description)
                    .model(model)
                    .toolkit(toolkit)
                    .skillBox(skillBox)
                    .hooks(List.of(new LoggingHook()))
                    .memory(new InMemoryMemory())
                    .build();
            SubAgentConfig subAgentConfig = SubAgentConfig.builder()
                    .toolName(name)                    // 自定义工具名称
                    .description(description)              // 自定义描述
                    .forwardEvents(true)                       // 转发子智能体事件
                    .build();
            mainToolkit.registration()
                    .subAgent(() -> customerAgent, subAgentConfig).apply();
        } catch (Exception e) {
            log.warn("sub agent{}启动失败", name, e);
        }
    }
}