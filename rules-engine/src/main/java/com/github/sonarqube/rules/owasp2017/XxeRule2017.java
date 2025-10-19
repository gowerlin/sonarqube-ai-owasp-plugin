package com.github.sonarqube.rules.owasp2017;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2017 A4: XML External Entities (XXE)
 */
public class XxeRule2017 extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2017-a4-001";
    private static final Pattern XXE_VULNERABLE = Pattern.compile("(?:DocumentBuilderFactory|SAXParserFactory|XMLInputFactory)\\.newInstance\\(\\)(?!.*setFeature)", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXTERNAL_ENTITIES = Pattern.compile("setFeature.*FEATURE_SECURE_PROCESSING.*false", Pattern.CASE_INSENSITIVE);

    public XxeRule2017() {
        super(RuleDefinition.builder()
            .ruleKey(RULE_ID).name("OWASP 2017 A4: XML External Entities (XXE)").description("Detects XXE vulnerabilities")
            .severity(RuleDefinition.RuleSeverity.CRITICAL).type(RuleDefinition.RuleType.VULNERABILITY).owaspCategory("A4").owaspVersion("2017")
            .cweId("CWE-611").cweId("CWE-827")
            .language("java").tag("owasp-2017").tag("xxe").build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, XXE_VULNERABLE)) {
            violations.add(createViolation(line, "XXE vulnerability: XML parser not configured securely (CWE-611)", code, "Disable external entities"));
        }
        for (int line : findMatchingLines(code, EXTERNAL_ENTITIES)) {
            violations.add(createViolation(line, "Insecure XML processing feature (CWE-611)", code, "Enable FEATURE_SECURE_PROCESSING"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
