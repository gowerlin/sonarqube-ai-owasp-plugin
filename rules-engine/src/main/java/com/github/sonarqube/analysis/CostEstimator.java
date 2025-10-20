package com.github.sonarqube.analysis;

import com.github.sonarqube.rules.OwaspRule;
import com.github.sonarqube.rules.RuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI API 成本估算工具
 *
 * 掃描前顯示預估的 AI API 調用成本，包含 token 數量和費用估算。
 * 支援多種 AI 供應商的定價模型（OpenAI, Claude）。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.4.0 (Epic 6, Story 6.7)
 */
public class CostEstimator {

    private static final Logger logger = LoggerFactory.getLogger(CostEstimator.class);

    // AI 供應商定價（每 1K tokens 的價格，美元）
    private static final Map<String, AiPricing> PRICING_TABLE = Map.of(
        "openai-gpt-4o", new AiPricing(0.0025, 0.01),           // Input: $0.0025, Output: $0.01
        "openai-gpt-3.5-turbo", new AiPricing(0.0005, 0.0015),  // Input: $0.0005, Output: $0.0015
        "claude-3.5-sonnet", new AiPricing(0.003, 0.015),       // Input: $0.003, Output: $0.015
        "claude-3-opus", new AiPricing(0.015, 0.075),           // Input: $0.015, Output: $0.075
        "claude-3-haiku", new AiPricing(0.00025, 0.00125)       // Input: $0.00025, Output: $0.00125
    );

    private final RuleRegistry registry;
    private final String aiModel;
    private final double tokenMultiplier; // Token 估算倍數（考慮 prompt engineering）

    /**
     * 建構子
     *
     * @param registry 規則註冊表
     * @param aiModel AI 模型名稱（例：openai-gpt-4o, claude-3.5-sonnet）
     * @param tokenMultiplier Token 估算倍數（預設 1.5，考慮 prompt overhead）
     */
    public CostEstimator(RuleRegistry registry, String aiModel, double tokenMultiplier) {
        this.registry = registry;
        this.aiModel = aiModel;
        this.tokenMultiplier = tokenMultiplier;
    }

    /**
     * 建構子（使用預設倍數）
     *
     * @param registry 規則註冊表
     * @param aiModel AI 模型名稱
     */
    public CostEstimator(RuleRegistry registry, String aiModel) {
        this(registry, aiModel, 1.5);
    }

    /**
     * 估算單一檔案的分析成本
     *
     * @param filePath 檔案路徑
     * @param owaspVersion OWASP 版本
     * @return 成本估算結果
     */
    public CostEstimate estimateFileCost(Path filePath, String owaspVersion) {
        try {
            String code = Files.readString(filePath);
            int codeTokens = estimateTokens(code);

            // 取得需要 AI 的規則數量
            int aiRulesCount = countAiRequiredRules(owaspVersion);

            // 計算總 input tokens（程式碼 + prompt × 規則數）
            int inputTokens = codeTokens + (500 * aiRulesCount); // 假設每個規則 prompt 500 tokens
            int outputTokens = 300 * aiRulesCount; // 假設每個規則輸出 300 tokens

            // 應用倍數（考慮實際使用情況）
            inputTokens = (int) (inputTokens * tokenMultiplier);
            outputTokens = (int) (outputTokens * tokenMultiplier);

            // 計算費用
            double cost = calculateCost(inputTokens, outputTokens);

            return new CostEstimate(
                filePath,
                codeTokens,
                inputTokens,
                outputTokens,
                aiRulesCount,
                cost,
                aiModel
            );

        } catch (IOException e) {
            logger.error("Failed to estimate cost for file: {}", filePath, e);
            return new CostEstimate(filePath, 0, 0, 0, 0, 0.0, aiModel);
        }
    }

    /**
     * 估算批次檔案的分析成本
     *
     * @param filePaths 檔案路徑列表
     * @param owaspVersion OWASP 版本
     * @return 批次成本估算結果
     */
    public BatchCostEstimate estimateBatchCost(List<Path> filePaths, String owaspVersion) {
        logger.info("Estimating cost for {} files using {} (version: {})",
            filePaths.size(), aiModel, owaspVersion);

        List<CostEstimate> fileEstimates = filePaths.stream()
            .map(path -> estimateFileCost(path, owaspVersion))
            .collect(Collectors.toList());

        return new BatchCostEstimate(fileEstimates, aiModel);
    }

    /**
     * 估算程式碼的 token 數量（使用簡化算法）
     *
     * @param code 程式碼內容
     * @return 估算的 token 數量
     */
    private int estimateTokens(String code) {
        // 簡化算法：英文約 0.75 tokens/word，中文約 1.5-2 tokens/char
        // 這裡採用保守估算：4 chars/token（適用於程式碼）
        return Math.max(1, code.length() / 4);
    }

    /**
     * 計算 AI API 調用費用
     *
     * @param inputTokens 輸入 token 數
     * @param outputTokens 輸出 token 數
     * @return 費用（美元）
     */
    private double calculateCost(int inputTokens, int outputTokens) {
        AiPricing pricing = PRICING_TABLE.get(aiModel);
        if (pricing == null) {
            logger.warn("Unknown AI model: {}, using default pricing", aiModel);
            pricing = new AiPricing(0.003, 0.015); // Default to Claude 3.5 Sonnet
        }

        double inputCost = (inputTokens / 1000.0) * pricing.inputPricePerK;
        double outputCost = (outputTokens / 1000.0) * pricing.outputPricePerK;

        return inputCost + outputCost;
    }

