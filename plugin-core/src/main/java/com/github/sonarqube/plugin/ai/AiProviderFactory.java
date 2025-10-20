package com.github.sonarqube.plugin.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AI Provider 工廠
 *
 * 統一管理 AI 模型的 API 模式與 CLI 模式，
 * 根據配置自動選擇最佳執行方式。
 *
 * 支援模式：
 * - API 模式：OpenAI API, Anthropic API, Google Gemini API
 * - CLI 模式：Gemini CLI, Copilot CLI, Claude CLI
 *
 * 模式選擇邏輯：
 * 1. 檢查配置的 provider 類型（openai, anthropic, gemini-api, gemini-cli, copilot-cli, claude-cli）
 * 2. 對於 CLI 模式，驗證 CLI 工具是否可用
 * 3. 若 CLI 不可用，自動降級到對應的 API 模式（若有配置 API 金鑰）
 * 4. 記錄模式切換日誌
 *
 * @since 3.0.0 (Epic 9)
 * @author SonarQube AI OWASP Plugin Team
 */
public class AiProviderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(AiProviderFactory.class);

    /**
     * AI Provider 類型
     */
    public enum ProviderType {
        OPENAI_API("openai"),
        ANTHROPIC_API("anthropic"),
        GEMINI_API("gemini-api"),
        GEMINI_CLI("gemini-cli"),
        COPILOT_CLI("copilot-cli"),
        CLAUDE_CLI("claude-cli");

        private final String configValue;

        ProviderType(String configValue) {
            this.configValue = configValue;
        }

        public String getConfigValue() {
            return configValue;
        }

        public static ProviderType fromConfigValue(String value) {
            for (ProviderType type : values()) {
                if (type.configValue.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown provider type: " + value);
        }

        public boolean isCli() {
            return this == GEMINI_CLI || this == COPILOT_CLI || this == CLAUDE_CLI;
        }

        public boolean isApi() {
            return this == OPENAI_API || this == ANTHROPIC_API || this == GEMINI_API;
        }
    }

    /**
     * AI Provider 配置
     */
    public static class ProviderConfig {
        private final ProviderType type;
        private final String apiKey;
        private final String model;
        private final double temperature;
        private final int maxTokens;
        private final int timeoutSeconds;

        // CLI 配置
        private final String cliPath;

        private ProviderConfig(Builder builder) {
            this.type = builder.type;
            this.apiKey = builder.apiKey;
            this.model = builder.model;
            this.temperature = builder.temperature;
            this.maxTokens = builder.maxTokens;
            this.timeoutSeconds = builder.timeoutSeconds;
            this.cliPath = builder.cliPath;
        }

        public ProviderType getType() {
            return type;
        }

        public String getApiKey() {
            return apiKey;
        }

        public String getModel() {
            return model;
        }

        public double getTemperature() {
            return temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public String getCliPath() {
            return cliPath;
        }

        public static Builder builder(ProviderType type) {
            return new Builder(type);
        }

        public static class Builder {
            private final ProviderType type;
            private String apiKey;
            private String model;
            private double temperature = 0.3;
            private int maxTokens = 2000;
            private int timeoutSeconds = 60;
            private String cliPath;

            public Builder(ProviderType type) {
                this.type = type;
            }

            public Builder apiKey(String apiKey) {
                this.apiKey = apiKey;
                return this;
            }

            public Builder model(String model) {
                this.model = model;
                return this;
            }

            public Builder temperature(double temperature) {
                this.temperature = temperature;
                return this;
            }

            public Builder maxTokens(int maxTokens) {
                this.maxTokens = maxTokens;
                return this;
            }

            public Builder timeoutSeconds(int timeoutSeconds) {
                this.timeoutSeconds = timeoutSeconds;
                return this;
            }

            public Builder cliPath(String cliPath) {
                this.cliPath = cliPath;
                return this;
            }

            public ProviderConfig build() {
                return new ProviderConfig(this);
            }
        }
    }

    /**
     * 建立 AI Provider 執行器
     *
     * @param config Provider 配置
     * @return CLI 執行器或 null（如果是 API 模式）
     */
    public static CliExecutor createExecutor(ProviderConfig config) {
        LOG.info("建立 AI Provider: type={}, model={}", config.getType(), config.getModel());

        if (!config.getType().isCli()) {
            LOG.info("API 模式不需要 CLI 執行器: {}", config.getType());
            return null;
        }

        try {
            CliExecutor executor = createCliExecutor(config);

            // 驗證 CLI 工具是否可用
            if (!executor.isAvailable()) {
                LOG.warn("CLI 工具不可用: {}, path={}", config.getType(), config.getCliPath());
                return null;
            }

            LOG.info("CLI 執行器建立成功: {}, version={}", config.getType(), executor.getVersion());
            return executor;

        } catch (Exception e) {
            LOG.error("建立 CLI 執行器失敗: {}", config.getType(), e);
            return null;
        }
    }

    /**
     * 建立 CLI 執行器
     */
    private static CliExecutor createCliExecutor(ProviderConfig config) {
        switch (config.getType()) {
            case GEMINI_CLI:
                return new GeminiCliExecutor(
                        config.getCliPath(),
                        config.getModel(),
                        config.getTemperature(),
                        config.getMaxTokens(),
                        config.getTimeoutSeconds()
                );

            case COPILOT_CLI:
                return new CopilotCliExecutor(
                        config.getCliPath(),
                        config.getTimeoutSeconds()
                );

            case CLAUDE_CLI:
                return new ClaudeCliExecutor(
                        config.getCliPath(),
                        config.getModel(),
                        config.getTemperature(),
                        config.getMaxTokens(),
                        config.getTimeoutSeconds()
                );

            default:
                throw new IllegalArgumentException("Unsupported CLI provider: " + config.getType());
        }
    }

    /**
     * 檢查 CLI 工具是否已安裝並可用
     *
     * @param type Provider 類型
     * @param cliPath CLI 路徑
     * @return true 如果 CLI 可用
     */
    public static boolean isCliAvailable(ProviderType type, String cliPath) {
        if (!type.isCli()) {
            return false;
        }

        try {
            ProviderConfig config = ProviderConfig.builder(type)
                    .cliPath(cliPath)
                    .build();

            CliExecutor executor = createCliExecutor(config);
            return executor.isAvailable();

        } catch (Exception e) {
            LOG.debug("檢查 CLI 可用性失敗: {}", type, e);
            return false;
        }
    }

    /**
     * 取得 CLI 版本資訊
     *
     * @param type Provider 類型
     * @param cliPath CLI 路徑
     * @return 版本字串
     */
    public static String getCliVersion(ProviderType type, String cliPath) {
        if (!type.isCli()) {
            return "N/A (API mode)";
        }

        try {
            ProviderConfig config = ProviderConfig.builder(type)
                    .cliPath(cliPath)
                    .build();

            CliExecutor executor = createCliExecutor(config);
            return executor.getVersion();

        } catch (Exception e) {
            LOG.debug("取得 CLI 版本失敗: {}", type, e);
            return "unknown";
        }
    }

    /**
     * 取得推薦的 CLI 路徑
     *
     * @param type Provider 類型
     * @return 預設 CLI 路徑
     */
    public static String getDefaultCliPath(ProviderType type) {
        switch (type) {
            case GEMINI_CLI:
                return "/usr/local/bin/gemini";
            case COPILOT_CLI:
                return "/usr/local/bin/gh";
            case CLAUDE_CLI:
                return "/usr/local/bin/claude";
            default:
                return null;
        }
    }

    /**
     * 檢查 CLI 認證狀態
     *
     * @param type Provider 類型
     * @param cliPath CLI 路徑
     * @return 認證資訊字串
     */
    public static String getAuthenticationStatus(ProviderType type, String cliPath) {
        if (!type.isCli()) {
            return "N/A (API mode)";
        }

        try {
            ProviderConfig config = ProviderConfig.builder(type)
                    .cliPath(cliPath)
                    .build();

            CliExecutor executor = createCliExecutor(config);

            if (executor instanceof GeminiCliExecutor) {
                GeminiCliExecutor gemini = (GeminiCliExecutor) executor;
                return gemini.isAuthenticated() ?
                        "Authenticated: " + gemini.getAuthenticatedAccount() :
                        "Not authenticated";
            } else if (executor instanceof CopilotCliExecutor) {
                CopilotCliExecutor copilot = (CopilotCliExecutor) executor;
                return copilot.isAuthenticated() ?
                        "Authenticated: " + copilot.getAuthenticatedUser() :
                        "Not authenticated";
            } else if (executor instanceof ClaudeCliExecutor) {
                ClaudeCliExecutor claude = (ClaudeCliExecutor) executor;
                return claude.isConfigured() ?
                        "Configured" :
                        "Not configured";
            }

            return "unknown";

        } catch (Exception e) {
            LOG.debug("檢查認證狀態失敗: {}", type, e);
            return "Error: " + e.getMessage();
        }
    }
}
