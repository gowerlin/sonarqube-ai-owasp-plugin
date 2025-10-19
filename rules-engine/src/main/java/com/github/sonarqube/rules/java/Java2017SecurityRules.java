package com.github.sonarqube.rules.java;

import com.github.sonarqube.rules.owasp.Owasp2017Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Java 安全規則定義（OWASP 2017 版本）
 *
 * 定義針對 Java 語言的 OWASP 2017 安全規則。
 *
 * 注意：規則內容與 2021 版本類似，但使用 OWASP 2017 分類。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class Java2017SecurityRules {

    private static final String LANGUAGE = "java";
    private static final List<RuleInfo> RULES = new ArrayList<>();

    /**
     * 規則資訊（簡化版，用於 2017 規則）
     */
    public static class RuleInfo {
        private final String ruleKey;
        private final String name;
        private final String description;
        private final String severity;  // BLOCKER, CRITICAL, MAJOR, MINOR
        private final Owasp2017Category owaspCategory;
        private final List<String> cweIds;
        private final List<String> tags;
        private final String remediationCost;

        public RuleInfo(String ruleKey, String name, String description, String severity,
                        Owasp2017Category owaspCategory, List<String> cweIds,
                        List<String> tags, String remediationCost) {
            this.ruleKey = ruleKey;
            this.name = name;
            this.description = description;
            this.severity = severity;
            this.owaspCategory = owaspCategory;
            this.cweIds = List.copyOf(cweIds);
            this.tags = List.copyOf(tags);
            this.remediationCost = remediationCost;
        }

        // Getters
        public String getRuleKey() { return ruleKey; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getSeverity() { return severity; }
        public Owasp2017Category getOwaspCategory() { return owaspCategory; }
        public List<String> getCweIds() { return cweIds; }
        public List<String> getTags() { return tags; }
        public String getRemediationCost() { return remediationCost; }
        public String getLanguage() { return LANGUAGE; }
    }

    static {
        // A1:2017 - Injection
        RULES.add(new RuleInfo(
            "java-owasp2017-a1-001",
            "SQL 注入漏洞",
            "使用者輸入直接拼接到 SQL 語句中，未使用參數化查詢，導致 SQL 注入風險。",
            "BLOCKER",
            Owasp2017Category.A1_INJECTION,
            List.of("CWE-89"),
            List.of("owasp", "security", "sql-injection", "owasp2017"),
            "20min"
        ));

        RULES.add(new RuleInfo(
            "java-owasp2017-a1-002",
            "命令注入漏洞",
            "使用者輸入直接用於執行系統命令，未經驗證可能導致任意命令執行。",
            "BLOCKER",
            Owasp2017Category.A1_INJECTION,
            List.of("CWE-78", "CWE-77"),
            List.of("owasp", "security", "command-injection", "owasp2017"),
            "25min"
        ));

        RULES.add(new RuleInfo(
            "java-owasp2017-a1-003",
            "LDAP 注入漏洞",
            "使用者輸入未經驗證直接用於 LDAP 查詢，可能導致未授權資料存取。",
            "CRITICAL",
            Owasp2017Category.A1_INJECTION,
            List.of("CWE-90"),
            List.of("owasp", "security", "ldap-injection", "owasp2017"),
            "20min"
        ));

        // A2:2017 - Broken Authentication
        RULES.add(new RuleInfo(
            "java-owasp2017-a2-001",
            "硬編碼的密碼",
            "密碼或憑證硬編碼在程式碼中，容易被反編譯獲取。",
            "BLOCKER",
            Owasp2017Category.A2_BROKEN_AUTHENTICATION,
            List.of("CWE-259", "CWE-798"),
            List.of("owasp", "security", "hardcoded-password", "owasp2017"),
            "30min"
        ));

        RULES.add(new RuleInfo(
            "java-owasp2017-a2-002",
            "弱密碼要求",
            "密碼複雜度要求過低，容易被暴力破解。",
            "MAJOR",
            Owasp2017Category.A2_BROKEN_AUTHENTICATION,
            List.of("CWE-521"),
            List.of("owasp", "security", "weak-password", "owasp2017"),
            "20min"
        ));

        // A3:2017 - Sensitive Data Exposure
        RULES.add(new RuleInfo(
            "java-owasp2017-a3-001",
            "使用弱加密演算法",
            "使用過時或不安全的加密演算法（如 DES, MD5, SHA-1），容易被破解。",
            "CRITICAL",
            Owasp2017Category.A3_SENSITIVE_DATA_EXPOSURE,
            List.of("CWE-327", "CWE-328"),
            List.of("owasp", "security", "cryptography", "owasp2017"),
            "15min"
        ));

        RULES.add(new RuleInfo(
            "java-owasp2017-a3-002",
            "硬編碼的加密金鑰",
            "加密金鑰硬編碼在程式碼中，容易洩露且難以更換。",
            "CRITICAL",
            Owasp2017Category.A3_SENSITIVE_DATA_EXPOSURE,
            List.of("CWE-321", "CWE-798"),
            List.of("owasp", "security", "hardcoded-secret", "owasp2017"),
            "30min"
        ));

        // A4:2017 - XML External Entities (XXE)
        RULES.add(new RuleInfo(
            "java-owasp2017-a4-001",
            "XML 外部實體注入 (XXE)",
            "XML 解析器未禁用外部實體，可能導致檔案讀取、SSRF 等攻擊。",
            "CRITICAL",
            Owasp2017Category.A4_XML_EXTERNAL_ENTITIES,
            List.of("CWE-611", "CWE-776"),
            List.of("owasp", "security", "xxe", "owasp2017"),
            "20min"
        ));

        // A5:2017 - Broken Access Control
        RULES.add(new RuleInfo(
            "java-owasp2017-a5-001",
            "缺少權限檢查的敏感操作",
            "在執行敏感操作（如刪除、修改）之前未進行適當的權限檢查。",
            "CRITICAL",
            Owasp2017Category.A5_BROKEN_ACCESS_CONTROL,
            List.of("CWE-284", "CWE-862"),
            List.of("owasp", "security", "access-control", "owasp2017"),
            "30min"
        ));

        RULES.add(new RuleInfo(
            "java-owasp2017-a5-002",
            "路徑遍歷漏洞",
            "使用者輸入直接用於檔案路徑操作，未經驗證可能導致路徑遍歷攻擊。",
            "CRITICAL",
            Owasp2017Category.A5_BROKEN_ACCESS_CONTROL,
            List.of("CWE-22", "CWE-23"),
            List.of("owasp", "security", "path-traversal", "owasp2017"),
            "20min"
        ));

        // A6:2017 - Security Misconfiguration
        RULES.add(new RuleInfo(
            "java-owasp2017-a6-001",
            "使用不安全的隨機數生成器",
            "使用 Random 而非 SecureRandom 生成安全相關的隨機數，可預測性高。",
            "MAJOR",
            Owasp2017Category.A6_SECURITY_MISCONFIGURATION,
            List.of("CWE-330", "CWE-338"),
            List.of("owasp", "security", "random", "owasp2017"),
            "10min"
        ));

        // A7:2017 - Cross-Site Scripting (XSS)
        RULES.add(new RuleInfo(
            "java-owasp2017-a7-001",
            "跨站腳本攻擊 (XSS) 漏洞",
            "未經淨化的使用者輸入直接輸出到 HTML，可能導致 XSS 攻擊。",
            "BLOCKER",
            Owasp2017Category.A7_CROSS_SITE_SCRIPTING,
            List.of("CWE-79"),
            List.of("owasp", "security", "xss", "owasp2017"),
            "20min"
        ));

        // A8:2017 - Insecure Deserialization
        RULES.add(new RuleInfo(
            "java-owasp2017-a8-001",
            "不安全的反序列化",
            "反序列化不受信任的資料，可能導致遠端程式碼執行。",
            "BLOCKER",
            Owasp2017Category.A8_INSECURE_DESERIALIZATION,
            List.of("CWE-502"),
            List.of("owasp", "security", "deserialization", "owasp2017"),
            "40min"
        ));

        // A9:2017 - Using Components with Known Vulnerabilities
        RULES.add(new RuleInfo(
            "java-owasp2017-a9-001",
            "使用已知漏洞的依賴套件",
            "專案依賴包含已知安全漏洞的函式庫版本。",
            "CRITICAL",
            Owasp2017Category.A9_USING_COMPONENTS_WITH_KNOWN_VULNERABILITIES,
            List.of("CWE-1104"),
            List.of("owasp", "security", "dependencies", "owasp2017"),
            "30min"
        ));

        // A10:2017 - Insufficient Logging & Monitoring
        RULES.add(new RuleInfo(
            "java-owasp2017-a10-001",
            "缺少安全事件記錄",
            "關鍵安全操作（登入、權限變更）未記錄日誌，難以偵測和追蹤攻擊。",
            "MAJOR",
            Owasp2017Category.A10_INSUFFICIENT_LOGGING_MONITORING,
            List.of("CWE-778"),
            List.of("owasp", "security", "logging", "owasp2017"),
            "20min"
        ));

        RULES.add(new RuleInfo(
            "java-owasp2017-a10-002",
            "敏感資訊記錄在日誌中",
            "日誌包含敏感資訊（密碼、信用卡號），可能導致資訊洩露。",
            "CRITICAL",
            Owasp2017Category.A10_INSUFFICIENT_LOGGING_MONITORING,
            List.of("CWE-532"),
            List.of("owasp", "security", "sensitive-data-in-logs", "owasp2017"),
            "15min"
        ));
    }

    public static List<RuleInfo> getAllRules() {
        return Collections.unmodifiableList(RULES);
    }

    public static List<RuleInfo> getRulesByCategory(Owasp2017Category category) {
        if (category == null) {
            return Collections.emptyList();
        }

        return RULES.stream()
            .filter(rule -> rule.getOwaspCategory() == category)
            .toList();
    }

    public static RuleInfo getRuleByKey(String ruleKey) {
        if (ruleKey == null) {
            return null;
        }

        return RULES.stream()
            .filter(rule -> rule.getRuleKey().equals(ruleKey))
            .findFirst()
            .orElse(null);
    }

    public static Map<Owasp2017Category, Long> getRuleStatistics() {
        return RULES.stream()
            .filter(rule -> rule.getOwaspCategory() != null)
            .collect(Collectors.groupingBy(
                RuleInfo::getOwaspCategory,
                Collectors.counting()
            ));
    }

    public static int getRuleCount() {
        return RULES.size();
    }
}
