package com.github.sonarqube.report.markdown;

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
 * Unit tests for MarkdownReportGenerator
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 5.7)
 */
class MarkdownReportGeneratorTest {

    private MarkdownReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new MarkdownReportGenerator();
    }

    @Test
    void testGetFormat() {
        assertThat(generator.getFormat()).isEqualTo("markdown");
    }

    @Test
    void testGetFileExtension() {
        assertThat(generator.getFileExtension()).isEqualTo(".md");
    }

    @Test
    void testGenerateEmptyReport() {
        // Given
        AnalysisReport report = createEmptyReport();

        // When
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).isNotNull();
        assertThat(markdown).contains("# 安全分析報告");
        assertThat(markdown).contains("## 專案資訊");
        assertThat(markdown).contains("TestProject");
        assertThat(markdown).contains("OWASP 版本");
        assertThat(markdown).contains("2021");
        assertThat(markdown).contains("## 📊 執行摘要");
        assertThat(markdown).contains("總發現數");
        assertThat(markdown).contains("| 0 |");
    }

    @Test
    void testGenerateReportWithFindings() {
        // Given
        AnalysisReport report = createReportWithFindings();

        // When
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).contains("# 安全分析報告");
        assertThat(markdown).contains("TestProject");
        assertThat(markdown).contains("總發現數");
        assertThat(markdown).contains("| 2 |");
        assertThat(markdown).contains("SQL Injection Vulnerability");
        assertThat(markdown).contains("XSS Attack Risk");
        assertThat(markdown).contains("CRITICAL");
        assertThat(markdown).contains("MAJOR");
        assertThat(markdown).contains("A03:2021-Injection");
    }

    @Test
    void testHeaderSection() {
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
        String markdown = generator.generate(report);

        // Then
        // 驗證標題區塊包含所有必要資訊
        assertThat(markdown).contains("# 安全分析報告");
        assertThat(markdown).contains("## 專案資訊");
        assertThat(markdown).contains("| 項目 | 值 |");
        assertThat(markdown).contains("|------|----");
        assertThat(markdown).contains("| **專案名稱** | TestProject |");
        assertThat(markdown).contains("| **OWASP 版本** | 2021 |");
        assertThat(markdown).contains("| **分析時間** |");
        assertThat(markdown).contains("| **AI 模型** | GPT-4 |");
        assertThat(markdown).contains("---");
    }

    @Test
    void testSummarySection() {
        // Given
        AnalysisReport report = createReportWithMultipleSeverities();

        // When
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).contains("## 📊 執行摘要");
        assertThat(markdown).contains("| **總發現數** | 5 |");
        assertThat(markdown).contains("| **🚨 阻斷性 (BLOCKER)** | 1 |");
        assertThat(markdown).contains("| **🔴 嚴重 (CRITICAL)** | 1 |");
        assertThat(markdown).contains("| **🟠 主要 (MAJOR)** | 1 |");
    }

    @Test
    void testSeverityBreakdown() {
        // Given
        AnalysisReport report = createReportWithMultipleSeverities();

        // When
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).contains("## 🎯 嚴重性分布");
        assertThat(markdown).contains("- 🚨 **BLOCKER**: 1 個問題");
        assertThat(markdown).contains("- 🔴 **CRITICAL**: 1 個問題");
        assertThat(markdown).contains("- 🟠 **MAJOR**: 1 個問題");
        assertThat(markdown).contains("- 🟡 **MINOR**: 1 個問題");
        assertThat(markdown).contains("- ℹ️ **INFO**: 1 個問題");
    }

    @Test
    void testCategoryBreakdown() {
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
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).contains("## 🏷️ OWASP 分類分布");
        assertThat(markdown).contains("A03:2021-Injection");
        assertThat(markdown).contains("A01:2021-Broken Access Control");
        assertThat(markdown).contains("A02:2021-Cryptographic Failures");

        // 驗證按數量降序排列（5 > 3 > 2）
        int injectionIndex = markdown.indexOf("A03:2021-Injection");
        int accessControlIndex = markdown.indexOf("A01:2021-Broken Access Control");
        int cryptoIndex = markdown.indexOf("A02:2021-Cryptographic Failures");

        assertThat(injectionIndex).isLessThan(accessControlIndex);
        assertThat(accessControlIndex).isLessThan(cryptoIndex);
    }

    @Test
    void testFindingsSection() {
        // Given
        AnalysisReport report = createReportWithFindings();

        // When
        String markdown = generator.generate(report);

        // Then
        // 驗證詳細發現區塊存在
        assertThat(markdown).contains("## 🔍 詳細發現");

        // 驗證依嚴重性分組標題
        assertThat(markdown).contains("### 🔴 CRITICAL (1 個)");
        assertThat(markdown).contains("### 🟠 MAJOR (1 個)");

        // 驗證發現項目標題
        assertThat(markdown).contains("#### 1. SQL Injection Vulnerability");
        assertThat(markdown).contains("#### 1. XSS Attack Risk");

        // 驗證基本資訊
        assertThat(markdown).contains("**位置**: `/src/main/java/com/example/UserController.java:45`");
        assertThat(markdown).contains("**規則**: `java:S3649`");
        assertThat(markdown).contains("**OWASP 分類**: A03:2021-Injection");
        assertThat(markdown).contains("**CWE**: CWE-89");

        // 驗證代碼區塊
        assertThat(markdown).contains("```java");
        assertThat(markdown).contains("SELECT * FROM users WHERE id =");
        assertThat(markdown).contains("```");

        // 驗證修復建議
        assertThat(markdown).contains("**💡 修復建議**:");
        assertThat(markdown).contains("使用 PreparedStatement 來防止 SQL Injection");
    }

    @Test
    void testCodeBlockFormatting() {
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
        String markdown = generator.generate(report);

        // Then
        // 驗證代碼區塊正確格式化
        assertThat(markdown).contains("**代碼片段**:");
        assertThat(markdown).contains("```java\n");
        assertThat(markdown).contains("String query = \"SELECT * FROM users WHERE id = \" + userId;\n");
        assertThat(markdown).contains("```");
    }

    @Test
    void testFooterSection() {
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
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).contains("## 📝 備註");
        assertThat(markdown).contains("此報告由 SonarQube AI OWASP Plugin 自動生成");
        assertThat(markdown).contains("- 分析時間:");
        assertThat(markdown).contains("- OWASP 版本: 2021");
        assertThat(markdown).contains("- AI 模型: GPT-4");
    }

    @Test
    void testMarkdownStructureValidity() {
        // Given
        AnalysisReport report = createReportWithFindings();

        // When
        String markdown = generator.generate(report);

        // Then
        // 驗證 Markdown 結構完整性

        // 標題層級正確
        assertThat(markdown).contains("# 安全分析報告\n");
        assertThat(markdown).contains("## 專案資訊\n");
        assertThat(markdown).contains("## 📊 執行摘要\n");
        assertThat(markdown).contains("## 🎯 嚴重性分布\n");
        assertThat(markdown).contains("## 🏷️ OWASP 分類分布\n");
        assertThat(markdown).contains("## 🔍 詳細發現\n");
        assertThat(markdown).contains("## 📝 備註\n");

        // 表格格式正確
        assertThat(markdown).contains("| 項目 | 值 |");
        assertThat(markdown).contains("|------|----");

        // 清單格式正確
        assertThat(markdown).contains("- 🔴 **CRITICAL**:");
        assertThat(markdown).contains("- 🟠 **MAJOR**:");

        // 分隔線正確
        int separatorCount = markdown.split("---").length - 1;
        assertThat(separatorCount).isGreaterThanOrEqualTo(3); // 至少有 3 個分隔線
    }

    @Test
    void testEmptySeverityBreakdown() {
        // Given
        AnalysisReport report = createEmptyReport();

        // When
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).contains("## 🎯 嚴重性分布");
        assertThat(markdown).contains("*無安全發現*");
    }

    @Test
    void testEmptyCategoryBreakdown() {
        // Given
        ReportSummary summary = ReportSummary.builder()
                .totalFindings(0)
                .categoryCounts(new HashMap<>())
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
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).contains("## 🏷️ OWASP 分類分布");
        assertThat(markdown).contains("*無分類資訊*");
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
        String markdown = generator.generate(report);

        // Then
        // 驗證可選欄位不存在時仍能正確生成
        assertThat(markdown).isNotNull();
        assertThat(markdown).contains("Test Finding");
        assertThat(markdown).contains("**位置**: `/path/to/file.java`");
        assertThat(markdown).doesNotContain("AI 模型"); // 可選欄位不應出現在標題表格
        assertThat(markdown).doesNotContain("**問題描述**:"); // 無描述時不顯示
        assertThat(markdown).doesNotContain("**代碼片段**:"); // 無代碼片段時不顯示
        assertThat(markdown).doesNotContain("**💡 修復建議**:"); // 無修復建議時不顯示
    }

    @Test
    void testCweIdsFormatting() {
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
        String markdown = generator.generate(report);

        // Then
        // 驗證 CWE IDs 以逗號分隔格式顯示
        assertThat(markdown).contains("**CWE**: CWE-89, CWE-564, CWE-943");
    }

    @Test
    void testFilesAnalyzedField() {
        // Given
        ReportSummary summary = ReportSummary.builder()
                .totalFindings(5)
                .filesAnalyzed(42)
                .blockerCount(0)
                .criticalCount(1)
                .majorCount(2)
                .minorCount(1)
                .infoCount(1)
                .severityCounts(new HashMap<>())
                .categoryCounts(new HashMap<>())
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(Collections.emptyList())
                .reportSummary(summary)
                .build();

        // When
        String markdown = generator.generate(report);

        // Then
        assertThat(markdown).contains("| **分析檔案數** | 42 |");
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
