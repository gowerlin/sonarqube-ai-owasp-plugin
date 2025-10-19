package com.github.sonarqube.rules;

import com.github.sonarqube.ai.AiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * OWASP 規則引擎
 *
 * 負責執行所有已註冊的 OWASP 安全規則，支援並行和順序執行模式。
 * 整合 AI 服務進行智能分析，提供完整的分析結果。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.1)
 */
public class RuleEngine {

    private static final Logger logger = LoggerFactory.getLogger(RuleEngine.class);

    private final RuleRegistry registry;
    private final ExecutionMode executionMode;
    private final int maxParallelThreads;

    /**
     * 建構子
     *
     * @param registry 規則註冊表
     * @param executionMode 執行模式
     * @param maxParallelThreads 最大並行執行緒數（僅 PARALLEL 模式有效）
     */
    public RuleEngine(RuleRegistry registry, ExecutionMode executionMode, int maxParallelThreads) {
        this.registry = registry;
        this.executionMode = executionMode;
        this.maxParallelThreads = maxParallelThreads;
    }

    /**
     * 建構子（預設順序執行）
     *
     * @param registry 規則註冊表
     */
    public RuleEngine(RuleRegistry registry) {
        this(registry, ExecutionMode.SEQUENTIAL, Runtime.getRuntime().availableProcessors());
    }

    /**
     * 分析程式碼
     *
     * @param code 程式碼內容
     * @param language 程式語言
     * @param owaspVersion OWASP 版本
     * @param aiService AI 服務（可選，為 null 時跳過需要 AI 的規則）
     * @return 分析結果
     */
    public AnalysisResult analyze(String code, String language, String owaspVersion, AiService aiService) {
        return analyze(code, language, owaspVersion, aiService, null, null);
    }

    /**
     * 分析程式碼（完整參數）
     *
     * @param code 程式碼內容
     * @param language 程式語言
     * @param owaspVersion OWASP 版本
     * @param aiService AI 服務（可選）
     * @param fileName 檔案名稱（可選）
     * @param filePath 檔案路徑（可選）
     * @return 分析結果
     */
    public AnalysisResult analyze(String code, String language, String owaspVersion,
                                   AiService aiService, String fileName, Path filePath) {
        long startTime = System.currentTimeMillis();

        // 建立執行上下文
        RuleContext context = RuleContext.builder(code, language)
            .owaspVersion(owaspVersion)
            .aiService(aiService)
            .fileName(fileName)
            .filePath(filePath)
            .build();

        logger.info("Starting analysis: language={}, version={}, hasAi={}, file={}",
            language, owaspVersion, aiService != null, fileName);

        // 取得適用的規則
        List<OwaspRule> applicableRules = getApplicableRules(context);
        logger.info("Found {} applicable rules (total registered: {})",
            applicableRules.size(), registry.getRuleCount());

        // 執行規則
        List<RuleResult> results = executeRules(context, applicableRules);

        long totalTime = System.currentTimeMillis() - startTime;

        // 建立分析結果
        return AnalysisResult.builder()
            .context(context)
            .results(results)
            .executionTimeMs(totalTime)
            .build();
    }

    /**
     * 取得適用的規則
     *
     * @param context 執行上下文
     * @return 適用的規則列表
     */
    private List<OwaspRule> getApplicableRules(RuleContext context) {
        List<OwaspRule> candidates = new ArrayList<>();

        // 依語言過濾
        List<OwaspRule> languageRules = registry.getRulesByLanguage(context.getLanguage());
        if (!languageRules.isEmpty()) {
            candidates.addAll(languageRules);
        }

        // 依版本過濾
        List<OwaspRule> versionRules = registry.getRulesByVersion(context.getOwaspVersion());
        if (!versionRules.isEmpty()) {
            candidates.addAll(versionRules);
        }

        // 若兩者都沒有，使用所有已啟用的規則
        if (candidates.isEmpty()) {
            candidates.addAll(registry.getEnabledRules());
        }

        // 使用 matches() 快速過濾
        return candidates.stream()
            .distinct()
            .filter(rule -> registry.isEnabled(rule.getRuleId()))
            .filter(rule -> {
                // 跳過需要 AI 但沒有 AI 服務的規則
                if (rule.requiresAi() && !context.hasAiService()) {
                    logger.debug("Skipping AI-required rule {} (no AI service available)", rule.getRuleId());
                    return false;
                }
                return rule.matches(context);
            })
            .collect(Collectors.toList());
    }

    /**
     * 執行規則
     *
     * @param context 執行上下文
     * @param rules 規則列表
     * @return 執行結果列表
     */
    public List<RuleResult> executeRules(RuleContext context, List<OwaspRule> rules) {
        if (rules.isEmpty()) {
            logger.info("No rules to execute");
            return Collections.emptyList();
        }

        logger.info("Executing {} rules in {} mode", rules.size(), executionMode);

        switch (executionMode) {
            case PARALLEL:
                return executeRulesParallel(context, rules);
            case SEQUENTIAL:
            default:
                return executeRulesSequential(context, rules);
        }
    }

