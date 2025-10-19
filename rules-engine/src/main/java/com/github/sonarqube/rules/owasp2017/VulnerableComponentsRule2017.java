package com.github.sonarqube.rules.owasp2017;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2017 A9: Using Components with Known Vulnerabilities
 */
public class VulnerableComponentsRule2017 extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2017-a9-001";
    private static final Pattern OUTDATED_DEPENDENCY = Pattern.compile("(?:SNAPSHOT|alpha|beta|rc)", Pattern.CASE_INSENSITIVE);

    public VulnerableComponentsRule2017() {
        super(RuleDefinition.builder()
            .ruleKey(RULE_ID).name("OWASP 2017 A9: Using Components with Known Vulnerabilities").description("Detects vulnerable components")
            .severity(RuleDefinition.RuleSeverity.MAJOR).type(RuleDefinition.RuleType.VULNERABILITY).owaspCategory("A9").owaspVersion("2017")
            .cweId("CWE-1035").cweId("CWE-1104")
            .language("java").tag("owasp-2017").tag("dependencies").build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, OUTDATED_DEPENDENCY)) {
            violations.add(createViolation(line, "Potentially vulnerable component version (CWE-1104)", code, "Use stable releases"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
