package com.github.sonarqube.ai.model;

/**
 * AI 配置類別
 *
 * 封裝 AI 服務的所有配置參數，包含 API 金鑰、模型選擇、超時設定等。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class AiConfig {

    private final AiModel model;
    private final String apiKey;
    private final String apiEndpoint;
    private final int timeoutSeconds;
    private final double temperature;
    private final int maxTokens;
    private final int maxRetries;
    private final long retryDelayMs;

    private AiConfig(Builder builder) {
        this.model = builder.model;
        this.apiKey = builder.apiKey;
        this.apiEndpoint = builder.apiEndpoint;
        this.timeoutSeconds = builder.timeoutSeconds;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.maxRetries = builder.maxRetries;
        this.retryDelayMs = builder.retryDelayMs;
    }

    public AiModel getModel() {
        return model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiEndpoint() {
        return apiEndpoint;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    /**
     * 驗證配置是否有效
     *
     * @return true 如果配置有效
     */
    public boolean isValid() {
        return model != null
            && apiKey != null && !apiKey.trim().isEmpty()
            && apiEndpoint != null && !apiEndpoint.trim().isEmpty()
            && timeoutSeconds > 0
            && temperature >= 0.0 && temperature <= 2.0
            && maxTokens > 0
            && maxRetries >= 0;
    }

    /**
     * 建立新的 Builder 實例
     *
     * @return Builder 實例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * AI 配置建構器
     */
    public static class Builder {
        private AiModel model;
        private String apiKey;
        private String apiEndpoint;
        private int timeoutSeconds = 60; // 預設 60 秒
        private double temperature = 0.3; // 預設 0.3（較低溫度，更確定性的回應）
        private int maxTokens = 4096; // 預設 4096
        private int maxRetries = 3; // 預設重試 3 次
        private long retryDelayMs = 1000; // 預設延遲 1 秒

        public Builder model(AiModel model) {
            this.model = model;
            // 自動設定預設 API endpoint
            if (model != null) {
                if (model.isOpenAI()) {
                    this.apiEndpoint = "https://api.openai.com/v1/chat/completions";
                } else if (model.isClaude()) {
                    this.apiEndpoint = "https://api.anthropic.com/v1/messages";
                }
            }
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder apiEndpoint(String apiEndpoint) {
            this.apiEndpoint = apiEndpoint;
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
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

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder retryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
            return this;
        }

        public AiConfig build() {
            AiConfig config = new AiConfig(this);
            if (!config.isValid()) {
                throw new IllegalStateException("Invalid AI configuration");
            }
            return config;
        }
    }

    @Override
    public String toString() {
        return String.format("AiConfig[model=%s, timeout=%ds, temperature=%.2f, maxTokens=%d]",
            model.getModelId(), timeoutSeconds, temperature, maxTokens);
    }
}
