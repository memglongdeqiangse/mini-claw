package com.miniclaw.cron.schedule.service;

import com.miniclaw.agent.UserContext;
import com.miniclaw.bus.AgentMessageQueue;
import com.miniclaw.config.Config;
import com.miniclaw.config.ConfigLoader;
import com.miniclaw.cron.schedule.hook.DeliverToUserHook;
import com.miniclaw.provider.ApiType;
import com.miniclaw.provider.CommonModelConfig;
import com.miniclaw.provider.Provider;
import io.agentscope.core.model.AnthropicChatModel;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.scheduler.BaseScheduleAgentTask;
import io.agentscope.extensions.scheduler.ScheduleAgentTask;
import io.agentscope.extensions.scheduler.config.ModelConfig;
import io.agentscope.extensions.scheduler.config.RuntimeAgentConfig;
import io.agentscope.extensions.scheduler.config.ScheduleConfig;
import io.agentscope.extensions.scheduler.config.ScheduleMode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 定时任务调度服务。
 *
 * <p>基于 agentscope-extensions-scheduler 的基础设施实现，
 * 参考 nanobot 的 cron.py 设计。
 *
 * <p>支持三种类型的定时任务：
 * <ul>
 *   <li>周期性任务（every_seconds）- 每隔 N 秒执行</li>
 *   <li>Cron 表达式任务（cron_expr）- 按照 cron 表达式执行</li>
 *   <li>一次性任务（at）- 在指定时间执行一次</li>
 * </ul>
 *
 * @author lei
 */
