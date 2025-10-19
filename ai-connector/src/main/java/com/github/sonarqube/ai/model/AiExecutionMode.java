package com.github.sonarqube.ai.model;

/**
 * AI 服務執行模式
 *
 * 定義 AI 服務的兩種執行方式：
 * - API 模式：透過 HTTP REST API 調用雲端 AI 服務
 * - CLI 模式：透過命令列工具調用本地或雲端 AI 服務
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9)
 */
public enum AiExecutionMode {

    /**
     * API 模式 - 使用 HTTP REST API 調用 AI 服務
     *
     * 優點：
     * - 標準化的 HTTP 協議
     * - 豐富的 API 功能
     * - 較好的錯誤處理
     *
     * 缺點：
     * - 需要 API Key
     * - 可能產生費用
     * - 需要網路連接
     */
    API("api", "API 模式", "透過 HTTP REST API 調用 AI 服務"),

    /**
     * CLI 模式 - 使用命令列工具調用 AI 服務
     *
     * 優點：
     * - 可能無需 API Key（某些工具）
     * - 可能降低費用
     * - 本地執行（某些工具）
     *
     * 缺點：
     * - 需要安裝 CLI 工具
     * - 輸出格式可能變更
     * - 執行速度可能較慢
     */
    CLI("cli", "CLI 模式", "透過命令列工具調用 AI 服務");

    private final String code;
    private final String displayName;
    private final String description;

    AiExecutionMode(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 從代碼字串轉換為枚舉值
     *
     * @param code 執行模式代碼（"api" 或 "cli"）
     * @return 對應的枚舉值
     * @throws IllegalArgumentException 如果代碼無效
     */
    public static AiExecutionMode fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return API; // 預設為 API 模式
        }

        for (AiExecutionMode mode : values()) {
            if (mode.code.equalsIgnoreCase(code.trim())) {
                return mode;
            }
        }

        throw new IllegalArgumentException("Unknown execution mode: " + code);
    }

    /**
     * 判斷是否為 API 模式
     */
    public boolean isApi() {
        return this == API;
    }

    /**
     * 判斷是否為 CLI 模式
     */
    public boolean isCli() {
        return this == CLI;
    }

    @Override
    public String toString() {
        return displayName + " (" + code + ")";
    }
}
