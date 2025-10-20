package com.github.sonarqube.config;

/**
 * AI 模型參數配置
 *
 * 允許用戶調整 AI 參數（溫度、最大 token、超時）。
 * 支援多 AI 供應商的配置管理。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.5.0 (Epic 7, Story 7.2)
 */
public class AiConfiguration {

    // 預設值
    public static final double DEFAULT_TEMPERATURE = 0.7;
    public static final int DEFAULT_MAX_TOKENS = 2048;
    public static final long DEFAULT_TIMEOUT_MILLIS = 30000L; // 30 seconds
    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final boolean DEFAULT_ENABLE_CACHE = true;

    private final String aiProvider;        // "openai" 或 "claude"
    private final String modelName;         // 例：gpt-4o, claude-3.5-sonnet
    private final String apiKey;
    private final double temperature;       // 0.0 ~ 1.0
    private final int maxTokens;            // 最大 token 數
    private final long timeoutMillis;       // 超時時間（毫秒）
    private final int maxRetries;           // 最大重試次數
    private final boolean enableCache;      // 是否啟用快取

    private AiConfiguration(Builder builder) {
        this.aiProvider = builder.aiProvider;
        this.modelName = builder.modelName;
        this.apiKey = builder.apiKey;
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.timeoutMillis = builder.timeoutMillis;
        this.maxRetries = builder.maxRetries;
        this.enableCache = builder.enableCache;
    }

    // Getters
    public String getAiProvider() {
        return aiProvider;
    }

    public String getModelName() {
        return modelName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public boolean isEnableCache() {
        return enableCache;
    }

    /**
     * 驗證配置是否有效
     *
     * @return true 若配置有效
     */
    public boolean isValid() {
        return aiProvider != null && !aiProvider.isEmpty()
            && modelName != null && !modelName.isEmpty()
            && apiKey != null && !apiKey.isEmpty()
            && temperature >= 0.0 && temperature <= 1.0
            && maxTokens > 0
            && timeoutMillis > 0
            && maxRetries >= 0;
    }

    /**
     * 取得配置摘要（不包含 API Key）
     *
     * @return 配置摘要字串
     */
    public String getSummary() {
        return String.format(
            "AiConfiguration{provider=%s, model=%s, temp=%.2f, maxTokens=%d, timeout=%dms, retries=%d, cache=%s}",
            aiProvider, modelName, temperature, maxTokens, timeoutMillis, maxRetries, enableCache
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String aiProvider = "openai";
        private String modelName = "gpt-4o";
        private String apiKey;
        private double temperature = DEFAULT_TEMPERATURE;
        private int maxTokens = DEFAULT_MAX_TOKENS;
        private long timeoutMillis = DEFAULT_TIMEOUT_MILLIS;
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private boolean enableCache = DEFAULT_ENABLE_CACHE;

        public Builder aiProvider(String aiProvider) {
            this.aiProvider = aiProvider;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder temperature(double temperature) {
            if (temperature < 0.0 || temperature > 1.0) {
                throw new IllegalArgumentException("Temperature must be between 0.0 and 1.0");
            }
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            if (maxTokens <= 0) {
                throw new IllegalArgumentException("Max tokens must be positive");
            }
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder timeoutMillis(long timeoutMillis) {
            if (timeoutMillis <= 0) {
                throw new IllegalArgumentException("Timeout must be positive");
            }
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            if (maxRetries < 0) {
                throw new IllegalArgumentException("Max retries cannot be negative");
            }
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder enableCache(boolean enableCache) {
            this.enableCache = enableCache;
            return this;
        }

        public AiConfiguration build() {
            return new AiConfiguration(this);
        }
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
