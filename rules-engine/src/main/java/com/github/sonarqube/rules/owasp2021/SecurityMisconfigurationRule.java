package com.github.sonarqube.rules.owasp2021;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2021 A05: Security Misconfiguration
 */
public class SecurityMisconfigurationRule extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2021-a05-001";
    private static final Pattern DEBUG_ENABLED = Pattern.compile("debug\\s*=\\s*true|DEBUG|@Profile\\(\"dev\"\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEFAULT_CREDENTIALS = Pattern.compile("admin:admin|root:root|password:password", Pattern.CASE_INSENSITIVE);

    public SecurityMisconfigurationRule() {
        super(RuleDefinition.builder(RULE_ID)
            .severity(RuleDefinition.RuleSeverity.MAJOR).type(RuleDefinition.RuleType.VULNERABILITY).owaspCategory("A05")
            .cweId("CWE-2").cweId("CWE-11").cweId("CWE-13").cweId("CWE-15").cweId("CWE-16").cweId("CWE-260")
            .cweId("CWE-315").cweId("CWE-520").cweId("CWE-526").cweId("CWE-537").cweId("CWE-541").cweId("CWE-547")
            .cweId("CWE-611").cweId("CWE-614").cweId("CWE-756").cweId("CWE-776").cweId("CWE-942").cweId("CWE-1004")
            .cweId("CWE-1032").cweId("CWE-1174")
            .language("java").tag("owasp-2021").tag("security").tag("configuration").build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, DEBUG_ENABLED)) {
            violations.add(createViolation(line, "Debug mode enabled in production (CWE-489)", code, "Disable debug mode"));
        }
        for (int line : findMatchingLines(code, DEFAULT_CREDENTIALS)) {
            violations.add(createViolation(line, "Default credentials detected (CWE-798)", code, "Change default credentials"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
