package com.github.sonarqube.rules.owasp2021;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2021 A09: Security Logging and Monitoring Failures
 */
public class SecurityLoggingFailuresRule extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2021-a09-001";
    private static final Pattern MISSING_LOGGING = Pattern.compile("(?:login|authenticate|authorize).*(?:fail|error|exception)(?!.*log)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOG_INJECTION = Pattern.compile("log\\.(?:info|warn|error).*(?:\\+|concat).*(?:request\\.|params\\.)", Pattern.CASE_INSENSITIVE);

    public SecurityLoggingFailuresRule() {
        super(RuleDefinition.builder(RULE_ID)
            .severity(RuleDefinition.RuleSeverity.MAJOR).type(RuleDefinition.RuleType.VULNERABILITY).owaspCategory("A09")
            .cweId("CWE-117").cweId("CWE-223").cweId("CWE-532").cweId("CWE-778")
            .language("java").tag("owasp-2021").tag("security").tag("logging").build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, MISSING_LOGGING)) {
            violations.add(createViolation(line, "Missing security event logging (CWE-778)", code, "Log security events with appropriate detail"));
        }
        for (int line : findMatchingLines(code, LOG_INJECTION)) {
            violations.add(createViolation(line, "Log injection vulnerability (CWE-117)", code, "Sanitize user input before logging"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
