package com.github.sonarqube.rules.owasp2021;

import com.github.sonarqube.rules.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2021 A03: Injection 檢測規則
 *
 * 檢測各種注入攻擊漏洞，包括：
 * - SQL Injection (SQL 注入)
 * - XSS (Cross-Site Scripting 跨站腳本)
 * - Command Injection (命令注入)
 * - LDAP Injection (LDAP 注入)
 * - XML Injection (XML 注入)
 * - Expression Language Injection (表達式語言注入)
 *
 * CWE 映射：CWE-20, CWE-74, CWE-75, CWE-77, CWE-78, CWE-79, CWE-80, CWE-83,
 * CWE-87, CWE-88, CWE-89, CWE-90, CWE-91, CWE-93, CWE-94, CWE-95, CWE-96,
 * CWE-97, CWE-98, CWE-99, CWE-100, CWE-113, CWE-116, CWE-138, CWE-184,
 * CWE-470, CWE-471, CWE-564, CWE-610, CWE-643, CWE-644, CWE-652, CWE-917
 * (共 33 個 CWE)
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.4)
 */
public class InjectionRule extends AbstractOwaspRule {

    private static final String RULE_ID = "owasp-2021-a03-001";
    private static final String RULE_NAME = "Injection Vulnerabilities Detection";
    private static final String DESCRIPTION = "Detects injection vulnerabilities including SQL injection, " +
        "XSS, command injection, LDAP injection, XML injection, and expression language injection.";

