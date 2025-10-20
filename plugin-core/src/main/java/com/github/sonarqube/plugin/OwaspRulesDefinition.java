package com.github.sonarqube.plugin;

import com.github.sonarqube.rules.RuleDefinition;
import com.github.sonarqube.rules.RuleEngineService;
import com.github.sonarqube.rules.java.JavaSecurityRules;
import com.github.sonarqube.rules.javascript.JavaScriptSecurityRules;
import com.github.sonarqube.version.OwaspVersion;
import com.github.sonarqube.version.VersionManager;
import org.sonar.api.server.rule.RulesDefinition;

import java.util.List;

/**
 * OWASP 安全規則定義
 *
 * 將安全規則註冊到 SonarQube 規則引擎。
 * 支援 Java 和 JavaScript 語言的 OWASP Top 10 安全規則。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class OwaspRulesDefinition implements RulesDefinition {

    public static final String REPOSITORY_KEY = "owasp-security";
    public static final String REPOSITORY_NAME = "OWASP Security Rules";

    private static final String JAVA_REPOSITORY_KEY = "owasp-java";
    private static final String JAVASCRIPT_REPOSITORY_KEY = "owasp-javascript";

    @Override
    public void define(Context context) {
        // 根據當前版本註冊規則
        OwaspVersion currentVersion = VersionManager.getCurrentVersion();

        // 註冊 Java 規則庫
        defineJavaRules(context, currentVersion);

        // 註冊 JavaScript 規則庫
        defineJavaScriptRules(context, currentVersion);
    }

    /**
     * 定義 Java 語言的安全規則
     */
    private void defineJavaRules(Context context, OwaspVersion version) {
        NewRepository repository = context
                .createRepository(JAVA_REPOSITORY_KEY, "java")
                .setName("OWASP Security Rules for Java");

        // 獲取當前版本的 Java 規則
        List<RuleDefinition> rules = JavaSecurityRules.getAllRules();

        // 註冊每條規則
        for (RuleDefinition rule : rules) {
            createRule(repository, rule);
        }

        repository.done();
    }

    /**
     * 定義 JavaScript 語言的安全規則
     */
    private void defineJavaScriptRules(Context context, OwaspVersion version) {
        NewRepository repository = context
                .createRepository(JAVASCRIPT_REPOSITORY_KEY, "js")
                .setName("OWASP Security Rules for JavaScript");

        // 獲取當前版本的 JavaScript 規則
        List<RuleDefinition> rules = JavaScriptSecurityRules.getAllRules();

        // 註冊每條規則
        for (RuleDefinition rule : rules) {
            createRule(repository, rule);
        }

        repository.done();
    }

    /**
     * 建立單一規則
     */
    private void createRule(NewRepository repository, RuleDefinition ruleDef) {
        NewRule rule = repository.createRule(ruleDef.getRuleKey());

        // 基本資訊
        rule.setName(ruleDef.getName());
        rule.setHtmlDescription(formatDescription(ruleDef));

        // 嚴重性
        rule.setSeverity(mapSeverity(ruleDef.getSeverity()));

        // 規則類型
        rule.setType(mapRuleType(ruleDef.getType()));

        // 標籤
        if (ruleDef.getTags() != null && !ruleDef.getTags().isEmpty()) {
            rule.setTags(ruleDef.getTags().toArray(new String[0]));
        }

        // 修復成本（技術債）
        if (ruleDef.getRemediationCost() != null) {
            rule.setDebtRemediationFunction(
                    rule.debtRemediationFunctions().constantPerIssue(ruleDef.getRemediationCost())
            );
        }

        // 設定為啟用狀態
        rule.setActivatedByDefault(true);
    }

    /**
     * 格式化規則描述為 HTML
     */
    private String formatDescription(RuleDefinition ruleDef) {
        StringBuilder html = new StringBuilder();

        // 描述
        html.append("<p>").append(escapeHtml(ruleDef.getDescription())).append("</p>");

        // OWASP 分類
        html.append("<h3>OWASP 分類</h3>");
        html.append("<p>").append(escapeHtml(ruleDef.getOwaspCategory().getFullName())).append("</p>");
        html.append("<p>").append(escapeHtml(ruleDef.getOwaspCategory().getDescription())).append("</p>");

        // CWE 資訊
        if (ruleDef.getCweIds() != null && !ruleDef.getCweIds().isEmpty()) {
            html.append("<h3>相關 CWE</h3>");
            html.append("<ul>");
            for (String cweId : ruleDef.getCweIds()) {
                html.append("<li>").append(escapeHtml(cweId)).append("</li>");
            }
            html.append("</ul>");
        }

        // Note: getFixSuggestion(), getWrongExample(), getCorrectExample()
        // 方法在 RuleDefinition 中不存在，已移除相關程式碼
        // 如需這些功能，應在 RuleDefinition 類別中新增對應的欄位和方法

        return html.toString();
    }

    /**
     * 對應嚴重性等級
     */
    private String mapSeverity(RuleDefinition.RuleSeverity severity) {
        switch (severity) {
            case BLOCKER:
                return org.sonar.api.rule.Severity.BLOCKER;
            case CRITICAL:
                return org.sonar.api.rule.Severity.CRITICAL;
            case MAJOR:
                return org.sonar.api.rule.Severity.MAJOR;
            case MINOR:
                return org.sonar.api.rule.Severity.MINOR;
            case INFO:
                return org.sonar.api.rule.Severity.INFO;
            default:
                return org.sonar.api.rule.Severity.MAJOR;
        }
    }

    /**
     * 對應規則類型
     */
    private org.sonar.api.rules.RuleType mapRuleType(RuleDefinition.RuleType type) {
        switch (type) {
            case VULNERABILITY:
                return org.sonar.api.rules.RuleType.VULNERABILITY;
            case SECURITY_HOTSPOT:
                return org.sonar.api.rules.RuleType.SECURITY_HOTSPOT;
            case CODE_SMELL:
                return org.sonar.api.rules.RuleType.CODE_SMELL;
            case BUG:
                return org.sonar.api.rules.RuleType.BUG;
            default:
                return org.sonar.api.rules.RuleType.VULNERABILITY;
        }
    }

    /**
     * HTML 轉義
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
