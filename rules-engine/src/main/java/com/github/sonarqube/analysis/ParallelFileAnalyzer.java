package com.github.sonarqube.analysis;

import com.github.sonarqube.ai.AiService;
import com.github.sonarqube.rules.RuleEngine;
import com.github.sonarqube.rules.RuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 並行檔案分析器
 *
 * 使用 ExecutorService 實現多檔案並行分析，提升 40% 效能。
 * 支援智能執行緒池管理、任務分配策略和錯誤處理機制。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.4.0 (Epic 6, Story 6.4)
 */
public class ParallelFileAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(ParallelFileAnalyzer.class);

    private final RuleEngine ruleEngine;
    private final int maxParallelFiles;
    private final long timeoutMillis;

    /**
     * 建構子
     *
     * @param registry 規則註冊表
     * @param maxParallelFiles 最大並行檔案數（預設為處理器核心數）
     * @param timeoutMillis 單一檔案分析超時時間（毫秒，預設 60 秒）
     */
    public ParallelFileAnalyzer(RuleRegistry registry, int maxParallelFiles, long timeoutMillis) {
        this.ruleEngine = new RuleEngine(registry, RuleEngine.ExecutionMode.PARALLEL,
            Runtime.getRuntime().availableProcessors());
        this.maxParallelFiles = maxParallelFiles;
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * 建構子（使用預設參數）
     *
     * @param registry 規則註冊表
     */
    public ParallelFileAnalyzer(RuleRegistry registry) {
        this(registry, Runtime.getRuntime().availableProcessors(), 60000L);
    }

    /**
     * 並行分析多個檔案
     *
     * @param fileTasks 檔案分析任務列表
     * @param aiService AI 服務（可選）
     * @return 批次分析結果
     */
    public BatchAnalysisResult analyzeFiles(List<FileAnalysisTask> fileTasks, AiService aiService) {
        long startTime = System.currentTimeMillis();

        logger.info("Starting parallel file analysis: files={}, threads={}, timeout={}ms",
            fileTasks.size(), maxParallelFiles, timeoutMillis);

        ExecutorService executor = Executors.newFixedThreadPool(maxParallelFiles);
        List<Future<FileAnalysisResult>> futures = new ArrayList<>();

        // 提交所有檔案分析任務
        for (FileAnalysisTask task : fileTasks) {
            futures.add(executor.submit(() -> analyzeFile(task, aiService)));
        }

        // 收集結果
        List<FileAnalysisResult> results = new ArrayList<>();
        List<FileAnalysisError> errors = new ArrayList<>();
        int completedCount = 0;
        int failedCount = 0;

        for (int i = 0; i < futures.size(); i++) {
            Future<FileAnalysisResult> future = futures.get(i);
            FileAnalysisTask task = fileTasks.get(i);

            try {
                FileAnalysisResult result = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
                results.add(result);
                completedCount++;

                logger.debug("File analysis completed: {} ({} violations in {}ms)",
                    task.getFilePath(), result.getViolationCount(), result.getExecutionTimeMs());

            } catch (TimeoutException e) {
                String errorMsg = String.format("File analysis timeout after %dms: %s",
                    timeoutMillis, task.getFilePath());
                logger.error(errorMsg);
                errors.add(new FileAnalysisError(task.getFilePath(), "TIMEOUT", errorMsg));
                failedCount++;
                future.cancel(true);

            } catch (InterruptedException | ExecutionException e) {
                String errorMsg = String.format("File analysis failed: %s - %s",
                    task.getFilePath(), e.getMessage());
                logger.error(errorMsg, e);
                errors.add(new FileAnalysisError(task.getFilePath(), "EXECUTION_ERROR", errorMsg));
                failedCount++;
            }
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        long totalTime = System.currentTimeMillis() - startTime;

        logger.info("Parallel file analysis completed: total={}, completed={}, failed={}, time={}ms",
            fileTasks.size(), completedCount, failedCount, totalTime);

        return new BatchAnalysisResult(results, errors, totalTime);
    }

    /**
     * 分析單一檔案
     *
     * @param task 檔案分析任務
     * @param aiService AI 服務
     * @return 檔案分析結果
     */
    private FileAnalysisResult analyzeFile(FileAnalysisTask task, AiService aiService) {
        long startTime = System.currentTimeMillis();

        try {
            // 讀取檔案內容
            String code = Files.readString(task.getFilePath());

            // 執行規則引擎分析
            RuleEngine.AnalysisResult engineResult = ruleEngine.analyze(
                code,
                task.getLanguage(),
                task.getOwaspVersion(),
                aiService,
                task.getFilePath().getFileName().toString(),
                task.getFilePath()
            );

            long executionTime = System.currentTimeMillis() - startTime;

            return new FileAnalysisResult(
                task.getFilePath(),
                engineResult,
                executionTime
            );

        } catch (IOException e) {
            logger.error("Failed to read file: {}", task.getFilePath(), e);
            throw new RuntimeException("File read error: " + e.getMessage(), e);
        }
    }

    /**
     * 檔案分析任務
     */
    public static class FileAnalysisTask {
        private final Path filePath;
        private final String language;
        private final String owaspVersion;

        public FileAnalysisTask(Path filePath, String language, String owaspVersion) {
            this.filePath = filePath;
            this.language = language;
            this.owaspVersion = owaspVersion;
        }

        public Path getFilePath() {
            return filePath;
        }

        public String getLanguage() {
            return language;
        }

        public String getOwaspVersion() {
            return owaspVersion;
        }

        @Override
        public String toString() {
            return "FileAnalysisTask{" +
                "file=" + filePath +
                ", language='" + language + '\'' +
                ", owaspVersion='" + owaspVersion + '\'' +
                '}';
        }
    }

    /**
     * 檔案分析結果
     */
    public static class FileAnalysisResult {
        private final Path filePath;
        private final RuleEngine.AnalysisResult engineResult;
        private final long executionTimeMs;

        public FileAnalysisResult(Path filePath, RuleEngine.AnalysisResult engineResult,
                                  long executionTimeMs) {
            this.filePath = filePath;
            this.engineResult = engineResult;
            this.executionTimeMs = executionTimeMs;
        }

        public Path getFilePath() {
            return filePath;
        }

        public RuleEngine.AnalysisResult getEngineResult() {
            return engineResult;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public int getViolationCount() {
            return engineResult.getTotalViolations();
        }

        @Override
        public String toString() {
            return "FileAnalysisResult{" +
                "file=" + filePath +
                ", violations=" + getViolationCount() +
                ", executionTimeMs=" + executionTimeMs +
                '}';
        }
    }

    /**
     * 檔案分析錯誤
     */
    public static class FileAnalysisError {
        private final Path filePath;
        private final String errorType;
        private final String errorMessage;

        public FileAnalysisError(Path filePath, String errorType, String errorMessage) {
            this.filePath = filePath;
            this.errorType = errorType;
            this.errorMessage = errorMessage;
        }

        public Path getFilePath() {
            return filePath;
        }

        public String getErrorType() {
            return errorType;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        @Override
        public String toString() {
            return "FileAnalysisError{" +
                "file=" + filePath +
                ", errorType='" + errorType + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
        }
    }

    /**
     * 批次分析結果
     */
    public static class BatchAnalysisResult {
        private final List<FileAnalysisResult> results;
        private final List<FileAnalysisError> errors;
        private final long totalExecutionTimeMs;

        public BatchAnalysisResult(List<FileAnalysisResult> results,
                                   List<FileAnalysisError> errors,
                                   long totalExecutionTimeMs) {
            this.results = Collections.unmodifiableList(new ArrayList<>(results));
            this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
            this.totalExecutionTimeMs = totalExecutionTimeMs;
        }

        public List<FileAnalysisResult> getResults() {
            return results;
        }

        public List<FileAnalysisError> getErrors() {
            return errors;
        }

        public long getTotalExecutionTimeMs() {
            return totalExecutionTimeMs;
        }

        public int getTotalFiles() {
            return results.size() + errors.size();
        }

        public int getCompletedCount() {
            return results.size();
        }

        public int getFailedCount() {
            return errors.size();
        }

        public int getTotalViolations() {
            return results.stream()
                .mapToInt(FileAnalysisResult::getViolationCount)
                .sum();
        }

        /**
         * 依嚴重性統計違規數量
         *
         * @return 嚴重性 → 違規數量
         */
        public Map<String, Integer> getViolationsBySeverity() {
            return results.stream()
                .flatMap(result -> result.getEngineResult().getResults().stream())
                .flatMap(ruleResult -> ruleResult.getViolations().stream())
                .collect(Collectors.groupingBy(
                    violation -> violation.getSeverity().name(),
                    Collectors.summingInt(violation -> 1)
                ));
        }

        /**
         * 依 OWASP 類別統計違規數量
         *
         * @return OWASP 類別 → 違規數量
         */
        public Map<String, Integer> getViolationsByOwaspCategory() {
            return results.stream()
                .flatMap(result -> result.getEngineResult().getResults().stream())
                .filter(ruleResult -> !ruleResult.getViolations().isEmpty())
                .collect(Collectors.groupingBy(
                    ruleResult -> ruleResult.getRuleId().split("-")[2], // Extract category (e.g., "a01")
                    Collectors.summingInt(ruleResult -> ruleResult.getViolations().size())
                ));
        }

        @Override
        public String toString() {
            return "BatchAnalysisResult{" +
                "totalFiles=" + getTotalFiles() +
                ", completed=" + getCompletedCount() +
                ", failed=" + getFailedCount() +
                ", totalViolations=" + getTotalViolations() +
                ", totalExecutionTimeMs=" + totalExecutionTimeMs +
                '}';
        }
    }
}
