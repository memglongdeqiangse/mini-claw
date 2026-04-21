package com.miniclaw.tool.file;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件搜索工具类
 * 对应copaw的file_search.py功能
 * 提供grep内容搜索和glob文件发现功能
 */
public class FileSearchTools {

    private static final int MAX_MATCHES = 200;
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;
    private static final int MAX_DEPTH = 20;
    private static final long TIMEOUT_MS = 30000;

    // 二进制文件扩展名集合
    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".ico", ".webp", ".svg",
            ".mp3", ".mp4", ".avi", ".mov", ".mkv", ".flac", ".wav",
            ".zip", ".tar", ".gz", ".bz2", ".7z", ".rar",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".exe", ".dll", ".so", ".dylib", ".bin", ".dat",
            ".woff", ".woff2", ".ttf", ".eot", ".otf",
            ".pyc", ".pyo", ".class", ".o", ".a"
    );

    private final String workspace;

    public FileSearchTools(String workspace) {
        this.workspace = workspace;
    }

    /**
     * 检查正则表达式是否安全，防止ReDoS攻击
     * 检测嵌套的重复量词（如 (a+)+, (a*)* 等）
     */
    private boolean isSafeRegex(String pattern) {
        int nestingDepth = 0;
        int maxNestingDepth = 0;
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '(') {
                nestingDepth++;
                maxNestingDepth = Math.max(maxNestingDepth, nestingDepth);
            } else if (c == ')') {
                nestingDepth--;
            }
            if (nestingDepth >= 2 && (c == '*' || c == '+' || c == '?')) {
                return false;
            }
        }
        return maxNestingDepth <= 3;
    }

    /**
     * Grep搜索：在文件内容中搜索匹配模式的行
     *
     * @param pattern 搜索模式（字符串或正则表达式）
     * @param path 搜索路径（文件或目录，可选，默认为WORKING_DIR）
     * @param isRegex 是否将pattern视为正则表达式
     * @param caseSensitive 是否区分大小写
     * @param contextLines 匹配行前后显示的上下文行数
     * @return 搜索结果
     */
    @Tool(description = "Search file contents by pattern, recursively. Relative paths resolve from WORKING_DIR. " +
                       "Output format: path:line_number: content. " +
                       "Use is_regex=true for regex patterns. Use context_lines for surrounding context.")
    public String grepSearch(
            @ToolParam(name = "pattern", description = "Search string (or regex when is_regex=true)") String pattern,
            @ToolParam(name = "path", description = "File or directory to search in (optional, defaults to WORKING_DIR)", required = false) String path,
            @ToolParam(name = "is_regex", description = "Treat pattern as a regular expression", required = false) Boolean isRegex,
            @ToolParam(name = "case_sensitive", description = "Case-sensitive matching", required = false) Boolean caseSensitive,
            @ToolParam(name = "context_lines", description = "Context lines before and after each match (like grep -C)", required = false) Integer contextLines) {

        if (pattern == null || pattern.isEmpty()) {
            return "Error: No search pattern provided.";
        }

        // 设置默认值
        boolean useRegex = isRegex != null ? isRegex : false;
        boolean caseSense = caseSensitive != null ? caseSensitive : true;
        int ctxLines = contextLines != null ? contextLines : 0;

        Path searchRoot = path != null ? resolveFilePath(path) : Paths.get(workspace);

        if (!Files.exists(searchRoot)) {
            return "Error: The path " + searchRoot + " does not exist.";
        }

        // 编译正则表达式
        Pattern regex;
        try {
            int flags = caseSense ? 0 : Pattern.CASE_INSENSITIVE;
            String regexPattern = useRegex ? pattern : Pattern.quote(pattern);
            if (useRegex && !isSafeRegex(regexPattern)) {
                return "Error: Regex pattern too complex (nested quantifiers not allowed)";
            }
            regex = Pattern.compile(regexPattern, flags);
        } catch (PatternSyntaxException e) {
            return "Error: Invalid regex pattern — " + e.getMessage();
        }

        List<String> matches = new ArrayList<>();
        List<String> skippedFiles = new ArrayList<>();
        boolean truncated = false;
        long startTime = System.currentTimeMillis();

        try {
            // 收集要搜索的文件
            List<Path> filesToSearch;
            boolean singleFile = Files.isRegularFile(searchRoot);

            if (singleFile) {
                filesToSearch = Collections.singletonList(searchRoot);
            } else {
                filesToSearch = collectTextFiles(searchRoot);
            }

            for (Path filePath : filesToSearch) {
                if (truncated) {
                    break;
                }

                if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
                    return "Error: Search timeout after " + TIMEOUT_MS + "ms";
                }

                List<String> lines = new ArrayList<>();
                try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        lines.add(line);
                    }
                } catch (IOException e) {
                    skippedFiles.add(filePath.getFileName().toString());
                    continue;
                }

                for (int lineNo = 1; lineNo <= lines.size(); lineNo++) {
                    String line = lines.get(lineNo - 1);
                    if (regex.matcher(line).find()) {
                        if (matches.size() >= MAX_MATCHES) {
                            truncated = true;
                            break;
                        }

                        int start = Math.max(1, lineNo - ctxLines);
                        int end = Math.min(lines.size(), lineNo + ctxLines);
                        String relPath = singleFile ? filePath.getFileName().toString() : getRelativePath(filePath, searchRoot);

                        for (int ctxIdx = start; ctxIdx <= end; ctxIdx++) {
                            String prefix = (ctxIdx == lineNo) ? ">" : " ";
                            matches.add(relPath + ":" + ctxIdx + ":" + prefix + " " + lines.get(ctxIdx - 1));
                        }

                        if (ctxLines > 0) {
                            matches.add("---");
                        }
                    }
                }
            }
        } catch (IOException e) {
            return "Error: Search failed due to " + e.getMessage();
        }

        if (matches.isEmpty()) {
            return "No matches found for pattern: " + pattern;
        }

        StringBuilder result = new StringBuilder();
        for (String match : matches) {
            result.append(match).append("\n");
        }

        if (truncated) {
            result.append("\n(Results truncated at ").append(MAX_MATCHES).append(" matches.)");
        }

        if (!skippedFiles.isEmpty()) {
            result.append("\n(Warning: Skipped ").append(skippedFiles.size()).append(" unreadable files)");
        }

        return result.toString().trim();
    }

    /**
     * Glob搜索：根据glob模式查找文件
     *
     * @param pattern Glob模式（如 "*.java", "**\/*.xml"）
     * @param path 搜索根目录（可选，默认为WORKING_DIR）
     * @return 匹配的文件列表
     */
    @Tool(description = "Find files matching a glob pattern (e.g. '*.java', '**/*.xml'). " +
                       "Relative paths resolve from WORKING_DIR. " +
                       "Returns a list of matching file paths.")
    public String globSearch(
            @ToolParam(name = "pattern", description = "Glob pattern to match (e.g. '*.java', '**/*.xml')") String pattern,
            @ToolParam(name = "path", description = "Root directory to search from (optional, defaults to WORKING_DIR)", required = false) String path) {

        if (pattern == null || pattern.isEmpty()) {
            return "Error: No glob pattern provided.";
        }

        Path searchRoot = path != null ? resolveFilePath(path) : Paths.get(workspace);

        if (!Files.exists(searchRoot)) {
            return "Error: The path " + searchRoot + " does not exist.";
        }

        if (!Files.isDirectory(searchRoot)) {
            return "Error: The path " + searchRoot + " is not a directory.";
        }

        try {
            List<String> results = new ArrayList<>();
            boolean truncated = false;

            // 转换glob模式为PathMatcher语法
            String globPattern = pattern;
            if (!pattern.startsWith("glob:")) {
                globPattern = "glob:" + pattern;
            }

            PathMatcher matcher = FileSystems.getDefault().getPathMatcher(globPattern);

            // 递归搜索匹配的文件
            try (Stream<Path> stream = Files.walk(searchRoot, MAX_DEPTH)) {
                List<Path> matchedPaths = stream
                        .filter(p -> !Files.isSymbolicLink(p))
                        .filter(p -> matcher.matches(searchRoot.relativize(p)))
                        .sorted()
                        .collect(Collectors.toList());

                for (Path entry : matchedPaths) {
                    String rel = getRelativePath(entry, searchRoot);
                    String suffix = Files.isDirectory(entry) ? "/" : "";
                    results.add(rel + suffix);

                    if (results.size() >= MAX_MATCHES) {
                        truncated = true;
                        break;
                    }
                }
            }

            if (results.isEmpty()) {
                return "No files matched pattern: " + pattern;
            }

            StringBuilder text = new StringBuilder();
            for (String result : results) {
                text.append(result).append("\n");
            }

            if (truncated) {
                text.append("\n(Results truncated at ").append(MAX_MATCHES).append(" entries.)");
            }

            return text.toString().trim();

        } catch (IllegalArgumentException e) {
            return "Error: Invalid glob pattern — " + e.getMessage();
        } catch (IOException e) {
            return "Error: Search failed — " + e.getMessage();
        } catch (Exception e) {
            return "Error: Search failed. Please try again.";
        }
    }

    /**
     * 收集所有文本文件（递归）
     */
    private List<Path> collectTextFiles(Path root) throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root, MAX_DEPTH)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> !Files.isSymbolicLink(p))
                  .filter(this::isTextFile)
                  .forEach(files::add);
        }
        Collections.sort(files);
        return files;
    }

    /**
     * 判断是否为文本文件（启发式检查）
     */
    private boolean isTextFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            String ext = fileName.substring(dotIndex);
            if (BINARY_EXTENSIONS.contains(ext)) {
                return false;
            }
        }

        try {
            if (Files.size(path) > MAX_FILE_SIZE) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * 获取相对路径
     */
    private String getRelativePath(Path target, Path root) {
        try {
            return root.relativize(target).toString();
        } catch (Exception e) {
            return target.toString();
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
}
