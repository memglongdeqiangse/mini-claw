package com.miniclaw.tool.shell;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Shell命令执行工具类
 * 对应copaw的shell.py功能
 * 提供执行shell命令的能力
 */
public class ShellTools {

    private static final int DEFAULT_TIMEOUT = 60;
    private static final int MAX_OUTPUT_LINES = 1000;
    private static final int MAX_OUTPUT_BYTES = 30 * 1024; // 30KB
    private static final int THREAD_JOIN_TIMEOUT_SHORT_MS = 500;
    private static final int THREAD_JOIN_TIMEOUT_LONG_MS = 1000;

    private final String workspace;
    private final ExecutorService executor;

    public ShellTools(String workspace) {
        this.workspace = workspace;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 执行shell命令
     *
     * @param command 要执行的shell命令
     * @param timeout 超时时间（秒），默认60秒
     * @return 执行结果，包含返回码、标准输出和标准错误
     */
    @Tool(description = "Execute a shell command and return the output. " +
                       "The command runs in the workspace directory. " +
                       "Use timeout parameter to set max execution time in seconds.")
    public String executeShellCommand(
            @ToolParam(name = "command", description = "The shell command to execute") String command,
            @ToolParam(name = "timeout", description = "Maximum execution time in seconds (default 60)", required = false) Integer timeout) {

        if (command == null || command.trim().isEmpty()) {
            return "Error: No command provided.";
        }

        int timeoutSeconds = timeout != null ? timeout : DEFAULT_TIMEOUT;

        try {
            ProcessBuilder processBuilder = buildProcess(command);
            processBuilder.redirectErrorStream(false);

            Process process = processBuilder.start();

            // 读取输出
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            // 使用ExecutorService提交读取任务，便于管理和关闭
            Future<?> stdoutFuture = executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (stdout.length() > 0) {
                            stdout.append("\n");
                        }
                        stdout.append(line);
                    }
                } catch (IOException e) {
                    // 进程已终止或流已关闭，读取时可能出现异常
                    // 这是预期的行为，因为进程可能已经结束，无需特殊处理
                }
            });

            Future<?> stderrFuture = executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream(), Charset.defaultCharset()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (stderr.length() > 0) {
                            stderr.append("\n");
                        }
                        stderr.append(line);
                    }
                } catch (IOException e) {
                    // 进程已终止或流已关闭，读取时可能出现异常
                    // 这是预期的行为，因为进程可能已经结束，无需特殊处理
                }
            });

            // 等待进程完成或超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                // 超时处理：强制终止进程并取消读取任务
                process.destroyForcibly();
                process.waitFor(1, TimeUnit.SECONDS);

                // 取消读取任务
                stdoutFuture.cancel(true);
                stderrFuture.cancel(true);

                String truncatedStdout = truncateOutput(stdout.toString());
                String truncatedStderr = truncateOutput(stderr.toString());

                StringBuilder result = new StringBuilder();
                result.append("Command execution exceeded the timeout of ").append(timeoutSeconds).append(" seconds.\n");
                if (truncatedStdout.length() > 0) {
                    result.append("\n[stdout]\n").append(truncatedStdout);
                }
                if (truncatedStderr.length() > 0) {
                    result.append("\n[stderr]\n").append(truncatedStderr);
                }
                return result.toString();
            }

            // 等待读取任务完成
            try {
                stdoutFuture.get(THREAD_JOIN_TIMEOUT_LONG_MS, TimeUnit.MILLISECONDS);
                stderrFuture.get(THREAD_JOIN_TIMEOUT_LONG_MS, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                // 读取任务执行异常，但不影响主流程
            }

            int exitCode = process.exitValue();
            String stdoutStr = truncateOutput(stdout.toString());
            String stderrStr = truncateOutput(stderr.toString());

            // 格式化输出
            if (exitCode == 0) {
                if (stdoutStr.length() > 0) {
                    return stdoutStr;
                } else {
                    return "Command executed successfully (no output).";
                }
            } else {
                StringBuilder result = new StringBuilder();
                result.append("Command failed with exit code ").append(exitCode).append(".");
                if (stdoutStr.length() > 0) {
                    result.append("\n\n[stdout]\n").append(stdoutStr);
                }
                if (stderrStr.length() > 0) {
                    result.append("\n\n[stderr]\n").append(stderrStr);
                }
                return result.toString();
            }

        } catch (IOException e) {
            return "Error: Shell command execution failed due to " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error: Shell command execution was interrupted.";
        }
    }

    /**
     * 构建进程构建器
     */
    private ProcessBuilder buildProcess(String command) {
        String osName = System.getProperty("os.name").toLowerCase();
        ProcessBuilder processBuilder;

        if (osName.contains("win")) {
            // Windows系统
            processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            // Unix/Linux/Mac系统
            processBuilder = new ProcessBuilder("/bin/sh", "-c", command);
        }

        processBuilder.directory(new java.io.File(workspace));
        return processBuilder;
    }

    /**
     * 截断输出，保留尾部内容
     * 与Python版本的truncate_shell_output功能一致
     */
    private String truncateOutput(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= MAX_OUTPUT_BYTES) {
            String[] lines = text.split("\n", -1);
            if (lines.length <= MAX_OUTPUT_LINES) {
                return text;
            }
        }

        String[] lines = text.split("\n", -1);
        int totalLines = lines.length;
        Deque<String> keptLines = new ArrayDeque<>();
        int currentBytes = 0;

        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i];
            int lineBytes = line.getBytes(StandardCharsets.UTF_8).length + 1;

            if (keptLines.size() >= MAX_OUTPUT_LINES || currentBytes + lineBytes > MAX_OUTPUT_BYTES) {
                break;
            }

            keptLines.addFirst(line);
            currentBytes += lineBytes;
        }

        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (String line : keptLines) {
            if (!first) {
                result.append("\n");
            }
            result.append(line);
            first = false;
        }

        int startLine = totalLines - keptLines.size() + 1;
        result.append("\n\n[Output truncated: showing lines ")
              .append(startLine).append("-").append(totalLines)
              .append(" of ").append(totalLines).append(" total]");

        return result.toString();
    }

    public void shutdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(THREAD_JOIN_TIMEOUT_SHORT_MS, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}