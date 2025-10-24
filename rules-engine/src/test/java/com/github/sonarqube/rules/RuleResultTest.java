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

    // ========== Story 10.1: 新欄位測試 ==========

    @Test
    void testCodeExampleBasicFunctionality() {
        // 測試 CodeExample 的基本功能
        CodeExample example = new CodeExample(
            "String query = \"SELECT * FROM users WHERE id = \" + userId;",
            "PreparedStatement stmt = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\");"
        );

        assertEquals("String query = \"SELECT * FROM users WHERE id = \" + userId;", example.getBefore());
        assertEquals("PreparedStatement stmt = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\");", example.getAfter());
    }

    @Test
    void testCodeExampleDefaultConstructor() {
        // 測試 CodeExample 的預設建構函數
        CodeExample example = new CodeExample();

        assertNull(example.getBefore());
        assertNull(example.getAfter());
    }

    @Test
    void testCodeExampleEquals() {
        // 測試 CodeExample 的 equals 方法
        CodeExample example1 = new CodeExample("before", "after");
        CodeExample example2 = new CodeExample("before", "after");
        CodeExample example3 = new CodeExample("before", "different");

        assertEquals(example1, example2);
        assertNotEquals(example1, example3);
        assertEquals(example1, example1); // 自己等於自己
        assertNotEquals(example1, null); // 不等於 null
        assertNotEquals(example1, "not a CodeExample"); // 不等於其他類型
    }

    @Test
    void testCodeExampleHashCode() {
        // 測試 CodeExample 的 hashCode 方法
        CodeExample example1 = new CodeExample("before", "after");
        CodeExample example2 = new CodeExample("before", "after");

        assertEquals(example1.hashCode(), example2.hashCode());
    }

    @Test
    void testCodeExampleToString() {
        // 測試 CodeExample 的 toString 方法
        CodeExample example = new CodeExample("before code", "after code");
        String str = example.toString();

        assertTrue(str.contains("CodeExample"));
        assertTrue(str.contains("before"));
        assertTrue(str.contains("after"));
    }

    @Test
    void testViolationWithCodeExample() {
        // 測試 RuleViolation 包含 CodeExample
        CodeExample example = new CodeExample(
            "String query = \"SELECT * FROM users WHERE id = \" + userId;",
            "PreparedStatement stmt = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\");"
        );

        RuleResult.RuleViolation violation = RuleResult.RuleViolation.builder()
            .lineNumber(15)
            .message("SQL Injection vulnerability")
            .severity(RuleDefinition.RuleSeverity.CRITICAL)
            .codeSnippet("String query = \"SELECT * FROM users WHERE id = \" + userId;")
            .fixSuggestion("Use PreparedStatement instead")
            .codeExample(example)
            .build();

        assertNotNull(violation.getCodeExample());
        assertEquals(example, violation.getCodeExample());
        assertEquals("String query = \"SELECT * FROM users WHERE id = \" + userId;", violation.getCodeExample().getBefore());
        assertEquals("PreparedStatement stmt = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\");", violation.getCodeExample().getAfter());
    }

    @Test
    void testViolationWithEffortEstimate() {
        // 測試 RuleViolation 包含 effortEstimate
        RuleResult.RuleViolation violation = RuleResult.RuleViolation.builder()
            .lineNumber(20)
            .message("Security issue")
            .severity(RuleDefinition.RuleSeverity.MAJOR)
            .effortEstimate("15 minutes")
            .build();

        assertNotNull(violation.getEffortEstimate());
        assertEquals("15 minutes", violation.getEffortEstimate());
    }

    @Test
    void testViolationWithBothNewFields() {
        // 測試 RuleViolation 同時包含 codeExample 和 effortEstimate
        CodeExample example = new CodeExample(
            "password = request.getParameter(\"password\");",
            "String password = hashPassword(request.getParameter(\"password\"));"
        );

        RuleResult.RuleViolation violation = RuleResult.RuleViolation.builder()
            .lineNumber(25)
            .message("Weak password handling")
            .severity(RuleDefinition.RuleSeverity.CRITICAL)
            .codeSnippet("password = request.getParameter(\"password\");")
            .fixSuggestion("Hash password before storage")
            .codeExample(example)
            .effortEstimate("30 minutes")
            .build();

        assertNotNull(violation.getCodeExample());
        assertNotNull(violation.getEffortEstimate());
        assertEquals(example, violation.getCodeExample());
        assertEquals("30 minutes", violation.getEffortEstimate());
    }

    @Test
    void testViolationBackwardCompatibility() {
        // 測試向後兼容性：不設置新欄位應該仍然可以正常運作
        RuleResult.RuleViolation violation = RuleResult.RuleViolation.builder()
            .lineNumber(10)
            .message("Legacy issue")
            .severity(RuleDefinition.RuleSeverity.MINOR)
            .codeSnippet("legacy code")
            .fixSuggestion("fix it")
            .build();

        // 新欄位應該為 null（向後兼容）
        assertNull(violation.getCodeExample());
        assertNull(violation.getEffortEstimate());

        // 舊欄位應該正常運作
        assertEquals(10, violation.getLineNumber());
        assertEquals("Legacy issue", violation.getMessage());
        assertEquals(RuleDefinition.RuleSeverity.MINOR, violation.getSeverity());
        assertEquals("legacy code", violation.getCodeSnippet());
        assertEquals("fix it", violation.getFixSuggestion());
    }

    @Test
    void testViolationWithNullCodeExample() {
        // 測試 codeExample 可以為 null
        RuleResult.RuleViolation violation = RuleResult.RuleViolation.builder()
            .lineNumber(15)
            .message("Issue without code example")
            .codeExample(null)
            .build();

        assertNull(violation.getCodeExample());
    }

    @Test
    void testViolationWithNullEffortEstimate() {
        // 測試 effortEstimate 可以為 null
        RuleResult.RuleViolation violation = RuleResult.RuleViolation.builder()
            .lineNumber(20)
            .message("Issue without effort estimate")
            .effortEstimate(null)
            .build();

        assertNull(violation.getEffortEstimate());
    }
}
