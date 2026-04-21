package com.miniclaw.runner;

import com.miniclaw.agent.AgentUtil;
import com.miniclaw.agent.PromptManager;
import com.miniclaw.agent.UserContext;
import com.miniclaw.bus.AgentMessageQueue;
import com.miniclaw.channel.ChannelType;
import com.miniclaw.channel.config.ChannelManager;
import com.miniclaw.channel.config.FeiShuChannelConfig;
import com.miniclaw.channel.schema.AgentRequest;
import com.miniclaw.channel.schema.AgentResponse;
import com.miniclaw.config.CustomerAgentConfig;
import com.miniclaw.cron.schedule.service.SchedulerService;
import com.miniclaw.hook.LoggingHook;
import com.miniclaw.config.Config;
import com.miniclaw.skill.SkillManager;
import com.miniclaw.tool.ToolManager;
import com.miniclaw.config.ConfigLoader;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.autocontext.AutoContextConfig;
import io.agentscope.core.memory.autocontext.AutoContextMemory;
import io.agentscope.core.message.*;
import io.agentscope.core.model.Model;
import io.agentscope.core.session.JsonSession;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.ToolExecutionContext;
import io.agentscope.core.tool.Toolkit;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Agent 运行器
 * 负责初始化 Agent、渠道、调度服务，并处理请求循环
 *
 * @author lei
 * @date 2026/3/20
 */
@Slf4j
public class AgentRunner implements Runnable {

    @Override
    public void run() {
        // 加载配置
        Config config = ConfigLoader.loadConfig();
        String workspace = config.getWorkspace();
        // 消息队列
        AgentMessageQueue agentMessageQueue = new AgentMessageQueue();
        // 渠道管理器
        ChannelManager channelManager = new ChannelManager(config, agentMessageQueue);
        channelManager.start();
        // 初始化调度服务
        SchedulerService schedulerService = new SchedulerService(agentMessageQueue);
        log.info("调度服务已启动");
        // 注册工具
        Toolkit mainToolkit = new Toolkit();
        // 注册基础工具
        ToolManager.registerBasicTool(mainToolkit, workspace);
        // 注册定时任务工具
        ToolManager.registerCornTool(mainToolkit, schedulerService);
        // 创建model
        Model model = ConfigLoader.getChatModel(config);

        // 获取自定义agent
        List<CustomerAgentConfig> agentConfigList = config.getAgent();
        if (agentConfigList != null && !agentConfigList.isEmpty()) {
            //注册自定义agent
            for (CustomerAgentConfig agentConfig : agentConfigList) {
                AgentUtil.registerCustomerAgent(mainToolkit, agentConfig, model);
            }
        }
        // 飞书渠道配置
        FeiShuChannelConfig feiShuChannelConfig = (FeiShuChannelConfig) config.getChannels().get(ChannelType.FEISHU.getChannelName());
        if (feiShuChannelConfig == null) {
            log.info("飞书渠道配置不存在，跳过");
        }
        // 自动上下文配置
        AutoContextConfig autoContextConfig = AutoContextConfig.builder()
                .msgThreshold(50)
                .maxToken(64 * 1024)
                .lastKeep(20)
                .build();
        try {
            while (true) {
                // 从队列中获取请求
                AgentRequest agentRequest = channelManager.takeRequest();
                processRequest(agentRequest, model, mainToolkit, autoContextConfig, config, channelManager);
            }

        } catch (InterruptedException e) {
            log.info("Agent 运行器被中断");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Agent 运行错误", e);
        } finally {
            // 停止调度服务
            channelManager.stop();
        }
    }

    private static void processRequest(AgentRequest agentRequest, Model model, Toolkit mainToolkit, AutoContextConfig autoContextConfig, Config config, ChannelManager channelManager) throws InterruptedException {
        String sessionId = agentRequest.sessionId();
        String userId = agentRequest.userId();
        ChannelType channelType = agentRequest.channelType();
        Map<String, Object> channelMeta = agentRequest.channelMeta();
        List<String> skillPath = config.getSkillPath();
        SkillBox skillBox = null;
        if (skillPath != null && !skillPath.isEmpty()) {
            skillBox = SkillManager.registerSkills(skillPath);
        }
        ReActAgent mainAgent = ReActAgent.builder()
                .name("Assistant")
                .sysPrompt(PromptManager.buildSystemPrompt())
                .model(model)
                .toolkit(mainToolkit)
                .toolExecutionContext(ToolExecutionContext.builder().register(new UserContext(userId, sessionId, channelType, channelMeta)).build())
                .skillBox(skillBox)
                .hooks(List.of(new LoggingHook()))
                .memory(new AutoContextMemory(autoContextConfig, model))
                .build();

        // 加载或创建会话
        // 创建会话存储
        Path sessionDirectory = Path.of(config.getWorkspace() + "/sessions/" + sessionId);
        if (!sessionDirectory.toFile().exists()) {
            sessionDirectory.toFile().mkdirs();
        }
        JsonSession session = new JsonSession(sessionDirectory);
        if (mainAgent.loadIfExists(session, sessionId)) {
            log.info("已加载会话: {}", sessionId);
        } else {
            log.info("新建会话: {}", sessionId);
        }
        // 调用智能体
        List<Msg> input = agentRequest.input();
        Msg output = mainAgent.call(input).block();
        // 保存会话
        mainAgent.saveTo(session, sessionId);
        // 创建响应并放入队列
        AgentResponse agentResponse = new AgentResponse(
                sessionId, userId, output, channelType, channelMeta
        );
        channelManager.putResponse(agentResponse);
    }

}