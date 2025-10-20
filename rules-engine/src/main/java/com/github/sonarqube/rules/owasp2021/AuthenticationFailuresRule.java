package com.github.sonarqube.rules.owasp2021;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2021 A07: Identification and Authentication Failures
 */
public class AuthenticationFailuresRule extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2021-a07-001";
    private static final Pattern WEAK_SESSION = Pattern.compile("(?:sessionId|JSESSIONID).*(?:=|predictable|sequential)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MISSING_MFA = Pattern.compile("@PostMapping.*(?:/login|/signin)(?!.*(?:MFA|2FA|OTP))", Pattern.CASE_INSENSITIVE);

    public AuthenticationFailuresRule() {
        super(RuleDefinition.builder(RULE_ID)
            .severity(RuleDefinition.RuleSeverity.CRITICAL).type(RuleDefinition.RuleType.VULNERABILITY).owaspCategory("A07")
            .cweId("CWE-255").cweId("CWE-259").cweId("CWE-287").cweId("CWE-288").cweId("CWE-290").cweId("CWE-294")
            .cweId("CWE-295").cweId("CWE-297").cweId("CWE-300").cweId("CWE-302").cweId("CWE-304").cweId("CWE-306")
            .cweId("CWE-307").cweId("CWE-346").cweId("CWE-384").cweId("CWE-521").cweId("CWE-613").cweId("CWE-620")
            .cweId("CWE-640").cweId("CWE-798").cweId("CWE-940").cweId("CWE-1216")
            .language("java").tag("owasp-2021").tag("security").tag("authentication").build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, WEAK_SESSION)) {
            violations.add(createViolation(line, "Weak session management (CWE-384)", code, "Use cryptographically secure session IDs"));
        }
        for (int line : findMatchingLines(code, MISSING_MFA)) {
            violations.add(createViolation(line, "Missing MFA for sensitive operations (CWE-308)", code, "Implement multi-factor authentication"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
