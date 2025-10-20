package com.github.sonarqube.plugin.cost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AI 成本估算工具
 *
 * 追蹤 AI API 使用量，計算成本，提供預算控制與優化建議。
 *
 * 功能特性：
 * - Token 使用量追蹤（輸入/輸出 token）
 * - 多 AI 供應商價格支援（OpenAI, Anthropic, Google Gemini）
 * - 成本預算控制與警告
 * - 統計分析與成本報告
 * - 批次成本估算
 *
 * 定價參考（2025-10-20）：
 * - OpenAI GPT-4: $0.03/1K input, $0.06/1K output
 * - Anthropic Claude 3: $0.015/1K input, $0.075/1K output
 * - Google Gemini Pro: $0.00025/1K input, $0.0005/1K output
 *
 * @since 2.9.0 (Epic 6, Story 6.7)
 * @author SonarQube AI OWASP Plugin Team
 */
public class CostEstimator {

    private static final Logger LOG = LoggerFactory.getLogger(CostEstimator.class);

    // Singleton instance
    private static CostEstimator instance;

    // Token 使用量追蹤
    private final AtomicLong totalInputTokens;
    private final AtomicLong totalOutputTokens;
    private final AtomicInteger totalApiCalls;

    // 成本預算設定
    private double budgetLimit = 100.0; // 預設 $100
    private double warningThreshold = 0.8; // 80% 時警告

    // AI 供應商價格表（美元/1K tokens）
    private final Map<AiProvider, Pricing> pricingTable;

    /**
     * 私有建構子（Singleton 模式）
     */
    private CostEstimator() {
        this.totalInputTokens = new AtomicLong(0);
        this.totalOutputTokens = new AtomicLong(0);
        this.totalApiCalls = new AtomicInteger(0);
        this.pricingTable = initializePricingTable();

        LOG.info("CostEstimator 已初始化");
    }

    /**
     * 取得 Singleton 實例
     */
    public static synchronized CostEstimator getInstance() {
        if (instance == null) {
            instance = new CostEstimator();
        }
        return instance;
    }

    /**
     * 初始化價格表
     */
    private Map<AiProvider, Pricing> initializePricingTable() {
        Map<AiProvider, Pricing> table = new HashMap<>();

        // OpenAI GPT-4
        table.put(AiProvider.OPENAI_GPT4, new Pricing(0.03, 0.06));

        // OpenAI GPT-4 Turbo
        table.put(AiProvider.OPENAI_GPT4_TURBO, new Pricing(0.01, 0.03));

        // OpenAI GPT-3.5 Turbo
        table.put(AiProvider.OPENAI_GPT35_TURBO, new Pricing(0.0015, 0.002));

        // Anthropic Claude 3 Opus
        table.put(AiProvider.ANTHROPIC_CLAUDE3_OPUS, new Pricing(0.015, 0.075));

        // Anthropic Claude 3 Sonnet
        table.put(AiProvider.ANTHROPIC_CLAUDE3_SONNET, new Pricing(0.003, 0.015));

        // Anthropic Claude 3 Haiku
        table.put(AiProvider.ANTHROPIC_CLAUDE3_HAIKU, new Pricing(0.00025, 0.00125));

        // Google Gemini 1.5 Pro
        table.put(AiProvider.GOOGLE_GEMINI_PRO, new Pricing(0.00025, 0.0005));

        // Google Gemini 1.5 Flash
        table.put(AiProvider.GOOGLE_GEMINI_FLASH, new Pricing(0.000125, 0.00025));

        return table;
    }

    /**
     * 記錄 API 呼叫
     *
     * @param provider AI 供應商
     * @param inputTokens 輸入 token 數
     * @param outputTokens 輸出 token 數
     */
    public void recordApiCall(AiProvider provider, long inputTokens, long outputTokens) {
        totalInputTokens.addAndGet(inputTokens);
        totalOutputTokens.addAndGet(outputTokens);
        totalApiCalls.incrementAndGet();

        // 計算成本
        double cost = calculateCost(provider, inputTokens, outputTokens);

        LOG.debug("API 呼叫記錄: provider={}, input={}, output={}, cost=${:.4f}",
                provider, inputTokens, outputTokens, cost);

        // 檢查預算
        checkBudgetWarning();
    }

