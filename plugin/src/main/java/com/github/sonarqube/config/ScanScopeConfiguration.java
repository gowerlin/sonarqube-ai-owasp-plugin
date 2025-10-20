package com.github.sonarqube.config;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 掃描範圍配置
 *
 * 用戶可選擇全專案、增量掃描或手動選擇檔案。
 * 支援排除規則、檔案類型過濾和路徑模式匹配。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.5.0 (Epic 7, Story 7.3)
 */
public class ScanScopeConfiguration {

    // 預設值
    public static final ScanMode DEFAULT_SCAN_MODE = ScanMode.FULL_PROJECT;
    public static final int DEFAULT_MAX_FILE_SIZE_MB = 10;
    public static final boolean DEFAULT_SKIP_TESTS = true;
    public static final boolean DEFAULT_SKIP_GENERATED = true;

    private final ScanMode scanMode;
    private final Set<Path> includedPaths;          // 包含的路徑（手動選擇模式）
    private final Set<String> excludedPatterns;     // 排除的模式（例：*.test.js, node_modules/*）
    private final Set<String> includedExtensions;   // 包含的副檔名（例：.java, .js, .py）
    private final int maxFileSizeMb;                // 最大檔案大小（MB）
    private final boolean skipTests;                // 是否跳過測試檔案
    private final boolean skipGenerated;            // 是否跳過生成的檔案
    private final String gitBaseBranch;             // 增量掃描基準分支（例：main）

    private ScanScopeConfiguration(Builder builder) {
        this.scanMode = builder.scanMode;
        this.includedPaths = Collections.unmodifiableSet(new HashSet<>(builder.includedPaths));
        this.excludedPatterns = Collections.unmodifiableSet(new HashSet<>(builder.excludedPatterns));
        this.includedExtensions = Collections.unmodifiableSet(new HashSet<>(builder.includedExtensions));
        this.maxFileSizeMb = builder.maxFileSizeMb;
        this.skipTests = builder.skipTests;
        this.skipGenerated = builder.skipGenerated;
        this.gitBaseBranch = builder.gitBaseBranch;
    }

    // Getters
    public ScanMode getScanMode() {
        return scanMode;
    }

    public Set<Path> getIncludedPaths() {
        return includedPaths;
    }

    public Set<String> getExcludedPatterns() {
        return excludedPatterns;
    }

    public Set<String> getIncludedExtensions() {
        return includedExtensions;
    }

    public int getMaxFileSizeMb() {
        return maxFileSizeMb;
    }

    public boolean isSkipTests() {
        return skipTests;
    }

    public boolean isSkipGenerated() {
        return skipGenerated;
    }

    public String getGitBaseBranch() {
        return gitBaseBranch;
    }

    /**
     * 檢查檔案是否應該被掃描
     *
     * @param filePath 檔案路徑
     * @param fileSizeBytes 檔案大小（位元組）
     * @return true 若應該掃描
     */
    public boolean shouldScanFile(Path filePath, long fileSizeBytes) {
        // 檢查檔案大小
        long maxSizeBytes = (long) maxFileSizeMb * 1024 * 1024;
        if (fileSizeBytes > maxSizeBytes) {
            return false;
        }

        String filePathStr = filePath.toString();
        String fileName = filePath.getFileName().toString();

        // 檢查副檔名
        if (!includedExtensions.isEmpty()) {
            boolean hasValidExtension = includedExtensions.stream()
                .anyMatch(ext -> fileName.endsWith(ext));
            if (!hasValidExtension) {
                return false;
            }
        }

        // 檢查排除模式
        for (String pattern : excludedPatterns) {
            if (matchesPattern(filePathStr, pattern)) {
                return false;
            }
        }

        // 檢查測試檔案
        if (skipTests && isTestFile(filePathStr)) {
            return false;
        }

        // 檢查生成的檔案
        if (skipGenerated && isGeneratedFile(filePathStr)) {
            return false;
        }

        return true;
    }

    /**
     * 檢查路徑是否符合模式
     *
     * @param path 路徑
     * @param pattern 模式（支援 * 萬用字元）
     * @return true 若符合
     */
    private boolean matchesPattern(String path, String pattern) {
        // 簡化的模式匹配（支援 * 萬用字元）
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");
        return path.matches(regex);
    }

    /**
     * 檢查是否為測試檔案
     *
     * @param path 檔案路徑
     * @return true 若為測試檔案
     */
    private boolean isTestFile(String path) {
        String lowerPath = path.toLowerCase();
        return lowerPath.contains("/test/")
            || lowerPath.contains("\\test\\")
            || lowerPath.contains("/tests/")
            || lowerPath.contains("\\tests\\")
            || lowerPath.endsWith(".test.js")
            || lowerPath.endsWith(".test.ts")
            || lowerPath.endsWith(".spec.js")
            || lowerPath.endsWith(".spec.ts")
            || lowerPath.endsWith("test.java")
            || lowerPath.endsWith("test.py");
    }

