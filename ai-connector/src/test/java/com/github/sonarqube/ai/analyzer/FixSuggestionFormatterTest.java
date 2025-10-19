package com.github.sonarqube.ai.analyzer;

import com.github.sonarqube.ai.model.SecurityIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FixSuggestionFormatter 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
class FixSuggestionFormatterTest {

    private FixSuggestionFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new FixSuggestionFormatter();
    }

    @Test
    void testFormatSingleIssueComplete() {
        SecurityIssue issue = new SecurityIssue();
        issue.setOwaspCategory("A03:2021-Injection");
        issue.setCweId("CWE-89");
        issue.setSeverity(SecurityIssue.Severity.HIGH);
        issue.setLineNumber(42);
        issue.setDescription("SQL Injection vulnerability");
        issue.setFixSuggestion("Use prepared statements");
        issue.setEffortEstimate("Simple (0.5-1 hour)");

        SecurityIssue.CodeExample example = new SecurityIssue.CodeExample();
        example.setBefore("SELECT * FROM users WHERE id = '" + "userId" + "'");
        example.setAfter("SELECT * FROM users WHERE id = ?");
        issue.setCodeExample(example);

        String result = formatter.formatSingleIssue(issue);

        assertNotNull(result);
        assertTrue(result.contains("A03:2021-Injection"));
        assertTrue(result.contains("CWE-89"));
        assertTrue(result.contains("HIGH"));
        assertTrue(result.contains("Line 42"));
        assertTrue(result.contains("SQL Injection vulnerability"));
        assertTrue(result.contains("Use prepared statements"));
        assertTrue(result.contains("Simple (0.5-1 hour)"));
        assertTrue(result.contains("修復前"));
        assertTrue(result.contains("修復後"));
    }

    @Test
    void testFormatSingleIssueMinimal() {
        SecurityIssue issue = new SecurityIssue();
        issue.setOwaspCategory("A01");
        issue.setDescription("Minimal issue");

        String result = formatter.formatSingleIssue(issue);

        assertNotNull(result);
        assertTrue(result.contains("A01"));
        assertTrue(result.contains("Minimal issue"));
        assertFalse(result.contains("CWE"));
        assertFalse(result.contains("Line"));
    }

    @Test
    void testFormatSingleIssueNull() {
        String result = formatter.formatSingleIssue(null);
        assertEquals("", result);
    }

    @Test
    void testFormatReportEmpty() {
        List<SecurityIssue> emptyList = new ArrayList<>();
        String result = formatter.formatReport(emptyList);

        assertTrue(result.contains("未發現安全問題"));
    }

    @Test
    void testFormatReportNull() {
        String result = formatter.formatReport(null);
        assertTrue(result.contains("未發現安全問題"));
    }

    @Test
    void testFormatReportWithMultipleSeverities() {
        List<SecurityIssue> issues = new ArrayList<>();

        // High
        SecurityIssue high = new SecurityIssue();
        high.setOwaspCategory("A03");
        high.setSeverity(SecurityIssue.Severity.HIGH);
        high.setDescription("High severity issue");
        issues.add(high);

        // Medium
        SecurityIssue medium = new SecurityIssue();
        medium.setOwaspCategory("A02");
        medium.setSeverity(SecurityIssue.Severity.MEDIUM);
        medium.setDescription("Medium severity issue");
        issues.add(medium);

        // Low
        SecurityIssue low = new SecurityIssue();
        low.setOwaspCategory("A01");
        low.setSeverity(SecurityIssue.Severity.LOW);
        low.setDescription("Low severity issue");
        issues.add(low);

        String result = formatter.formatReport(issues);

        assertNotNull(result);
        assertTrue(result.contains("安全分析報告"));
        assertTrue(result.contains("問題摘要"));
        assertTrue(result.contains("總計**: 3"));
        assertTrue(result.contains("高嚴重性**: 1"));
        assertTrue(result.contains("中嚴重性**: 1"));
        assertTrue(result.contains("低嚴重性**: 1"));
        assertTrue(result.contains("高嚴重性問題"));
        assertTrue(result.contains("中嚴重性問題"));
        assertTrue(result.contains("低嚴重性問題"));
    }

    @Test
    void testFormatReportOnlyHighSeverity() {
        List<SecurityIssue> issues = new ArrayList<>();

        SecurityIssue high1 = new SecurityIssue();
        high1.setOwaspCategory("A03");
        high1.setSeverity(SecurityIssue.Severity.HIGH);
        high1.setDescription("First high issue");
        issues.add(high1);

        SecurityIssue high2 = new SecurityIssue();
        high2.setOwaspCategory("A02");
        high2.setSeverity(SecurityIssue.Severity.HIGH);
        high2.setDescription("Second high issue");
        issues.add(high2);

        String result = formatter.formatReport(issues);

        assertTrue(result.contains("高嚴重性問題 (2)"));
        assertTrue(result.contains("高嚴重性**: 2"));
        assertTrue(result.contains("中嚴重性**: 0"));
        assertTrue(result.contains("低嚴重性**: 0"));
        assertFalse(result.contains("中嚴重性問題"));
        assertFalse(result.contains("低嚴重性問題"));
    }

    @Test
    void testFormatShortSummary() {
        SecurityIssue issue = new SecurityIssue();
        issue.setOwaspCategory("A03:2021-Injection");
        issue.setDescription("SQL Injection found in user input validation");

        String result = formatter.formatShortSummary(issue);

        assertEquals("[A03:2021-Injection] SQL Injection found in user input validation", result);
    }

    @Test
    void testFormatShortSummaryLongDescription() {
        SecurityIssue issue = new SecurityIssue();
        issue.setOwaspCategory("A03");
        issue.setDescription("This is a very long description that exceeds 200 characters. " +
            "It contains detailed information about the security vulnerability and how it can be exploited. " +
            "This text should be truncated to fit within the 200 character limit.");

        String result = formatter.formatShortSummary(issue);

        assertTrue(result.length() <= 210); // "[A03] " + 200 chars + "..."
        assertTrue(result.contains("[A03]"));
        assertTrue(result.endsWith("..."));
    }

    @Test
    void testFormatShortSummaryNull() {
        String result = formatter.formatShortSummary(null);
        assertEquals("", result);
    }

    @Test
    void testFormatShortSummaryNullFields() {
        SecurityIssue issue = new SecurityIssue();
        String result = formatter.formatShortSummary(issue);
        assertEquals("", result);
    }

    @Test
    void testSeverityBadges() {
        SecurityIssue high = new SecurityIssue();
        high.setOwaspCategory("Test");
        high.setSeverity(SecurityIssue.Severity.HIGH);
        high.setDescription("Test");

        SecurityIssue medium = new SecurityIssue();
        medium.setOwaspCategory("Test");
        medium.setSeverity(SecurityIssue.Severity.MEDIUM);
        medium.setDescription("Test");

        SecurityIssue low = new SecurityIssue();
        low.setOwaspCategory("Test");
        low.setSeverity(SecurityIssue.Severity.LOW);
        low.setDescription("Test");

        String highResult = formatter.formatSingleIssue(high);
        String mediumResult = formatter.formatSingleIssue(medium);
        String lowResult = formatter.formatSingleIssue(low);

        assertTrue(highResult.contains("🚨 HIGH"));
        assertTrue(mediumResult.contains("⚠️ MEDIUM"));
        assertTrue(lowResult.contains("ℹ️ LOW"));
    }

    @Test
    void testCodeExampleWithOnlyBefore() {
        SecurityIssue issue = new SecurityIssue();
        issue.setOwaspCategory("A03");
        issue.setDescription("Test");

        SecurityIssue.CodeExample example = new SecurityIssue.CodeExample();
        example.setBefore("Bad code example");
        issue.setCodeExample(example);

        String result = formatter.formatSingleIssue(issue);

        assertTrue(result.contains("修復前"));
        assertTrue(result.contains("Bad code example"));
        assertFalse(result.contains("修復後"));
    }

    @Test
    void testCodeExampleWithOnlyAfter() {
        SecurityIssue issue = new SecurityIssue();
        issue.setOwaspCategory("A03");
        issue.setDescription("Test");

        SecurityIssue.CodeExample example = new SecurityIssue.CodeExample();
        example.setAfter("Good code example");
        issue.setCodeExample(example);

        String result = formatter.formatSingleIssue(issue);

        assertTrue(result.contains("修復後"));
        assertTrue(result.contains("Good code example"));
        assertFalse(result.contains("修復前"));
    }
}
