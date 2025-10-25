package com.github.sonarqube.ai.model;

/**
 * AI 模型列舉
 *
 * 定義支援的 AI 模型及其配置參數。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public enum AiModel {

    // ============================================================
    // OpenAI Models (2025 最新，按推薦順序排列)
    // ============================================================

    /** OpenAI GPT-4o 模型 - 旗艦模型 ($2.5/1M in, $10/1M out) */
    GPT_4O("gpt-4o", "OpenAI", 16384, 128000),

    /** OpenAI GPT-4o Mini 模型 - 極高性價比 ($0.15/1M in, $0.6/1M out) - 最推薦 */
    GPT_4O_MINI("gpt-4o-mini", "OpenAI", 16384, 128000),

    /** OpenAI GPT-4 Turbo 模型 - 穩定版 ($10/1M in, $30/1M out) */
    GPT_4_TURBO("gpt-4-turbo", "OpenAI", 4096, 128000),

    /** OpenAI GPT-4 模型 - 經典版本 */
    GPT_4("gpt-4", "OpenAI", 8192, 128000),

    /** OpenAI GPT-3.5 Turbo 模型 - 經濟版本 */
    GPT_3_5_TURBO("gpt-3.5-turbo", "OpenAI", 4096, 16385),

    // ============================================================
    // Anthropic Claude Models (2025 最新，按推薦順序排列)
    // ============================================================

    /** Anthropic Claude Sonnet 4.5 - 最聰明模型 (2025-09) - $3/1M in, $15/1M out - 最推薦 */
    CLAUDE_SONNET_4_5("claude-sonnet-4-5-20250929", "Anthropic", 8192, 200000),

    /** Anthropic Claude Haiku 4.5 - 超高性價比 (2025-10) - $1/1M in, $5/1M out - 最推薦 */
    CLAUDE_HAIKU_4_5("claude-haiku-4-5-20251001", "Anthropic", 8192, 200000),

    /** Anthropic Claude Opus 4.1 - 旗艦版本 (2025-08) - $15/1M in, $75/1M out */
    CLAUDE_OPUS_4_1("claude-opus-4-1-20250805", "Anthropic", 8192, 200000),

    /** Anthropic Claude Sonnet 4 - 平衡版本 (2025-05) - $3/1M in, $15/1M out */
    CLAUDE_SONNET_4("claude-sonnet-4-20250514", "Anthropic", 8192, 200000),

    /** Anthropic Claude Opus 4 - 推理專用 (2025-05) - $15/1M in, $75/1M out */
    CLAUDE_OPUS_4("claude-opus-4-20250514", "Anthropic", 8192, 200000),

    /** Anthropic Claude 3.7 Sonnet - 穩定版 (2025-02) - $3/1M in, $15/1M out */
    CLAUDE_3_7_SONNET("claude-3-7-sonnet-20250219", "Anthropic", 8192, 200000),

    /** Anthropic Claude 3.5 Sonnet - 頂尖編碼 (2024-10) - $3/1M in, $15/1M out */
    CLAUDE_3_5_SONNET("claude-3-5-sonnet-20241022", "Anthropic", 8192, 200000),

    /** Anthropic Claude 3.5 Haiku - 快速版 (2024-10) - $1/1M in, $5/1M out */
    CLAUDE_3_5_HAIKU("claude-3-5-haiku-20241022", "Anthropic", 8192, 200000),

    /** Anthropic Claude 3 Opus - 最強推理 ($15/1M in, $75/1M out) */
    CLAUDE_3_OPUS("claude-3-opus-20240229", "Anthropic", 4096, 200000),

    /** Anthropic Claude 3 Sonnet - 平衡版本 */
    CLAUDE_3_SONNET("claude-3-sonnet-20240229", "Anthropic", 4096, 200000),

    /** Anthropic Claude 3 Haiku - 快速經濟版本 */
    CLAUDE_3_HAIKU("claude-3-haiku-20240307", "Anthropic", 4096, 200000),

    // ============================================================
    // Google Gemini Models (2025 最新，按推薦順序排列)
    // ============================================================

    /** Google Gemini 2.5 Pro - 最新 thinking 模型 (2025) */
    GEMINI_2_5_PRO("gemini-2.5-pro", "Google", 8192, 2000000),

    /** Google Gemini 2.5 Flash - thinking enabled (2025) - 最推薦 */
    GEMINI_2_5_FLASH("gemini-2.5-flash", "Google", 8192, 1000000),

    /** Google Gemini 2.5 Flash Lite - 超快超便宜 ($0.1/1M in, $0.4/1M out) - 最推薦 */
    GEMINI_2_5_FLASH_LITE("gemini-2.5-flash-lite", "Google", 8192, 1000000),

    /** Google Gemini 2.0 Flash - $0.1/1M in, $0.4/1M out */
    GEMINI_2_0_FLASH("gemini-2.0-flash", "Google", 8192, 1000000),

    /** Google Gemini 2.0 Flash Lite - 預覽版，最低成本 */
    GEMINI_2_0_FLASH_LITE("gemini-2.0-flash-lite", "Google", 8192, 1000000),

    /** Google Gemini 2.0 Pro Experimental - 編碼專用實驗版 */
    GEMINI_2_0_PRO_EXP("gemini-2.0-pro-exp", "Google", 8192, 1000000),

    /** Google Gemini 1.5 Pro - 旗艦版本 (2M context) */
    GEMINI_1_5_PRO("gemini-1.5-pro", "Google", 8192, 2000000),

    /** Google Gemini 1.5 Flash - 快速版本 (1M context) */
    GEMINI_1_5_FLASH("gemini-1.5-flash", "Google", 8192, 1000000),

    /** Google Gemini Pro - 穩定版本 */
    GEMINI_PRO("gemini-pro", "Google", 8192, 32768);

    private final String modelId;
    private final String provider;
    private final int maxOutputTokens;
    private final int maxContextTokens;

    AiModel(String modelId, String provider, int maxOutputTokens, int maxContextTokens) {
        this.modelId = modelId;
        this.provider = provider;
        this.maxOutputTokens = maxOutputTokens;
        this.maxContextTokens = maxContextTokens;
    }

    /**
     * 獲取模型 ID
     *
     * @return 模型 ID 字串
     */
    public String getModelId() {
        return modelId;
    }

    /**
     * 獲取提供者名稱
     *
     * @return 提供者名稱 ("OpenAI" 或 "Anthropic")
     */
    public String getProvider() {
        return provider;
    }

    /**
     * 獲取最大輸出 token 數
     *
     * @return 最大輸出 token 數
     */
    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    /**
     * 獲取最大上下文 token 數
     *
     * @return 最大上下文 token 數（包含輸入和輸出）
     */
    public int getMaxContextTokens() {
        return maxContextTokens;
    }

    /**
     * 根據模型 ID 字串查找對應的 AiModel
     *
     * @param modelId 模型 ID 字串
     * @return 對應的 AiModel，如果找不到則返回 null
     */
    public static AiModel fromModelId(String modelId) {
        for (AiModel model : values()) {
            if (model.modelId.equalsIgnoreCase(modelId)) {
                return model;
            }
        }
        return null;
    }

    /**
     * 檢查是否為 OpenAI 模型
     *
     * @return true 如果是 OpenAI 模型
     */
    public boolean isOpenAI() {
        return "OpenAI".equals(provider);
    }

    /**
     * 檢查是否為 Claude 模型
     *
     * @return true 如果是 Claude 模型
     */
    public boolean isClaude() {
        return "Anthropic".equals(provider);
    }

    /**
     * 檢查是否為 Gemini 模型
     *
     * @return true 如果是 Gemini 模型
     */
    public boolean isGemini() {
        return "Google".equals(provider);
    }
}
