package com.github.sonarqube.rules.javascript;

import com.github.sonarqube.rules.RuleDefinition;
import com.github.sonarqube.rules.RuleDefinition.RuleSeverity;
import com.github.sonarqube.rules.RuleDefinition.RuleType;
import com.github.sonarqube.rules.owasp.Owasp2021Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JavaScript 安全規則定義
 *
 * 定義針對 JavaScript/Node.js 的 OWASP 2021 安全規則。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class JavaScriptSecurityRules {

    private static final String LANGUAGE = "javascript";
    private static final List<RuleDefinition> RULES = new ArrayList<>();

    static {
        // A01:2021 - Broken Access Control
        RULES.add(RuleDefinition.builder("js-owasp-a01-001")
            .name("缺少授權檢查")
            .description("API 端點或敏感操作缺少適當的授權檢查，可能導致未授權存取。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A01_BROKEN_ACCESS_CONTROL)
            .cweIds(List.of("CWE-862", "CWE-285"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "authorization"))
            .remediationCost("30min")
            .build());

        RULES.add(RuleDefinition.builder("js-owasp-a01-002")
            .name("路徑遍歷漏洞")
            .description("使用者輸入直接用於檔案路徑操作，未經驗證可能導致讀取任意檔案。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A01_BROKEN_ACCESS_CONTROL)
            .cweIds(List.of("CWE-22", "CWE-23"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "path-traversal"))
            .remediationCost("20min")
            .build());

        // A02:2021 - Cryptographic Failures
        RULES.add(RuleDefinition.builder("js-owasp-a02-001")
            .name("使用弱加密演算法")
            .description("使用已知不安全的加密演算法（如 MD5, SHA1），容易被破解。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A02_CRYPTOGRAPHIC_FAILURES)
            .cweIds(List.of("CWE-327", "CWE-328"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "cryptography"))
            .remediationCost("15min")
            .build());

        RULES.add(RuleDefinition.builder("js-owasp-a02-002")
            .name("硬編碼的加密金鑰或密碼")
            .description("密碼或加密金鑰硬編碼在程式碼中，容易洩露。")
            .severity(RuleSeverity.BLOCKER)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A02_CRYPTOGRAPHIC_FAILURES)
            .cweIds(List.of("CWE-798", "CWE-259"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "hardcoded-secret"))
            .remediationCost("30min")
            .build());

        RULES.add(RuleDefinition.builder("js-owasp-a02-003")
            .name("不安全的隨機數生成")
            .description("使用 Math.random() 生成安全相關的隨機數，可預測性高。")
            .severity(RuleSeverity.MAJOR)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A02_CRYPTOGRAPHIC_FAILURES)
            .cweIds(List.of("CWE-330", "CWE-338"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "random"))
            .remediationCost("10min")
            .build());

        // A03:2021 - Injection
        RULES.add(RuleDefinition.builder("js-owasp-a03-001")
            .name("跨站腳本攻擊 (XSS)")
            .description("未經淨化的使用者輸入直接輸出到 HTML，可能導致 XSS 攻擊。")
            .severity(RuleSeverity.BLOCKER)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A03_INJECTION)
            .cweIds(List.of("CWE-79"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "xss"))
            .remediationCost("20min")
            .build());

        RULES.add(RuleDefinition.builder("js-owasp-a03-002")
            .name("NoSQL 注入")
            .description("使用者輸入直接拼接到 NoSQL 查詢中，可能導致資料洩露或操縱。")
            .severity(RuleSeverity.BLOCKER)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A03_INJECTION)
            .cweIds(List.of("CWE-943"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "nosql-injection"))
            .remediationCost("25min")
            .build());

        RULES.add(RuleDefinition.builder("js-owasp-a03-003")
            .name("命令注入")
            .description("使用者輸入直接用於執行系統命令（exec, spawn），可能執行任意命令。")
            .severity(RuleSeverity.BLOCKER)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A03_INJECTION)
            .cweIds(List.of("CWE-78", "CWE-77"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "command-injection"))
            .remediationCost("25min")
            .build());

        RULES.add(RuleDefinition.builder("js-owasp-a03-004")
            .name("危險的 eval() 使用")
            .description("使用 eval() 執行動態程式碼，可能導致程式碼注入攻擊。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A03_INJECTION)
            .cweIds(List.of("CWE-95"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "code-injection"))
            .remediationCost("20min")
            .build());

        // A04:2021 - Insecure Design
        RULES.add(RuleDefinition.builder("js-owasp-a04-001")
            .name("敏感資料記錄在日誌中")
            .description("日誌包含敏感資訊（密碼、Token），可能導致資訊洩露。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A04_INSECURE_DESIGN)
            .cweIds(List.of("CWE-532", "CWE-215"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "sensitive-data-in-logs"))
            .remediationCost("15min")
            .build());

        RULES.add(RuleDefinition.builder("js-owasp-a04-002")
            .name("原型污染攻擊")
            .description("未經驗證的物件合併操作可能導致原型污染，影響全域物件。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A04_INSECURE_DESIGN)
            .cweIds(List.of("CWE-1321"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "prototype-pollution"))
            .remediationCost("30min")
            .build());

        // A05:2021 - Security Misconfiguration
        RULES.add(RuleDefinition.builder("js-owasp-a05-001")
            .name("不安全的 CORS 配置")
            .description("CORS 配置過於寬鬆（如 Access-Control-Allow-Origin: *），可能導致跨域攻擊。")
            .severity(RuleSeverity.MAJOR)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A05_SECURITY_MISCONFIGURATION)
            .cweIds(List.of("CWE-942"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "cors"))
            .remediationCost("15min")
            .build());

        RULES.add(RuleDefinition.builder("js-owasp-a05-002")
            .name("缺少安全標頭")
            .description("HTTP 回應缺少關鍵安全標頭（如 X-Frame-Options, CSP）。")
            .severity(RuleSeverity.MAJOR)
            .type(RuleType.SECURITY_HOTSPOT)
            .owaspCategory(Owasp2021Category.A05_SECURITY_MISCONFIGURATION)
            .cweIds(List.of("CWE-1021"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "http-headers"))
            .remediationCost("20min")
            .build());

        // A06:2021 - Vulnerable and Outdated Components
        RULES.add(RuleDefinition.builder("js-owasp-a06-001")
            .name("使用已知漏洞的依賴套件")
            .description("專案依賴包含已知安全漏洞的套件版本。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A06_VULNERABLE_OUTDATED_COMPONENTS)
            .cweIds(List.of("CWE-1104"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "dependencies"))
            .remediationCost("30min")
            .build());

        // A07:2021 - Identification and Authentication Failures
        RULES.add(RuleDefinition.builder("js-owasp-a07-001")
            .name("弱 JWT 密鑰")
            .description("JWT 使用弱密鑰或硬編碼密鑰，容易被暴力破解。")
            .severity(RuleSeverity.BLOCKER)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A07_IDENTIFICATION_AUTHENTICATION_FAILURES)
            .cweIds(List.of("CWE-798", "CWE-321"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "jwt"))
            .remediationCost("25min")
            .build());

        RULES.add(RuleDefinition.builder("js-owasp-a07-002")
            .name("缺少速率限制")
            .description("登入或敏感操作缺少速率限制，容易遭受暴力破解攻擊。")
            .severity(RuleSeverity.MAJOR)
            .type(RuleType.SECURITY_HOTSPOT)
            .owaspCategory(Owasp2021Category.A07_IDENTIFICATION_AUTHENTICATION_FAILURES)
            .cweIds(List.of("CWE-307"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "rate-limiting"))
            .remediationCost("30min")
            .build());

        // A08:2021 - Software and Data Integrity Failures
        RULES.add(RuleDefinition.builder("js-owasp-a08-001")
            .name("不安全的反序列化")
            .description("反序列化不受信任的資料（如 eval(JSON.parse())），可能導致遠端程式碼執行。")
            .severity(RuleSeverity.BLOCKER)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A08_SOFTWARE_DATA_INTEGRITY_FAILURES)
            .cweIds(List.of("CWE-502"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "deserialization"))
            .remediationCost("35min")
            .build());

        RULES.add(RuleDefinition.builder("js-owasp-a08-002")
            .name("缺少完整性檢查的外部資源")
            .description("載入外部 JavaScript 資源未使用 Subresource Integrity (SRI)。")
            .severity(RuleSeverity.MAJOR)
            .type(RuleType.SECURITY_HOTSPOT)
            .owaspCategory(Owasp2021Category.A08_SOFTWARE_DATA_INTEGRITY_FAILURES)
            .cweIds(List.of("CWE-353"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "sri"))
            .remediationCost("10min")
            .build());

        // A09:2021 - Security Logging and Monitoring Failures
        RULES.add(RuleDefinition.builder("js-owasp-a09-001")
            .name("缺少安全事件記錄")
            .description("關鍵安全操作（登入失敗、權限變更）未記錄日誌。")
            .severity(RuleSeverity.MAJOR)
            .type(RuleType.SECURITY_HOTSPOT)
            .owaspCategory(Owasp2021Category.A09_SECURITY_LOGGING_MONITORING_FAILURES)
            .cweIds(List.of("CWE-778"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "logging"))
            .remediationCost("20min")
            .build());

        // A10:2021 - Server-Side Request Forgery
        RULES.add(RuleDefinition.builder("js-owasp-a10-001")
            .name("伺服器端請求偽造 (SSRF)")
            .description("使用者提供的 URL 未經驗證直接發送請求（如 axios, fetch），可能存取內部資源。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A10_SERVER_SIDE_REQUEST_FORGERY)
            .cweIds(List.of("CWE-918"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "ssrf"))
            .remediationCost("30min")
            .build());
    }

    /**
     * 獲取所有 JavaScript 安全規則
     *
     * @return 規則列表
     */
    public static List<RuleDefinition> getAllRules() {
        return Collections.unmodifiableList(RULES);
    }

    /**
     * 根據 OWASP 分類獲取規則
     *
     * @param category OWASP 2021 分類
     * @return 規則列表
     */
    public static List<RuleDefinition> getRulesByCategory(Owasp2021Category category) {
        if (category == null) {
            return Collections.emptyList();
        }

        return RULES.stream()
            .filter(rule -> rule.getOwaspCategory() == category)
            .toList();
    }

    /**
     * 根據嚴重性獲取規則
     *
     * @param severity 嚴重性
     * @return 規則列表
     */
    public static List<RuleDefinition> getRulesBySeverity(RuleSeverity severity) {
        if (severity == null) {
            return Collections.emptyList();
        }

        return RULES.stream()
            .filter(rule -> rule.getSeverity() == severity)
            .toList();
    }

    /**
     * 根據規則鍵獲取規則
     *
     * @param ruleKey 規則鍵
     * @return 規則定義，如果找不到則返回 null
     */
    public static RuleDefinition getRuleByKey(String ruleKey) {
        if (ruleKey == null) {
            return null;
        }

        return RULES.stream()
            .filter(rule -> rule.getRuleKey().equals(ruleKey))
            .findFirst()
            .orElse(null);
    }

    /**
     * 獲取規則統計資訊
     *
     * @return 按 OWASP 分類分組的規則數量
     */
    public static Map<Owasp2021Category, Long> getRuleStatistics() {
        return RULES.stream()
            .filter(rule -> rule.getOwaspCategory() != null)
            .collect(Collectors.groupingBy(
                RuleDefinition::getOwaspCategory,
                Collectors.counting()
            ));
    }

    /**
     * 獲取規則總數
     *
     * @return 規則數量
     */
    public static int getRuleCount() {
        return RULES.size();
    }
}
