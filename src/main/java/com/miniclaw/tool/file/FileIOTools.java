package com.miniclaw.tool.file;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件IO工具类
 * 对应copaw的file_io.py功能
 */
public class FileIOTools {

    private final String workspace;

    public FileIOTools(String workspace) {
        this.workspace = workspace;
    }

    /**
     * 读取文件内容
     *
     * @param filePath 文件路径（相对路径从WORKING_DIR解析）
     * @param startLine 起始行号（1-based，可选）
     * @param endLine 结束行号（1-based，可选）
     * @return 文件内容
     */
    @Tool(description = "Read a file. Relative paths resolve from WORKING_DIR. " +
                       "Use start_line/end_line to read a specific line range. " +
                       "Omit both to read the full file.")
    public String readFile(
            @ToolParam(name = "file_path", description = "Path to the file") String filePath,
            @ToolParam(name = "start_line", description = "First line to read (1-based, inclusive)", required = false) Integer startLine,
            @ToolParam(name = "end_line", description = "Last line to read (1-based, inclusive)", required = false) Integer endLine) {

        if (filePath == null || filePath.isEmpty()) {
            return "Error: No file_path provided.";
        }

        Path resolvedPath = resolveFilePath(filePath);

        if (!Files.exists(resolvedPath)) {
            return "Error: The file " + resolvedPath + " does not exist.";
        }

        if (!Files.isRegularFile(resolvedPath)) {
            return "Error: The path " + resolvedPath + " is not a file.";
        }

        try {
            String content = Files.readString(resolvedPath, StandardCharsets.UTF_8);
            String[] allLines = content.split("\n", -1);
            int total = allLines.length;

            int s = (startLine != null) ? Math.max(1, startLine) : 1;
            int e = (endLine != null) ? Math.min(total, endLine) : total;

            if (s > total) {
                return "Error: start_line " + s + " exceeds file length (" + total + " lines).";
            }

            if (s > e) {
                return "Error: start_line (" + s + ") > end_line (" + e + ").";
            }

            StringBuilder result = new StringBuilder();
            result.append(filePath).append(" (lines ").append(s).append("-").append(e).append(" of ").append(total).append(")\n");

            for (int i = s - 1; i < e; i++) {
                result.append(i + 1).append(": ").append(allLines[i]).append("\n");
            }

            if (e < total) {
                result.append("\n[").append(total - e).append(" more lines. Use start_line=").append(e + 1).append(" to continue.]");
            }

            return result.toString();

        } catch (IOException e) {
            return "Error: Read file failed due to " + e.getMessage();
        }
    }

    /**
     * 写入文件
     *
     * @param filePath 文件路径
     * @param content 文件内容
     * @return 操作结果
     */
    @Tool(description = "Create or overwrite a file. Relative paths resolve from WORKING_DIR.")
    public String writeFile(
            @ToolParam(name = "file_path", description = "Path to the file") String filePath,
            @ToolParam(name = "content", description = "Content to write") String content) {

        if (filePath == null || filePath.isEmpty()) {
            return "Error: No file_path provided.";
        }

        Path resolvedPath = resolveFilePath(filePath);

        try {
            Files.createDirectories(resolvedPath.getParent());
            Files.writeString(resolvedPath, content, StandardCharsets.UTF_8);
            return "Wrote " + content.getBytes(StandardCharsets.UTF_8).length + " bytes to " + filePath + ".";
        } catch (IOException e) {
            return "Error: Write file failed due to " + e.getMessage();
        }
    }

    /**
     * 编辑文件（查找替换）
     *
     * @param filePath 文件路径
     * @param oldText 要查找的文本
     * @param newText 替换文本
     * @return 操作结果
     */
    @Tool(description = "Find-and-replace text in a file. All occurrences of old_text are " +
                       "replaced with new_text. Relative paths resolve from WORKING_DIR.")
    public String editFile(
            @ToolParam(name = "file_path", description = "Path to the file") String filePath,
            @ToolParam(name = "old_text", description = "Exact text to find") String oldText,
            @ToolParam(name = "new_text", description = "Replacement text") String newText) {

        if (filePath == null || filePath.isEmpty()) {
            return "Error: No file_path provided.";
        }

        if (oldText == null || oldText.isEmpty()) {
            return "Error: No old_text provided.";
        }

        Path resolvedPath = resolveFilePath(filePath);

        if (!Files.exists(resolvedPath)) {
            return "Error: The file " + resolvedPath + " does not exist.";
        }

        if (!Files.isRegularFile(resolvedPath)) {
            return "Error: The path " + resolvedPath + " is not a file.";
        }

        try {
            String content = Files.readString(resolvedPath, StandardCharsets.UTF_8);

            if (!content.contains(oldText)) {
                return "Error: The text to replace was not found in " + filePath + ".";
            }

            String newContent = content.replace(oldText, newText);
            Files.writeString(resolvedPath, newContent, StandardCharsets.UTF_8);

            int count = countOccurrences(content, oldText);
            return "Successfully replaced " + count + " occurrence(s) in " + filePath + ".";

        } catch (IOException e) {
            return "Error: Edit file failed due to " + e.getMessage();
        }
    }

    /**
     * 追加内容到文件
     *
     * @param filePath 文件路径
     * @param content 要追加的内容
     * @return 操作结果
     */
    @Tool(description = "Append content to the end of a file. Relative paths resolve from WORKING_DIR.")
    public String appendFile(
            @ToolParam(name = "file_path", description = "Path to the file") String filePath,
            @ToolParam(name = "content", description = "Content to append") String content) {

        if (filePath == null || filePath.isEmpty()) {
            return "Error: No file_path provided.";
        }

        if (content == null) {
            content = "";
        }

        Path resolvedPath = resolveFilePath(filePath);

        try {
            if (!Files.exists(resolvedPath)) {
                Files.createDirectories(resolvedPath.getParent());
                Files.createFile(resolvedPath);
            }

            Files.writeString(resolvedPath, content, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            return "Appended " + content.getBytes(StandardCharsets.UTF_8).length + " bytes to " + filePath + ".";

        } catch (IOException e) {
            return "Error: Append file failed due to " + e.getMessage();
        }
    }

    /**
     * 解析文件路径：绝对路径直接使用，相对路径从WORKING_DIR解析
     */
    private Path resolveFilePath(String filePath) {
        Path path = Paths.get(filePath);
        if (path.isAbsolute()) {
            return path;
        } else {
            return Paths.get(workspace, filePath);
        }
    }

    /**
     * 统计字符串出现次数
     */
    private int countOccurrences(String content, String target) {
        int count = 0;
        int index = 0;
        while ((index = content.indexOf(target, index)) != -1) {
            count++;
            index += target.length();
        }
        return count;
    }
}
