package com.github.sonarqube.report.pdf;

import com.github.sonarqube.report.model.AnalysisReport;
import com.github.sonarqube.report.model.ReportSummary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

/**
 * PdfExecutiveSummary 單元測試
 *
 * <p>測試執行摘要章節的統計表格、顏色標籤、自動文字生成。</p>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.3)
 */
class PdfExecutiveSummaryTest {

    @TempDir
    Path tempDir;

    /**
     * 測試：執行摘要包含統計表格和章節標題
     */
    @Test
    void shouldCreateExecutiveSummaryWithStatisticsTable() throws Exception {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();

        ReportSummary summary = ReportSummary.builder()
                .totalFindings(25)
                .blockerCount(3)
                .criticalCount(7)
                .majorCount(10)
                .minorCount(5)
                .infoCount(0)
                .analyzedFilesCount(50)
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(new ArrayList<>())
                .reportSummary(summary)
                .build();

        // When
        String outputPath = generator.generate(report);

        // Then
        assertThat(outputPath).isNotNull();
        File pdfFile = new File(outputPath);
        assertThat(pdfFile).exists();

        try (PDDocument doc = PDDocument.load(pdfFile)) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(3); // Cover + TOC + Summary

            // 驗證執行摘要包含統計數字
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            assertThat(text).contains("Executive Summary");
            assertThat(text).contains("Total Findings");
            assertThat(text).contains("25"); // Total findings count
            assertThat(text).contains("BLOCKER Issues");
            assertThat(text).contains("3"); // Blocker count
            assertThat(text).contains("CRITICAL Issues");
            assertThat(text).contains("7"); // Critical count
            assertThat(text).contains("MAJOR Issues");
            assertThat(text).contains("10"); // Major count
            assertThat(text).contains("Analyzed Files");
            assertThat(text).contains("50"); // Analyzed files count
        }
    }

    /**
     * 測試：無發現時顯示正確的摘要文字
     */
    @Test
    void shouldGenerateCorrectSummaryTextForZeroFindings() throws Exception {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();

        ReportSummary summary = ReportSummary.builder()
                .totalFindings(0)
                .blockerCount(0)
                .criticalCount(0)
                .majorCount(0)
                .minorCount(0)
                .infoCount(0)
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

            assertThat(text).contains("No security issues were detected");
            assertThat(text).contains("OWASP security standards");
        }
    }

    /**
     * 測試：少量發現（1-10個）時顯示正確的摘要文字
     */
    @Test
    void shouldGenerateCorrectSummaryTextForFewFindings() throws Exception {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();

        ReportSummary summary = ReportSummary.builder()
                .totalFindings(5)
                .blockerCount(1)
                .criticalCount(2)
                .majorCount(2)
                .minorCount(0)
                .infoCount(0)
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("SmallProject")
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

            assertThat(text).contains("5 security findings");
            assertThat(text).contains("3 of which require immediate attention"); // 1 BLOCKER + 2 CRITICAL
            assertThat(text).contains("BLOCKER and CRITICAL issues as a priority");
        }
    }

    /**
     * 測試：大量發現（>10個）時顯示正確的摘要文字
     */
    @Test
    void shouldGenerateCorrectSummaryTextForManyFindings() throws Exception {
        // Given
        PdfReportGenerator generator = new PdfReportGenerator();

        ReportSummary summary = ReportSummary.builder()
                .totalFindings(50)
                .blockerCount(5)
                .criticalCount(15)
                .majorCount(20)
                .minorCount(10)
                .infoCount(0)
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("LargeProject")
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

            assertThat(text).contains("50 security findings");
            assertThat(text).contains("20 critical issues"); // 5 BLOCKER + 15 CRITICAL
            assertThat(text).contains("prioritizing the BLOCKER and CRITICAL issues first");
            assertThat(text).contains("detailed findings section");
        }
    }

    /**
     * 測試：PdfStyleConstants 包含新的嚴重性顏色常數
     */
    @Test
    void shouldHaveSeverityColorConstants() {
        assertThat(PdfStyleConstants.SEVERITY_BLOCKER_COLOR).isNotNull();
        assertThat(PdfStyleConstants.SEVERITY_CRITICAL_COLOR).isNotNull();
        assertThat(PdfStyleConstants.SEVERITY_MAJOR_COLOR).isNotNull();
        assertThat(PdfStyleConstants.SEVERITY_MINOR_COLOR).isNotNull();
        assertThat(PdfStyleConstants.SEVERITY_INFO_COLOR).isNotNull();
    }

    /**
     * 測試：PdfStyleConstants 包含新的表格樣式常數
     */
    @Test
    void shouldHaveTableStyleConstants() {
        assertThat(PdfStyleConstants.TABLE_HEADER_BACKGROUND).isNotNull();
        assertThat(PdfStyleConstants.TABLE_HEADER_TEXT_COLOR).isNotNull();
        assertThat(PdfStyleConstants.TABLE_DATA_BACKGROUND_LIGHT).isNotNull();
        assertThat(PdfStyleConstants.TABLE_DATA_BACKGROUND_DARK).isNotNull();
        assertThat(PdfStyleConstants.TABLE_BORDER_COLOR).isNotNull();
        assertThat(PdfStyleConstants.TABLE_CELL_PADDING).isEqualTo(10f);
    }
}
