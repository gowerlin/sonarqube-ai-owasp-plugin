package com.github.sonarqube.rules;

import com.github.sonarqube.rules.java.Java2017SecurityRules;
import com.github.sonarqube.rules.java.JavaSecurityRules;
import com.github.sonarqube.rules.javascript.JavaScript2017SecurityRules;
import com.github.sonarqube.rules.javascript.JavaScriptSecurityRules;
import com.github.sonarqube.version.OwaspVersion;
import com.github.sonarqube.version.VersionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 規則引擎整合服務
 *
 * 統一管理多版本 OWASP 規則引擎，提供版本感知的規則查詢功能。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class RuleEngineService {

    /**
     * 獲取當前版本的所有 Java 規則
     *
     * @return Java 規則列表
     */
    public static List<Object> getJavaRules() {
        return getJavaRules(VersionManager.getCurrentVersion());
    }

    /**
     * 獲取指定版本的所有 Java 規則
     *
     * @param version OWASP 版本
     * @return Java 規則列表
     */
    public static List<Object> getJavaRules(OwaspVersion version) {
        List<Object> rules = new ArrayList<>();

        if (version == OwaspVersion.OWASP_2017) {
            rules.addAll(Java2017SecurityRules.getAllRules());
        } else if (version == OwaspVersion.OWASP_2021) {
            rules.addAll(JavaSecurityRules.getAllRules());
        }

        return Collections.unmodifiableList(rules);
    }

    /**
     * 獲取當前版本的所有 JavaScript 規則
     *
     * @return JavaScript 規則列表
     */
    public static List<Object> getJavaScriptRules() {
        return getJavaScriptRules(VersionManager.getCurrentVersion());
    }

    /**
     * 獲取指定版本的所有 JavaScript 規則
     *
     * @param version OWASP 版本
     * @return JavaScript 規則列表
     */
    public static List<Object> getJavaScriptRules(OwaspVersion version) {
        List<Object> rules = new ArrayList<>();

        if (version == OwaspVersion.OWASP_2017) {
            rules.addAll(JavaScript2017SecurityRules.getAllRules());
        } else if (version == OwaspVersion.OWASP_2021) {
            rules.addAll(JavaScriptSecurityRules.getAllRules());
        }

        return Collections.unmodifiableList(rules);
    }

    /**
     * 獲取當前版本的所有規則（所有語言）
     *
     * @return 所有規則列表
     */
    public static List<Object> getAllRules() {
        return getAllRules(VersionManager.getCurrentVersion());
    }

    /**
     * 獲取指定版本的所有規則（所有語言）
     *
     * @param version OWASP 版本
     * @return 所有規則列表
     */
    public static List<Object> getAllRules(OwaspVersion version) {
        List<Object> allRules = new ArrayList<>();
        allRules.addAll(getJavaRules(version));
        allRules.addAll(getJavaScriptRules(version));
        return Collections.unmodifiableList(allRules);
    }

    /**
     * 獲取規則總數
     *
     * @return 規則總數
     */
    public static int getRuleCount() {
        return getRuleCount(VersionManager.getCurrentVersion());
    }

    /**
     * 獲取指定版本的規則總數
     *
     * @param version OWASP 版本
     * @return 規則總數
     */
    public static int getRuleCount(OwaspVersion version) {
        return getAllRules(version).size();
    }

    /**
     * 獲取規則統計資訊
     *
     * @return 統計資訊字串
     */
    public static String getRuleStatistics() {
        return getRuleStatistics(VersionManager.getCurrentVersion());
    }

    /**
     * 獲取指定版本的規則統計資訊
     *
     * @param version OWASP 版本
     * @return 統計資訊字串
     */
    public static String getRuleStatistics(OwaspVersion version) {
        int javaCount = getJavaRules(version).size();
        int jsCount = getJavaScriptRules(version).size();
        int total = javaCount + jsCount;

        return String.format("OWASP %s Rules - Total: %d (Java: %d, JavaScript: %d)",
            version.getVersion(), total, javaCount, jsCount);
    }

    /**
     * 檢查是否支援指定語言
     *
     * @param language 語言名稱
     * @return true 如果支援
     */
    public static boolean isLanguageSupported(String language) {
        if (language == null) {
            return false;
        }

        String normalized = language.toLowerCase();
        return normalized.equals("java") || normalized.equals("javascript") || normalized.equals("js");
    }

    /**
     * 獲取支援的語言列表
     *
     * @return 語言列表
     */
    public static List<String> getSupportedLanguages() {
        return List.of("java", "javascript");
    }
}
