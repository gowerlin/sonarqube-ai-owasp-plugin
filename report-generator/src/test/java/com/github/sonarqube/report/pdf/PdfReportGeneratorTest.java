package com.github.sonarqube.report.pdf;

import com.github.sonarqube.report.ReportGenerator;
import com.github.sonarqube.report.model.AnalysisReport;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

/**
 * PDF 報表生成器單元測試
 *
 * <p>測試 PdfReportGenerator 的核心功能，確保：</p>
 * <ul>
 *   <li>正確實作 ReportGenerator 介面</li>
 *   <li>能夠生成有效的 PDF 檔案</li>
 *   <li>PDF 檔案結構符合規範（使用 Apache PDFBox 驗證）</li>
 *   <li>錯誤處理正確</li>
 * </ul>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.1)
 */
class PdfReportGeneratorTest {

    /**
     * 測試：PdfReportGenerator 正確實作 ReportGenerator 介面
     */
    @Test
    void shouldImplementReportGeneratorInterface() {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();

        // Then
        assertThat(generator).isInstanceOf(ReportGenerator.class);
    }

    /**
     * 測試：getFormat() 方法回傳 "pdf"
     */
    @Test
    void shouldReturnCorrectFormat() {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();

        // When
        String format = generator.getFormat();

        // Then
        assertThat(format).isEqualTo("pdf");
    }

    /**
     * 測試：getFileExtension() 方法回傳 ".pdf"
     */
    @Test
    void shouldReturnCorrectFileExtension() {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();

        // When
        String extension = generator.getFileExtension();

        // Then
        assertThat(extension).isEqualTo(".pdf");
    }

    /**
     * 測試：生成有效的空白 PDF 檔案
     *
     * <p>使用 Apache PDFBox 驗證 PDF 結構正確性。</p>
     */
    @Test
    void shouldGenerateValidEmptyPdf() throws Exception {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();
        AnalysisReport mockReport = createMockReport();

        // When
        String outputPath = generator.generate(mockReport);

        // Then
        assertThat(outputPath).isNotNull();

        File pdfFile = new File(outputPath);
        assertThat(pdfFile).exists();
        assertThat(pdfFile.length()).isGreaterThan(0);

        // 使用 Apache PDFBox 驗證 PDF 結構
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            assertThat(doc).isNotNull();
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            assertThat(doc.isEncrypted()).isFalse();
        }
    }

    /**
     * 測試：null 報告應拋出 IllegalArgumentException
     */
    @Test
    void shouldThrowExceptionWhenReportIsNull() {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();

        // When & Then
        assertThatThrownBy(() -> generator.generate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AnalysisReport cannot be null");
    }

    /**
     * 測試：空報告（0 個發現）應能正常生成 PDF
     *
     * <p>註：Story 1.7 將新增「No security issues found」訊息處理。</p>
     */
    @Test
    void shouldGeneratePdfWithEmptyFindings() throws Exception {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();
        AnalysisReport emptyReport = AnalysisReport.builder()
                .projectName("EmptyProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(new ArrayList<>()) // 空發現列表
                .build();

        // When
        String outputPath = generator.generate(emptyReport);

        // Then
        assertThat(outputPath).isNotNull();

        File pdfFile = new File(outputPath);
        assertThat(pdfFile).exists();

        try (PDDocument doc = PDDocument.load(pdfFile)) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);
        }
    }

    /**
     * 測試：PdfReportConfig Builder Pattern
     */
    @Test
    void shouldBuildConfigWithDefaultValues() {
        // When
        PdfReportConfig config = PdfReportConfig.builder().build();

        // Then
        assertThat(config.getReportTitle()).isEqualTo("OWASP Security Analysis Report");
        assertThat(config.getColorTheme()).isEqualTo(PdfReportConfig.ColorTheme.DEFAULT);
        assertThat(config.isHeaderFooterEnabled()).isTrue();
        assertThat(config.getLogoPath()).isNull();
    }

    /**
     * 測試：PdfReportConfig 自訂配置
     */
    @Test
    void shouldBuildConfigWithCustomValues() {
        // When
        PdfReportConfig config = PdfReportConfig.builder()
                .logoPath("/path/to/logo.png")
                .reportTitle("Custom Security Report")
                .colorTheme(PdfReportConfig.ColorTheme.DARK)
                .headerFooterEnabled(false)
                .build();

        // Then
        assertThat(config.getLogoPath()).isEqualTo("/path/to/logo.png");
        assertThat(config.getReportTitle()).isEqualTo("Custom Security Report");
        assertThat(config.getColorTheme()).isEqualTo(PdfReportConfig.ColorTheme.DARK);
        assertThat(config.isHeaderFooterEnabled()).isFalse();
    }

    /**
     * 測試：報表標題超過 100 字元應拋出例外
     */
    @Test
    void shouldThrowExceptionWhenTitleTooLong() {
        // Given
        String longTitle = "A".repeat(101); // 101 個字元

        // When & Then
        assertThatThrownBy(() ->
                PdfReportConfig.builder()
                        .reportTitle(longTitle)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not exceed 100 characters");
    }

    /**
     * 建立模擬的 AnalysisReport 物件用於測試
     *
     * @return 簡單的模擬報告（將在後續 Stories 擴展）
     */
    private AnalysisReport createMockReport() {
        return AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(new ArrayList<>())
                .build();
    }
}
