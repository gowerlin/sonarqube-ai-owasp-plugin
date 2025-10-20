package com.github.sonarqube.rules.owasp2021;

import com.github.sonarqube.rules.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2021 A01: Broken Access Control 檢測規則
 *
 * 檢測權限控制相關的安全漏洞，包括：
 * - Path Traversal (路徑遍歷攻擊)
 * - Insecure Direct Object Reference (不安全的直接物件引用)
 * - Missing Authorization Checks (缺少授權檢查)
 * - Elevation of Privilege (權限提升)
 *
 * CWE 映射：CWE-22, CWE-23, CWE-35, CWE-59, CWE-200, CWE-201, CWE-219,
 * CWE-264, CWE-275, CWE-284, CWE-285, CWE-352, CWE-359, CWE-377, CWE-402,
 * CWE-425, CWE-441, CWE-497, CWE-538, CWE-540, CWE-548, CWE-552, CWE-566,
 * CWE-601, CWE-639, CWE-651, CWE-668, CWE-706, CWE-862, CWE-863, CWE-913,
 * CWE-922, CWE-1275 (共 33 個 CWE)
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.2)
 */
public class BrokenAccessControlRule extends AbstractOwaspRule {

    private static final String RULE_ID = "owasp-2021-a01-001";
    private static final String RULE_NAME = "Broken Access Control Detection";
    private static final String DESCRIPTION = "Detects access control vulnerabilities including path traversal, " +
        "insecure direct object references, missing authorization checks, and privilege escalation attempts.";

