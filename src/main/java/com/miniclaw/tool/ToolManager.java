package com.miniclaw.tool;

import com.miniclaw.cron.schedule.service.SchedulerService;
import com.miniclaw.tool.cron.CornTools;
import com.miniclaw.tool.date.DateTools;
import com.miniclaw.tool.file.FileIOTools;
import com.miniclaw.tool.file.FileSearchTools;
import com.miniclaw.tool.shell.ShellTools;
import io.agentscope.core.tool.Toolkit;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ToolManager {
    private static ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 注册基础工具
     *
     * @param toolkit   toolkit
     * @param workspace 工作目录
     */
    public static void registerBasicTool(Toolkit toolkit, String workspace) {
        toolkit.registerTool(new FileIOTools(workspace));
        toolkit.registerTool(new FileSearchTools(workspace));
        toolkit.registerTool(new ShellTools(workspace));
        toolkit.registerTool(new DateTools());
    }

    /**
     * 注册定时任务工具
     * @param toolkit
     * @param schedulerService
     */
    public static void registerCornTool(Toolkit toolkit, SchedulerService schedulerService) {
        toolkit.registerTool(new CornTools(schedulerService,executor));
    }

}