package com.github.sonarqube.plugin;

import com.github.sonarqube.ai.model.SecurityIssue;
import com.github.sonarqube.rules.RuleDefinition;
import com.github.sonarqube.rules.OwaspCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OwaspSensor 單元測試
 *
 * Story 10.3: 驗證 AI → RuleViolation → SonarQube Issue 的資料流
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0 (Epic 10, Story 10.3)
 */
class OwaspSensorTest {

    private OwaspSensor sensor;
    private Method buildEnhancedMessageMethod;
    private Method buildLegacyIssueMessageMethod;
    private Method buildIssueMessageMethod;
    private Method truncateMethod;

    @BeforeEach
    void setUp() throws Exception {
        sensor = new OwaspSensor();

        // 使用反射訪問私有方法
        buildEnhancedMessageMethod = OwaspSensor.class.getDeclaredMethod(
            "buildEnhancedMessage", SecurityIssue.class, RuleDefinition.class);
        buildEnhancedMessageMethod.setAccessible(true);

        buildLegacyIssueMessageMethod = OwaspSensor.class.getDeclaredMethod(
            "buildLegacyIssueMessage", SecurityIssue.class, RuleDefinition.class);
        buildLegacyIssueMessageMethod.setAccessible(true);

        buildIssueMessageMethod = OwaspSensor.class.getDeclaredMethod(
            "buildIssueMessage", SecurityIssue.class, RuleDefinition.class);
        buildIssueMessageMethod.setAccessible(true);

        truncateMethod = OwaspSensor.class.getDeclaredMethod(
            "truncate", String.class, int.class);
        truncateMethod.setAccessible(true);
    }

    // ========== Story 10.3: 基本資訊流測試 ==========

    @Test
    void testBuildEnhancedMessage_WithAiData_UsesFullFormat() throws Exception {
        // Given: SecurityIssue with AI enhancement data
        SecurityIssue issue = createSecurityIssueWithAiData();
        RuleDefinition rule = createMockRule();

        // When: Build enhanced message
        String message = (String) buildEnhancedMessageMethod.invoke(sensor, issue, rule);

        // Then: Should use full format with all AI information
        assertThat(message)
            .as("Message should contain rule name")
            .contains("SQL Injection Vulnerability");

        assertThat(message)
            .as("Message should contain description")
            .contains("User input not sanitized");

        assertThat(message)
            .as("Message should contain fix suggestion")
            .contains("Use PreparedStatement");

        assertThat(message)
            .as("Message should contain code example (before)")
            .contains("程式碼範例（修復前）")
            .contains("String query = \"SELECT * FROM users");

        assertThat(message)
            .as("Message should contain code example (after)")
            .contains("程式碼範例（修復後）")
            .contains("PreparedStatement stmt = conn.prepareStatement");

        assertThat(message)
            .as("Message should contain effort estimate")
            .contains("工作量評估")
            .contains("30 minutes");
    }

    @Test
    void testBuildEnhancedMessage_WithoutAiData_UsesSimpleFormat() throws Exception {
        // Given: SecurityIssue without AI enhancement data
        SecurityIssue issue = createSecurityIssueWithoutAiData();
        RuleDefinition rule = createMockRule();

        // When: Build enhanced message
        String message = (String) buildEnhancedMessageMethod.invoke(sensor, issue, rule);

        // Then: Should use simple format without AI details
        assertThat(message)
            .as("Message should contain rule name")
            .contains("SQL Injection Vulnerability");

        assertThat(message)
            .as("Message should contain description")
            .contains("User input not sanitized");

        assertThat(message)
            .as("Message should NOT contain code example markers")
            .doesNotContain("程式碼範例");

        assertThat(message)
            .as("Message should NOT contain effort estimate markers")
            .doesNotContain("工作量評估");
    }

    @Test
    void testBuildLegacyIssueMessage_IncludesAllAiInformation() throws Exception {
        // Given: SecurityIssue with complete AI data
        SecurityIssue issue = createSecurityIssueWithAiData();
        RuleDefinition rule = createMockRule();

        // When: Build legacy issue message
        String message = (String) buildLegacyIssueMessageMethod.invoke(sensor, issue, rule);

        // Then: Should include all AI information
        assertThat(message)
            .as("Should contain all AI data: rule, description, fix, code example, effort")
            .contains("SQL Injection Vulnerability")
            .contains("User input not sanitized")
            .contains("建議: Use PreparedStatement")
            .contains("程式碼範例（修復前）")
            .contains("String query = \"SELECT * FROM users")
            .contains("程式碼範例（修復後）")
            .contains("PreparedStatement stmt = conn.prepareStatement")
            .contains("工作量評估: 30 minutes");
    }

    @Test
    void testBuildIssueMessage_SimpleConciseFormat() throws Exception {
        // Given: SecurityIssue with basic data
        SecurityIssue issue = createSecurityIssueWithoutAiData();
        RuleDefinition rule = createMockRule();

        // When: Build simple issue message
        String message = (String) buildIssueMessageMethod.invoke(sensor, issue, rule);

        // Then: Should be concise format
        assertThat(message)
            .as("Should contain rule name and description")
            .contains("SQL Injection Vulnerability")
            .contains("User input not sanitized");

        assertThat(message)
            .as("Simple format should still include fix suggestion if present")
            .contains("建議: Use PreparedStatement");
    }

    @Test
    void testTruncate_ShortText_ReturnsOriginal() throws Exception {
        // Given: Short text
        String text = "Short text";

        // When: Truncate with large limit
        String result = (String) truncateMethod.invoke(sensor, text, 1000);

        // Then: Should return original
        assertThat(result)
            .as("Short text should not be truncated")
            .isEqualTo(text);
    }

