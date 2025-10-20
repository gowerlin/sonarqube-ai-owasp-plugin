package com.github.sonarqube.plugin.cost;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Cost Estimator 單元測試
 *
 * 測試範圍：
 * - 成本計算（8 種 AI 提供商）
 * - Token 統計
 * - 預算控制
 * - 警告機制
 * - 成本比較
 *
 * @since 3.0.0 (Epic 8, Story 8.1)
 */
@DisplayName("CostEstimator Unit Tests")
public class CostEstimatorTest {

    private CostEstimator costEstimator;

    @BeforeEach
    void setUp() {
        // 預算 10 美元
        costEstimator = new CostEstimator(10.0);
    }

    @Test
    @DisplayName("測試 GPT-4 成本計算")
    void testGpt4CostCalculation() {
        // GPT-4: $0.03/1K input, $0.06/1K output
        double cost = costEstimator.calculateCost(
                CostEstimator.AiProvider.GPT_4,
                1000, // 1K input tokens
                1000  // 1K output tokens
        );

        // 預期: 0.03 + 0.06 = 0.09
        assertEquals(0.09, cost, 0.001);
    }

    @Test
    @DisplayName("測試 GPT-3.5 Turbo 成本計算")
    void testGpt35CostCalculation() {
        // GPT-3.5 Turbo: $0.0005/1K input, $0.0015/1K output
        double cost = costEstimator.calculateCost(
                CostEstimator.AiProvider.GPT_3_5_TURBO,
                2000, // 2K input tokens
                2000  // 2K output tokens
        );

        // 預期: (2 * 0.0005) + (2 * 0.0015) = 0.001 + 0.003 = 0.004
        assertEquals(0.004, cost, 0.0001);
    }

    @Test
    @DisplayName("測試 Claude 3 Opus 成本計算")
    void testClaude3OpusCostCalculation() {
        // Claude 3 Opus: $0.015/1K input, $0.075/1K output
        double cost = costEstimator.calculateCost(
                CostEstimator.AiProvider.CLAUDE_3_OPUS,
                1000,
                1000
        );

        // 預期: 0.015 + 0.075 = 0.09
        assertEquals(0.09, cost, 0.001);
    }

    @Test
    @DisplayName("測試 Claude 3 Sonnet 成本計算")
    void testClaude3SonnetCostCalculation() {
        // Claude 3 Sonnet: $0.003/1K input, $0.015/1K output
        double cost = costEstimator.calculateCost(
                CostEstimator.AiProvider.CLAUDE_3_SONNET,
                5000,
                3000
        );

        // 預期: (5 * 0.003) + (3 * 0.015) = 0.015 + 0.045 = 0.06
        assertEquals(0.06, cost, 0.001);
    }

    @Test
    @DisplayName("測試 Gemini Pro 成本計算")
    void testGeminiProCostCalculation() {
        // Gemini Pro: $0.00025/1K input, $0.0005/1K output
        double cost = costEstimator.calculateCost(
                CostEstimator.AiProvider.GEMINI_PRO,
                10000,
                10000
        );

        // 預期: (10 * 0.00025) + (10 * 0.0005) = 0.0025 + 0.005 = 0.0075
        assertEquals(0.0075, cost, 0.0001);
    }

    @Test
    @DisplayName("測試 API 呼叫記錄")
    void testRecordApiCall() {
        costEstimator.recordApiCall(
                CostEstimator.AiProvider.GPT_4,
                1000,
                500
        );

        CostEstimator.CostStatistics stats = costEstimator.getStatistics();

        assertEquals(1, stats.totalApiCalls);
        assertEquals(1000, stats.totalInputTokens);
        assertEquals(500, stats.totalOutputTokens);
    }

    @Test
    @DisplayName("測試多次 API 呼叫累計")
    void testMultipleApiCalls() {
        // 呼叫 3 次
        costEstimator.recordApiCall(CostEstimator.AiProvider.GPT_4, 1000, 500);
        costEstimator.recordApiCall(CostEstimator.AiProvider.GPT_4, 2000, 1000);
        costEstimator.recordApiCall(CostEstimator.AiProvider.GPT_3_5_TURBO, 500, 250);

        CostEstimator.CostStatistics stats = costEstimator.getStatistics();

        assertEquals(3, stats.totalApiCalls);
        assertEquals(3500, stats.totalInputTokens);
        assertEquals(1750, stats.totalOutputTokens);
    }

    @Test
    @DisplayName("測試預算警告 - 75% 門檻")
    void testBudgetWarning75Percent() {
        // 預算 10 美元，使用到 7.5 美元（75%）
        // GPT-4: $0.03/1K input
        // 需要 250K input tokens 達到 7.5 美元
        costEstimator.recordApiCall(
                CostEstimator.AiProvider.GPT_4,
                250000,
                0
        );

        CostEstimator.CostStatistics stats = costEstimator.getStatistics();

        // 應該觸發 75% 警告
        assertTrue(stats.budgetUsagePercent >= 75.0);
        assertTrue(stats.budgetUsagePercent < 90.0);
    }

    @Test
    @DisplayName("測試預算警告 - 90% 門檻")
    void testBudgetWarning90Percent() {
        // 預算 10 美元，使用到 9 美元（90%）
        // GPT-4: $0.03/1K input
        // 需要 300K input tokens 達到 9 美元
        costEstimator.recordApiCall(
                CostEstimator.AiProvider.GPT_4,
                300000,
                0
        );

        CostEstimator.CostStatistics stats = costEstimator.getStatistics();

        // 應該觸發 90% 警告
        assertTrue(stats.budgetUsagePercent >= 90.0);
    }

