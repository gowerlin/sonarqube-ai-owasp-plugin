package com.github.sonarqube.rules.owasp2021;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2021 A08: Software and Data Integrity Failures
 */
public class DataIntegrityFailuresRule extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2021-a08-001";
    private static final Pattern UNSAFE_DESERIALIZATION = Pattern.compile("(?:ObjectInputStream|readObject|XMLDecoder|Yaml\\.load|JSON\\.parse).*(?:request\\.|params\\.)", Pattern.CASE_INSENSITIVE);

    public DataIntegrityFailuresRule() {
        super(RuleDefinition.builder()
            .ruleKey(RULE_ID).name("Data Integrity Failures").description("Detects insecure deserialization")
            .severity(RuleDefinition.RuleSeverity.BLOCKER).type(RuleDefinition.RuleType.VULNERABILITY).owaspCategory("A08")
            .cweId("CWE-345").cweId("CWE-353").cweId("CWE-426").cweId("CWE-494").cweId("CWE-502")
            .cweId("CWE-565").cweId("CWE-784").cweId("CWE-829").cweId("CWE-830").cweId("CWE-915")
            .language("java").tag("owasp-2021").tag("security").tag("deserialization").build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, UNSAFE_DESERIALIZATION)) {
            violations.add(createViolation(line, "Unsafe deserialization (CWE-502)", code, "Validate and sanitize deserialized data, use safe serialization formats"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
