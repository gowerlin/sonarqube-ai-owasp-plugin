package com.github.sonarqube.plugin.incremental;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 增量掃描管理器
 *
 * 整合 Git 功能，偵測專案變更檔案，僅掃描修改過的檔案，
 * 大幅降低掃描時間和 AI API 成本。
 *
 * 功能特性：
 * - Git diff 整合（支援 working directory, staged, commit 比較）
 * - 變更檔案偵測（新增、修改、刪除）
 * - 檔案類型過濾（僅掃描程式碼檔案）
 * - 基準比較（與特定 commit/branch 比較）
 * - 統計資訊（變更檔案數、掃描節省比例）
 *
 * 效能提升：
 * - 典型變更：5-10% 檔案修改
 * - 掃描時間節省：90-95%
 * - AI 成本節省：90-95%
 * - 適用場景：CI/CD 流程、PR 驗證、增量開發
 *
 * @since 2.9.0 (Epic 6, Story 6.6)
 * @author SonarQube AI OWASP Plugin Team
 */
public class IncrementalScanManager {

    private static final Logger LOG = LoggerFactory.getLogger(IncrementalScanManager.class);

    // Singleton instance
    private static IncrementalScanManager instance;

    // Git 可執行檔路徑
    private String gitExecutable = "git";

    // 支援的程式碼檔案副檔名
    private static final Set<String> CODE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".java", ".js", ".ts", ".jsx", ".tsx", ".py", ".go", ".rb", ".php",
            ".c", ".cpp", ".h", ".cs", ".kt", ".scala", ".swift", ".m", ".mm"
    ));

    /**
     * 私有建構子（Singleton 模式）
     */
    private IncrementalScanManager() {
        LOG.info("IncrementalScanManager 已初始化");
    }

    /**
     * 取得 Singleton 實例
     */
    public static synchronized IncrementalScanManager getInstance() {
        if (instance == null) {
            instance = new IncrementalScanManager();
        }
        return instance;
    }

    /**
     * 取得變更的檔案列表（與 HEAD 比較）
     *
     * @param projectPath 專案根目錄
     * @return 變更檔案的絕對路徑列表
     */
    public List<String> getChangedFiles(String projectPath) {
        return getChangedFiles(projectPath, "HEAD");
    }

    /**
     * 取得變更的檔案列表（與指定基準比較）
     *
     * @param projectPath 專案根目錄
     * @param baseline 基準（commit hash, branch name, tag）
     * @return 變更檔案的絕對路徑列表
     */
    public List<String> getChangedFiles(String projectPath, String baseline) {
        LOG.info("偵測變更檔案: projectPath={}, baseline={}", projectPath, baseline);

        File projectDir = new File(projectPath);
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            LOG.error("專案目錄不存在: {}", projectPath);
            return Collections.emptyList();
        }

        // 檢查是否為 Git 專案
        if (!isGitRepository(projectPath)) {
            LOG.warn("非 Git 專案，無法執行增量掃描: {}", projectPath);
            return Collections.emptyList();
        }

        try {
            // 取得變更檔案
            List<String> changedFiles = executeGitDiff(projectPath, baseline);

            // 過濾程式碼檔案
            List<String> codeFiles = filterCodeFiles(changedFiles);

            // 轉換為絕對路徑
            List<String> absolutePaths = codeFiles.stream()
                    .map(file -> Paths.get(projectPath, file).toAbsolutePath().toString())
                    .collect(Collectors.toList());

            LOG.info("偵測到 {} 個變更的程式碼檔案", absolutePaths.size());

            return absolutePaths;

        } catch (Exception e) {
            LOG.error("取得變更檔案時發生錯誤", e);
            return Collections.emptyList();
        }
    }

    /**
     * 檢查是否為 Git 專案
     */
    private boolean isGitRepository(String projectPath) {
        File gitDir = new File(projectPath, ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }

    /**
     * 執行 git diff 取得變更檔案
     */
    private List<String> executeGitDiff(String projectPath, String baseline) throws IOException {
        List<String> changedFiles = new ArrayList<>();

        // 建構 git diff 命令
        List<String> command = new ArrayList<>();
        command.add(gitExecutable);
        command.add("diff");
        command.add("--name-only");  // 僅列出檔案名稱

        if ("HEAD".equals(baseline)) {
            // 比較 working directory 與 HEAD
            command.add("HEAD");
        } else {
            // 比較 working directory 與指定基準
            command.add(baseline);
        }

        // 執行命令
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(projectPath));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // 讀取輸出
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    changedFiles.add(line.trim());
                }
            }
        }

        // 等待命令完成
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOG.warn("git diff 命令返回非零值: {}", exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("git diff 命令被中斷", e);
        }

        // 同時取得 staged 變更
        List<String> stagedFiles = getStagedFiles(projectPath);
        changedFiles.addAll(stagedFiles);

        // 移除重複
        return changedFiles.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 取得 staged 變更檔案
     */
    private List<String> getStagedFiles(String projectPath) throws IOException {
        List<String> stagedFiles = new ArrayList<>();

        // git diff --cached --name-only
        ProcessBuilder pb = new ProcessBuilder(
                gitExecutable, "diff", "--cached", "--name-only"
        );
        pb.directory(new File(projectPath));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    stagedFiles.add(line.trim());
                }
            }
        }

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return stagedFiles;
    }

    /**
     * 過濾程式碼檔案（根據副檔名）
     */
    private List<String> filterCodeFiles(List<String> files) {
        return files.stream()
                .filter(file -> {
                    String lower = file.toLowerCase();
                    return CODE_EXTENSIONS.stream().anyMatch(lower::endsWith);
                })
                .collect(Collectors.toList());
    }

    /**
     * 計算增量掃描統計資訊
     *
     * @param projectPath 專案根目錄
     * @param changedFiles 變更檔案列表
     * @return 統計資訊
     */
    public IncrementalScanStatistics calculateStatistics(String projectPath, List<String> changedFiles) {
        int totalFiles = countTotalCodeFiles(projectPath);
        int changedCount = changedFiles.size();

        return new IncrementalScanStatistics(
                totalFiles,
                changedCount,
                totalFiles - changedCount
        );
    }

    /**
     * 計算專案中程式碼檔案總數
     */
    private int countTotalCodeFiles(String projectPath) {
        try {
            return (int) Files.walk(Paths.get(projectPath))
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        return CODE_EXTENSIONS.stream().anyMatch(fileName::endsWith);
                    })
                    .count();
        } catch (IOException e) {
            LOG.error("計算檔案總數時發生錯誤", e);
            return 0;
        }
    }

    /**
     * 取得目前 Git 分支名稱
     */
    public String getCurrentBranch(String projectPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    gitExecutable, "rev-parse", "--abbrev-ref", "HEAD"
            );
            pb.directory(new File(projectPath));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String branch = reader.readLine();
                process.waitFor();
                return branch != null ? branch.trim() : "unknown";
            }
        } catch (Exception e) {
            LOG.error("取得當前分支時發生錯誤", e);
            return "unknown";
        }
    }

    /**
     * 取得最新 commit hash
     */
    public String getLatestCommit(String projectPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    gitExecutable, "rev-parse", "HEAD"
            );
            pb.directory(new File(projectPath));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String commit = reader.readLine();
                process.waitFor();
                return commit != null ? commit.trim().substring(0, 8) : "unknown";
            }
        } catch (Exception e) {
            LOG.error("取得最新 commit 時發生錯誤", e);
            return "unknown";
        }
    }

    /**
     * 設定 Git 可執行檔路徑
     */
    public void setGitExecutable(String gitExecutable) {
        this.gitExecutable = gitExecutable;
        LOG.info("Git 可執行檔路徑已設為: {}", gitExecutable);
    }

    /**
     * 增量掃描統計資訊
     */
    public static class IncrementalScanStatistics {
        private final int totalFiles;
        private final int changedFiles;
        private final int skippedFiles;

        public IncrementalScanStatistics(int totalFiles, int changedFiles, int skippedFiles) {
            this.totalFiles = totalFiles;
            this.changedFiles = changedFiles;
            this.skippedFiles = skippedFiles;
        }

        public int getTotalFiles() {
            return totalFiles;
        }

        public int getChangedFiles() {
            return changedFiles;
        }

        public int getSkippedFiles() {
            return skippedFiles;
        }

        public double getReductionPercentage() {
            return totalFiles == 0 ? 0.0 : (double) skippedFiles / totalFiles * 100.0;
        }

        /**
         * 估算節省的時間（假設每個檔案分析需要 60 秒）
         */
        public long getEstimatedTimeSavedSeconds() {
            return (long) skippedFiles * 60;
        }

        /**
         * 估算節省的 AI 成本（假設每個檔案 $0.05）
         */
        public double getEstimatedCostSaved() {
            return skippedFiles * 0.05;
        }

        @Override
        public String toString() {
            return String.format(
                    "IncrementalScanStatistics{total=%d, changed=%d, skipped=%d, reduction=%.2f%%, timeSaved=%ds, costSaved=$%.2f}",
                    totalFiles, changedFiles, skippedFiles, getReductionPercentage(),
                    getEstimatedTimeSavedSeconds(), getEstimatedCostSaved()
            );
        }
    }
}
