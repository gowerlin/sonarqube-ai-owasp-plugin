package com.github.sonarqube.rules.javascript;

import com.github.sonarqube.rules.owasp.Owasp2017Category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JavaScript 安全規則定義（OWASP 2017 版本）
 *
 * 定義針對 JavaScript/Node.js 的 OWASP 2017 安全規則。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class JavaScript2017SecurityRules {

    private static final String LANGUAGE = "javascript";
    private static final List<RuleInfo> RULES = new ArrayList<>();

    public static class RuleInfo {
        private final String ruleKey;
        private final String name;
        private final String description;
        private final String severity;
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
            "js-owasp2017-a1-001",
            "NoSQL 注入",
            "使用者輸入直接拼接到 NoSQL 查詢中，可能導致資料洩露或操縱。",
            "BLOCKER",
            Owasp2017Category.A1_INJECTION,
            List.of("CWE-943"),
            List.of("owasp", "security", "nosql-injection", "owasp2017"),
            "25min"
        ));

        RULES.add(new RuleInfo(
            "js-owasp2017-a1-002",
            "命令注入",
            "使用者輸入直接用於執行系統命令（exec, spawn），可能執行任意命令。",
            "BLOCKER",
            Owasp2017Category.A1_INJECTION,
            List.of("CWE-78", "CWE-77"),
            List.of("owasp", "security", "command-injection", "owasp2017"),
            "25min"
        ));

        RULES.add(new RuleInfo(
            "js-owasp2017-a1-003",
            "危險的 eval() 使用",
            "使用 eval() 執行動態程式碼，可能導致程式碼注入攻擊。",
            "CRITICAL",
            Owasp2017Category.A1_INJECTION,
            List.of("CWE-95"),
            List.of("owasp", "security", "code-injection", "owasp2017"),
            "20min"
        ));

        // A2:2017 - Broken Authentication
        RULES.add(new RuleInfo(
            "js-owasp2017-a2-001",
            "弱 JWT 密鑰",
            "JWT 使用弱密鑰或硬編碼密鑰，容易被暴力破解。",
            "BLOCKER",
            Owasp2017Category.A2_BROKEN_AUTHENTICATION,
            List.of("CWE-798", "CWE-321"),
            List.of("owasp", "security", "jwt", "owasp2017"),
            "25min"
        ));

        RULES.add(new RuleInfo(
            "js-owasp2017-a2-002",
            "缺少速率限制",
            "登入或敏感操作缺少速率限制，容易遭受暴力破解攻擊。",
            "MAJOR",
            Owasp2017Category.A2_BROKEN_AUTHENTICATION,
            List.of("CWE-307"),
            List.of("owasp", "security", "rate-limiting", "owasp2017"),
            "30min"
        ));

        // A3:2017 - Sensitive Data Exposure
        RULES.add(new RuleInfo(
            "js-owasp2017-a3-001",
            "使用弱加密演算法",
            "使用已知不安全的加密演算法（如 MD5, SHA1），容易被破解。",
            "CRITICAL",
            Owasp2017Category.A3_SENSITIVE_DATA_EXPOSURE,
            List.of("CWE-327", "CWE-328"),
            List.of("owasp", "security", "cryptography", "owasp2017"),
            "15min"
        ));

        RULES.add(new RuleInfo(
            "js-owasp2017-a3-002",
            "硬編碼的加密金鑰或密碼",
            "密碼或加密金鑰硬編碼在程式碼中，容易洩露。",
            "BLOCKER",
            Owasp2017Category.A3_SENSITIVE_DATA_EXPOSURE,
            List.of("CWE-798", "CWE-259"),
            List.of("owasp", "security", "hardcoded-secret", "owasp2017"),
            "30min"
        ));

        // A4:2017 - XML External Entities (XXE)
        RULES.add(new RuleInfo(
            "js-owasp2017-a4-001",
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
            "js-owasp2017-a5-001",
            "缺少授權檢查",
            "API 端點或敏感操作缺少適當的授權檢查，可能導致未授權存取。",
            "CRITICAL",
            Owasp2017Category.A5_BROKEN_ACCESS_CONTROL,
            List.of("CWE-862", "CWE-285"),
            List.of("owasp", "security", "authorization", "owasp2017"),
            "30min"
        ));

        RULES.add(new RuleInfo(
            "js-owasp2017-a5-002",
            "路徑遍歷漏洞",
            "使用者輸入直接用於檔案路徑操作，未經驗證可能導致讀取任意檔案。",
            "CRITICAL",
            Owasp2017Category.A5_BROKEN_ACCESS_CONTROL,
            List.of("CWE-22", "CWE-23"),
            List.of("owasp", "security", "path-traversal", "owasp2017"),
            "20min"
        ));

        // A6:2017 - Security Misconfiguration
        RULES.add(new RuleInfo(
            "js-owasp2017-a6-001",
            "不安全的 CORS 配置",
            "CORS 配置過於寬鬆（如 Access-Control-Allow-Origin: *）。",
            "MAJOR",
            Owasp2017Category.A6_SECURITY_MISCONFIGURATION,
            List.of("CWE-942"),
            List.of("owasp", "security", "cors", "owasp2017"),
            "15min"
        ));

        RULES.add(new RuleInfo(
            "js-owasp2017-a6-002",
            "缺少安全標頭",
            "HTTP 回應缺少關鍵安全標頭（如 X-Frame-Options, CSP）。",
            "MAJOR",
            Owasp2017Category.A6_SECURITY_MISCONFIGURATION,
            List.of("CWE-1021"),
            List.of("owasp", "security", "http-headers", "owasp2017"),
            "20min"
        ));

        // A7:2017 - Cross-Site Scripting (XSS)
        RULES.add(new RuleInfo(
            "js-owasp2017-a7-001",
            "跨站腳本攻擊 (XSS)",
            "未經淨化的使用者輸入直接輸出到 HTML，可能導致 XSS 攻擊。",
            "BLOCKER",
            Owasp2017Category.A7_CROSS_SITE_SCRIPTING,
            List.of("CWE-79"),
            List.of("owasp", "security", "xss", "owasp2017"),
            "20min"
        ));

        RULES.add(new RuleInfo(
            "js-owasp2017-a7-002",
            "DOM-based XSS",
            "客戶端 JavaScript 未安全處理 DOM 操作，可能導致 DOM XSS。",
            "BLOCKER",
            Owasp2017Category.A7_CROSS_SITE_SCRIPTING,
            List.of("CWE-79"),
            List.of("owasp", "security", "dom-xss", "owasp2017"),
            "25min"
        ));

        // A8:2017 - Insecure Deserialization
        RULES.add(new RuleInfo(
            "js-owasp2017-a8-001",
            "不安全的反序列化",
            "反序列化不受信任的資料（如 eval(JSON.parse())）。",
            "BLOCKER",
            Owasp2017Category.A8_INSECURE_DESERIALIZATION,
            List.of("CWE-502"),
            List.of("owasp", "security", "deserialization", "owasp2017"),
            "35min"
        ));

        // A9:2017 - Using Components with Known Vulnerabilities
        RULES.add(new RuleInfo(
            "js-owasp2017-a9-001",
            "使用已知漏洞的依賴套件",
            "專案依賴包含已知安全漏洞的套件版本。",
            "CRITICAL",
            Owasp2017Category.A9_USING_COMPONENTS_WITH_KNOWN_VULNERABILITIES,
            List.of("CWE-1104"),
            List.of("owasp", "security", "dependencies", "owasp2017"),
            "30min"
        ));

        // A10:2017 - Insufficient Logging & Monitoring
        RULES.add(new RuleInfo(
            "js-owasp2017-a10-001",
            "缺少安全事件記錄",
            "關鍵安全操作（登入失敗、權限變更）未記錄日誌。",
            "MAJOR",
            Owasp2017Category.A10_INSUFFICIENT_LOGGING_MONITORING,
            List.of("CWE-778"),
            List.of("owasp", "security", "logging", "owasp2017"),
            "20min"
        ));

        RULES.add(new RuleInfo(
            "js-owasp2017-a10-002",
            "敏感資料記錄在日誌中",
            "日誌包含敏感資訊（密碼、Token），可能導致資訊洩露。",
            "CRITICAL",
            Owasp2017Category.A10_INSUFFICIENT_LOGGING_MONITORING,
            List.of("CWE-532", "CWE-215"),
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