    /**
     * 計算成本
     *
     * @param provider AI 供應商
     * @param inputTokens 輸入 token 數
     * @param outputTokens 輸出 token 數
     * @return 成本（美元）
     */
    public double calculateCost(AiProvider provider, long inputTokens, long outputTokens) {
        Pricing pricing = pricingTable.get(provider);
        if (pricing == null) {
            LOG.warn("未知的 AI 供應商: {}, 使用預設價格", provider);
            pricing = new Pricing(0.01, 0.03); // 預設價格
        }

        double inputCost = (inputTokens / 1000.0) * pricing.inputPricePerK;
        double outputCost = (outputTokens / 1000.0) * pricing.outputPricePerK;

        return inputCost + outputCost;
    }

    /**
     * 估算批次掃描成本
     *
     * @param provider AI 供應商
     * @param fileCount 檔案數量
     * @param avgInputTokensPerFile 平均每檔案輸入 token 數
     * @param avgOutputTokensPerFile 平均每檔案輸出 token 數
     * @return 批次估算結果
     */
    public BatchCostEstimate estimateBatchCost(
            AiProvider provider,
            int fileCount,
            long avgInputTokensPerFile,
            long avgOutputTokensPerFile) {

        long totalInput = fileCount * avgInputTokensPerFile;
        long totalOutput = fileCount * avgOutputTokensPerFile;
        double totalCost = calculateCost(provider, totalInput, totalOutput);

        return new BatchCostEstimate(
                fileCount,
                totalInput,
                totalOutput,
                totalCost,
                provider
        );
    }

    /**
     * 取得目前總成本
     */
    public double getCurrentTotalCost(AiProvider provider) {
        return calculateCost(provider, totalInputTokens.get(), totalOutputTokens.get());
    }

    /**
     * 檢查預算警告
     */
    private void checkBudgetWarning() {
        // 使用 GPT-4 作為參考價格
        double currentCost = getCurrentTotalCost(AiProvider.OPENAI_GPT4);
        double usagePercentage = (currentCost / budgetLimit) * 100.0;

        if (usagePercentage >= warningThreshold * 100) {
            LOG.warn("⚠️ 預算警告: 已使用 {:.2f}% (${:.2f}/${:.2f})",
                    usagePercentage, currentCost, budgetLimit);
        }

        if (currentCost >= budgetLimit) {
            LOG.error("🚨 預算超支: ${:.2f} / ${:.2f}",
                    currentCost, budgetLimit);
        }
    }

    /**
     * 取得成本統計資訊
     */
    public CostStatistics getStatistics() {
        return new CostStatistics(
                totalApiCalls.get(),
                totalInputTokens.get(),
                totalOutputTokens.get(),
                budgetLimit,
                pricingTable
        );
    }

    /**
     * 重置統計資料
     */
    public void reset() {
        totalInputTokens.set(0);
        totalOutputTokens.set(0);
        totalApiCalls.set(0);
        LOG.info("成本統計已重置");
    }

    /**
     * 設定預算限制
     */
    public void setBudgetLimit(double budgetLimit) {
        this.budgetLimit = Math.max(0, budgetLimit);
        LOG.info("預算限制已設為: ${:.2f}", this.budgetLimit);
    }

    /**
     * 設定警告閾值
     */
    public void setWarningThreshold(double warningThreshold) {
        this.warningThreshold = Math.max(0, Math.min(1.0, warningThreshold));
        LOG.info("警告閾值已設為: {:.0f}%", this.warningThreshold * 100);
    }

    /**
     * 比較不同 AI 供應商的成本
     */
    public Map<AiProvider, Double> compareCosts(long inputTokens, long outputTokens) {
        Map<AiProvider, Double> comparison = new HashMap<>();

        for (Map.Entry<AiProvider, Pricing> entry : pricingTable.entrySet()) {
            double cost = calculateCost(entry.getKey(), inputTokens, outputTokens);
            comparison.put(entry.getKey(), cost);
        }

        return comparison;
    }

    /**
     * AI 供應商枚舉
     */
    public enum AiProvider {
        OPENAI_GPT4("OpenAI GPT-4"),
        OPENAI_GPT4_TURBO("OpenAI GPT-4 Turbo"),
        OPENAI_GPT35_TURBO("OpenAI GPT-3.5 Turbo"),
        ANTHROPIC_CLAUDE3_OPUS("Anthropic Claude 3 Opus"),
        ANTHROPIC_CLAUDE3_SONNET("Anthropic Claude 3 Sonnet"),
        ANTHROPIC_CLAUDE3_HAIKU("Anthropic Claude 3 Haiku"),
        GOOGLE_GEMINI_PRO("Google Gemini 1.5 Pro"),
        GOOGLE_GEMINI_FLASH("Google Gemini 1.5 Flash");

