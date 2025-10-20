package com.github.sonarqube.rules.owasp2021;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2021 A06: Vulnerable and Outdated Components
 */
public class VulnerableComponentsRule extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2021-a06-001";
    private static final Pattern OUTDATED_DEPENDENCY = Pattern.compile("<version>.*(?:SNAPSHOT|alpha|beta|M\\d|RC\\d)</version>", Pattern.CASE_INSENSITIVE);

    public VulnerableComponentsRule() {
        super(RuleDefinition.builder(RULE_ID)
            .severity(RuleDefinition.RuleSeverity.CRITICAL).type(RuleDefinition.RuleType.VULNERABILITY).owaspCategory("A06")
            .cweId("CWE-1035").cweId("CWE-1104")
            .language("java").tag("owasp-2021").tag("security").tag("dependencies").build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, OUTDATED_DEPENDENCY)) {
            violations.add(createViolation(line, "Unstable dependency version detected (CWE-1104)", code, "Use stable release versions"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
