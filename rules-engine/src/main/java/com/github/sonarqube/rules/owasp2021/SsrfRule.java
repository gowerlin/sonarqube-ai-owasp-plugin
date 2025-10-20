package com.github.sonarqube.rules.owasp2021;

import com.github.sonarqube.rules.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2021 A10: Server-Side Request Forgery (SSRF)
 */
public class SsrfRule extends AbstractOwaspRule {
    private static final String RULE_ID = "owasp-2021-a10-001";
    private static final Pattern SSRF_PATTERN = Pattern.compile("(?:HttpClient|RestTemplate|URL|URLConnection|HttpURLConnection)\\.(?:get|post|connect|openConnection).*(?:request\\.|params\\.|input\\.)", Pattern.CASE_INSENSITIVE);

    public SsrfRule() {
        super(RuleDefinition.builder(RULE_ID)
            .severity(RuleDefinition.RuleSeverity.CRITICAL).type(RuleDefinition.RuleType.VULNERABILITY).owaspCategory("A10")
            .cweId("CWE-918")
            .language("java").tag("owasp-2021").tag("security").tag("ssrf").build());
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        String code = context.getCode();

        for (int line : findMatchingLines(code, SSRF_PATTERN)) {
            violations.add(createViolation(line, "SSRF vulnerability: User-controlled URL in server-side request (CWE-918)", code, "Validate URLs against whitelist, block internal IPs"));
        }

        return RuleResult.builder(getRuleId()).success(true).violations(violations).build();
    }
}
