package com.github.sonarqube.report.html;

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
 * Unit tests for HtmlReportGenerator
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 5.2)
 */
class HtmlReportGeneratorTest {

    private HtmlReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new HtmlReportGenerator();
    }

    @Test
    void testGetFormat() {
        assertThat(generator.getFormat()).isEqualTo("html");
    }

    @Test
    void testGetFileExtension() {
        assertThat(generator.getFileExtension()).isEqualTo(".html");
    }

    @Test
    void testGenerateEmptyReport() {
        // Given
        AnalysisReport report = createEmptyReport();

        // When
        String html = generator.generate(report);

        // Then
        assertThat(html).isNotNull();
        assertThat(html).contains("<!DOCTYPE html>");
        assertThat(html).contains("<html lang=\"zh-TW\">");
        assertThat(html).contains("安全分析報告");
        assertThat(html).contains("TestProject");
        assertThat(html).contains("OWASP 2021");
        assertThat(html).contains("未發現安全問題");
        assertThat(html).contains("</html>");
    }

    @Test
    void testGenerateReportWithFindings() {
        // Given
        AnalysisReport report = createReportWithFindings();

        // When
        String html = generator.generate(report);

        // Then
        assertThat(html).isNotNull();
        assertThat(html).contains("<!DOCTYPE html>");
        assertThat(html).contains("TestProject");
        assertThat(html).contains("總發現數");
        assertThat(html).contains("SQL Injection Vulnerability");
        assertThat(html).contains("XSS Attack Risk");
        assertThat(html).contains("CRITICAL");
        assertThat(html).contains("MAJOR");
        assertThat(html).contains("A03:2021-Injection");
    }

    @Test
    void testGenerateReportWithCharts() {
        // Given
        AnalysisReport report = createReportWithFindings();

        // When
        String html = generator.generate(report);

        // Then
        // 驗證包含 Chart.js 和圖表
        assertThat(html).contains("chart.js");
        assertThat(html).contains("severityChart");
        assertThat(html).contains("categoryChart");
        assertThat(html).contains("new Chart");
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
        String html = generator.generate(report);

        // Then
        assertThat(html).contains("AI 模型");
        assertThat(html).contains("GPT-4");
    }

    @Test
    void testHtmlEscaping() {
        // Given
        SecurityFinding finding = SecurityFinding.builder()
                .ruleName("Test <script>alert('XSS')</script>")
                .ruleKey("test-rule")
                .severity("MAJOR")
                .owaspCategory("A03:2021-Injection")
                .filePath("/path/to/file.java")
                .description("Test & \"quoted\" description")
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(Collections.singletonList(finding))
                .reportSummary(createSummary(1, 0, 0, 1, 0, 0))
                .build();

        // When
        String html = generator.generate(report);

        // Then
        // 驗證 HTML 特殊字元被正確轉義
        assertThat(html).contains("&lt;script&gt;");
        assertThat(html).contains("&amp;");
        assertThat(html).contains("&quot;");
        assertThat(html).doesNotContain("<script>alert");
    }

    @Test
    void testResponsiveDesign() {
        // Given
        AnalysisReport report = createEmptyReport();

        // When
        String html = generator.generate(report);

        // Then
        // 驗證響應式設計相關的 meta 和 CSS
        assertThat(html).contains("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        assertThat(html).contains("@media (max-width: 768px)");
    }

    @Test
    void testSeverityBadges() {
        // Given
        AnalysisReport report = createReportWithMultipleSeverities();

        // When
        String html = generator.generate(report);

        // Then
        assertThat(html).contains("severity-badge blocker");
        assertThat(html).contains("severity-badge critical");
        assertThat(html).contains("severity-badge major");
        assertThat(html).contains("severity-badge minor");
        assertThat(html).contains("severity-badge info");
    }

    @Test
    void testCodeSnippetFormatting() {
        // Given
        SecurityFinding finding = SecurityFinding.builder()
                .ruleName("Test Finding")
                .ruleKey("test-rule")
                .severity("MAJOR")
                .owaspCategory("A03:2021-Injection")
                .filePath("/path/to/file.java")
                .codeSnippet("String query = \"SELECT * FROM users WHERE id = \" + userId;")
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(Collections.singletonList(finding))
                .reportSummary(createSummary(1, 0, 0, 1, 0, 0))
                .build();

        // When
        String html = generator.generate(report);

        // Then
        assertThat(html).contains("code-snippet");
        assertThat(html).contains("SELECT * FROM users");
    }

    @Test
    void testFixSuggestionFormatting() {
        // Given
        SecurityFinding finding = SecurityFinding.builder()
                .ruleName("Test Finding")
                .ruleKey("test-rule")
                .severity("MAJOR")
                .owaspCategory("A03:2021-Injection")
                .filePath("/path/to/file.java")
                .fixSuggestion("使用 PreparedStatement 來防止 SQL Injection")
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(Collections.singletonList(finding))
                .reportSummary(createSummary(1, 0, 0, 1, 0, 0))
                .build();

        // When
        String html = generator.generate(report);

        // Then
        assertThat(html).contains("fix-suggestion");
        assertThat(html).contains("💡 修復建議");
        assertThat(html).contains("PreparedStatement");
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