    // Path Traversal 模式
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "\\.\\.[\\\\/]|\\.\\.%2[fF]|%2[eE]%2[eE]%2[fF]|%252[eE]%252[eE]%252[fF]"
    );

    // File operations with user input
    private static final Pattern FILE_OPERATION_PATTERN = Pattern.compile(
        "(?:new\\s+File|FileInputStream|FileOutputStream|FileReader|FileWriter|" +
        "Files\\.(?:read|write|delete|move|copy)|" +
        "Path\\.(?:get|of)|Paths\\.get)\\s*\\([^)]*(?:request\\.|params\\.|input\\.|user\\.)[^)]*\\)"
    );

    // Database queries with direct ID reference
    private static final Pattern DIRECT_OBJECT_REFERENCE_PATTERN = Pattern.compile(
        "(?:SELECT|UPDATE|DELETE)\\s+.*\\s+WHERE\\s+id\\s*=\\s*['\"]?\\s*(?:\\$|\\{|request\\.|params\\.)"
    );

    // Missing authorization checks (common frameworks)
    private static final Pattern MISSING_AUTH_PATTERN = Pattern.compile(
        "@(?:GetMapping|PostMapping|PutMapping|DeleteMapping|RequestMapping|Route)\\s*\\([^)]*\\)\\s*(?!.*@(?:PreAuthorize|Secured|RolesAllowed))"
    );

    // Unsafe redirect patterns
    private static final Pattern UNSAFE_REDIRECT_PATTERN = Pattern.compile(
        "(?:response\\.sendRedirect|redirect:|window\\.location|location\\.href)\\s*\\(?\\s*(?:request\\.|params\\.|input\\.)"
    );

    /**
     * 建構子
     */
    public BrokenAccessControlRule() {
        super(createRuleDefinition());
    }

    private static RuleDefinition createRuleDefinition() {
        return RuleDefinition.builder(RULE_ID)
            .name(RULE_NAME)
            .description(DESCRIPTION)
            .severity(RuleDefinition.RuleSeverity.CRITICAL)
            .type(RuleDefinition.RuleType.VULNERABILITY)
            .owaspCategory("A01")
            .cweId("CWE-22").cweId("CWE-23").cweId("CWE-35").cweId("CWE-59")
            .cweId("CWE-200").cweId("CWE-201").cweId("CWE-219").cweId("CWE-264")
            .cweId("CWE-275").cweId("CWE-284").cweId("CWE-285").cweId("CWE-352")
            .cweId("CWE-359").cweId("CWE-377").cweId("CWE-402").cweId("CWE-425")
            .cweId("CWE-441").cweId("CWE-497").cweId("CWE-538").cweId("CWE-540")
            .cweId("CWE-548").cweId("CWE-552").cweId("CWE-566").cweId("CWE-601")
            .cweId("CWE-639").cweId("CWE-651").cweId("CWE-668").cweId("CWE-706")
            .cweId("CWE-862").cweId("CWE-863").cweId("CWE-913").cweId("CWE-922")
            .cweId("CWE-1275")
            .language("java")
            .tag("owasp-2021").tag("security").tag("access-control")
            .build();
    }

    @Override
    public boolean matches(RuleContext context) {
        // 先呼叫父類別的基本匹配檢查
        if (!super.matches(context)) {
            return false;
        }

        // 快速檢查：是否包含可疑的關鍵字
        String code = context.getCode();
        return containsKeyword(code, "File") ||
               containsKeyword(code, "Path") ||
               containsKeyword(code, "request") ||
               containsKeyword(code, "Mapping") ||
               containsKeyword(code, "redirect") ||
               containsKeyword(code, "WHERE");
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        String code = context.getCode();
        List<RuleResult.RuleViolation> violations = new ArrayList<>();

        // 1. 檢測 Path Traversal
        violations.addAll(detectPathTraversal(code));

        // 2. 檢測不安全的檔案操作
        violations.addAll(detectUnsafeFileOperations(code));

        // 3. 檢測不安全的直接物件引用
        violations.addAll(detectDirectObjectReference(code));

        // 4. 檢測缺少授權檢查
        violations.addAll(detectMissingAuthorization(code));

        // 5. 檢測不安全的重導向
        violations.addAll(detectUnsafeRedirect(code));

        // 如果有 AI 服務，進行更深入的分析
        if (context.hasAiService() && !violations.isEmpty()) {
            violations = enhanceViolationsWithAi(context, violations);
        }

        return RuleResult.builder(getRuleId())
            .success(true)
            .violations(violations)
            .build();
    }

    /**
     * 檢測 Path Traversal 攻擊
     */
    private List<RuleResult.RuleViolation> detectPathTraversal(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, PATH_TRAVERSAL_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "Path Traversal vulnerability detected: Code contains '../' or encoded traversal sequences",
                code,
                "Validate and sanitize file paths. Use Path.normalize() and check if the resolved path is within allowed directories."
            ));
        }

        return violations;
    }

    /**
     * 檢測不安全的檔案操作
     */
    private List<RuleResult.RuleViolation> detectUnsafeFileOperations(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, FILE_OPERATION_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "Unsafe file operation with user input: Direct use of user-controlled input in file operations",
                code,
                "Validate file paths against a whitelist. Use secure file handling libraries and restrict access to specific directories."
            ));
        }

        return violations;
    }

    /**
     * 檢測不安全的直接物件引用
     */
    private List<RuleResult.RuleViolation> detectDirectObjectReference(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, DIRECT_OBJECT_REFERENCE_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "Insecure Direct Object Reference: Database query uses direct ID from user input without authorization check",
                code,
                "Implement proper authorization checks. Verify that the current user has permission to access the requested object."
            ));
        }

        return violations;
    }

    /**
     * 檢測缺少授權檢查
     */
    private List<RuleResult.RuleViolation> detectMissingAuthorization(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, MISSING_AUTH_PATTERN);

        for (int lineNumber : lines) {
            String snippet = getCodeSnippet(code, lineNumber);
            // 檢查接下來的幾行是否有授權檢查
            boolean hasAuthCheck = checkForAuthorizationInContext(code, lineNumber);

            if (!hasAuthCheck) {
                violations.add(createViolation(
                    lineNumber,
                    "Missing authorization check: Endpoint lacks @PreAuthorize, @Secured, or @RolesAllowed annotation",
                    code,
                    "Add appropriate authorization annotation (@PreAuthorize, @Secured, or @RolesAllowed) to restrict access."
                ));
            }
        }

        return violations;
    }

    /**
     * 檢測不安全的重導向
     */
    private List<RuleResult.RuleViolation> detectUnsafeRedirect(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, UNSAFE_REDIRECT_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "Unsafe redirect with user input: Open redirect vulnerability (CWE-601)",
                code,
                "Validate redirect URLs against a whitelist of allowed domains. Never use user input directly in redirects."
            ));
        }

        return violations;
    }

    /**
     * 檢查上下文中是否有授權檢查
     */
    private boolean checkForAuthorizationInContext(String code, int lineNumber) {
        // 檢查該行及前後 5 行是否有授權相關的程式碼
        String context = getCodeContext(code, lineNumber, 5);

        return context.contains("@PreAuthorize") ||
               context.contains("@Secured") ||
               context.contains("@RolesAllowed") ||
               context.contains("hasRole") ||
               context.contains("hasAuthority") ||
               context.contains("checkPermission") ||
               context.contains("isAuthorized");
    }

    /**
     * 使用 AI 增強違規項目的描述和修復建議
     */
    private List<RuleResult.RuleViolation> enhanceViolationsWithAi(RuleContext context, List<RuleResult.RuleViolation> violations) {
        // AI 增強邏輯可以在這裡實作
        // 目前返回原始違規列表
        // 未來可以呼叫 context.getAiService() 進行深入分析
        return violations;
    }

    @Override
    public boolean requiresAi() {
        return false; // 基本檢測不需要 AI，但 AI 可以增強結果
    }

    @Override
    public String toString() {
        return "BrokenAccessControlRule{" +
            "id='" + getRuleId() + '\'' +
            ", category='A01'" +
            ", cweCount=33" +
            '}';
    }
}
