package com.github.sonarqube.rules.owasp2017;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2017 A8: Insecure Deserialization
 */
public class InsecureDeserializationRule2017 extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2017-a8-001";
    private static final Pattern UNSAFE_DESERIALIZATION = Pattern.compile("(?:ObjectInputStream|readObject|readUnshared|XMLDecoder)", Pattern.CASE_INSENSITIVE);

    public InsecureDeserializationRule2017() {
        super(RuleDefinition.builder(RULE_ID)
            .cweId("CWE-502")
            .language("java").tag("owasp-2017").tag("deserialization").build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, UNSAFE_DESERIALIZATION)) {
            violations.add(createViolation(line, "Insecure deserialization (CWE-502)", code, "Validate before deserialization"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
