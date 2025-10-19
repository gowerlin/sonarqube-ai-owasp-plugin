package com.github.sonarqube.rules.owasp2017;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2017 A2: Broken Authentication
 */
public class BrokenAuthenticationRule2017 extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2017-a2-001";
    private static final Pattern WEAK_SESSION = Pattern.compile("(?:sessionId|session_id)\\s*=\\s*(?:request\\.|cookie\\.).*(?:Math\\.random|Random\\.next)", Pattern.CASE_INSENSITIVE);
    private static final Pattern HARDCODED_CREDENTIALS = Pattern.compile("(?:password|passwd|pwd)\\s*=\\s*['\"][^'\"]{3,}['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern MISSING_TIMEOUT = Pattern.compile("(?:session\\.setMaxInactiveInterval).*(?:[3-9]\\d{3,}|\\d{5,})", Pattern.CASE_INSENSITIVE);

    public BrokenAuthenticationRule2017() {
        super(RuleDefinition.builder()
            .ruleKey(RULE_ID).name("OWASP 2017 A2: Broken Authentication").description("Detects authentication issues")
            .severity(RuleDefinition.RuleSeverity.CRITICAL).type(RuleDefinition.RuleType.VULNERABILITY).owaspCategory("A2").owaspVersion("2017")
            .cweId("CWE-287").cweId("CWE-384").cweId("CWE-307")
            .language("java").tag("owasp-2017").tag("authentication").build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, WEAK_SESSION)) {
            violations.add(createViolation(line, "Weak session ID generation (CWE-384)", code, "Use SecureRandom"));
        }
        for (int line : findMatchingLines(code, HARDCODED_CREDENTIALS)) {
            violations.add(createViolation(line, "Hardcoded credentials (CWE-798)", code, "Use secure credential storage"));
        }
        for (int line : findMatchingLines(code, MISSING_TIMEOUT)) {
            violations.add(createViolation(line, "Excessive session timeout (CWE-613)", code, "Limit session timeout to 30 minutes"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
