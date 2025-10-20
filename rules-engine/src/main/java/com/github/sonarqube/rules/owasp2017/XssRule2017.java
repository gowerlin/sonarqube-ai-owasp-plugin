package com.github.sonarqube.rules.owasp2017;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2017 A7: Cross-Site Scripting (XSS)
 */
public class XssRule2017 extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2017-a7-001";
    private static final Pattern XSS_OUTPUT = Pattern.compile("(?:response\\.getWriter|out\\.print|innerHTML|document\\.write).*(?:request\\.|params\\.)", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSAFE_EVAL = Pattern.compile("eval\\s*\\(.*(?:request\\.|params\\.|user)", Pattern.CASE_INSENSITIVE);

    public XssRule2017() {
        super(RuleDefinition.builder(RULE_ID)
            .name("OWASP 2017 A7: Cross-Site Scripting (XSS)")
            .description("Detects XSS vulnerabilities")
            .severity(RuleDefinition.RuleSeverity.CRITICAL)
            .type(RuleDefinition.RuleType.VULNERABILITY)
            .owaspCategory("A7")
            .cweId("CWE-79")
            .cweId("CWE-80")
            .language("java")
            .tag("owasp-2017")
            .tag("xss")
            .build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, XSS_OUTPUT)) {
            violations.add(createViolation(line, "XSS vulnerability: Unescaped output (CWE-79)", code, "Escape HTML entities"));
        }
        for (int line : findMatchingLines(code, UNSAFE_EVAL)) {
            violations.add(createViolation(line, "Unsafe eval with user input (CWE-95)", code, "Avoid eval"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