    @Test
    void testTruncate_LongText_TruncatesWithEllipsis() throws Exception {
        // Given: Long text exceeding limit
        String text = "a".repeat(1500);

        // When: Truncate to 1000 chars
        String result = (String) truncateMethod.invoke(sensor, text, 1000);

        // Then: Should truncate and add ellipsis
        assertThat(result)
            .as("Long text should be truncated")
            .hasSize(1000)
            .endsWith("...");

        assertThat(result.substring(0, 997))
            .as("Should preserve first 997 characters")
            .isEqualTo("a".repeat(997));
    }

    @Test
    void testTruncate_NullText_ReturnsNull() throws Exception {
        // When: Truncate null
        String result = (String) truncateMethod.invoke(sensor, null, 1000);

        // Then: Should return null
        assertThat(result)
            .as("Null text should return null")
            .isNull();
    }

    @Test
    void testDataFlow_SecurityIssueToMessage_PreservesAiData() throws Exception {
        // Given: SecurityIssue with complete AI data (simulating AI response)
        SecurityIssue issue = new SecurityIssue();
        issue.setOwaspCategory("A03:2021");
        issue.setCweId("CWE-89");
        issue.setSeverity(SecurityIssue.Severity.HIGH);
        issue.setDescription("SQL Injection in user query");
        issue.setLineNumber(45);
        issue.setFixSuggestion("Use parameterized queries");

        // AI-generated code example
        SecurityIssue.CodeExample aiCodeExample = new SecurityIssue.CodeExample(
            "query = \"SELECT * FROM users WHERE id = \" + userId",
            "query = \"SELECT * FROM users WHERE id = ?\"\nps.setInt(1, userId)"
        );
        issue.setCodeExample(aiCodeExample);

        // AI-generated effort estimate
        issue.setEffortEstimate("15-20 minutes");

        RuleDefinition rule = createMockRule();

        // When: Convert to message format
        String message = (String) buildEnhancedMessageMethod.invoke(sensor, issue, rule);

        // Then: All AI data should be preserved in message
        assertThat(message)
            .as("Data flow: AI CodeExample → Message")
            .contains("query = \"SELECT * FROM users WHERE id = \" + userId")
            .contains("query = \"SELECT * FROM users WHERE id = ?\"")
            .contains("ps.setInt(1, userId)");

        assertThat(message)
            .as("Data flow: AI EffortEstimate → Message")
            .contains("工作量評估")
            .contains("15-20 minutes");

        assertThat(message)
            .as("Data flow: AI FixSuggestion → Message")
            .contains("Use parameterized queries");
    }

    @Test
    void testDataFlow_WithOnlyCodeExample_TriggersFullFormat() throws Exception {
        // Given: SecurityIssue with only code example (no effort estimate)
        SecurityIssue issue = createSecurityIssueWithoutAiData();
        SecurityIssue.CodeExample codeExample = new SecurityIssue.CodeExample(
            "String password = getPassword();",
            "String password = hashPassword(getPassword());"
        );
        issue.setCodeExample(codeExample);

        RuleDefinition rule = createMockRule();

        // When: Build message
        String message = (String) buildEnhancedMessageMethod.invoke(sensor, issue, rule);

        // Then: Should use full format (because codeExample exists)
        assertThat(message)
            .as("Code example alone should trigger full format")
            .contains("程式碼範例（修復前）")
            .contains("String password = getPassword()")
            .contains("程式碼範例（修復後）")
            .contains("String password = hashPassword");
    }

    @Test
    void testDataFlow_WithOnlyEffortEstimate_TriggersFullFormat() throws Exception {
        // Given: SecurityIssue with only effort estimate (no code example)
        SecurityIssue issue = createSecurityIssueWithoutAiData();
        issue.setEffortEstimate("10 minutes");

        RuleDefinition rule = createMockRule();

        // When: Build message
        String message = (String) buildEnhancedMessageMethod.invoke(sensor, issue, rule);

        // Then: Should use full format (because effortEstimate exists)
        assertThat(message)
            .as("Effort estimate alone should trigger full format")
            .contains("工作量評估")
            .contains("10 minutes");
    }

    // ========== Helper Methods ==========

    private SecurityIssue createSecurityIssueWithAiData() {
        SecurityIssue issue = new SecurityIssue();
        issue.setOwaspCategory("A03:2021");
        issue.setCweId("CWE-89");
        issue.setSeverity(SecurityIssue.Severity.HIGH);
        issue.setDescription("User input not sanitized");
        issue.setLineNumber(42);
        issue.setFixSuggestion("Use PreparedStatement");

        SecurityIssue.CodeExample codeExample = new SecurityIssue.CodeExample(
            "String query = \"SELECT * FROM users WHERE id = \" + userId;",
            "PreparedStatement stmt = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\");\nstmt.setInt(1, userId);"
        );
        issue.setCodeExample(codeExample);
        issue.setEffortEstimate("30 minutes");

        return issue;
    }

    private SecurityIssue createSecurityIssueWithoutAiData() {
        SecurityIssue issue = new SecurityIssue();
        issue.setOwaspCategory("A03:2021");
        issue.setCweId("CWE-89");
        issue.setSeverity(SecurityIssue.Severity.HIGH);
        issue.setDescription("User input not sanitized");
        issue.setLineNumber(42);
        issue.setFixSuggestion("Use PreparedStatement");
        // No code example or effort estimate

        return issue;
    }

    private RuleDefinition createMockRule() {
        return new RuleDefinition(
            "sql-injection",
            "SQL Injection Vulnerability",
            "Prevents SQL injection attacks",
            RuleDefinition.RuleSeverity.CRITICAL,
            RuleDefinition.RuleType.VULNERABILITY,
            OwaspCategory.A03_2021_INJECTION,
            java.util.List.of("CWE-89")
        );
    }
}