    /**
     * 計算需要 AI 的規則數量
     *
     * @param owaspVersion OWASP 版本
     * @return AI 規則數量
     */
    private int countAiRequiredRules(String owaspVersion) {
        return (int) registry.getRulesByVersion(owaspVersion).stream()
            .filter(OwaspRule::requiresAi)
            .count();
    }

    /**
     * AI 定價資訊
     */
    private static class AiPricing {
        private final double inputPricePerK;  // 每 1K input tokens 的價格
        private final double outputPricePerK; // 每 1K output tokens 的價格

        public AiPricing(double inputPricePerK, double outputPricePerK) {
            this.inputPricePerK = inputPricePerK;
            this.outputPricePerK = outputPricePerK;
        }
    }

    /**
     * 單一檔案成本估算結果
     */
    public static class CostEstimate {
        private final Path filePath;
        private final int codeTokens;
        private final int inputTokens;
        private final int outputTokens;
        private final int aiRulesCount;
        private final double estimatedCost;
        private final String aiModel;

        public CostEstimate(Path filePath, int codeTokens, int inputTokens, int outputTokens,
                            int aiRulesCount, double estimatedCost, String aiModel) {
            this.filePath = filePath;
            this.codeTokens = codeTokens;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.aiRulesCount = aiRulesCount;
            this.estimatedCost = estimatedCost;
            this.aiModel = aiModel;
        }

        public Path getFilePath() {
            return filePath;
        }

        public int getCodeTokens() {
            return codeTokens;
        }

        public int getInputTokens() {
            return inputTokens;
        }

        public int getOutputTokens() {
            return outputTokens;
        }

        public int getTotalTokens() {
            return inputTokens + outputTokens;
        }

        public int getAiRulesCount() {
            return aiRulesCount;
        }

        public double getEstimatedCost() {
            return estimatedCost;
        }

        public String getAiModel() {
            return aiModel;
        }

        @Override
        public String toString() {
            return String.format(
                "CostEstimate{file=%s, codeTokens=%d, totalTokens=%d, aiRules=%d, cost=$%.4f, model=%s}",
                filePath.getFileName(), codeTokens, getTotalTokens(), aiRulesCount, estimatedCost, aiModel
            );
        }
    }

    /**
     * 批次成本估算結果
     */
    public static class BatchCostEstimate {
        private final List<CostEstimate> fileEstimates;
        private final String aiModel;

        public BatchCostEstimate(List<CostEstimate> fileEstimates, String aiModel) {
            this.fileEstimates = fileEstimates;
            this.aiModel = aiModel;
        }

        public List<CostEstimate> getFileEstimates() {
            return fileEstimates;
        }

        public String getAiModel() {
            return aiModel;
        }

        public int getTotalFiles() {
            return fileEstimates.size();
        }

        public int getTotalCodeTokens() {
            return fileEstimates.stream().mapToInt(CostEstimate::getCodeTokens).sum();
        }

        public int getTotalInputTokens() {
            return fileEstimates.stream().mapToInt(CostEstimate::getInputTokens).sum();
        }

        public int getTotalOutputTokens() {
            return fileEstimates.stream().mapToInt(CostEstimate::getOutputTokens).sum();
        }

        public int getTotalTokens() {
            return getTotalInputTokens() + getTotalOutputTokens();
        }

        public int getTotalAiRulesCount() {
            return fileEstimates.stream().mapToInt(CostEstimate::getAiRulesCount).sum();
        }

        public double getTotalEstimatedCost() {
            return fileEstimates.stream().mapToDouble(CostEstimate::getEstimatedCost).sum();
        }

        public double getAverageCostPerFile() {
            return fileEstimates.isEmpty() ? 0.0 : getTotalEstimatedCost() / fileEstimates.size();
        }

        /**
         * 產生成本報告摘要
         *
         * @return 成本報告字串
         */
        public String generateSummary() {
            return String.format(
                "=== AI API Cost Estimate ===\n" +
                "Model: %s\n" +
                "Files: %d\n" +
                "Code Tokens: %,d\n" +
                "Total Tokens: %,d (Input: %,d, Output: %,d)\n" +
                "AI Rules Executions: %,d\n" +
                "Estimated Cost: $%.4f USD\n" +
                "Average Cost/File: $%.4f USD\n" +
                "============================",
                aiModel,
                getTotalFiles(),
                getTotalCodeTokens(),
                getTotalTokens(),
                getTotalInputTokens(),
                getTotalOutputTokens(),
                getTotalAiRulesCount(),
                getTotalEstimatedCost(),
                getAverageCostPerFile()
            );
        }

        @Override
        public String toString() {
            return String.format(
                "BatchCostEstimate{files=%d, totalTokens=%,d, totalCost=$%.4f, model=%s}",
                getTotalFiles(), getTotalTokens(), getTotalEstimatedCost(), aiModel
            );
        }
    }
}
