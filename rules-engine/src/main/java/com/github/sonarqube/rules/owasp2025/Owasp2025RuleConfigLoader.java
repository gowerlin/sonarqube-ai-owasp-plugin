package com.github.sonarqube.rules.owasp2025;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

/**
 * OWASP 2025 規則配置檔載入器
 *
 * 功能：
 * - 從 owasp2025-rules.yaml 載入規則配置
 * - 支援規則啟用/停用、嚴重性調整
 * - 支援熱載入（當 OWASP 2025 正式發布時快速更新）
 * - 提供配置驗證與版本檢查
 *
 * 使用範例：
 * ```java
 * Owasp2025RuleConfigLoader loader = new Owasp2025RuleConfigLoader();
 * Owasp2025Config config = loader.loadConfig();
 *
 * if (config.isPreview()) {
 *     logger.warn("OWASP 2025 is in preview status, waiting for official release");
 * }
 *
 * RuleConfig promptInjection = config.getRule("owasp-2025-a03-prompt-injection");
 * if (promptInjection.isEnabled()) {
 *     // 執行規則檢測
 * }
 * ```
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.6.0 (Epic 6, Story 6.3)
 */
public class Owasp2025RuleConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(Owasp2025RuleConfigLoader.java);
    private static final String CONFIG_FILE = "owasp2025-rules.yaml";

    /**
     * 載入 OWASP 2025 規則配置
     *
     * @return 規則配置物件
     * @throws RuntimeException 若配置檔不存在或格式錯誤
     */
    public Owasp2025Config loadConfig() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (inputStream == null) {
                throw new RuntimeException("Configuration file not found: " + CONFIG_FILE);
            }

            Yaml yaml = new Yaml();
            Map<String, Object> configMap = yaml.load(inputStream);

            return parseConfig(configMap);

        } catch (Exception e) {
            logger.error("Failed to load OWASP 2025 configuration", e);
            throw new RuntimeException("Failed to load OWASP 2025 configuration: " + e.getMessage(), e);
        }
    }

    /**
     * 解析配置 Map 為 Owasp2025Config 物件
     */
    @SuppressWarnings("unchecked")
    private Owasp2025Config parseConfig(Map<String, Object> configMap) {
        String owaspVersion = (String) configMap.getOrDefault("owasp_version", "2025");
        String status = (String) configMap.getOrDefault("status", "preview");
        String lastUpdated = (String) configMap.getOrDefault("last_updated", "unknown");

        // 解析預設設定
        Map<String, Object> defaults = (Map<String, Object>) configMap.getOrDefault("defaults", new HashMap<>());
        boolean defaultEnabled = (Boolean) defaults.getOrDefault("enabled", true);
        String defaultSeverity = (String) defaults.getOrDefault("severity", "MAJOR");
        boolean defaultRequiresAi = (Boolean) defaults.getOrDefault("requires_ai", false);

        // 解析規則列表
        List<Map<String, Object>> rulesList = (List<Map<String, Object>>) configMap.getOrDefault("rules", new ArrayList<>());
        Map<String, RuleConfig> rulesMap = new HashMap<>();

        for (Map<String, Object> ruleMap : rulesList) {
            RuleConfig rule = parseRule(ruleMap, defaultEnabled, defaultSeverity, defaultRequiresAi);
            rulesMap.put(rule.getRuleId(), rule);
        }

        logger.info("Loaded OWASP 2025 configuration: version={}, status={}, lastUpdated={}, rulesCount={}",
            owaspVersion, status, lastUpdated, rulesMap.size());

        return new Owasp2025Config(owaspVersion, status, lastUpdated, rulesMap);
    }

    /**
     * 解析單一規則配置
     */
    @SuppressWarnings("unchecked")
    private RuleConfig parseRule(Map<String, Object> ruleMap, boolean defaultEnabled,
                                  String defaultSeverity, boolean defaultRequiresAi) {
        String ruleId = (String) ruleMap.get("rule_id");
        String category = (String) ruleMap.get("category");
        String name = (String) ruleMap.get("name");
        String description = (String) ruleMap.get("description");
        boolean enabled = (Boolean) ruleMap.getOrDefault("enabled", defaultEnabled);
        String severity = (String) ruleMap.getOrDefault("severity", defaultSeverity);
        boolean requiresAi = (Boolean) ruleMap.getOrDefault("requires_ai", defaultRequiresAi);

        List<String> cweIds = (List<String>) ruleMap.getOrDefault("cwe_ids", new ArrayList<>());
        List<String> previewFeatures = (List<String>) ruleMap.getOrDefault("preview_features", new ArrayList<>());

        return new RuleConfig(ruleId, category, name, description, enabled, severity, requiresAi, cweIds, previewFeatures);
    }

    /**
     * OWASP 2025 配置物件
     */
    public static class Owasp2025Config {
        private final String owaspVersion;
        private final String status;
        private final String lastUpdated;
        private final Map<String, RuleConfig> rules;

        public Owasp2025Config(String owaspVersion, String status, String lastUpdated, Map<String, RuleConfig> rules) {
            this.owaspVersion = owaspVersion;
            this.status = status;
            this.lastUpdated = lastUpdated;
            this.rules = Collections.unmodifiableMap(new HashMap<>(rules));
        }

        public String getOwaspVersion() {
            return owaspVersion;
        }

        public String getStatus() {
            return status;
        }

        public String getLastUpdated() {
            return lastUpdated;
        }

        public Map<String, RuleConfig> getRules() {
            return rules;
        }

        public RuleConfig getRule(String ruleId) {
            return rules.get(ruleId);
        }

        public boolean isPreview() {
            return "preview".equalsIgnoreCase(status);
        }

        public boolean isStable() {
            return "stable".equalsIgnoreCase(status);
        }

        public int getRuleCount() {
            return rules.size();
        }

        public long getEnabledRuleCount() {
            return rules.values().stream().filter(RuleConfig::isEnabled).count();
        }

        @Override
        public String toString() {
            return String.format("Owasp2025Config{version=%s, status=%s, lastUpdated=%s, rulesCount=%d, enabledCount=%d}",
                owaspVersion, status, lastUpdated, getRuleCount(), getEnabledRuleCount());
        }
    }

    /**
     * 規則配置物件
     */
    public static class RuleConfig {
        private final String ruleId;
        private final String category;
        private final String name;
        private final String description;
        private final boolean enabled;
        private final String severity;
        private final boolean requiresAi;
        private final List<String> cweIds;
        private final List<String> previewFeatures;

        public RuleConfig(String ruleId, String category, String name, String description,
                          boolean enabled, String severity, boolean requiresAi,
                          List<String> cweIds, List<String> previewFeatures) {
            this.ruleId = ruleId;
            this.category = category;
            this.name = name;
            this.description = description;
            this.enabled = enabled;
            this.severity = severity;
            this.requiresAi = requiresAi;
            this.cweIds = Collections.unmodifiableList(new ArrayList<>(cweIds));
            this.previewFeatures = Collections.unmodifiableList(new ArrayList<>(previewFeatures));
        }

        public String getRuleId() {
            return ruleId;
        }

        public String getCategory() {
            return category;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getSeverity() {
            return severity;
        }

        public boolean isRequiresAi() {
            return requiresAi;
        }

        public List<String> getCweIds() {
            return cweIds;
        }

        public List<String> getPreviewFeatures() {
            return previewFeatures;
        }

        @Override
        public String toString() {
            return String.format("RuleConfig{ruleId=%s, category=%s, enabled=%s, severity=%s, cweCount=%d}",
                ruleId, category, enabled, severity, cweIds.size());
        }
    }
}
