package com.github.sonarqube.rules.owasp2021;

import com.github.sonarqube.rules.RuleContext;
import com.github.sonarqube.rules.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InsecureDesignRuleTest {
    private InsecureDesignRule rule;

    @BeforeEach
    void setUp() {
        rule = new InsecureDesignRule();
    }

    @Test
    void testMetadata() {
        assertEquals("owasp-2021-a04-001", rule.getRuleId());
        assertEquals(40, rule.getCweIds().size());
    }

    @Test
    void testUnrestrictedUpload() {
        String code = "MultipartFile file = request.getFile(\"upload\");\nfile.transferTo(new File(\"/uploads/\" + file.getOriginalFilename()));";
        RuleResult result = rule.execute(RuleContext.builder(code, "java").owaspVersion("2021").build());
        assertTrue(result.hasViolations());
    }
}
