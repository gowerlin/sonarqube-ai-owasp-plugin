package com.github.sonarqube.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置管理服務
 *
 * 集中管理 AI 配置和掃描範圍配置。
 * 支援專案級配置、全域配置和配置驗證。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.5.0 (Epic 7, Stories 7.2 & 7.3)
 */
public class ConfigurationManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationManager.class);

    // 單例實例
    private static volatile ConfigurationManager instance;

    // 專案級配置
    private final Map<String, AiConfiguration> projectAiConfigs = new ConcurrentHashMap<>();
    private final Map<String, ScanScopeConfiguration> projectScanConfigs = new ConcurrentHashMap<>();

    // 全域配置
    private volatile AiConfiguration globalAiConfig;
    private volatile ScanScopeConfiguration globalScanConfig;

    /**
     * 私有建構子（單例模式）
     */
    private ConfigurationManager() {
        // 初始化預設全域配置
        this.globalAiConfig = AiConfiguration.builder().build();
        this.globalScanConfig = ScanScopeConfiguration.builder().build();
    }

    /**
     * 取得單例實例
     *
     * @return ConfigurationManager 實例
     */
    public static ConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (ConfigurationManager.class) {
                if (instance == null) {
                    instance = new ConfigurationManager();
                }
            }
        }
        return instance;
    }

    // ==================== AI 配置管理 ====================

    /**
     * 設定專案級 AI 配置
     *
     * @param projectKey 專案鍵值
     * @param config AI 配置
     */
    public void setProjectAiConfiguration(String projectKey, AiConfiguration config) {
        if (!config.isValid()) {
            throw new IllegalArgumentException("Invalid AI configuration: " + config.getSummary());
        }
        projectAiConfigs.put(projectKey, config);
        logger.info("AI configuration set for project {}: {}", projectKey, config.getSummary());
    }

    /**
     * 取得專案級 AI 配置（若無則返回全域配置）
     *
     * @param projectKey 專案鍵值
     * @return AI 配置
     */
    public AiConfiguration getProjectAiConfiguration(String projectKey) {
        return projectAiConfigs.getOrDefault(projectKey, globalAiConfig);
    }

    /**
     * 設定全域 AI 配置
     *
     * @param config AI 配置
     */
    public void setGlobalAiConfiguration(AiConfiguration config) {
        if (!config.isValid()) {
            throw new IllegalArgumentException("Invalid global AI configuration: " + config.getSummary());
        }
        this.globalAiConfig = config;
        logger.info("Global AI configuration set: {}", config.getSummary());
    }

    /**
     * 取得全域 AI 配置
     *
     * @return AI 配置
     */
    public AiConfiguration getGlobalAiConfiguration() {
        return globalAiConfig;
    }

    /**
     * 移除專案級 AI 配置（回退至全域配置）
     *
     * @param projectKey 專案鍵值
     */
    public void removeProjectAiConfiguration(String projectKey) {
        AiConfiguration removed = projectAiConfigs.remove(projectKey);
        if (removed != null) {
            logger.info("AI configuration removed for project {}, falling back to global", projectKey);
        }
    }

    // ==================== 掃描範圍配置管理 ====================

    /**
     * 設定專案級掃描範圍配置
     *
     * @param projectKey 專案鍵值
     * @param config 掃描範圍配置
     */
    public void setProjectScanScopeConfiguration(String projectKey, ScanScopeConfiguration config) {
        projectScanConfigs.put(projectKey, config);
        logger.info("Scan scope configuration set for project {}: {}", projectKey, config.getSummary());
    }

    /**
     * 取得專案級掃描範圍配置（若無則返回全域配置）
     *
     * @param projectKey 專案鍵值
     * @return 掃描範圍配置
     */
    public ScanScopeConfiguration getProjectScanScopeConfiguration(String projectKey) {
        return projectScanConfigs.getOrDefault(projectKey, globalScanConfig);
    }

    /**
     * 設定全域掃描範圍配置
     *
     * @param config 掃描範圍配置
     */
    public void setGlobalScanScopeConfiguration(ScanScopeConfiguration config) {
        this.globalScanConfig = config;
        logger.info("Global scan scope configuration set: {}", config.getSummary());
    }

    /**
     * 取得全域掃描範圍配置
     *
     * @return 掃描範圍配置
     */
    public ScanScopeConfiguration getGlobalScanScopeConfiguration() {
        return globalScanConfig;
    }

    /**
     * 移除專案級掃描範圍配置（回退至全域配置）
     *
     * @param projectKey 專案鍵值
     */
    public void removeProjectScanScopeConfiguration(String projectKey) {
        ScanScopeConfiguration removed = projectScanConfigs.remove(projectKey);
        if (removed != null) {
            logger.info("Scan scope configuration removed for project {}, falling back to global", projectKey);
        }
    }

    // ==================== 配置驗證 ====================

    /**
     * 驗證專案配置是否完整且有效
     *
     * @param projectKey 專案鍵值
     * @return 驗證結果
     */
    public ConfigurationValidationResult validateProjectConfiguration(String projectKey) {
        AiConfiguration aiConfig = getProjectAiConfiguration(projectKey);
        ScanScopeConfiguration scanConfig = getProjectScanScopeConfiguration(projectKey);

        boolean aiValid = aiConfig.isValid();
        boolean scanValid = scanConfig != null;

        if (aiValid && scanValid) {
            return new ConfigurationValidationResult(true, "Configuration is valid");
        }

        StringBuilder errors = new StringBuilder();
        if (!aiValid) {
            errors.append("AI configuration is invalid. ");
        }
        if (!scanValid) {
            errors.append("Scan scope configuration is missing. ");
        }

        return new ConfigurationValidationResult(false, errors.toString());
    }

    // ==================== 配置重置 ====================

    /**
     * 重置所有配置為預設值
     */
    public void resetAllConfigurations() {
        projectAiConfigs.clear();
        projectScanConfigs.clear();
        this.globalAiConfig = AiConfiguration.builder().build();
        this.globalScanConfig = ScanScopeConfiguration.builder().build();
        logger.info("All configurations reset to defaults");
    }

    /**
     * 重置專案配置為全域配置
     *
     * @param projectKey 專案鍵值
     */
    public void resetProjectConfiguration(String projectKey) {
        removeProjectAiConfiguration(projectKey);
        removeProjectScanScopeConfiguration(projectKey);
        logger.info("Project configuration reset for {}", projectKey);
    }

    // ==================== 配置統計 ====================

    /**
     * 取得配置統計資訊
     *
     * @return 配置統計
     */
    public ConfigurationStatistics getStatistics() {
        return new ConfigurationStatistics(
            projectAiConfigs.size(),
            projectScanConfigs.size(),
            globalAiConfig,
            globalScanConfig
        );
    }

    /**
     * 配置驗證結果
     */
    public static class ConfigurationValidationResult {
        private final boolean valid;
        private final String message;

        public ConfigurationValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return String.format("ConfigurationValidationResult{valid=%s, message='%s'}", valid, message);
        }
    }

    /**
     * 配置統計資訊
     */
    public static class ConfigurationStatistics {
        private final int projectAiConfigCount;
        private final int projectScanConfigCount;
        private final AiConfiguration globalAiConfig;
        private final ScanScopeConfiguration globalScanConfig;

        public ConfigurationStatistics(int projectAiConfigCount, int projectScanConfigCount,
                                       AiConfiguration globalAiConfig, ScanScopeConfiguration globalScanConfig) {
            this.projectAiConfigCount = projectAiConfigCount;
            this.projectScanConfigCount = projectScanConfigCount;
            this.globalAiConfig = globalAiConfig;
            this.globalScanConfig = globalScanConfig;
        }

        public int getProjectAiConfigCount() {
            return projectAiConfigCount;
        }

        public int getProjectScanConfigCount() {
            return projectScanConfigCount;
        }

        public AiConfiguration getGlobalAiConfig() {
            return globalAiConfig;
        }

        public ScanScopeConfiguration getGlobalScanConfig() {
            return globalScanConfig;
        }

        @Override
        public String toString() {
            return String.format(
                "ConfigurationStatistics{projectAiConfigs=%d, projectScanConfigs=%d, globalAi=%s, globalScan=%s}",
                projectAiConfigCount, projectScanConfigCount,
                globalAiConfig.getSummary(), globalScanConfig.getSummary()
            );
        }
    }
}
