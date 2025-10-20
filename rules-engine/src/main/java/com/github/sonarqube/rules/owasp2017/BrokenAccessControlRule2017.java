package com.github.sonarqube.rules.owasp2017;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2017 A5: Broken Access Control
 */
public class BrokenAccessControlRule2017 extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2017-a5-001";
    private static final Pattern PATH_TRAVERSAL = Pattern.compile("\\.\\.[\\\\/]|\\.\\.%2[fF]", Pattern.CASE_INSENSITIVE);
    private static final Pattern MISSING_AUTH = Pattern.compile("@(?:GetMapping|PostMapping|RequestMapping).*(?!.*@(?:PreAuthorize|Secured))", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNSAFE_REDIRECT = Pattern.compile("(?:sendRedirect|redirect:)\\s*\\(\\s*(?:request\\.|params\\.)", Pattern.CASE_INSENSITIVE);

    public BrokenAccessControlRule2017() {
        super(RuleDefinition.builder(RULE_ID)
            .cweId("CWE-22").cweId("CWE-284").cweId("CWE-601")
            .language("java").tag("owasp-2017").tag("access-control").build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, PATH_TRAVERSAL)) {
            violations.add(createViolation(line, "Path traversal vulnerability (CWE-22)", code, "Validate file paths"));
        }
        for (int line : findMatchingLines(code, MISSING_AUTH)) {
            violations.add(createViolation(line, "Missing authorization check (CWE-862)", code, "Add @PreAuthorize"));
        }
        for (int line : findMatchingLines(code, UNSAFE_REDIRECT)) {
            violations.add(createViolation(line, "Open redirect vulnerability (CWE-601)", code, "Validate redirect URLs"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
