package com.github.sonarqube.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 增量掃描器
 *
 * 整合 Git diff，僅分析變更的檔案，大幅減少掃描時間。
 * 支援多種比較模式（工作目錄、暫存區、分支、提交）。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.4.0 (Epic 6, Story 6.6)
 */
public class IncrementalScanner {

    private static final Logger logger = LoggerFactory.getLogger(IncrementalScanner.class);

    // Git diff 檔案路徑正則表達式
    private static final Pattern DIFF_FILE_PATTERN = Pattern.compile("^diff --git a/(.*) b/(.*)$");
    private static final Pattern FILE_STATUS_PATTERN = Pattern.compile("^([ACDMRT])\\s+(.*)$");

    private final Path repositoryRoot;

    /**
     * 建構子
     *
     * @param repositoryRoot Git 倉庫根目錄
     */
    public IncrementalScanner(Path repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
    }

    /**
     * 取得工作目錄中變更的檔案（未暫存的變更）
     *
     * @return 變更檔案路徑列表
     */
    public List<Path> getModifiedFilesInWorkingDirectory() {
        return executeGitCommand("git diff --name-only HEAD");
    }

    /**
     * 取得暫存區中變更的檔案（已暫存但未提交）
     *
     * @return 變更檔案路徑列表
     */
    public List<Path> getModifiedFilesInStagingArea() {
        return executeGitCommand("git diff --name-only --cached");
    }

    /**
     * 取得兩個提交之間變更的檔案
     *
     * @param fromCommit 起始提交 hash（例：HEAD~1）
     * @param toCommit 結束提交 hash（例：HEAD）
     * @return 變更檔案路徑列表
     */
    public List<Path> getModifiedFilesBetweenCommits(String fromCommit, String toCommit) {
        String command = String.format("git diff --name-only %s %s", fromCommit, toCommit);
        return executeGitCommand(command);
    }

    /**
     * 取得與指定分支的差異檔案
     *
     * @param branchName 分支名稱（例：main, develop）
     * @return 變更檔案路徑列表
     */
    public List<Path> getModifiedFilesAgainstBranch(String branchName) {
        String command = String.format("git diff --name-only %s", branchName);
        return executeGitCommand(command);
    }

    /**
     * 取得自指定提交以來的變更檔案
     *
     * @param sinceCommit 起始提交 hash
     * @return 變更檔案路徑列表
     */
    public List<Path> getModifiedFilesSince(String sinceCommit) {
        String command = String.format("git diff --name-only %s..HEAD", sinceCommit);
        return executeGitCommand(command);
    }

    /**
     * 取得檔案變更狀態（新增、修改、刪除等）
     *
     * @return 檔案變更狀態列表
     */
    public List<FileChangeStatus> getFileChangeStatuses() {
        String command = "git status --porcelain";
        List<String> output = executeGitCommandRaw(command);

        return output.stream()
            .map(this::parseFileStatus)
            .filter(status -> status != null)
            .collect(Collectors.toList());
    }

    /**
     * 取得指定檔案的變更統計
     *
     * @param filePath 檔案路徑
     * @return 變更統計（新增行數、刪除行數）
     */
    public FileChangeStatistics getFileChangeStatistics(Path filePath) {
        String command = String.format("git diff --numstat HEAD -- %s", filePath);
        List<String> output = executeGitCommandRaw(command);

        if (output.isEmpty()) {
            return new FileChangeStatistics(filePath, 0, 0);
        }

        String[] parts = output.get(0).split("\\s+");
        if (parts.length >= 2) {
            int addedLines = parseIntSafe(parts[0]);
            int deletedLines = parseIntSafe(parts[1]);
            return new FileChangeStatistics(filePath, addedLines, deletedLines);
        }

        return new FileChangeStatistics(filePath, 0, 0);
    }

