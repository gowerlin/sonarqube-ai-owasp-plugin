package com.github.sonarqube.rules;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OWASP 版本管理服務
 *
 * 管理 OWASP 規則的版本切換、版本查詢和版本驗證功能。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.2.0 (Epic 4, Story 4.2)
 */
public class OwaspVersionManager {

    /**
     * OWASP 版本枚舉
     */
    public enum OwaspVersion {
        OWASP_2017("2017", "OWASP Top 10 2017"),
        OWASP_2021("2021", "OWASP Top 10 2021");

        private final String version;
        private final String displayName;

        OwaspVersion(String version, String displayName) {
            this.version = version;
            this.displayName = displayName;
        }

        public String getVersion() {
            return version;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static OwaspVersion fromString(String version) {
            for (OwaspVersion v : values()) {
                if (v.version.equals(version)) {
                    return v;
                }
            }
            throw new IllegalArgumentException("Unknown OWASP version: " + version);
        }
    }

    private final RuleRegistry ruleRegistry;
    private final Map<String, OwaspVersion> activeVersions = new ConcurrentHashMap<>();

    /**
     * 預設建構子
     *
     * @param ruleRegistry 規則註冊表
     */
    public OwaspVersionManager(RuleRegistry ruleRegistry) {
        this.ruleRegistry = ruleRegistry;
        // 預設啟用 OWASP 2021
        setActiveVersion(OwaspVersion.OWASP_2021);
    }

    /**
     * 設定當前活躍版本
     *
     * @param version OWASP 版本
     */
    public void setActiveVersion(OwaspVersion version) {
        activeVersions.put("default", version);
    }

    /**
     * 設定專案特定版本
     *
     * @param projectKey 專案鍵值
     * @param version OWASP 版本
     */
    public void setProjectVersion(String projectKey, OwaspVersion version) {
        activeVersions.put(projectKey, version);
    }

    /**
     * 取得當前活躍版本
     *
     * @return OWASP 版本
     */
    public OwaspVersion getActiveVersion() {
        return activeVersions.getOrDefault("default", OwaspVersion.OWASP_2021);
    }

    /**
     * 取得專案特定版本
     *
     * @param projectKey 專案鍵值
     * @return OWASP 版本，如果專案未設定則返回預設版本
     */
    public OwaspVersion getProjectVersion(String projectKey) {
        return activeVersions.getOrDefault(projectKey, getActiveVersion());
    }

    /**
     * 取得所有支援的版本
     *
     * @return OWASP 版本列表
     */
    public List<OwaspVersion> getSupportedVersions() {
        return Arrays.asList(OwaspVersion.values());
    }

    /**
     * 驗證版本是否支援
     *
     * @param version 版本字串
     * @return true 如果支援該版本
     */
    public boolean isVersionSupported(String version) {
        try {
            OwaspVersion.fromString(version);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 取得指定版本的規則數量
     *
     * @param version OWASP 版本
     * @return 規則數量
     */
    public int getRuleCountForVersion(OwaspVersion version) {
        return ruleRegistry.getRulesByVersion(version.getVersion()).size();
    }

    /**
     * 取得指定版本的 OWASP 類別
     *
     * @param version OWASP 版本
     * @return OWASP 類別列表
     */
    public List<String> getCategoriesForVersion(OwaspVersion version) {
        Set<String> categories = new HashSet<>();
        for (OwaspRule rule : ruleRegistry.getRulesByVersion(version.getVersion())) {
            categories.add(rule.getOwaspCategory());
        }
        List<String> sortedCategories = new ArrayList<>(categories);
        Collections.sort(sortedCategories);
        return sortedCategories;
    }

    /**
     * 切換版本
     *
     * @param fromVersion 來源版本
     * @param toVersion 目標版本
     * @return 版本切換資訊
     */
    public VersionSwitchInfo switchVersion(OwaspVersion fromVersion, OwaspVersion toVersion) {
        int fromRuleCount = getRuleCountForVersion(fromVersion);
        int toRuleCount = getRuleCountForVersion(toVersion);

        setActiveVersion(toVersion);

        return new VersionSwitchInfo(
            fromVersion.getVersion(),
            toVersion.getVersion(),
            fromRuleCount,
            toRuleCount,
            getCategoriesForVersion(toVersion)
        );
    }

    /**
     * 版本切換資訊
     */
    public static class VersionSwitchInfo {
        private final String fromVersion;
        private final String toVersion;
        private final int fromRuleCount;
        private final int toRuleCount;
        private final List<String> availableCategories;

        public VersionSwitchInfo(String fromVersion, String toVersion, int fromRuleCount,
                                 int toRuleCount, List<String> availableCategories) {
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
            this.fromRuleCount = fromRuleCount;
            this.toRuleCount = toRuleCount;
            this.availableCategories = availableCategories;
        }

        public String getFromVersion() { return fromVersion; }
        public String getToVersion() { return toVersion; }
        public int getFromRuleCount() { return fromRuleCount; }
        public int getToRuleCount() { return toRuleCount; }
        public List<String> getAvailableCategories() { return availableCategories; }

        @Override
        public String toString() {
            return String.format("Switched from OWASP %s (%d rules) to OWASP %s (%d rules). Categories: %s",
                fromVersion, fromRuleCount, toVersion, toRuleCount, availableCategories);
        }
    }
}
