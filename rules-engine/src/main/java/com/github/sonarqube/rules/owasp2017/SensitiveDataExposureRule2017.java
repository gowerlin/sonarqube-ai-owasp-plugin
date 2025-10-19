package com.github.sonarqube.rules.owasp2017;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2017 A3: Sensitive Data Exposure
 */
public class SensitiveDataExposureRule2017 extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2017-a3-001";
    private static final Pattern HTTP_PLAINTEXT = Pattern.compile("(?:http://|url\\s*=\\s*['\"]http:|setUrl\\(\\s*['\"]http:)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WEAK_CRYPTO = Pattern.compile("(?:DES|RC4|MD5)(?:['\"]|\\s|\\()", Pattern.CASE_INSENSITIVE);
    private static final Pattern INSECURE_SSL = Pattern.compile("(?:SSLv2|SSLv3|TLSv1\\.0)", Pattern.CASE_INSENSITIVE);

    public SensitiveDataExposureRule2017() {
        super(RuleDefinition.builder()
            .ruleKey(RULE_ID).name("OWASP 2017 A3: Sensitive Data Exposure").description("Detects sensitive data exposure")
            .severity(RuleDefinition.RuleSeverity.CRITICAL).type(RuleDefinition.RuleType.VULNERABILITY).owaspCategory("A3").owaspVersion("2017")
            .cweId("CWE-319").cweId("CWE-327").cweId("CWE-326")
            .language("java").tag("owasp-2017").tag("crypto").build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, HTTP_PLAINTEXT)) {
            violations.add(createViolation(line, "Plaintext HTTP transmission (CWE-319)", code, "Use HTTPS"));
        }
        for (int line : findMatchingLines(code, WEAK_CRYPTO)) {
            violations.add(createViolation(line, "Weak cryptographic algorithm (CWE-327)", code, "Use AES-256"));
        }
        for (int line : findMatchingLines(code, INSECURE_SSL)) {
            violations.add(createViolation(line, "Insecure SSL/TLS version (CWE-326)", code, "Use TLS 1.2+"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
