package com.github.sonarqube.ai.analyzer;

import com.github.sonarqube.ai.model.SecurityIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AiResponseParser 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
class AiResponseParserTest {

    private AiResponseParser parser;

    @BeforeEach
    void setUp() {
        parser = new AiResponseParser();
    }

    @Test
    void testParseValidJsonResponse() {
        String jsonResponse = """
            {
              "issues": [
                {
                  "owaspCategory": "A01:2021-Broken Access Control",
                  "cweId": "CWE-284",
                  "severity": "HIGH",
                  "description": "Missing access control check",
                  "lineNumber": 42,
                  "fixSuggestion": "Add authorization check before accessing resource",
                  "codeExample": {
                    "before": "public void deleteUser(int id) { ... }",
                    "after": "public void deleteUser(int id) { if (!hasPermission()) throw new SecurityException(); ... }"
                  },
                  "effortEstimate": "Simple (0.5-1 hour)"
                }
              ]
            }
            """;

        List<SecurityIssue> issues = parser.parseSecurityIssues(jsonResponse);

        assertNotNull(issues);
        assertEquals(1, issues.size());

        SecurityIssue issue = issues.get(0);
        assertEquals("A01:2021-Broken Access Control", issue.getOwaspCategory());
        assertEquals("CWE-284", issue.getCweId());
        assertEquals(SecurityIssue.Severity.HIGH, issue.getSeverity());
        assertEquals("Missing access control check", issue.getDescription());
        assertEquals(42, issue.getLineNumber());
        assertNotNull(issue.getFixSuggestion());
        assertNotNull(issue.getCodeExample());
        assertEquals("Simple (0.5-1 hour)", issue.getEffortEstimate());
    }

    @Test
    void testParseMultipleIssues() {
        String jsonResponse = """
            {
              "issues": [
                {
                  "owaspCategory": "A03:2021-Injection",
                  "cweId": "CWE-89",
                  "severity": "HIGH",
                  "description": "SQL Injection vulnerability",
                  "lineNumber": 15
                },
                {
                  "owaspCategory": "A02:2021-Cryptographic Failures",
                  "cweId": "CWE-327",
                  "severity": "MEDIUM",
                  "description": "Weak cryptographic algorithm",
                  "lineNumber": 28
                }
              ]
            }
            """;

        List<SecurityIssue> issues = parser.parseSecurityIssues(jsonResponse);

        assertEquals(2, issues.size());
        assertEquals("A03:2021-Injection", issues.get(0).getOwaspCategory());
        assertEquals("A02:2021-Cryptographic Failures", issues.get(1).getOwaspCategory());
    }

    @Test
    void testParseJsonWithSurroundingText() {
        String responseWithText = """
            Here is the security analysis:

            {
              "issues": [
                {
                  "owaspCategory": "A03:2021-Injection",
                  "severity": "HIGH",
                  "description": "SQL Injection found"
                }
              ]
            }

            Please review these issues carefully.
            """;

        List<SecurityIssue> issues = parser.parseSecurityIssues(responseWithText);

        assertEquals(1, issues.size());
        assertEquals("A03:2021-Injection", issues.get(0).getOwaspCategory());
    }

    @Test
    void testParseEmptyIssuesArray() {
        String jsonResponse = """
            {
              "issues": []
            }
            """;

        List<SecurityIssue> issues = parser.parseSecurityIssues(jsonResponse);

        assertNotNull(issues);
        assertTrue(issues.isEmpty());
    }

    @Test
    void testParseNullOrEmptyInput() {
        List<SecurityIssue> issues1 = parser.parseSecurityIssues(null);
        List<SecurityIssue> issues2 = parser.parseSecurityIssues("");
        List<SecurityIssue> issues3 = parser.parseSecurityIssues("   ");

        assertNotNull(issues1);
        assertTrue(issues1.isEmpty());
        assertTrue(issues2.isEmpty());
        assertTrue(issues3.isEmpty());
    }

    @Test
    void testParseInvalidJson() {
        String invalidJson = "This is not JSON at all";

        List<SecurityIssue> issues = parser.parseSecurityIssues(invalidJson);

        assertNotNull(issues);
        assertTrue(issues.isEmpty());
    }

