package com.github.sonarqube.rules;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RuleResult 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.1)
 */
class RuleResultTest {

    @Test
    void testSuccessResultNoViolations() {
        RuleResult result = RuleResult.success("test-rule-001");

        assertTrue(result.isSuccess());
        assertEquals("test-rule-001", result.getRuleId());
        assertEquals(0, result.getViolationCount());
        assertFalse(result.hasViolations());
        assertNull(result.getErrorMessage());
    }

    @Test
    void testSuccessResultWithViolations() {
        RuleResult.RuleViolation violation = RuleResult.RuleViolation.builder()
            .lineNumber(10)
            .message("Security issue detected")
            .severity(RuleDefinition.RuleSeverity.CRITICAL)
            .build();

        RuleResult result = RuleResult.success("test-rule-002", List.of(violation));

        assertTrue(result.isSuccess());
        assertEquals("test-rule-002", result.getRuleId());
        assertEquals(1, result.getViolationCount());
        assertTrue(result.hasViolations());
        assertEquals(violation, result.getViolations().get(0));
    }

    @Test
    void testFailureResult() {
        RuleResult result = RuleResult.failure("test-rule-003", "Rule execution failed");

        assertFalse(result.isSuccess());
        assertEquals("test-rule-003", result.getRuleId());
        assertEquals(0, result.getViolationCount());
        assertEquals("Rule execution failed", result.getErrorMessage());
    }

    @Test
    void testBuilderWithViolations() {
        RuleResult.RuleViolation violation1 = RuleResult.RuleViolation.builder()
            .lineNumber(5)
            .message("Issue 1")
            .build();

        RuleResult.RuleViolation violation2 = RuleResult.RuleViolation.builder()
            .lineNumber(10)
            .message("Issue 2")
            .build();

        RuleResult result = RuleResult.builder("test-rule-004")
            .success(true)
            .violation(violation1)
            .violation(violation2)
            .executionTimeMs(100)
            .build();

        assertTrue(result.isSuccess());
        assertEquals(2, result.getViolationCount());
        assertEquals(100, result.getExecutionTimeMs());
    }

    @Test
    void testBuilderWithViolationsList() {
        List<RuleResult.RuleViolation> violations = Arrays.asList(
            RuleResult.RuleViolation.builder().lineNumber(1).message("Issue 1").build(),
            RuleResult.RuleViolation.builder().lineNumber(2).message("Issue 2").build(),
            RuleResult.RuleViolation.builder().lineNumber(3).message("Issue 3").build()
        );

        RuleResult result = RuleResult.builder("test-rule-005")
            .success(true)
            .violations(violations)
            .build();

        assertEquals(3, result.getViolationCount());
    }

    @Test
    void testViolationBuilder() {
        RuleResult.RuleViolation violation = RuleResult.RuleViolation.builder()
            .lineNumber(15)
            .message("SQL Injection vulnerability")
            .severity(RuleDefinition.RuleSeverity.CRITICAL)
            .codeSnippet("String query = \"SELECT * FROM users WHERE id = \" + userId;")
            .fixSuggestion("Use PreparedStatement instead")
            .build();

        assertEquals(15, violation.getLineNumber());
        assertEquals("SQL Injection vulnerability", violation.getMessage());
        assertEquals(RuleDefinition.RuleSeverity.CRITICAL, violation.getSeverity());
        assertEquals("String query = \"SELECT * FROM users WHERE id = \" + userId;", violation.getCodeSnippet());
        assertEquals("Use PreparedStatement instead", violation.getFixSuggestion());
    }

    @Test
    void testViolationDefaultSeverity() {
        RuleResult.RuleViolation violation = RuleResult.RuleViolation.builder()
            .lineNumber(20)
            .message("Issue")
            .build();

        assertEquals(RuleDefinition.RuleSeverity.MAJOR, violation.getSeverity());
    }

    @Test
    void testViolationNullMessage() {
        assertThrows(NullPointerException.class, () ->
            RuleResult.RuleViolation.builder()
                .lineNumber(25)
                .build()
        );
    }

    @Test
    void testResultNullRuleId() {
        assertThrows(NullPointerException.class, () ->
            RuleResult.builder(null).build()
        );
    }

    @Test
    void testToString() {
        RuleResult result = RuleResult.success("test-rule-006");
        String str = result.toString();
        assertTrue(str.contains("test-rule-006"));
        assertTrue(str.contains("success=true"));
    }

    @Test
    void testViolationToString() {
        RuleResult.RuleViolation violation = RuleResult.RuleViolation.builder()
            .lineNumber(30)
            .message("Test issue")
            .severity(RuleDefinition.RuleSeverity.BLOCKER)
            .build();

        String str = violation.toString();
        assertTrue(str.contains("30"));
        assertTrue(str.contains("Test issue"));
        assertTrue(str.contains("BLOCKER"));
    }
}