        private final String displayName;

        AiProvider(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 價格資料結構
     */
    private static class Pricing {
        private final double inputPricePerK;  // 輸入 token 價格（每 1K）
        private final double outputPricePerK; // 輸出 token 價格（每 1K）

        public Pricing(double inputPricePerK, double outputPricePerK) {
            this.inputPricePerK = inputPricePerK;
            this.outputPricePerK = outputPricePerK;
        }
    }

    /**
     * 批次成本估算結果
     */
    public static class BatchCostEstimate {
        private final int fileCount;
        private final long totalInputTokens;
        private final long totalOutputTokens;
        private final double totalCost;
        private final AiProvider provider;

        public BatchCostEstimate(int fileCount, long totalInputTokens, long totalOutputTokens,
                                  double totalCost, AiProvider provider) {
            this.fileCount = fileCount;
            this.totalInputTokens = totalInputTokens;
            this.totalOutputTokens = totalOutputTokens;
            this.totalCost = totalCost;
            this.provider = provider;
        }

        public int getFileCount() {
            return fileCount;
        }

        public long getTotalInputTokens() {
            return totalInputTokens;
        }

        public long getTotalOutputTokens() {
            return totalOutputTokens;
        }

        public double getTotalCost() {
            return totalCost;
        }

        public double getCostPerFile() {
            return fileCount == 0 ? 0.0 : totalCost / fileCount;
        }

        public AiProvider getProvider() {
            return provider;
        }

        @Override
        public String toString() {
            return String.format(
                    "BatchCostEstimate{files=%d, inputTokens=%d, outputTokens=%d, totalCost=$%.2f, costPerFile=$%.4f, provider=%s}",
                    fileCount, totalInputTokens, totalOutputTokens, totalCost, getCostPerFile(), provider.getDisplayName()
            );
        }
    }

    /**
     * 成本統計資訊
     */
    public static class CostStatistics {
        private final int totalApiCalls;
        private final long totalInputTokens;
        private final long totalOutputTokens;
        private final double budgetLimit;
        private final Map<AiProvider, Pricing> pricingTable;

        public CostStatistics(int totalApiCalls, long totalInputTokens, long totalOutputTokens,
                              double budgetLimit, Map<AiProvider, Pricing> pricingTable) {
            this.totalApiCalls = totalApiCalls;
            this.totalInputTokens = totalInputTokens;
            this.totalOutputTokens = totalOutputTokens;
            this.budgetLimit = budgetLimit;
            this.pricingTable = pricingTable;
        }

        public int getTotalApiCalls() {
            return totalApiCalls;
        }

        public long getTotalInputTokens() {
            return totalInputTokens;
        }

        public long getTotalOutputTokens() {
            return totalOutputTokens;
        }

        public long getTotalTokens() {
            return totalInputTokens + totalOutputTokens;
        }

        public double getBudgetLimit() {
            return budgetLimit;
        }

        /**
         * 計算使用指定 AI 供應商的總成本
         */
        public double getTotalCost(AiProvider provider) {
            Pricing pricing = pricingTable.get(provider);
            if (pricing == null) {
                return 0.0;
            }

            double inputCost = (totalInputTokens / 1000.0) * pricing.inputPricePerK;
            double outputCost = (totalOutputTokens / 1000.0) * pricing.outputPricePerK;

            return inputCost + outputCost;
        }

        /**
         * 計算預算使用百分比
         */
        public double getBudgetUsagePercentage(AiProvider provider) {
            double totalCost = getTotalCost(provider);
            return budgetLimit == 0 ? 0.0 : (totalCost / budgetLimit) * 100.0;
        }

        /**
         * 計算剩餘預算
         */
        public double getRemainingBudget(AiProvider provider) {
            return Math.max(0, budgetLimit - getTotalCost(provider));
        }

        @Override
        public String toString() {
            return String.format(
                    "CostStatistics{apiCalls=%d, inputTokens=%d, outputTokens=%d, totalTokens=%d, budget=$%.2f}",
                    totalApiCalls, totalInputTokens, totalOutputTokens, getTotalTokens(), budgetLimit
            );
        }
    }
}