    @Test
    void testParseJsonWithoutIssuesField() {
        String jsonResponse = """
            {
              "result": "success",
              "message": "No issues found"
            }
            """;

        List<SecurityIssue> issues = parser.parseSecurityIssues(jsonResponse);

        assertNotNull(issues);
        assertTrue(issues.isEmpty());
    }

    @Test
    void testParseSeverityVariations() {
        String jsonResponse = """
            {
              "issues": [
                {
                  "owaspCategory": "A01",
                  "severity": "high",
                  "description": "Test 1"
                },
                {
                  "owaspCategory": "A02",
                  "severity": "MEDIUM",
                  "description": "Test 2"
                },
                {
                  "owaspCategory": "A03",
                  "severity": "Low",
                  "description": "Test 3"
                },
                {
                  "owaspCategory": "A04",
                  "severity": "INVALID",
                  "description": "Test 4"
                }
              ]
            }
            """;

        List<SecurityIssue> issues = parser.parseSecurityIssues(jsonResponse);

        assertEquals(4, issues.size());
        assertEquals(SecurityIssue.Severity.HIGH, issues.get(0).getSeverity());
        assertEquals(SecurityIssue.Severity.MEDIUM, issues.get(1).getSeverity());
        assertEquals(SecurityIssue.Severity.LOW, issues.get(2).getSeverity());
        assertEquals(SecurityIssue.Severity.LOW, issues.get(3).getSeverity()); // INVALID -> LOW
    }

    @Test
    void testParseCodeExample() {
        String jsonResponse = """
            {
              "issues": [
                {
                  "owaspCategory": "A03:2021-Injection",
                  "description": "SQL Injection",
                  "codeExample": {
                    "before": "SELECT * FROM users WHERE id = '" + userId + "'",
                    "after": "SELECT * FROM users WHERE id = ?"
                  }
                }
              ]
            }
            """;

        List<SecurityIssue> issues = parser.parseSecurityIssues(jsonResponse);

        assertEquals(1, issues.size());
        assertNotNull(issues.get(0).getCodeExample());
        assertNotNull(issues.get(0).getCodeExample().getBefore());
        assertNotNull(issues.get(0).getCodeExample().getAfter());
    }

    @Test
    void testParseOptionalFields() {
        String jsonResponse = """
            {
              "issues": [
                {
                  "owaspCategory": "A01",
                  "description": "Minimal issue"
                }
              ]
            }
            """;

        List<SecurityIssue> issues = parser.parseSecurityIssues(jsonResponse);

        assertEquals(1, issues.size());
        SecurityIssue issue = issues.get(0);
        assertEquals("A01", issue.getOwaspCategory());
        assertEquals("Minimal issue", issue.getDescription());
        assertNull(issue.getCweId());
        assertNull(issue.getLineNumber());
        assertNull(issue.getFixSuggestion());
    }

    @Test
    void testIsValidIssue() {
        SecurityIssue validIssue = new SecurityIssue();
        validIssue.setOwaspCategory("A01");
        validIssue.setDescription("Valid issue");

        SecurityIssue missingCategory = new SecurityIssue();
        missingCategory.setDescription("Missing category");

        SecurityIssue missingDescription = new SecurityIssue();
        missingDescription.setOwaspCategory("A01");

        assertTrue(parser.isValidIssue(validIssue));
        assertFalse(parser.isValidIssue(missingCategory));
        assertFalse(parser.isValidIssue(missingDescription));
        assertFalse(parser.isValidIssue(null));
    }

    @Test
    void testParseNullFields() {
        String jsonResponse = """
            {
              "issues": [
                {
                  "owaspCategory": "A01",
                  "cweId": null,
                  "severity": null,
                  "description": "Test",
                  "lineNumber": null,
                  "fixSuggestion": null,
                  "codeExample": null,
                  "effortEstimate": null
                }
              ]
            }
            """;

        List<SecurityIssue> issues = parser.parseSecurityIssues(jsonResponse);

        assertEquals(1, issues.size());
        SecurityIssue issue = issues.get(0);
        assertNull(issue.getCweId());
        assertNull(issue.getSeverity());
        assertNull(issue.getLineNumber());
        assertNull(issue.getFixSuggestion());
        assertNull(issue.getCodeExample());
        assertNull(issue.getEffortEstimate());
    }
}