    /**
     * 檢查是否為 Git 倉庫
     *
     * @return true 若為 Git 倉庫
     */
    public boolean isGitRepository() {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "--is-inside-work-tree");
            pb.directory(repositoryRoot.toFile());
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            logger.debug("Not a Git repository: {}", repositoryRoot);
            return false;
        }
    }

    /**
     * 執行 Git 指令並返回檔案路徑列表
     *
     * @param command Git 指令
     * @return 檔案路徑列表
     */
    private List<Path> executeGitCommand(String command) {
        List<String> output = executeGitCommandRaw(command);

        return output.stream()
            .map(line -> repositoryRoot.resolve(line))
            .collect(Collectors.toList());
    }

    /**
     * 執行 Git 指令並返回原始輸出
     *
     * @param command Git 指令
     * @return 輸出行列表
     */
    private List<String> executeGitCommandRaw(String command) {
        try {
            logger.debug("Executing Git command: {}", command);

            ProcessBuilder pb = new ProcessBuilder(command.split("\\s+"));
            pb.directory(repositoryRoot.toFile());
            Process process = pb.start();

            List<String> output = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line.trim());
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                logger.error("Git command failed with exit code {}: {}", exitCode, command);
                return Collections.emptyList();
            }

            logger.debug("Git command output: {} lines", output.size());
            return output;

        } catch (IOException | InterruptedException e) {
            logger.error("Failed to execute Git command: {}", command, e);
            return Collections.emptyList();
        }
    }

    /**
     * 解析檔案狀態行（git status --porcelain）
     *
     * @param line 狀態行
     * @return 檔案變更狀態
     */
    private FileChangeStatus parseFileStatus(String line) {
        if (line.length() < 3) {
            return null;
        }

        String statusCode = line.substring(0, 2).trim();
        String filePath = line.substring(3).trim();

        FileChangeType changeType = parseChangeType(statusCode);
        return new FileChangeStatus(repositoryRoot.resolve(filePath), changeType, statusCode);
    }

    /**
     * 解析變更類型
     *
     * @param statusCode Git 狀態碼
     * @return 變更類型
     */
    private FileChangeType parseChangeType(String statusCode) {
        if (statusCode.startsWith("A")) return FileChangeType.ADDED;
        if (statusCode.startsWith("M")) return FileChangeType.MODIFIED;
        if (statusCode.startsWith("D")) return FileChangeType.DELETED;
        if (statusCode.startsWith("R")) return FileChangeType.RENAMED;
        if (statusCode.startsWith("C")) return FileChangeType.COPIED;
        if (statusCode.startsWith("U")) return FileChangeType.UNMERGED;
        if (statusCode.equals("??")) return FileChangeType.UNTRACKED;
        return FileChangeType.UNKNOWN;
    }

    /**
     * 安全解析整數
     *
     * @param str 字串
     * @return 整數，若解析失敗則返回 0
     */
    private int parseIntSafe(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 檔案變更類型
     */
    public enum FileChangeType {
        ADDED,      // 新增
        MODIFIED,   // 修改
        DELETED,    // 刪除
        RENAMED,    // 重新命名
        COPIED,     // 複製
        UNMERGED,   // 未合併
        UNTRACKED,  // 未追蹤
        UNKNOWN     // 未知
    }

    /**
     * 檔案變更狀態
     */
    public static class FileChangeStatus {
        private final Path filePath;
        private final FileChangeType changeType;
        private final String statusCode;

        public FileChangeStatus(Path filePath, FileChangeType changeType, String statusCode) {
            this.filePath = filePath;
            this.changeType = changeType;
            this.statusCode = statusCode;
        }

        public Path getFilePath() {
            return filePath;
        }

        public FileChangeType getChangeType() {
            return changeType;
        }

        public String getStatusCode() {
            return statusCode;
        }

        @Override
        public String toString() {
            return "FileChangeStatus{" +
                "file=" + filePath +
                ", type=" + changeType +
                ", status='" + statusCode + '\'' +
                '}';
        }
    }

    /**
     * 檔案變更統計
     */
    public static class FileChangeStatistics {
        private final Path filePath;
        private final int addedLines;
        private final int deletedLines;

        public FileChangeStatistics(Path filePath, int addedLines, int deletedLines) {
            this.filePath = filePath;
            this.addedLines = addedLines;
            this.deletedLines = deletedLines;
        }

        public Path getFilePath() {
            return filePath;
        }

        public int getAddedLines() {
            return addedLines;
        }

        public int getDeletedLines() {
            return deletedLines;
        }

        public int getTotalChangedLines() {
            return addedLines + deletedLines;
        }

        @Override
        public String toString() {
            return "FileChangeStatistics{" +
                "file=" + filePath +
                ", added=" + addedLines +
                ", deleted=" + deletedLines +
                ", total=" + getTotalChangedLines() +
                '}';
        }
    }
}
