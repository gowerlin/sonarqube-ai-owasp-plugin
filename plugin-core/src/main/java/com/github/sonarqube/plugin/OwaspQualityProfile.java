package com.github.sonarqube.plugin;

import com.github.sonarqube.rules.RuleDefinition;
import com.github.sonarqube.rules.java.JavaSecurityRules;
import com.github.sonarqube.rules.javascript.JavaScriptSecurityRules;
import org.sonar.api.server.profile.BuiltInQualityProfilesDefinition;

import java.util.List;

/**
 * OWASP 品質設定檔定義
 *
 * 建立內建的品質設定檔，預設啟用所有 OWASP 安全規則。
 * 為 Java 和 JavaScript 分別建立專屬的安全設定檔。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class OwaspQualityProfile implements BuiltInQualityProfilesDefinition {

    private static final String JAVA_PROFILE_NAME = "OWASP Security Profile for Java";
    private static final String JAVASCRIPT_PROFILE_NAME = "OWASP Security Profile for JavaScript";

    @Override
    public void define(Context context) {
        // 建立 Java 品質設定檔
        defineJavaProfile(context);

        // 建立 JavaScript 品質設定檔
        defineJavaScriptProfile(context);
    }

    /**
     * 定義 Java 語言的品質設定檔
     */
    private void defineJavaProfile(Context context) {
        NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(
                JAVA_PROFILE_NAME,
                "java"
        );

        profile.setDefault(false); // 不設為預設，讓使用者選擇

        // 獲取所有 Java 規則
        List<RuleDefinition> rules = JavaSecurityRules.getAllRules();

        // 啟用所有規則
        for (RuleDefinition rule : rules) {
            activateRule(profile, rule);
        }

        profile.done();
    }

    /**
     * 定義 JavaScript 語言的品質設定檔
     */
    private void defineJavaScriptProfile(Context context) {
        NewBuiltInQualityProfile profile = context.createBuiltInQualityProfile(
                JAVASCRIPT_PROFILE_NAME,
                "js"
        );

        profile.setDefault(false); // 不設為預設，讓使用者選擇

        // 獲取所有 JavaScript 規則
        List<RuleDefinition> rules = JavaScriptSecurityRules.getAllRules();

        // 啟用所有規則
        for (RuleDefinition rule : rules) {
            activateRule(profile, rule);
        }

        profile.done();
    }

    /**
     * 啟用單一規則
     */
    private void activateRule(NewBuiltInQualityProfile profile, RuleDefinition rule) {
        String language = rule.getLanguage();
        String repositoryKey;

        // 根據語言選擇正確的 repository
        if ("java".equals(language)) {
            repositoryKey = "owasp-java";
        } else if ("javascript".equals(language) || "js".equals(language)) {
            repositoryKey = "owasp-javascript";
        } else {
            // 預設使用通用 repository
            repositoryKey = "owasp-security";
        }

        // 啟用規則（使用預設嚴重性）
        profile.activateRule(repositoryKey, rule.getRuleKey());
    }
}