@Slf4j
public class SchedulerService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final com.miniclaw.cron.schedule.QuartzAgentScheduler scheduler;
    private final ModelConfig modelConfig;
    private final AgentMessageQueue agentMessageQueue;

    public SchedulerService(AgentMessageQueue agentMessageQueue) {
        this.agentMessageQueue = agentMessageQueue;
        this.scheduler = com.miniclaw.cron.schedule.QuartzAgentScheduler.builder().autoStart(true).build();

        // 加载配置
        Config config = ConfigLoader.loadConfig();
        Provider provider = config.getProvider();
        String type = provider.getApiType();
        String baseUrl = provider.getBaseUrl();
        String apiKey = provider.getApiKey();
        String modelName = provider.getModelName();
        Class<? extends Model> modelClass = OpenAIChatModel.class;
        if (ApiType.ANTHROPIC.getTypeName().equals(type)) {
            modelClass = AnthropicChatModel.class;
        }
        this.modelConfig = CommonModelConfig.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelClass(modelClass)
                .modelName(modelName)
                .build();

    }

    /**
     * 添加一个定时任务。
     *
     * @param userContext  用户上下文
     * @param message      任务执行时给 Agent 的指令
     * @param everySeconds 周期性任务的间隔秒数
     * @param cronExpr     cron 表达式
     * @param at           一次性任务的执行时间（格式：yyyy-MM-dd HH:mm:ss）
     * @param deliver      是否将执行结果投递给用户
     * @return 添加任务的响应信息
     */
    public String addJob(
            UserContext userContext,
            String message,
            Integer everySeconds,
            String cronExpr,
            String at,
            int delaySeconds,
            boolean deliver) {

        if (message == null || message.isBlank()) {
            return "Error: message is required for add";
        }

        try {
            ScheduleConfig scheduleConfig;
            if (everySeconds != null && everySeconds > 0) {
                // 周期性任务
                scheduleConfig = ScheduleConfig.builder().fixedRate(everySeconds * 1000L).build();
            } else if (cronExpr != null && !cronExpr.isBlank()) {
                // Cron 表达式任务
                scheduleConfig = ScheduleConfig.builder().cron(cronExpr).build();
            } else if (at != null && !at.isBlank()) {
                // 一次性任务
                // 转毫秒时间戳
                long atMs = LocalDateTime.parse(at, FORMATTER)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                // 一次性任务，算出延迟时间
                long delay = atMs - System.currentTimeMillis();
                scheduleConfig = ScheduleConfig.builder().fixedDelay(delay).build();
            } else if (delaySeconds >= 0) {
                scheduleConfig = ScheduleConfig.builder().initialDelay(delaySeconds * 1000L).build();
            } else {
                return "Error: either every_seconds, cron_expr, or at is required";
            }


            // 创建任务载荷
            // 直接使用 AgentScheduler 以支持 hooks

            Toolkit toolkit = new Toolkit();
            toolkit.registration().presetParameters(Map.of("userContext", Map.of("userContext", userContext)));

            RuntimeAgentConfig agentConfig = RuntimeAgentConfig.builder()
                    .name(userContext.getUserId() + ":" + userContext.getChannelType().getChannelName() + ":" + userContext.getSessionId() + ":" + System.currentTimeMillis())
                    .modelConfig(modelConfig)
                    .toolkit(toolkit)
                    .hooks(List.of(new DeliverToUserHook(agentMessageQueue, userContext, deliver)))
                    .sysPrompt("你是一个助手，需要根据用户指令执行任务。")
                    .build();

            ScheduleAgentTask task = scheduler.schedule(agentConfig, scheduleConfig, message);
            return String.format("Created job '%s' (id: %s)", task.getId(), task.getName(), message);
        } catch (Exception e) {
            log.error("Failed to add job", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * 列出所有已调度的任务。
     *
     * @param userId 用户ID（用于过滤用户相关的任务，目前暂未使用）
     * @return 任务列表信息
     */
    public String listJobs(String userId) {
        List<ScheduleAgentTask> tasks = scheduler.getAllScheduleAgentTasks();
        if (tasks.isEmpty()) {
            return "No scheduled jobs.";
        }

        StringBuilder sb = new StringBuilder("Scheduled jobs:\n");
        for (ScheduleAgentTask task : tasks) {
            if (!task.getName().startsWith(userId)) {
                continue;
            }
            String timing = formatTaskTiming(task);
            sb.append(String.format("- %s (id: %s, %s)\n", task.getName(), task.getId(), timing));
        }
        return sb.toString();
    }

    /**
     * 删除一个已调度的任务。
     *
     * @param jobId 任务ID或名称
     * @return 删除任务的响应信息
     */
    public String removeJob(String jobId) {
        if (jobId == null || jobId.isBlank()) {
            return "Error: job_id is required for remove";
        }
        boolean cancelled = scheduler.cancel(jobId);
        if (cancelled) {
            log.info("Removed job '{}'", jobId);
            return String.format("Removed job %s", jobId);
        } else {
            return String.format("Job %s not found", jobId);
        }
    }

    /**
     * 关闭调度服务。
     */
    public void shutdown() {
        log.info("SchedulerService shutdown completed");
    }


    private String formatEveryTiming(int seconds) {
        if (seconds >= 3600 && seconds % 3600 == 0) {
            return "每 " + (seconds / 3600) + "h";
        } else if (seconds >= 60 && seconds % 60 == 0) {
            return "每 " + (seconds / 60) + "m";
        } else {
            return "每 " + seconds + "s";
        }
    }

    private String formatTaskTiming(ScheduleAgentTask task) {
        if (task instanceof BaseScheduleAgentTask baseTask) {
            ScheduleConfig config = baseTask.getScheduleConfig();
            if (config == null) {
                return "unknown schedule";
            }

            ScheduleMode mode = config.getScheduleMode();
            switch (mode) {
                case CRON:
                    return "cron: " + config.getCronExpression();
                case FIXED_RATE:
                    long rateMs = config.getFixedRate();
                    return formatEveryTiming((int) (rateMs / 1000));
                case FIXED_DELAY:
                    long delayMs = config.getFixedDelay();
                    return (delayMs / 1000) + "s (delay)";
                case NONE:
                default:
                    if (config.getInitialDelay() != null) {
                        long initialDelay = config.getInitialDelay();
                        String atTime = Instant.ofEpochMilli(System.currentTimeMillis() + initialDelay)
                                .atZone(ZoneId.systemDefault())
                                .format(FORMATTER);
                        return "at: " + atTime;
                    }
                    return "manual";
            }
        }
        return "scheduled";
    }
}
