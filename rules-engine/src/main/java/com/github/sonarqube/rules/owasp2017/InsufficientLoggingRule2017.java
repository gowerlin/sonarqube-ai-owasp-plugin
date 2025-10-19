package com.github.sonarqube.rules.owasp2017;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2017 A10: Insufficient Logging & Monitoring
 */
public class InsufficientLoggingRule2017 extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2017-a10-001";
    private static final Pattern MISSING_LOGGING = Pattern.compile("(?:login|authenticate|authorize).*(?:fail|error|exception)(?!.*log)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOG_INJECTION = Pattern.compile("log\\.(?:info|warn|error).*(?:\\+|concat).*(?:request\\.|params\\.)", Pattern.CASE_INSENSITIVE);

    public InsufficientLoggingRule2017() {
        super(RuleDefinition.builder()
            .ruleKey(RULE_ID).name("OWASP 2017 A10: Insufficient Logging & Monitoring").description("Detects logging issues")
            .severity(RuleDefinition.RuleSeverity.MAJOR).type(RuleDefinition.RuleType.VULNERABILITY).owaspCategory("A10").owaspVersion("2017")
            .cweId("CWE-778").cweId("CWE-117")
            .language("java").tag("owasp-2017").tag("logging").build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, MISSING_LOGGING)) {
            violations.add(createViolation(line, "Missing security logging (CWE-778)", code, "Add security event logging"));
        }
        for (int line : findMatchingLines(code, LOG_INJECTION)) {
            violations.add(createViolation(line, "Log injection vulnerability (CWE-117)", code, "Sanitize log input"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
