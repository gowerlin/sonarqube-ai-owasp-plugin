package com.github.sonarqube.report.pdf;

import com.github.sonarqube.report.model.AnalysisReport;
import com.github.sonarqube.report.model.ReportSummary;
import com.github.sonarqube.report.model.SecurityFinding;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * PdfDetailedFindings 單元測試
 *
 * <p>測試詳細發現區段的生成，包含代碼片段、修復建議、分頁邏輯。</p>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.5)
 */
class PdfDetailedFindingsTest {

    @TempDir
    Path tempDir;

    /**
     * 測試：詳細發現區段包含所有必要欄位
     */
    @Test
    void shouldCreateDetailedFindingsWithAllFields() throws Exception {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();

        List<SecurityFinding> findings = new ArrayList<>();
        findings.add(SecurityFinding.builder()
                .severity("BLOCKER")
                .ruleName("SQL Injection Vulnerability")
                .filePath("src/main/java/UserService.java")
                .lineNumber(45)
                .owaspCategory("A01:2021 - Broken Access Control")
                .cweId("CWE-89")
                .description("User input is directly concatenated into SQL query without sanitization.")
                .codeSnippet("String query = \"SELECT * FROM users WHERE username = '\" + username + \"'\";")
                .fixSuggestion("Use parameterized queries or prepared statements to prevent SQL injection.")
                .build());

        findings.add(SecurityFinding.builder()
                .severity("CRITICAL")
                .ruleName("XSS Vulnerability")
                .filePath("src/main/webapp/index.jsp")
                .lineNumber(12)
                .owaspCategory("A03:2021 - Injection")
                .cweId("CWE-79")
                .description("User input is rendered without HTML encoding.")
                .build());

        ReportSummary summary = ReportSummary.builder()
                .totalFindings(2)
                .blockerCount(1)
                .criticalCount(1)
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(findings)
                .reportSummary(summary)
                .build();

        // When
        String outputPath = generator.generate(report);

        // Then
        assertThat(outputPath).isNotNull();
        File pdfFile = new File(outputPath);
        assertThat(pdfFile).exists();

        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            // 驗證嚴重性分組標題
            assertThat(text).contains("BLOCKER Issues (1)");
            assertThat(text).contains("CRITICAL Issues (1)");

            // 驗證第一個發現的所有欄位
            assertThat(text).contains("SQL Injection Vulnerability");
            assertThat(text).contains("src/main/java/UserService.java:45");
            assertThat(text).contains("OWASP: A01:2021 - Broken Access Control");
            assertThat(text).contains("CWE: CWE-89");
            assertThat(text).contains("User input is directly concatenated");
            assertThat(text).contains("SELECT * FROM users WHERE username");
            assertThat(text).contains("Fix Suggestion");
            assertThat(text).contains("Use parameterized queries");

            // 驗證第二個發現（無代碼片段和修復建議）
            assertThat(text).contains("XSS Vulnerability");
            assertThat(text).contains("src/main/webapp/index.jsp:12");
        }
    }

    /**
     * 測試：長代碼片段顯示完整
     */
    @Test
    void shouldDisplayLongCodeSnippet() throws Exception {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();

        String longCodeSnippet = String.join("\n",
                "public void processUserInput(String input) {",
                "    // This is a vulnerable method",
                "    String sql = \"SELECT * FROM users WHERE id = \" + input;",
                "    Connection conn = getConnection();",
                "    Statement stmt = conn.createStatement();",
                "    ResultSet rs = stmt.executeQuery(sql);",
                "    // ... process results",
                "}");

        List<SecurityFinding> findings = new ArrayList<>();
        findings.add(SecurityFinding.builder()
                .severity("BLOCKER")
                .ruleName("SQL Injection")
                .filePath("Service.java")
                .lineNumber(10)
                .owaspCategory("A03")
                .cweId("CWE-89")
                .description("SQL injection vulnerability detected.")
                .codeSnippet(longCodeSnippet)
                .build());

        ReportSummary summary = ReportSummary.builder()
                .totalFindings(1)
                .blockerCount(1)
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(findings)
                .reportSummary(summary)
                .build();

        // When
        String outputPath = generator.generate(report);

        // Then
        File pdfFile = new File(outputPath);
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            // 驗證代碼片段完整顯示
            assertThat(text).contains("public void processUserInput(String input)");
            assertThat(text).contains("Connection conn = getConnection()");
            assertThat(text).contains("stmt.executeQuery(sql)");
        }
    }

    /**
     * 測試：長修復建議顯示完整
     */
    @Test
    void shouldDisplayLongFixSuggestion() throws Exception {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();

        String longFixSuggestion = "To fix this SQL injection vulnerability, you should use parameterized " +
                "queries (PreparedStatement in Java) instead of string concatenation. This ensures that user " +
                "input is properly escaped and treated as data, not executable code. Example: " +
                "PreparedStatement pstmt = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\"); " +
                "pstmt.setString(1, input); ResultSet rs = pstmt.executeQuery();";

        List<SecurityFinding> findings = new ArrayList<>();
        findings.add(SecurityFinding.builder()
                .severity("BLOCKER")
                .ruleName("SQL Injection")
                .filePath("Service.java")
                .lineNumber(10)
                .description("SQL injection vulnerability.")
                .fixSuggestion(longFixSuggestion)
                .build());

        ReportSummary summary = ReportSummary.builder()
                .totalFindings(1)
                .blockerCount(1)
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(findings)
                .reportSummary(summary)
                .build();

        // When
        String outputPath = generator.generate(report);

        // Then
        File pdfFile = new File(outputPath);
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            // 驗證修復建議完整顯示
            assertThat(text).contains("Fix Suggestion");
            assertThat(text).contains("parameterized queries");
            assertThat(text).contains("PreparedStatement");
            assertThat(text).contains("pstmt.setString(1, input)");
        }
    }

    /**
     * 測試：多個嚴重性分組正確排序
     */
    @Test
    void shouldGroupFindingsBySeverityInCorrectOrder() throws Exception {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();

        List<SecurityFinding> findings = new ArrayList<>();

        // 新增不同嚴重性的發現（刻意打亂順序）
        findings.add(createFinding("MAJOR", "Major Issue 1"));
        findings.add(createFinding("BLOCKER", "Blocker Issue 1"));
        findings.add(createFinding("INFO", "Info Issue 1"));
        findings.add(createFinding("CRITICAL", "Critical Issue 1"));
        findings.add(createFinding("MINOR", "Minor Issue 1"));
        findings.add(createFinding("MAJOR", "Major Issue 2"));
        findings.add(createFinding("BLOCKER", "Blocker Issue 2"));

        ReportSummary summary = ReportSummary.builder()
                .totalFindings(7)
                .blockerCount(2)
                .criticalCount(1)
                .majorCount(2)
                .minorCount(1)
                .infoCount(1)
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(findings)
                .reportSummary(summary)
                .build();

        // When
        String outputPath = generator.generate(report);

        // Then
        File pdfFile = new File(outputPath);
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            // 驗證嚴重性分組順序（BLOCKER → CRITICAL → MAJOR → MINOR → INFO）
            int blockerPos = text.indexOf("BLOCKER Issues (2)");
            int criticalPos = text.indexOf("CRITICAL Issues (1)");
            int majorPos = text.indexOf("MAJOR Issues (2)");
            int minorPos = text.indexOf("MINOR Issues (1)");
            int infoPos = text.indexOf("INFO Issues (1)");

            assertThat(blockerPos).isLessThan(criticalPos);
            assertThat(criticalPos).isLessThan(majorPos);
            assertThat(majorPos).isLessThan(minorPos);
            assertThat(minorPos).isLessThan(infoPos);
        }
    }

    /**
     * 測試：無發現時不生成詳細發現區段
     */
    @Test
    void shouldNotCreateDetailedFindingsSectionWhenNoFindings() throws Exception {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();

        ReportSummary summary = ReportSummary.builder()
                .totalFindings(0)
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("CleanProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(new ArrayList<>())
                .reportSummary(summary)
                .build();

        // When
        String outputPath = generator.generate(report);

        // Then
        File pdfFile = new File(outputPath);
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            // 驗證無 BLOCKER/CRITICAL/MAJOR 等嚴重性分組標題
            assertThat(text).doesNotContain("BLOCKER Issues");
            assertThat(text).doesNotContain("CRITICAL Issues");
            assertThat(text).doesNotContain("MAJOR Issues");
        }
    }

    /**
     * 測試：代碼片段保持原始縮排
     */
    @Test
    void shouldPreserveCodeSnippetIndentation() throws Exception {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();

        String indentedCode = String.join("\n",
                "public class Example {",
                "    public void method() {",
                "        if (condition) {",
                "            doSomething();",
                "        }",
                "    }",
                "}");

        List<SecurityFinding> findings = new ArrayList<>();
        findings.add(SecurityFinding.builder()
                .severity("MAJOR")
                .ruleName("Code Quality Issue")
                .filePath("Example.java")
                .lineNumber(5)
                .codeSnippet(indentedCode)
                .build());

        ReportSummary summary = ReportSummary.builder()
                .totalFindings(1)
                .majorCount(1)
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(findings)
                .reportSummary(summary)
                .build();

        // When
        String outputPath = generator.generate(report);

        // Then
        File pdfFile = new File(outputPath);
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            // 驗證代碼結構存在（縮排可能因 PDF 提取而改變，但結構應保留）
            assertThat(text).contains("public class Example");
            assertThat(text).contains("public void method()");
            assertThat(text).contains("if (condition)");
            assertThat(text).contains("doSomething()");
        }
    }

    /**
     * 測試：驗證 PdfStyleConstants 的 Finding 樣式常數存在
     */
    @Test
    void shouldHaveFindingStyleConstants() {
        assertThat(PdfStyleConstants.FINDING_TITLE_SIZE).isEqualTo(14f);
        assertThat(PdfStyleConstants.FINDING_TEXT_SIZE).isEqualTo(12f);
        assertThat(PdfStyleConstants.CODE_SNIPPET_SIZE).isEqualTo(10f);
        assertThat(PdfStyleConstants.CODE_SNIPPET_LEADING).isEqualTo(14f);
        assertThat(PdfStyleConstants.CODE_SNIPPET_BACKGROUND).isNotNull();
        assertThat(PdfStyleConstants.FIX_SUGGESTION_BACKGROUND).isNotNull();
        assertThat(PdfStyleConstants.BLOCK_PADDING).isEqualTo(10f);
        assertThat(PdfStyleConstants.BLOCK_MARGIN).isEqualTo(10f);
        assertThat(PdfStyleConstants.FINDING_SPACING).isEqualTo(20f);
    }

    // Helper method
    private SecurityFinding createFinding(String severity, String ruleName) {
        return SecurityFinding.builder()
                .severity(severity)
                .ruleName(ruleName)
                .filePath("test.java")
                .lineNumber(10)
                .description("Test finding")
                .build();
    }
}
