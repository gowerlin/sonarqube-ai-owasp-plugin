package com.github.sonarqube.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 規則註冊表
 *
 * 管理所有 OWASP 規則的註冊、查詢、啟用/停用等操作。
 * 使用執行緒安全的設計，支援執行時期動態註冊規則。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.1)
 */
public class RuleRegistry {

    private static final Logger logger = LoggerFactory.getLogger(RuleRegistry.class);

    // 規則儲存：ruleId -> OwaspRule
    private final Map<String, OwaspRule> rules = new ConcurrentHashMap<>();

    // 規則啟用狀態：ruleId -> boolean
    private final Map<String, Boolean> ruleStatus = new ConcurrentHashMap<>();

    // 索引：owaspCategory -> List<ruleId>
    private final Map<String, List<String>> categoryIndex = new ConcurrentHashMap<>();

    // 索引：language -> List<ruleId>
    private final Map<String, List<String>> languageIndex = new ConcurrentHashMap<>();

    // 索引：owaspVersion -> List<ruleId>
    private final Map<String, List<String>> versionIndex = new ConcurrentHashMap<>();

    /**
     * 註冊規則
     *
     * @param rule OWASP 規則
     */
    public void registerRule(OwaspRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("Rule cannot be null");
        }

        String ruleId = rule.getRuleId();
        if (ruleId == null || ruleId.isEmpty()) {
            throw new IllegalArgumentException("Rule ID cannot be null or empty");
        }

        // 註冊規則
        rules.put(ruleId, rule);
        ruleStatus.put(ruleId, rule.isEnabled());

        // 更新索引
        updateCategoryIndex(rule);
        updateLanguageIndex(rule);
        updateVersionIndex(rule);

