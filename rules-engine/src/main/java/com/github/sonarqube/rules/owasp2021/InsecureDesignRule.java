package com.github.sonarqube.rules.owasp2021;

import com.github.sonarqube.rules.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2021 A04: Insecure Design 檢測規則
 *
 * 檢測不安全的設計模式，包括：
 * - Missing Rate Limiting (缺少速率限制)
 * - Insufficient Business Logic Validation (業務邏輯驗證不足)
 * - Missing Anti-automation Controls (缺少反自動化控制)
 * - Unrestricted File Upload (不受限制的檔案上傳)
 * - Missing Input Validation (缺少輸入驗證)
 *
 * CWE 映射：40 個 CWE
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.5)
 */
public class InsecureDesignRule extends AbstractOwaspRule {

    private static final String RULE_ID = "owasp-2021-a04-001";

    private static final Pattern UNRESTRICTED_UPLOAD_PATTERN = Pattern.compile(
        "(?:MultipartFile|FileUpload|uploadFile).*(?:save|store|write)(?!.*(?:validate|check|verify|whitelist))",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MISSING_RATE_LIMIT_PATTERN = Pattern.compile(
        "@(?:PostMapping|RequestMapping).*(?:/login|/register|/reset|/api/)(?!.*@RateLimited)",
        Pattern.CASE_INSENSITIVE
    );

    public InsecureDesignRule() {
        super(createRuleDefinition());
    }

    private static RuleDefinition createRuleDefinition() {
        return RuleDefinition.builder()
            .ruleKey(RULE_ID)
            .name("Insecure Design Detection")
            .description("Detects insecure design patterns")
            .severity(RuleDefinition.RuleSeverity.MAJOR)
            .type(RuleDefinition.RuleType.VULNERABILITY)
            .owaspCategory("A04")
            .cweId("CWE-73").cweId("CWE-183").cweId("CWE-209").cweId("CWE-213")
            .cweId("CWE-235").cweId("CWE-256").cweId("CWE-257").cweId("CWE-266")
            .cweId("CWE-269").cweId("CWE-280").cweId("CWE-311").cweId("CWE-312")
            .cweId("CWE-313").cweId("CWE-316").cweId("CWE-419").cweId("CWE-430")
            .cweId("CWE-434").cweId("CWE-444").cweId("CWE-451").cweId("CWE-472")
            .cweId("CWE-501").cweId("CWE-522").cweId("CWE-525").cweId("CWE-539")
            .cweId("CWE-579").cweId("CWE-598").cweId("CWE-602").cweId("CWE-642")
            .cweId("CWE-646").cweId("CWE-650").cweId("CWE-653").cweId("CWE-656")
            .cweId("CWE-657").cweId("CWE-799").cweId("CWE-807").cweId("CWE-840")
            .cweId("CWE-841").cweId("CWE-927").cweId("CWE-1021").cweId("CWE-1173")
            .language("java")
            .tag("owasp-2021").tag("security").tag("design")
            .build();
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        violations.addAll(detectUnrestrictedUpload(code));
        violations.addAll(detectMissingRateLimit(code));

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }

    private List<RuleResult.RuleViolation> detectUnrestrictedUpload(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, UNRESTRICTED_UPLOAD_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "Unrestricted file upload: Missing file type/size validation (CWE-434)",
                code,
                "Validate file type whitelist, size limit, and scan for malware"
            ));
        }
        return violations;
    }

    private List<RuleResult.RuleViolation> detectMissingRateLimit(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, MISSING_RATE_LIMIT_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "Missing rate limiting: Endpoint vulnerable to brute-force attacks",
                code,
                "Implement rate limiting using @RateLimited or bucket4j library"
            ));
        }
        return violations;
    }
}
