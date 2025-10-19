package com.github.sonarqube.rules.java;

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
 * Java 安全規則定義
 *
 * 定義針對 Java 語言的 OWASP 2021 安全規則。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class JavaSecurityRules {

    private static final String LANGUAGE = "java";
    private static final List<RuleDefinition> RULES = new ArrayList<>();

    static {
        // A01:2021 - Broken Access Control
        RULES.add(RuleDefinition.builder("java-owasp-a01-001")
            .name("缺少權限檢查的敏感操作")
            .description("在執行敏感操作（如刪除、修改）之前未進行適當的權限檢查，可能導致未授權存取。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A01_BROKEN_ACCESS_CONTROL)
            .cweIds(List.of("CWE-284", "CWE-862"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "access-control"))
            .remediationCost("30min")
            .build());

        RULES.add(RuleDefinition.builder("java-owasp-a01-002")
            .name("路徑遍歷漏洞")
            .description("使用者輸入直接用於檔案路徑操作，未經驗證可能導致路徑遍歷攻擊。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A01_BROKEN_ACCESS_CONTROL)
            .cweIds(List.of("CWE-22", "CWE-23"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "path-traversal"))
            .remediationCost("20min")
            .build());

        // A02:2021 - Cryptographic Failures
        RULES.add(RuleDefinition.builder("java-owasp-a02-001")
            .name("使用弱加密演算法")
            .description("使用過時或不安全的加密演算法（如 DES, MD5, SHA-1），容易被破解。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A02_CRYPTOGRAPHIC_FAILURES)
            .cweIds(List.of("CWE-327", "CWE-328"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "cryptography"))
            .remediationCost("15min")
            .build());

        RULES.add(RuleDefinition.builder("java-owasp-a02-002")
            .name("硬編碼的加密金鑰")
            .description("加密金鑰硬編碼在程式碼中，容易洩露且難以更換。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A02_CRYPTOGRAPHIC_FAILURES)
            .cweIds(List.of("CWE-321", "CWE-798"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "hardcoded-secret"))
            .remediationCost("30min")
            .build());

        RULES.add(RuleDefinition.builder("java-owasp-a02-003")
            .name("使用不安全的隨機數生成器")
            .description("使用 Random 而非 SecureRandom 生成安全相關的隨機數，可預測性高。")
            .severity(RuleSeverity.MAJOR)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A02_CRYPTOGRAPHIC_FAILURES)
            .cweIds(List.of("CWE-330", "CWE-338"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "random"))
            .remediationCost("10min")
            .build());

        // A03:2021 - Injection
        RULES.add(RuleDefinition.builder("java-owasp-a03-001")
            .name("SQL 注入漏洞")
            .description("使用者輸入直接拼接到 SQL 語句中，未使用參數化查詢，導致 SQL 注入風險。")
            .severity(RuleSeverity.BLOCKER)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A03_INJECTION)
            .cweIds(List.of("CWE-89"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "sql-injection"))
            .remediationCost("20min")
            .build());

        RULES.add(RuleDefinition.builder("java-owasp-a03-002")
            .name("命令注入漏洞")
            .description("使用者輸入直接用於執行系統命令，未經驗證可能導致任意命令執行。")
            .severity(RuleSeverity.BLOCKER)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A03_INJECTION)
            .cweIds(List.of("CWE-78", "CWE-77"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "command-injection"))
            .remediationCost("25min")
            .build());

        RULES.add(RuleDefinition.builder("java-owasp-a03-003")
            .name("LDAP 注入漏洞")
            .description("使用者輸入未經驗證直接用於 LDAP 查詢，可能導致未授權資料存取。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A03_INJECTION)
            .cweIds(List.of("CWE-90"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "ldap-injection"))
            .remediationCost("20min")
            .build());

        // A04:2021 - Insecure Design
        RULES.add(RuleDefinition.builder("java-owasp-a04-001")
            .name("敏感資訊記錄在錯誤訊息中")
            .description("錯誤訊息包含敏感資訊（如堆疊追蹤、SQL 語句），可能洩露系統內部實現。")
            .severity(RuleSeverity.MAJOR)
            .type(RuleType.SECURITY_HOTSPOT)
            .owaspCategory(Owasp2021Category.A04_INSECURE_DESIGN)
            .cweIds(List.of("CWE-209"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "information-disclosure"))
            .remediationCost("15min")
            .build());

        RULES.add(RuleDefinition.builder("java-owasp-a04-002")
            .name("未限制上傳檔案類型")
            .description("檔案上傳功能未驗證檔案類型和內容，可能導致惡意檔案上傳。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A04_INSECURE_DESIGN)
            .cweIds(List.of("CWE-434"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "file-upload"))
            .remediationCost("30min")
            .build());

        // A05:2021 - Security Misconfiguration
        RULES.add(RuleDefinition.builder("java-owasp-a05-001")
            .name("XML 外部實體注入 (XXE)")
            .description("XML 解析器未禁用外部實體，可能導致檔案讀取、SSRF 等攻擊。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A05_SECURITY_MISCONFIGURATION)
            .cweIds(List.of("CWE-611", "CWE-776"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "xxe"))
            .remediationCost("20min")
            .build());

        // A07:2021 - Identification and Authentication Failures
        RULES.add(RuleDefinition.builder("java-owasp-a07-001")
            .name("硬編碼的密碼")
            .description("密碼或憑證硬編碼在程式碼中，容易被反編譯獲取。")
            .severity(RuleSeverity.BLOCKER)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A07_IDENTIFICATION_AUTHENTICATION_FAILURES)
            .cweIds(List.of("CWE-259", "CWE-798"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "hardcoded-password"))
            .remediationCost("30min")
            .build());

        RULES.add(RuleDefinition.builder("java-owasp-a07-002")
            .name("弱密碼要求")
            .description("密碼複雜度要求過低，容易被暴力破解。")
            .severity(RuleSeverity.MAJOR)
            .type(RuleType.SECURITY_HOTSPOT)
            .owaspCategory(Owasp2021Category.A07_IDENTIFICATION_AUTHENTICATION_FAILURES)
            .cweIds(List.of("CWE-521"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "weak-password"))
            .remediationCost("20min")
            .build());

        // A08:2021 - Software and Data Integrity Failures
        RULES.add(RuleDefinition.builder("java-owasp-a08-001")
            .name("不安全的反序列化")
            .description("反序列化不受信任的資料，可能導致遠端程式碼執行。")
            .severity(RuleSeverity.BLOCKER)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A08_SOFTWARE_DATA_INTEGRITY_FAILURES)
            .cweIds(List.of("CWE-502"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "deserialization"))
            .remediationCost("40min")
            .build());

        // A09:2021 - Security Logging and Monitoring Failures
        RULES.add(RuleDefinition.builder("java-owasp-a09-001")
            .name("缺少安全事件記錄")
            .description("關鍵安全操作（登入、權限變更）未記錄日誌，難以偵測和追蹤攻擊。")
            .severity(RuleSeverity.MAJOR)
            .type(RuleType.SECURITY_HOTSPOT)
            .owaspCategory(Owasp2021Category.A09_SECURITY_LOGGING_MONITORING_FAILURES)
            .cweIds(List.of("CWE-778"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "logging"))
            .remediationCost("20min")
            .build());

        RULES.add(RuleDefinition.builder("java-owasp-a09-002")
            .name("敏感資訊記錄在日誌中")
            .description("日誌包含敏感資訊（密碼、信用卡號），可能導致資訊洩露。")
            .severity(RuleSeverity.CRITICAL)
            .type(RuleType.VULNERABILITY)
            .owaspCategory(Owasp2021Category.A09_SECURITY_LOGGING_MONITORING_FAILURES)
            .cweIds(List.of("CWE-532"))
            .language(LANGUAGE)
            .tags(List.of("owasp", "security", "sensitive-data-in-logs"))
            .remediationCost("15min")
            .build());

        // A10:2021 - Server-Side Request Forgery
        RULES.add(RuleDefinition.builder("java-owasp-a10-001")
            .name("伺服器端請求偽造 (SSRF)")
            .description("使用者提供的 URL 未經驗證直接發送請求，可能存取內部資源。")
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
     * 獲取所有 Java 安全規則
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