    // SQL Injection 模式
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?:executeQuery|executeUpdate|execute|createQuery|createNativeQuery)\\s*\\([^)]*(?:\\+|\\{|\\$\\{|concat).*(?:request\\.|params\\.|input\\.|user\\.)",
        Pattern.CASE_INSENSITIVE
    );

    // XSS 模式 (未轉義的輸出)
    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(?:response\\.getWriter\\(\\)\\.write|out\\.print|innerHTML|outerHTML|document\\.write)\\s*\\([^)]*(?:request\\.|params\\.|input\\.)",
        Pattern.CASE_INSENSITIVE
    );

    // Command Injection 模式
    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "(?:Runtime\\.getRuntime\\(\\)\\.exec|ProcessBuilder|Process\\.start|bash|sh|cmd\\.exe).*(?:request\\.|params\\.|input\\.|user\\.)",
        Pattern.CASE_INSENSITIVE
    );

    // LDAP Injection 模式
    private static final Pattern LDAP_INJECTION_PATTERN = Pattern.compile(
        "(?:search|lookup)\\s*\\([^)]*(?:\\+|concat).*(?:request\\.|params\\.|input\\.)",
        Pattern.CASE_INSENSITIVE
    );

    // XML Injection 模式
    private static final Pattern XML_INJECTION_PATTERN = Pattern.compile(
        "(?:DocumentBuilder|XMLReader|SAXParser).*(?:request\\.|params\\.|input\\.).*(?:parse|read)",
        Pattern.CASE_INSENSITIVE
    );

    // Expression Language Injection 模式
    private static final Pattern EL_INJECTION_PATTERN = Pattern.compile(
        "\\$\\{.*(?:request\\.|params\\.|param\\.).*\\}|ValueExpression.*evaluate.*(?:request\\.|params\\.)",
        Pattern.CASE_INSENSITIVE
    );

    // NoSQL Injection 模式
    private static final Pattern NOSQL_INJECTION_PATTERN = Pattern.compile(
        "(?:find|findOne|update|remove|aggregate)\\s*\\(\\s*\\{[^}]*(?:request\\.|params\\.|\\$where)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 建構子
     */
    public InjectionRule() {
        super(createRuleDefinition());
    }

    private static RuleDefinition createRuleDefinition() {
        return RuleDefinition.builder(RULE_ID)
            .name(RULE_NAME)
            .description(DESCRIPTION)
            .severity(RuleDefinition.RuleSeverity.BLOCKER)
            .type(RuleDefinition.RuleType.VULNERABILITY)
            .owaspCategory("A03")
            .cweId("CWE-20").cweId("CWE-74").cweId("CWE-75").cweId("CWE-77")
            .cweId("CWE-78").cweId("CWE-79").cweId("CWE-80").cweId("CWE-83")
            .cweId("CWE-87").cweId("CWE-88").cweId("CWE-89").cweId("CWE-90")
            .cweId("CWE-91").cweId("CWE-93").cweId("CWE-94").cweId("CWE-95")
            .cweId("CWE-96").cweId("CWE-97").cweId("CWE-98").cweId("CWE-99")
            .cweId("CWE-100").cweId("CWE-113").cweId("CWE-116").cweId("CWE-138")
            .cweId("CWE-184").cweId("CWE-470").cweId("CWE-471").cweId("CWE-564")
            .cweId("CWE-610").cweId("CWE-643").cweId("CWE-644").cweId("CWE-652")
            .cweId("CWE-917")
            .language("java")
            .tag("owasp-2021").tag("security").tag("injection")
            .build();
    }

    @Override
    public boolean matches(RuleContext context) {
        if (!super.matches(context)) {
            return false;
        }

        String code = context.getCode();
        return containsKeyword(code, "execute") ||
               containsKeyword(code, "Query") ||
               containsKeyword(code, "request") ||
               containsKeyword(code, "Runtime") ||
               containsKeyword(code, "Process") ||
               containsKeyword(code, "write") ||
               containsKeyword(code, "innerHTML") ||
               containsKeyword(code, "search");
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        String code = context.getCode();
        List<RuleResult.RuleViolation> violations = new ArrayList<>();

        // 1. SQL Injection
        violations.addAll(detectSqlInjection(code));

        // 2. XSS
        violations.addAll(detectXss(code));

        // 3. Command Injection
        violations.addAll(detectCommandInjection(code));

        // 4. LDAP Injection
        violations.addAll(detectLdapInjection(code));

        // 5. XML Injection
        violations.addAll(detectXmlInjection(code));

        // 6. Expression Language Injection
        violations.addAll(detectElInjection(code));

        // 7. NoSQL Injection
        violations.addAll(detectNoSqlInjection(code));

        return RuleResult.builder(getRuleId())
            .success(true)
            .violations(violations)
            .build();
    }

    private List<RuleResult.RuleViolation> detectSqlInjection(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, SQL_INJECTION_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "SQL Injection vulnerability: User input directly concatenated into SQL query (CWE-89)",
                code,
                "Use PreparedStatement with parameterized queries: PreparedStatement ps = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\"); ps.setString(1, userId);"
            ));
        }

        return violations;
    }

    private List<RuleResult.RuleViolation> detectXss(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, XSS_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "Cross-Site Scripting (XSS) vulnerability: Unescaped user input rendered in output (CWE-79)",
                code,
                "Escape user input before output: use OWASP Java Encoder, HtmlUtils.htmlEscape(), or JSTL <c:out> tag with escapeXml=\"true\""
            ));
        }

        return violations;
    }

    private List<RuleResult.RuleViolation> detectCommandInjection(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, COMMAND_INJECTION_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "Command Injection vulnerability: User input passed to system command execution (CWE-78)",
                code,
                "Avoid exec() with user input. Use ProcessBuilder with argument array, validate against whitelist, or use safer APIs"
            ));
        }

        return violations;
    }

    private List<RuleResult.RuleViolation> detectLdapInjection(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, LDAP_INJECTION_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "LDAP Injection vulnerability: Unsanitized user input in LDAP query (CWE-90)",
                code,
                "Use parameterized LDAP queries or escape special characters: ( ) \\ * / NUL"
            ));
        }

        return violations;
    }

    private List<RuleResult.RuleViolation> detectXmlInjection(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, XML_INJECTION_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "XML Injection vulnerability: User input parsed as XML without validation (CWE-91)",
                code,
                "Disable DTD processing, use XML schema validation, escape XML special characters: < > & \" '"
            ));
        }

        return violations;
    }

    private List<RuleResult.RuleViolation> detectElInjection(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, EL_INJECTION_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "Expression Language Injection: User input evaluated as EL expression (CWE-917)",
                code,
                "Avoid evaluating user input as EL expressions. Validate and sanitize before using in ${} expressions"
            ));
        }

        return violations;
    }

    private List<RuleResult.RuleViolation> detectNoSqlInjection(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, NOSQL_INJECTION_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "NoSQL Injection vulnerability: Unsanitized input in NoSQL query (CWE-943)",
                code,
                "Use parameterized NoSQL queries, validate input types, avoid $where operators with user input"
            ));
        }

        return violations;
    }

    @Override
    public boolean requiresAi() {
        return false;
    }

    @Override
    public String toString() {
        return "InjectionRule{" +
            "id='" + getRuleId() + '\'' +
            ", category='A03'" +
            ", cweCount=33" +
            '}';
    }
}
