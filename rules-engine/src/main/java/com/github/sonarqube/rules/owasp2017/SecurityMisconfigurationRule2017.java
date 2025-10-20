package com.github.sonarqube.rules.owasp2017;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2017 A6: Security Misconfiguration
 */
public class SecurityMisconfigurationRule2017 extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2017-a6-001";
    private static final Pattern DEBUG_MODE = Pattern.compile("(?:debug|DEBUG)\\s*=\\s*(?:true|1|enabled)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEFAULT_CREDENTIALS = Pattern.compile("(?:admin|root).*(?:admin|root|password)", Pattern.CASE_INSENSITIVE);

    public SecurityMisconfigurationRule2017() {
        super(RuleDefinition.builder(RULE_ID)
            .cweId("CWE-2").cweId("CWE-16").cweId("CWE-798")
            .language("java").tag("owasp-2017").tag("config").build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, DEBUG_MODE)) {
            violations.add(createViolation(line, "Debug mode enabled (CWE-489)", code, "Disable debug in production"));
        }
        for (int line : findMatchingLines(code, DEFAULT_CREDENTIALS)) {
            violations.add(createViolation(line, "Default credentials detected (CWE-798)", code, "Use unique credentials"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
