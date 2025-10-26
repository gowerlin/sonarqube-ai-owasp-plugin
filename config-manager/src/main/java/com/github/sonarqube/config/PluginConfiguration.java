package com.github.sonarqube.config;

import com.github.sonarqube.version.OwaspVersion;

/**
 * 插件配置管理
 *
 * 集中管理插件的所有配置項目。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class PluginConfiguration {

    // OWASP 版本配置
    private OwaspVersion owaspVersion = OwaspVersion.getDefault();

    // AI 配置
    private boolean aiAnalysisEnabled = true;
    private String aiModel = "gpt-4";
    private int aiTimeout = 30000; // 30 秒

    // AI Rate Limiting 配置 (TPM - Tokens Per Minute)
    private boolean aiRateLimitEnabled = true; // 預設啟用 Rate Limiting
    private int aiMaxTokensPerMinute = 30000; // OpenAI 預設 TPM 限制
    private double aiRateLimitBufferRatio = 0.9; // 使用 90% 的限制，保留 10% 緩衝
    private String aiRateLimitStrategy = "adaptive"; // adaptive 或 fixed

    // 快取配置
    private boolean cacheEnabled = true;
    private long cacheMaxSize = 1000L;
    private long cacheTtlHours = 24L;

    // 報告配置
    private String reportFormat = "markdown";
    private boolean autoGenerateReport = true;

    // 規則配置
    private boolean enableAllRules = true;
    private String[] enabledLanguages = {"java", "javascript"};

    // Singleton instance
    private static PluginConfiguration instance;

    private PluginConfiguration() {}

    public static PluginConfiguration getInstance() {
        if (instance == null) {
            synchronized (PluginConfiguration.class) {
                if (instance == null) {
                    instance = new PluginConfiguration();
                }
            }
        }
        return instance;
    }

    // Getters and Setters

    public OwaspVersion getOwaspVersion() {
        return owaspVersion;
    }

    public void setOwaspVersion(OwaspVersion owaspVersion) {
        this.owaspVersion = owaspVersion;
    }

    public boolean isAiAnalysisEnabled() {
        return aiAnalysisEnabled;
    }

    public void setAiAnalysisEnabled(boolean aiAnalysisEnabled) {
        this.aiAnalysisEnabled = aiAnalysisEnabled;
    }

    public String getAiModel() {
        return aiModel;
    }

    public void setAiModel(String aiModel) {
        this.aiModel = aiModel;
    }

    public int getAiTimeout() {
        return aiTimeout;
    }

    public void setAiTimeout(int aiTimeout) {
        this.aiTimeout = aiTimeout;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public long getCacheMaxSize() {
        return cacheMaxSize;
    }

    public void setCacheMaxSize(long cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }

    public long getCacheTtlHours() {
        return cacheTtlHours;
    }

    public void setCacheTtlHours(long cacheTtlHours) {
        this.cacheTtlHours = cacheTtlHours;
    }

    public String getReportFormat() {
        return reportFormat;
    }

    public void setReportFormat(String reportFormat) {
        this.reportFormat = reportFormat;
    }

    public boolean isAutoGenerateReport() {
        return autoGenerateReport;
    }

    public void setAutoGenerateReport(boolean autoGenerateReport) {
        this.autoGenerateReport = autoGenerateReport;
    }

    public boolean isEnableAllRules() {
        return enableAllRules;
    }

    public void setEnableAllRules(boolean enableAllRules) {
        this.enableAllRules = enableAllRules;
    }

    public String[] getEnabledLanguages() {
        return enabledLanguages;
    }

    public void setEnabledLanguages(String[] enabledLanguages) {
        this.enabledLanguages = enabledLanguages;
    }

    public boolean isAiRateLimitEnabled() {
        return aiRateLimitEnabled;
    }

    public void setAiRateLimitEnabled(boolean aiRateLimitEnabled) {
        this.aiRateLimitEnabled = aiRateLimitEnabled;
    }

    public int getAiMaxTokensPerMinute() {
        return aiMaxTokensPerMinute;
    }

    public void setAiMaxTokensPerMinute(int aiMaxTokensPerMinute) {
        this.aiMaxTokensPerMinute = aiMaxTokensPerMinute;
    }

    public double getAiRateLimitBufferRatio() {
        return aiRateLimitBufferRatio;
    }

    public void setAiRateLimitBufferRatio(double aiRateLimitBufferRatio) {
        this.aiRateLimitBufferRatio = aiRateLimitBufferRatio;
    }

    public String getAiRateLimitStrategy() {
        return aiRateLimitStrategy;
    }

    public void setAiRateLimitStrategy(String aiRateLimitStrategy) {
        this.aiRateLimitStrategy = aiRateLimitStrategy;
    }

    /**
     * 重置為預設配置
     */
    public void resetToDefaults() {
        this.owaspVersion = OwaspVersion.getDefault();
        this.aiAnalysisEnabled = true;
        this.aiModel = "gpt-4";
        this.aiTimeout = 30000;
        this.aiRateLimitEnabled = true;
        this.aiMaxTokensPerMinute = 30000;
        this.aiRateLimitBufferRatio = 0.9;
        this.aiRateLimitStrategy = "adaptive";
        this.cacheEnabled = true;
        this.cacheMaxSize = 1000L;
        this.cacheTtlHours = 24L;
        this.reportFormat = "markdown";
        this.autoGenerateReport = true;
        this.enableAllRules = true;
        this.enabledLanguages = new String[]{"java", "javascript"};
    }

    @Override
    public String toString() {
        return "PluginConfiguration{" +
            "owaspVersion=" + owaspVersion +
            ", aiAnalysisEnabled=" + aiAnalysisEnabled +
            ", aiModel='" + aiModel + '\'' +
            ", cacheEnabled=" + cacheEnabled +
            ", reportFormat='" + reportFormat + '\'' +
            '}';
    }
}