    @Test
    @DisplayName("測試預算超支檢測")
    void testBudgetExceeded() {
        // 預算 10 美元，使用超過 10 美元
        // GPT-4: $0.03/1K input
        // 需要 350K input tokens 達到 10.5 美元
        costEstimator.recordApiCall(
                CostEstimator.AiProvider.GPT_4,
                350000,
                0
        );

        CostEstimator.CostStatistics stats = costEstimator.getStatistics();

        // 預算使用率應該超過 100%
        assertTrue(stats.budgetUsagePercent > 100.0);
    }

    @Test
    @DisplayName("測試成本比較 - GPT-4 vs GPT-3.5")
    void testCostComparison() {
        // 相同 token 數量
        double gpt4Cost = costEstimator.calculateCost(
                CostEstimator.AiProvider.GPT_4,
                10000,
                10000
        );

        double gpt35Cost = costEstimator.calculateCost(
                CostEstimator.AiProvider.GPT_3_5_TURBO,
                10000,
                10000
        );

        // GPT-4 應該比 GPT-3.5 貴很多
        assertTrue(gpt4Cost > gpt35Cost * 10,
                "GPT-4 成本應該顯著高於 GPT-3.5");
    }

    @Test
    @DisplayName("測試成本比較 - Claude vs Gemini")
    void testClaudeVsGeminiCost() {
        double claudeCost = costEstimator.calculateCost(
                CostEstimator.AiProvider.CLAUDE_3_SONNET,
                10000,
                10000
        );

        double geminiCost = costEstimator.calculateCost(
                CostEstimator.AiProvider.GEMINI_PRO,
                10000,
                10000
        );

        // Claude 應該比 Gemini 貴
        assertTrue(claudeCost > geminiCost,
                "Claude 成本應該高於 Gemini");
    }

    @Test
    @DisplayName("測試重置統計")
    void testResetStatistics() {
        costEstimator.recordApiCall(CostEstimator.AiProvider.GPT_4, 1000, 500);

        CostEstimator.CostStatistics statsBefore = costEstimator.getStatistics();
        assertTrue(statsBefore.totalApiCalls > 0);

        // 重置
        costEstimator.reset();

        CostEstimator.CostStatistics statsAfter = costEstimator.getStatistics();
        assertEquals(0, statsAfter.totalApiCalls);
        assertEquals(0, statsAfter.totalInputTokens);
        assertEquals(0, statsAfter.totalOutputTokens);
    }

    @Test
    @DisplayName("測試估算單檔分析成本")
    void testEstimateSingleFileAnalysisCost() {
        double estimatedCost = costEstimator.estimateSingleFileAnalysisCost(
                CostEstimator.AiProvider.GPT_4,
                500 // 檔案行數
        );

        // 估算成本應該大於 0
        assertTrue(estimatedCost > 0);

        // 500 行檔案，GPT-4 成本應該約 $0.01-0.05
        assertTrue(estimatedCost >= 0.01 && estimatedCost <= 0.1,
                "單檔估算成本不合理: " + estimatedCost);
    }

    @Test
    @DisplayName("測試估算專案掃描成本")
    void testEstimateProjectScanCost() {
        double estimatedCost = costEstimator.estimateProjectScanCost(
                CostEstimator.AiProvider.GPT_4,
                100 // 100 個檔案
        );

        // 估算成本應該大於 0
        assertTrue(estimatedCost > 0);

        // 100 個檔案，GPT-4 成本應該約 $1-10
        assertTrue(estimatedCost >= 0.5 && estimatedCost <= 20,
                "專案估算成本不合理: " + estimatedCost);
    }

    @Test
    @DisplayName("測試零 Token 處理")
    void testZeroTokens() {
        double cost = costEstimator.calculateCost(
                CostEstimator.AiProvider.GPT_4,
                0,
                0
        );

        assertEquals(0.0, cost, "零 token 應該產生零成本");
    }

    @Test
    @DisplayName("測試大量 Token 計算")
    void testLargeTokenCalculation() {
        // 1M input tokens, 1M output tokens
        double cost = costEstimator.calculateCost(
                CostEstimator.AiProvider.GPT_4,
                1000000,
                1000000
        );

        // GPT-4: (1000 * $0.03) + (1000 * $0.06) = $30 + $60 = $90
        assertEquals(90.0, cost, 0.1);
    }

    @Test
    @DisplayName("測試所有提供商定價存在")
    void testAllProviderPricingExists() {
        // 確保所有提供商都有定價
        for (CostEstimator.AiProvider provider : CostEstimator.AiProvider.values()) {
            double cost = costEstimator.calculateCost(provider, 1000, 1000);
            assertTrue(cost >= 0, "提供商 " + provider + " 應該有有效定價");
        }
    }

    @Test
    @DisplayName("測試成本格式化")
    void testCostFormatting() {
        double cost = 1.23456789;

        // 格式化應該保留 4 位小數
        String formatted = String.format("%.4f", cost);
        assertEquals("1.2346", formatted);
    }

    @Test
    @DisplayName("測試預算使用率計算")
    void testBudgetUsagePercentageCalculation() {
        // 使用 5 美元（預算 10 美元）
        costEstimator.recordApiCall(
                CostEstimator.AiProvider.GPT_4,
                166667, // ≈ $5
                0
        );

        CostEstimator.CostStatistics stats = costEstimator.getStatistics();

        // 預算使用率應該約 50%
        assertTrue(stats.budgetUsagePercent >= 45.0 && stats.budgetUsagePercent <= 55.0,
                "預算使用率計算不正確: " + stats.budgetUsagePercent + "%");
    }
}
