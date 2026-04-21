package com.miniclaw.tool.cron;

import com.miniclaw.agent.UserContext;
import com.miniclaw.cron.schedule.service.SchedulerService;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * 定时任务工具，支持添加、列出、删除定时任务。
 * 参考 nanobot 的 CronTool 实现。
 *
 * @author zhanglei
 * @date 2026/4/12
 */
@Slf4j
public class CornTools {
    private final SchedulerService schedulerService;
    private final Executor executor;

    public CornTools(SchedulerService schedulerService, ExecutorService executor) {
        this.schedulerService = schedulerService;
        this.executor = executor;
    }

    @Tool(description = "管理定时任务。支持三种操作：add（添加任务）、list（列出任务）、remove（删除任务）。" +
            "可创建三种类型的定时任务：周期性任务（every_seconds）、cron表达式任务（cron_expr）、一次性任务（at）。" +
            "若 tz 未指定，cron 表达式和一次性任务将使用系统默认时区。")
    public String cron(
            @ToolParam(name = "action", description = "操作类型：add（添加任务）、list（列出任务）、remove（删除任务）")
            String action,
            @ToolParam(name = "message", description = "任务执行时给 Agent 的指令，例如 '给用户发送提醒：xxx' 或 '检查系统状态并报告'", required = false)
            String message,
            @ToolParam(name = "every_seconds", description = "周期性任务的间隔秒数，例如 3600，表示每小时执行一次", required = false)
            Integer everySeconds,
            @ToolParam(name = "cron_expr", description = "cron 表达式，格式：秒 分 时 日 月 周。例如 '0 30 9 * * ?' 表示每天9:30", required = false)
            String cronExpr,
            @ToolParam(name = "at", description = "一次性任务的执行时间，yyyy-MM-dd HH:mm:ss 格式如 '2026-04-12 10:30:00'", required = false)
            String at,
            @ToolParam(name = "delay_seconds", description = "一次性任务的延迟执行秒数，例如 3600，表示3600秒后执行", required = false)
            int delaySeconds,
            @ToolParam(name = "deliver", description = "是否将执行结果投递给用户，默认 true", required = false)
            Boolean deliver,
            @ToolParam(name = "job_id", description = "任务ID（用于 remove 操作）", required = false)
            String jobId,
            UserContext userContext) {
        executor.execute(() -> doCron(action, message, everySeconds, cronExpr, at, delaySeconds, deliver, jobId, userContext));
        return "Cron task submitted for execution";
    }

    private String doCron(String action, String message, Integer everySeconds, String cronExpr, String at, int delaySeconds, Boolean deliver, String jobId, UserContext userContext) {
        log.info("action: {}, message: {}, everySeconds: {}, cronExpr: {}, at: {}, delaySeconds: {}, deliver: {}, jobId: {}",
                action, message, everySeconds, cronExpr, at, delaySeconds, deliver, jobId);
        if (action == null || action.isBlank()) {
            return "Error: action is required";
        }
        if (userContext == null) {
            return "Error: no session context (channel/chat_id)";
        }
        boolean doDeliver = deliver == null || deliver;

        switch (action.toLowerCase()) {
            case "add":
                return schedulerService.addJob(userContext, message, everySeconds, cronExpr, at, delaySeconds, doDeliver);
            case "list":
                return schedulerService.listJobs(userContext.getUserId());
            case "remove":
                return schedulerService.removeJob(jobId);
            default:
                return "Unknown action: " + action;
        }
    }
}
