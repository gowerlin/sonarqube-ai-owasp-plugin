package com.github.sonarqube.rules.owasp2017;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2017 A1: Injection
 */
public class InjectionRule2017 extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2017-a1-001";
    private static final Pattern SQL_INJECTION = Pattern.compile("(?:executeQuery|executeUpdate|execute|createQuery)\\s*\\([^)]*(?:\\+|\\{|\\$\\{|concat).*(?:request\\.|params\\.)", Pattern.CASE_INSENSITIVE);
    private static final Pattern XSS_PATTERN = Pattern.compile("(?:response\\.getWriter\\(\\)\\.write|out\\.print|innerHTML|document\\.write)\\s*\\([^)]*(?:request\\.|params\\.)", Pattern.CASE_INSENSITIVE);
    private static final Pattern COMMAND_INJECTION = Pattern.compile("(?:Runtime\\.getRuntime\\(\\)\\.exec|ProcessBuilder|Process\\.start).*(?:request\\.|params\\.)", Pattern.CASE_INSENSITIVE);
    private static final Pattern LDAP_INJECTION = Pattern.compile("(?:search|lookup)\\s*\\([^)]*(?:\\+|concat).*(?:request\\.|params\\.)", Pattern.CASE_INSENSITIVE);

    public InjectionRule2017() {
        super(RuleDefinition.builder(RULE_ID)
            .cweId("CWE-89").cweId("CWE-79").cweId("CWE-78").cweId("CWE-90")
            .language("java").tag("owasp-2017").tag("injection").build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, SQL_INJECTION)) {
            violations.add(createViolation(line, "SQL Injection vulnerability (CWE-89)", code, "Use prepared statements"));
        }
        for (int line : findMatchingLines(code, XSS_PATTERN)) {
            violations.add(createViolation(line, "Cross-Site Scripting (XSS) vulnerability (CWE-79)", code, "Sanitize output"));
        }
        for (int line : findMatchingLines(code, COMMAND_INJECTION)) {
            violations.add(createViolation(line, "Command Injection vulnerability (CWE-78)", code, "Validate input"));
        }
        for (int line : findMatchingLines(code, LDAP_INJECTION)) {
            violations.add(createViolation(line, "LDAP Injection vulnerability (CWE-90)", code, "Use parameterized queries"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