        logger.info("Registered rule: {} (category: {}, version: {})",
            ruleId, rule.getOwaspCategory(), rule.getOwaspVersion());
    }

    /**
     * 批次註冊規則
     *
     * @param rules 規則列表
     */
    public void registerRules(Collection<OwaspRule> rules) {
        if (rules != null) {
            rules.forEach(this::registerRule);
        }
    }

    /**
     * 取消註冊規則
     *
     * @param ruleId 規則 ID
     * @return true 如果成功取消註冊
     */
    public boolean unregisterRule(String ruleId) {
        if (ruleId == null) {
            return false;
        }

        OwaspRule rule = rules.remove(ruleId);
        if (rule != null) {
            ruleStatus.remove(ruleId);
            removeFromIndex(rule);
            logger.info("Unregistered rule: {}", ruleId);
            return true;
        }

        return false;
    }

    /**
     * 獲取規則
     *
     * @param ruleId 規則 ID
     * @return OWASP 規則，如果不存在返回 null
     */
    public OwaspRule getRule(String ruleId) {
        return rules.get(ruleId);
    }

    /**
     * 獲取所有規則
     *
     * @return 不可變的規則列表
     */
    public List<OwaspRule> getAllRules() {
        return Collections.unmodifiableList(new ArrayList<>(rules.values()));
    }

    /**
     * 獲取已啟用的規則
     *
     * @return 已啟用的規則列表
     */
    public List<OwaspRule> getEnabledRules() {
        return rules.values().stream()
            .filter(rule -> Boolean.TRUE.equals(ruleStatus.get(rule.getRuleId())))
            .collect(Collectors.toList());
    }

    /**
     * 根據 OWASP 類別獲取規則
     *
     * @param category OWASP 類別（例如：A01）
     * @return 規則列表
     */
    public List<OwaspRule> getRulesByCategory(String category) {
        List<String> ruleIds = categoryIndex.getOrDefault(category, Collections.emptyList());
        return ruleIds.stream()
            .map(rules::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 根據程式語言獲取規則
     *
     * @param language 程式語言（例如：java）
     * @return 規則列表
     */
    public List<OwaspRule> getRulesByLanguage(String language) {
        List<String> ruleIds = languageIndex.getOrDefault(language.toLowerCase(), Collections.emptyList());
        return ruleIds.stream()
            .map(rules::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 根據 OWASP 版本獲取規則
     *
     * @param version OWASP 版本（例如：2021）
     * @return 規則列表
     */
    public List<OwaspRule> getRulesByVersion(String version) {
        List<String> ruleIds = versionIndex.getOrDefault(version, Collections.emptyList());
        return ruleIds.stream()
            .map(rules::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * 啟用規則
     *
     * @param ruleId 規則 ID
     */
    public void enableRule(String ruleId) {
        if (rules.containsKey(ruleId)) {
            ruleStatus.put(ruleId, true);
            logger.info("Enabled rule: {}", ruleId);
        }
    }

    /**
     * 停用規則
     *
     * @param ruleId 規則 ID
     */
    public void disableRule(String ruleId) {
        if (rules.containsKey(ruleId)) {
            ruleStatus.put(ruleId, false);
            logger.info("Disabled rule: {}", ruleId);
        }
    }

    /**
     * 檢查規則是否已註冊
     *
     * @param ruleId 規則 ID
     * @return true 如果已註冊
     */
    public boolean isRegistered(String ruleId) {
        return rules.containsKey(ruleId);
    }

    /**
     * 檢查規則是否已啟用
     *
     * @param ruleId 規則 ID
     * @return true 如果已啟用
     */
    public boolean isEnabled(String ruleId) {
        return Boolean.TRUE.equals(ruleStatus.get(ruleId));
    }

    /**
     * 獲取規則總數
     *
     * @return 規則總數
     */
    public int getRuleCount() {
        return rules.size();
    }

    /**
     * 獲取已啟用的規則數量
     *
     * @return 已啟用的規則數量
     */
    public int getEnabledRuleCount() {
        return (int) ruleStatus.values().stream()
            .filter(Boolean::booleanValue)
            .count();
    }

    /**
     * 清空所有規則
     */
    public void clear() {
        rules.clear();
        ruleStatus.clear();
        categoryIndex.clear();
        languageIndex.clear();
        versionIndex.clear();
        logger.info("Cleared all rules from registry");
    }

    /**
     * 獲取統計資訊
     *
     * @return 統計資訊字串
     */
    public String getStatistics() {
        return String.format("RuleRegistry Statistics - Total: %d, Enabled: %d, Categories: %d, Languages: %d, Versions: %d",
            getRuleCount(),
            getEnabledRuleCount(),
            categoryIndex.size(),
            languageIndex.size(),
            versionIndex.size());
    }

    // === Private Helper Methods ===

    private void updateCategoryIndex(OwaspRule rule) {
        String category = rule.getOwaspCategory();
        if (category != null && !category.isEmpty()) {
            categoryIndex.computeIfAbsent(category, k -> new ArrayList<>())
                .add(rule.getRuleId());
        }
    }

    private void updateLanguageIndex(OwaspRule rule) {
        List<String> languages = rule.getSupportedLanguages();
        if (languages != null) {
            for (String lang : languages) {
                String normalized = lang.toLowerCase();
                languageIndex.computeIfAbsent(normalized, k -> new ArrayList<>())
                    .add(rule.getRuleId());
            }
        }
    }

    private void updateVersionIndex(OwaspRule rule) {
        String version = rule.getOwaspVersion();
        if (version != null && !version.isEmpty()) {
            versionIndex.computeIfAbsent(version, k -> new ArrayList<>())
                .add(rule.getRuleId());
        }
    }

    private void removeFromIndex(OwaspRule rule) {
        String ruleId = rule.getRuleId();

        // 從類別索引移除
        String category = rule.getOwaspCategory();
        if (category != null) {
            List<String> categoryRules = categoryIndex.get(category);
            if (categoryRules != null) {
                categoryRules.remove(ruleId);
                if (categoryRules.isEmpty()) {
                    categoryIndex.remove(category);
                }
            }
        }

        // 從語言索引移除
        List<String> languages = rule.getSupportedLanguages();
        if (languages != null) {
            for (String lang : languages) {
                String normalized = lang.toLowerCase();
                List<String> langRules = languageIndex.get(normalized);
                if (langRules != null) {
                    langRules.remove(ruleId);
                    if (langRules.isEmpty()) {
                        languageIndex.remove(normalized);
                    }
                }
            }
        }

        // 從版本索引移除
        String version = rule.getOwaspVersion();
        if (version != null) {
            List<String> versionRules = versionIndex.get(version);
            if (versionRules != null) {
                versionRules.remove(ruleId);
                if (versionRules.isEmpty()) {
                    versionIndex.remove(version);
                }
            }
        }
    }
}
