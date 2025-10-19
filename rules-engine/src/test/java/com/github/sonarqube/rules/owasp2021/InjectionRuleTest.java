package com.github.sonarqube.rules.owasp2021;

import com.github.sonarqube.rules.RuleContext;
import com.github.sonarqube.rules.RuleDefinition;
import com.github.sonarqube.rules.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InjectionRuleTest {

    private InjectionRule rule;

    @BeforeEach
    void setUp() {
        rule = new InjectionRule();
    }

    @Test
    void testRuleMetadata() {
        assertEquals("owasp-2021-a03-001", rule.getRuleId());
        assertEquals("A03", rule.getOwaspCategory());
        assertEquals(RuleDefinition.RuleSeverity.BLOCKER, rule.getDefaultSeverity());
        assertEquals(33, rule.getCweIds().size());
    }

    @Test
    void testDetectSqlInjection() {
        String code = "String query = \"SELECT * FROM users WHERE id = \" + request.getParameter(\"id\");\n" +
                     "Statement stmt = conn.createStatement();\n" +
                     "ResultSet rs = stmt.executeQuery(query);";

        RuleResult result = rule.execute(RuleContext.builder(code, "java").owaspVersion("2021").build());
        assertTrue(result.hasViolations());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.getMessage().contains("SQL Injection")));
    }

    @Test
    void testDetectXss() {
        String code = "String userInput = request.getParameter(\"name\");\n" +
                     "response.getWriter().write(\"<h1>Hello \" + userInput + \"</h1>\");";

        RuleResult result = rule.execute(RuleContext.builder(code, "java").owaspVersion("2021").build());
        assertTrue(result.hasViolations());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.getMessage().contains("XSS")));
    }

    @Test
    void testDetectCommandInjection() {
        String code = "String filename = request.getParameter(\"file\");\n" +
                     "Runtime.getRuntime().exec(\"cat \" + filename);";

        RuleResult result = rule.execute(RuleContext.builder(code, "java").owaspVersion("2021").build());
        assertTrue(result.hasViolations());
        assertTrue(result.getViolations().stream().anyMatch(v -> v.getMessage().contains("Command Injection")));
    }

    @Test
    void testSecureCode() {
        String code = "PreparedStatement ps = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\");\n" +
                     "ps.setString(1, userId);\n" +
                     "ResultSet rs = ps.executeQuery();";

        RuleResult result = rule.execute(RuleContext.builder(code, "java").owaspVersion("2021").build());
        assertEquals(0, result.getViolationCount());
    }
}
