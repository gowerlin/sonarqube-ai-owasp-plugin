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

    /** OpenAI GPT-4 模型 */
    GPT_4("gpt-4", "OpenAI", 8192, 128000),

    /** OpenAI GPT-4 Turbo 模型 */
    GPT_4_TURBO("gpt-4-turbo", "OpenAI", 4096, 128000),

    /** OpenAI GPT-3.5 Turbo 模型 */
    GPT_3_5_TURBO("gpt-3.5-turbo", "OpenAI", 4096, 16385),

    /** Anthropic Claude 3 Opus 模型 */
    CLAUDE_3_OPUS("claude-3-opus-20240229", "Anthropic", 4096, 200000),

    /** Anthropic Claude 3 Sonnet 模型 */
    CLAUDE_3_SONNET("claude-3-sonnet-20240229", "Anthropic", 4096, 200000),

    /** Anthropic Claude 3 Haiku 模型 */
    CLAUDE_3_HAIKU("claude-3-haiku-20240307", "Anthropic", 4096, 200000),

    /** Google Gemini 1.5 Pro 模型 */
    GEMINI_1_5_PRO("gemini-1.5-pro", "Google", 8192, 1000000),

    /** Google Gemini 1.5 Flash 模型 */
    GEMINI_1_5_FLASH("gemini-1.5-flash", "Google", 8192, 1000000);

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