    /**
     * 順序執行規則
     *
     * @param context 執行上下文
     * @param rules 規則列表
     * @return 執行結果列表
     */
    private List<RuleResult> executeRulesSequential(RuleContext context, List<OwaspRule> rules) {
        List<RuleResult> results = new ArrayList<>();

        for (OwaspRule rule : rules) {
            try {
                long ruleStartTime = System.currentTimeMillis();
                RuleResult result = rule.execute(context);
                long ruleTime = System.currentTimeMillis() - ruleStartTime;

                logger.debug("Rule {} executed in {}ms, violations: {}",
                    rule.getRuleId(), ruleTime, result.getViolationCount());

                results.add(result);
            } catch (Exception e) {
                logger.error("Rule {} execution failed", rule.getRuleId(), e);
                results.add(RuleResult.failure(rule.getRuleId(), e.getMessage()));
            }
        }

        return results;
    }

    /**
     * 並行執行規則
     *
     * @param context 執行上下文
     * @param rules 規則列表
     * @return 執行結果列表
     */
    private List<RuleResult> executeRulesParallel(RuleContext context, List<OwaspRule> rules) {
        ExecutorService executor = Executors.newFixedThreadPool(maxParallelThreads);
        List<Future<RuleResult>> futures = new ArrayList<>();

        // 提交所有規則執行任務
        for (OwaspRule rule : rules) {
            futures.add(executor.submit(() -> {
                try {
                    long ruleStartTime = System.currentTimeMillis();
                    RuleResult result = rule.execute(context);
                    long ruleTime = System.currentTimeMillis() - ruleStartTime;

                    logger.debug("Rule {} executed in {}ms, violations: {}",
                        rule.getRuleId(), ruleTime, result.getViolationCount());

                    return result;
                } catch (Exception e) {
                    logger.error("Rule {} execution failed", rule.getRuleId(), e);
                    return RuleResult.failure(rule.getRuleId(), e.getMessage());
                }
            }));
        }

        // 收集結果
        List<RuleResult> results = new ArrayList<>();
        for (Future<RuleResult> future : futures) {
            try {
                results.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Failed to get rule execution result", e);
            }
        }

        executor.shutdown();
        return results;
    }

    /**
     * 執行模式
     */
    public enum ExecutionMode {
        /**
         * 順序執行（預設）
         */
        SEQUENTIAL,

        /**
         * 並行執行
         */
        PARALLEL
    }

    /**
     * 分析結果
     */
    public static class AnalysisResult {
        private final RuleContext context;
        private final List<RuleResult> results;
        private final long executionTimeMs;

        private AnalysisResult(Builder builder) {
            this.context = builder.context;
            this.results = Collections.unmodifiableList(new ArrayList<>(builder.results));
            this.executionTimeMs = builder.executionTimeMs;
        }

        public RuleContext getContext() {
            return context;
        }

        public List<RuleResult> getResults() {
            return results;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        /**
         * 取得所有違規
         *
         * @return 違規列表
         */
        public List<RuleResult.RuleViolation> getAllViolations() {
            return results.stream()
                .flatMap(result -> result.getViolations().stream())
                .collect(Collectors.toList());
        }

        /**
         * 取得違規總數
         *
         * @return 違規總數
         */
        public int getTotalViolations() {
            return results.stream()
                .mapToInt(RuleResult::getViolationCount)
                .sum();
        }

        /**
         * 取得成功執行的規則數量
         *
         * @return 成功執行的規則數量
         */
        public int getSuccessCount() {
            return (int) results.stream()
                .filter(RuleResult::isSuccess)
                .count();
        }

        /**
         * 取得失敗的規則數量
         *
         * @return 失敗的規則數量
         */
        public int getFailureCount() {
            return (int) results.stream()
                .filter(result -> !result.isSuccess())
                .count();
        }

        /**
         * 取得執行的規則總數
         *
         * @return 執行的規則總數
         */
        public int getTotalRulesExecuted() {
            return results.size();
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private RuleContext context;
            private final List<RuleResult> results = new ArrayList<>();
            private long executionTimeMs;

            public Builder context(RuleContext context) {
                this.context = context;
                return this;
            }

            public Builder result(RuleResult result) {
                if (result != null) {
                    this.results.add(result);
                }
                return this;
            }

            public Builder results(List<RuleResult> results) {
                if (results != null) {
                    this.results.addAll(results);
                }
                return this;
            }

            public Builder executionTimeMs(long executionTimeMs) {
                this.executionTimeMs = executionTimeMs;
                return this;
            }

            public AnalysisResult build() {
                return new AnalysisResult(this);
            }
        }

        @Override
        public String toString() {
            return "AnalysisResult{" +
                "totalRules=" + getTotalRulesExecuted() +
                ", violations=" + getTotalViolations() +
                ", success=" + getSuccessCount() +
                ", failures=" + getFailureCount() +
                ", executionTimeMs=" + executionTimeMs +
                '}';
        }
    }
}