    /**
     * 檢查是否為生成的檔案
     *
     * @param path 檔案路徑
     * @return true 若為生成的檔案
     */
    private boolean isGeneratedFile(String path) {
        String lowerPath = path.toLowerCase();
        return lowerPath.contains("/generated/")
            || lowerPath.contains("\\generated\\")
            || lowerPath.contains("/dist/")
            || lowerPath.contains("\\dist\\")
            || lowerPath.contains("/build/")
            || lowerPath.contains("\\build\\")
            || lowerPath.contains("/target/")
            || lowerPath.contains("\\target\\")
            || lowerPath.contains("node_modules")
            || lowerPath.contains(".min.js")
            || lowerPath.contains(".bundle.js");
    }

    /**
     * 取得配置摘要
     *
     * @return 配置摘要字串
     */
    public String getSummary() {
        return String.format(
            "ScanScopeConfiguration{mode=%s, includedPaths=%d, excludedPatterns=%d, extensions=%d, " +
            "maxSize=%dMB, skipTests=%s, skipGenerated=%s, gitBranch=%s}",
            scanMode, includedPaths.size(), excludedPatterns.size(), includedExtensions.size(),
            maxFileSizeMb, skipTests, skipGenerated, gitBaseBranch
        );
    }

    /**
     * 掃描模式
     */
    public enum ScanMode {
        /**
         * 全專案掃描
         */
        FULL_PROJECT,

        /**
         * 增量掃描（僅掃描變更的檔案）
         */
        INCREMENTAL,

        /**
         * 手動選擇檔案
         */
        MANUAL_SELECTION
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ScanMode scanMode = DEFAULT_SCAN_MODE;
        private final Set<Path> includedPaths = new HashSet<>();
        private final Set<String> excludedPatterns = new HashSet<>();
        private final Set<String> includedExtensions = new HashSet<>();
        private int maxFileSizeMb = DEFAULT_MAX_FILE_SIZE_MB;
        private boolean skipTests = DEFAULT_SKIP_TESTS;
        private boolean skipGenerated = DEFAULT_SKIP_GENERATED;
        private String gitBaseBranch = "main";

        public Builder scanMode(ScanMode scanMode) {
            this.scanMode = scanMode;
            return this;
        }

        public Builder includePath(Path path) {
            this.includedPaths.add(path);
            return this;
        }

        public Builder includePaths(Set<Path> paths) {
            this.includedPaths.addAll(paths);
            return this;
        }

        public Builder excludePattern(String pattern) {
            this.excludedPatterns.add(pattern);
            return this;
        }

        public Builder excludePatterns(Set<String> patterns) {
            this.excludedPatterns.addAll(patterns);
            return this;
        }

        public Builder includeExtension(String extension) {
            if (!extension.startsWith(".")) {
                extension = "." + extension;
            }
            this.includedExtensions.add(extension);
            return this;
        }

        public Builder includeExtensions(Set<String> extensions) {
            extensions.forEach(this::includeExtension);
            return this;
        }

        public Builder maxFileSizeMb(int maxFileSizeMb) {
            if (maxFileSizeMb <= 0) {
                throw new IllegalArgumentException("Max file size must be positive");
            }
            this.maxFileSizeMb = maxFileSizeMb;
            return this;
        }

        public Builder skipTests(boolean skipTests) {
            this.skipTests = skipTests;
            return this;
        }

        public Builder skipGenerated(boolean skipGenerated) {
            this.skipGenerated = skipGenerated;
            return this;
        }

        public Builder gitBaseBranch(String gitBaseBranch) {
            this.gitBaseBranch = gitBaseBranch;
            return this;
        }

        /**
         * 使用預設 Java 設定
         */
        public Builder withJavaDefaults() {
            this.includeExtension(".java");
            this.excludePattern("*/target/*");
            this.excludePattern("*/build/*");
            return this;
        }

        /**
         * 使用預設 JavaScript 設定
         */
        public Builder withJavaScriptDefaults() {
            this.includeExtension(".js");
            this.includeExtension(".ts");
            this.includeExtension(".jsx");
            this.includeExtension(".tsx");
            this.excludePattern("*/node_modules/*");
            this.excludePattern("*/dist/*");
            this.excludePattern("*.min.js");
            return this;
        }

        /**
         * 使用預設 Python 設定
         */
        public Builder withPythonDefaults() {
            this.includeExtension(".py");
            this.excludePattern("*/__pycache__/*");
            this.excludePattern("*/venv/*");
            this.excludePattern("*/.venv/*");
            return this;
        }

        public ScanScopeConfiguration build() {
            return new ScanScopeConfiguration(this);
        }
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
