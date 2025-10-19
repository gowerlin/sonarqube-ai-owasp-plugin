package com.github.sonarqube.report.json;

import com.github.sonarqube.report.model.AnalysisReport;
import com.github.sonarqube.report.model.ReportSummary;
import com.github.sonarqube.report.model.SecurityFinding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JsonReportGenerator
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 5.3)
 */
class JsonReportGeneratorTest {

    private JsonReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new JsonReportGenerator();
    }

    @Test
    void testGetFormat() {
        assertThat(generator.getFormat()).isEqualTo("json");
    }

    @Test
    void testGetFileExtension() {
        assertThat(generator.getFileExtension()).isEqualTo(".json");
    }

    @Test
    void testGenerateEmptyReport() {
        // Given
        AnalysisReport report = createEmptyReport();

        // When
        String json = generator.generate(report);

        // Then
        assertThat(json).isNotNull();
        assertThat(json).contains("\"metadata\"");
        assertThat(json).contains("\"summary\"");
        assertThat(json).contains("\"findings\"");
        assertThat(json).contains("\"projectName\": \"TestProject\"");
        assertThat(json).contains("\"owaspVersion\": \"2021\"");
        assertThat(json).contains("\"totalFindings\": 0");
        assertThat(json).contains("\"findings\": []");
    }

    @Test
    void testGenerateReportWithFindings() {
        // Given
        AnalysisReport report = createReportWithFindings();

        // When
        String json = generator.generate(report);

        // Then
        assertThat(json).contains("\"projectName\": \"TestProject\"");
        assertThat(json).contains("\"totalFindings\": 2");
        assertThat(json).contains("\"SQL Injection Vulnerability\"");
        assertThat(json).contains("\"XSS Attack Risk\"");
        assertThat(json).contains("\"CRITICAL\"");
        assertThat(json).contains("\"MAJOR\"");
        assertThat(json).contains("\"A03:2021-Injection\"");
        assertThat(json).contains("\"CWE-89\"");
    }

    @Test
    void testGenerateReportWithMetadata() {
        // Given
        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .aiModel("GPT-4")
                .findings(Collections.emptyList())
                .reportSummary(ReportSummary.builder().totalFindings(0).build())
                .build();

        // When
        String json = generator.generate(report);

        // Then
        assertThat(json).contains("\"aiModel\": \"GPT-4\"");
        assertThat(json).contains("\"generatedBy\": \"SonarQube AI OWASP Plugin\"");
    }

    @Test
    void testJsonEscaping() {
        // Given
        SecurityFinding finding = SecurityFinding.builder()
                .ruleName("Test \"quoted\" rule")
                .ruleKey("test-rule")
                .severity("MAJOR")
                .owaspCategory("A03:2021-Injection")
                .filePath("/path/to/file.java")
                .description("Test \n newline \t tab \\ backslash")
                .codeSnippet("String s = \"test\";")
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(Collections.singletonList(finding))
                .reportSummary(createSummary(1, 0, 0, 1, 0, 0))
                .build();

        // When
        String json = generator.generate(report);

        // Then
        // 驗證 JSON 特殊字元被正確轉義
        assertThat(json).contains("\\\"quoted\\\"");
        assertThat(json).contains("\\n");
        assertThat(json).contains("\\t");
        assertThat(json).contains("\\\\");
        assertThat(json).doesNotContain("\"test\""); // 應該被轉義為 \"test\"
    }

    @Test
    void testValidJsonStructure() {
        // Given
        AnalysisReport report = createReportWithFindings();

        // When
        String json = generator.generate(report);

        // Then
        // 驗證 JSON 結構完整性
        assertThat(json).startsWith("{");
        assertThat(json).endsWith("}");

        // 驗證主要區塊存在
        assertThat(json).contains("\"metadata\": {");
        assertThat(json).contains("\"summary\": {");
        assertThat(json).contains("\"findings\": [");

        // 驗證逗號分隔正確
        int metadataIndex = json.indexOf("\"metadata\"");
        int summaryIndex = json.indexOf("\"summary\"");
        int findingsIndex = json.indexOf("\"findings\"");

        assertThat(summaryIndex).isGreaterThan(metadataIndex);
        assertThat(findingsIndex).isGreaterThan(summaryIndex);
    }

    @Test
    void testSummarySection() {
        // Given
        AnalysisReport report = createReportWithMultipleSeverities();

        // When
        String json = generator.generate(report);

        // Then
        assertThat(json).contains("\"totalFindings\": 5");
        assertThat(json).contains("\"blockerCount\": 1");
        assertThat(json).contains("\"criticalCount\": 1");
        assertThat(json).contains("\"majorCount\": 1");
        assertThat(json).contains("\"minorCount\": 1");
        assertThat(json).contains("\"infoCount\": 1");
        assertThat(json).contains("\"severityCounts\"");
        assertThat(json).contains("\"categoryCounts\"");
    }

    @Test
    void testFindingsArray() {
        // Given
        AnalysisReport report = createReportWithFindings();

        // When
        String json = generator.generate(report);

        // Then
        // 驗證 findings 陣列結構
        assertThat(json).contains("\"findings\": [");
        assertThat(json).contains("\"ruleName\":");
        assertThat(json).contains("\"ruleKey\":");
        assertThat(json).contains("\"severity\":");
        assertThat(json).contains("\"owaspCategory\":");
        assertThat(json).contains("\"cweIds\": [");
        assertThat(json).contains("\"filePath\":");
        assertThat(json).contains("\"lineNumber\":");
    }

    @Test
    void testOptionalFields() {
        // Given
        SecurityFinding finding = SecurityFinding.builder()
                .ruleName("Test Finding")
                .ruleKey("test-rule")
                .severity("MAJOR")
                .owaspCategory("A03:2021-Injection")
                .filePath("/path/to/file.java")
                // 不設定 lineNumber, description, codeSnippet, fixSuggestion
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                // 不設定 aiModel
                .findings(Collections.singletonList(finding))
                .reportSummary(createSummary(1, 0, 0, 1, 0, 0))
                .build();

        // When
        String json = generator.generate(report);

        // Then
        // 驗證可選欄位不存在時 JSON 仍然有效
        assertThat(json).isNotNull();
        assertThat(json).contains("\"ruleName\": \"Test Finding\"");
        assertThat(json).doesNotContain("\"aiModel\""); // 可選欄位不應出現
    }

    @Test
    void testCweIdsArray() {
        // Given
        SecurityFinding finding = SecurityFinding.builder()
                .ruleName("Test Finding")
                .ruleKey("test-rule")
                .severity("MAJOR")
                .owaspCategory("A03:2021-Injection")
                .filePath("/path/to/file.java")
                .cweIds(Arrays.asList("CWE-89", "CWE-564", "CWE-943"))
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(Collections.singletonList(finding))
                .reportSummary(createSummary(1, 0, 0, 1, 0, 0))
                .build();

        // When
        String json = generator.generate(report);

        // Then
        assertThat(json).contains("\"cweIds\": [");
        assertThat(json).contains("\"CWE-89\"");
        assertThat(json).contains("\"CWE-564\"");
        assertThat(json).contains("\"CWE-943\"");
    }

    @Test
    void testMapSorting() {
        // Given
        Map<String, Integer> categoryCounts = new HashMap<>();
        categoryCounts.put("A03:2021-Injection", 5);
        categoryCounts.put("A01:2021-Broken Access Control", 3);
        categoryCounts.put("A02:2021-Cryptographic Failures", 2);

        ReportSummary summary = ReportSummary.builder()
                .totalFindings(10)
                .categoryCounts(categoryCounts)
                .severityCounts(new HashMap<>())
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(Collections.emptyList())
                .reportSummary(summary)
                .build();

        // When
        String json = generator.generate(report);

        // Then
        // 驗證 categoryCounts 按字母排序
        int a01Index = json.indexOf("\"A01:2021-Broken Access Control\"");
        int a02Index = json.indexOf("\"A02:2021-Cryptographic Failures\"");
        int a03Index = json.indexOf("\"A03:2021-Injection\"");

        assertThat(a01Index).isLessThan(a02Index);
        assertThat(a02Index).isLessThan(a03Index);
    }

    @Test
    void testNullHandling() {
        // Given
        SecurityFinding finding = SecurityFinding.builder()
                .ruleName("Test Finding")
                .ruleKey("test-rule")
                .severity("MAJOR")
                .owaspCategory("A03:2021-Injection")
                .filePath("/path/to/file.java")
                .description(null)
                .codeSnippet(null)
                .fixSuggestion(null)
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .aiModel(null)
                .findings(Collections.singletonList(finding))
                .reportSummary(createSummary(1, 0, 0, 1, 0, 0))
                .build();

        // When
        String json = generator.generate(report);

        // Then
        // 驗證 null 值不應該破壞 JSON 結構
        assertThat(json).isNotNull();
        assertThat(json).contains("\"ruleName\": \"Test Finding\"");
    }

    // Helper methods

    private AnalysisReport createEmptyReport() {
        return AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(Collections.emptyList())
                .reportSummary(ReportSummary.builder().totalFindings(0).build())
                .build();
    }

    private AnalysisReport createReportWithFindings() {
        SecurityFinding finding1 = SecurityFinding.builder()
                .ruleName("SQL Injection Vulnerability")
                .ruleKey("java:S3649")
                .severity("CRITICAL")
                .owaspCategory("A03:2021-Injection")
                .cweIds(Arrays.asList("CWE-89"))
                .filePath("/src/main/java/com/example/UserController.java")
                .lineNumber(45)
                .description("潛在的 SQL 注入風險")
                .codeSnippet("String query = \"SELECT * FROM users WHERE id = \" + userId;")
                .fixSuggestion("使用 PreparedStatement 來防止 SQL Injection")
                .build();

        SecurityFinding finding2 = SecurityFinding.builder()
                .ruleName("XSS Attack Risk")
                .ruleKey("java:S5131")
                .severity("MAJOR")
                .owaspCategory("A03:2021-Injection")
                .cweIds(Arrays.asList("CWE-79"))
                .filePath("/src/main/java/com/example/HomeController.java")
                .lineNumber(78)
                .description("未驗證的使用者輸入可能導致 XSS 攻擊")
                .build();

        ReportSummary summary = createSummary(2, 0, 1, 1, 0, 0);

        return AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(Arrays.asList(finding1, finding2))
                .reportSummary(summary)
                .build();
    }

    private AnalysisReport createReportWithMultipleSeverities() {
        SecurityFinding blocker = createFinding("BLOCKER", "A01:2021-Broken Access Control");
        SecurityFinding critical = createFinding("CRITICAL", "A02:2021-Cryptographic Failures");
        SecurityFinding major = createFinding("MAJOR", "A03:2021-Injection");
        SecurityFinding minor = createFinding("MINOR", "A04:2021-Insecure Design");
        SecurityFinding info = createFinding("INFO", "A05:2021-Security Misconfiguration");

        ReportSummary summary = createSummary(5, 1, 1, 1, 1, 1);

        return AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(Arrays.asList(blocker, critical, major, minor, info))
                .reportSummary(summary)
                .build();
    }

    private SecurityFinding createFinding(String severity, String category) {
        return SecurityFinding.builder()
                .ruleName("Test Finding - " + severity)
                .ruleKey("test-rule-" + severity.toLowerCase())
                .severity(severity)
                .owaspCategory(category)
                .filePath("/test/file.java")
                .build();
    }

    private ReportSummary createSummary(int total, int blocker, int critical, int major, int minor, int info) {
        Map<String, Integer> severityCounts = new HashMap<>();
        if (blocker > 0) severityCounts.put("BLOCKER", blocker);
        if (critical > 0) severityCounts.put("CRITICAL", critical);
        if (major > 0) severityCounts.put("MAJOR", major);
        if (minor > 0) severityCounts.put("MINOR", minor);
        if (info > 0) severityCounts.put("INFO", info);

        Map<String, Integer> categoryCounts = new HashMap<>();
        categoryCounts.put("A03:2021-Injection", total);

        return ReportSummary.builder()
                .totalFindings(total)
                .blockerCount(blocker)
                .criticalCount(critical)
                .majorCount(major)
                .minorCount(minor)
                .infoCount(info)
                .severityCounts(severityCounts)
                .categoryCounts(categoryCounts)
                .build();
    }
}
