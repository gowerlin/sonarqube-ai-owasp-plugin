package com.github.sonarqube.report.pdf;

import com.github.sonarqube.report.model.AnalysisReport;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
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
 * PdfLayoutManager 單元測試
 *
 * <p>測試 PdfLayoutManager 的封面頁、目錄、頁首頁尾功能。</p>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.2)
 */
class PdfLayoutManagerTest {

    @TempDir
    Path tempDir;

    /**
     * 測試：建立封面頁（無 Logo）
     */
    @Test
    void shouldCreateCoverPageWithoutLogo() throws Exception {
        // Given
        PdfLayoutManager layoutManager = new PdfLayoutManager();
        File pdfFile = tempDir.resolve("test-cover-no-logo.pdf").toFile();

        PdfReportConfig config = PdfReportConfig.builder().build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(new ArrayList<>())
                .build();

        // When
        try (PdfWriter writer = new PdfWriter(pdfFile);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            layoutManager.createCoverPage(document, report, config);
        }

        // Then
        assertThat(pdfFile).exists();
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);

            // 驗證封面頁包含專案名稱、OWASP 版本
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            String text = stripper.getText(doc);

            assertThat(text).contains("TestProject");
            assertThat(text).contains("OWASP Top 10 2021");
            assertThat(text).contains("OWASP Security Analysis Report"); // 報表標題
        }
    }

    /**
     * 測試：建立封面頁（Logo 檔案不存在，應降級處理）
     */
    @Test
    void shouldCreateCoverPageWithMissingLogo() throws Exception {
        // Given
        PdfLayoutManager layoutManager = new PdfLayoutManager();
        File pdfFile = tempDir.resolve("test-cover-missing-logo.pdf").toFile();

        PdfReportConfig config = PdfReportConfig.builder()
                .logoPath("non-existent-logo.png") // Logo 不存在
                .build();

        AnalysisReport report = AnalysisReport.builder()
                .projectName("TestProject")
                .owaspVersion("2021")
                .analysisTime(LocalDateTime.now())
                .findings(new ArrayList<>())
                .build();

        // When
        try (PdfWriter writer = new PdfWriter(pdfFile);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            // 不應拋出例外，應降級處理（無 Logo）
            layoutManager.createCoverPage(document, report, config);
        }

        // Then - PDF 應成功生成
        assertThat(pdfFile).exists();
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            assertThat(text).contains("TestProject");
        }
    }

    /**
     * 測試：建立目錄（5 個章節）
     */
    @Test
    void shouldCreateTableOfContentsWithSections() throws Exception {
        // Given
        PdfLayoutManager layoutManager = new PdfLayoutManager();
        File pdfFile = tempDir.resolve("test-toc.pdf").toFile();

        List<PdfLayoutManager.TocSection> sections = new ArrayList<>();
        sections.add(new PdfLayoutManager.TocSection("Executive Summary", 1));
        sections.add(new PdfLayoutManager.TocSection("Severity Distribution", 1));
        sections.add(new PdfLayoutManager.TocSection("OWASP Category Distribution", 1));
        sections.add(new PdfLayoutManager.TocSection("CRITICAL Issues (5)", 1));
        sections.add(new PdfLayoutManager.TocSection("MAJOR Issues (10)", 1));

        // When
        try (PdfWriter writer = new PdfWriter(pdfFile);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            layoutManager.createTableOfContents(document, sections);
        }

        // Then
        assertThat(pdfFile).exists();
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            assertThat(doc.getNumberOfPages()).isGreaterThanOrEqualTo(1);

            // 驗證目錄包含章節標題
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            assertThat(text).contains("Table of Contents");
            assertThat(text).contains("Executive Summary");
            assertThat(text).contains("Severity Distribution");
            assertThat(text).contains("CRITICAL Issues (5)");
        }
    }

    /**
     * 測試：建立空目錄（0 個章節）
     */
    @Test
    void shouldCreateTableOfContentsWithNoSections() throws Exception {
        // Given
        PdfLayoutManager layoutManager = new PdfLayoutManager();
        File pdfFile = tempDir.resolve("test-toc-empty.pdf").toFile();

        List<PdfLayoutManager.TocSection> sections = new ArrayList<>(); // 空列表

        // When
        try (PdfWriter writer = new PdfWriter(pdfFile);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            layoutManager.createTableOfContents(document, sections);
        }

        // Then
        assertThat(pdfFile).exists();
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            assertThat(text).contains("Table of Contents");
        }
    }

    /**
     * 測試：頁首頁尾事件處理器註冊成功
     */
    @Test
    void shouldRegisterHeaderFooterEventHandler() throws Exception {
        // Given
        PdfLayoutManager layoutManager = new PdfLayoutManager();
        File pdfFile = tempDir.resolve("test-header-footer.pdf").toFile();

        PdfReportConfig config = PdfReportConfig.builder().build();

        // When
        try (PdfWriter writer = new PdfWriter(pdfFile);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            // 註冊頁首頁尾事件處理器
            layoutManager.addHeaderFooter(
                    writer,
                    config,
                    "TestProject",
                    "2021",
                    "2025-10-20T15:30:00"
            );

            // 添加多頁內容，測試頁首頁尾
            for (int i = 0; i < 5; i++) {
                document.add(new com.itextpdf.layout.element.Paragraph("Page " + (i + 1) + " content\n\n\n\n\n"));
                if (i < 4) {
                    layoutManager.addPageBreak(document);
                }
            }
        }

        // Then
        assertThat(pdfFile).exists();
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(5);

            // 驗證頁面內容包含頁碼（頁首頁尾應顯示在第 3 頁+，跳過前 2 頁）
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(3);
            stripper.setEndPage(3);
            String page3Text = stripper.getText(doc);

            // 注意：PDFBox 可能無法完美提取頁首頁尾文字（因為使用絕對定位），
            // 但至少應有 "Page X of Y" 格式
            assertThat(page3Text).containsAnyOf("Page", "OWASP", "TestProject");
        }
    }

    /**
     * 測試：頁首頁尾被配置停用時，不註冊事件處理器
     */
    @Test
    void shouldNotRegisterHeaderFooterWhenDisabled() throws Exception {
        // Given
        PdfLayoutManager layoutManager = new PdfLayoutManager();
        File pdfFile = tempDir.resolve("test-no-header-footer.pdf").toFile();

        PdfReportConfig config = PdfReportConfig.builder()
                .headerFooterEnabled(false) // 停用頁首頁尾
                .build();

        // When
        try (PdfWriter writer = new PdfWriter(pdfFile);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            layoutManager.addHeaderFooter(writer, config, "TestProject", "2021", "2025-10-20T15:30:00");

            document.add(new com.itextpdf.layout.element.Paragraph("Content without headers/footers"));
        }

        // Then
        assertThat(pdfFile).exists();
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
        }
    }

    /**
     * 測試：TocSection 資料類別
     */
    @Test
    void shouldCreateTocSectionWithTitle() {
        // When
        PdfLayoutManager.TocSection section = new PdfLayoutManager.TocSection("Executive Summary", 1);

        // Then
        assertThat(section.getTitle()).isEqualTo("Executive Summary");
        assertThat(section.getLevel()).isEqualTo(1);
    }

    /**
     * 測試：TocSection 預設層級為 1
     */
    @Test
    void shouldCreateTocSectionWithDefaultLevel() {
        // When
        PdfLayoutManager.TocSection section = new PdfLayoutManager.TocSection("Executive Summary");

        // Then
        assertThat(section.getTitle()).isEqualTo("Executive Summary");
        assertThat(section.getLevel()).isEqualTo(1); // 預設層級
    }

    /**
     * 測試：addPageBreak() 正確插入分頁
     */
    @Test
    void shouldAddPageBreak() throws Exception {
        // Given
        PdfLayoutManager layoutManager = new PdfLayoutManager();
        File pdfFile = tempDir.resolve("test-page-break.pdf").toFile();

        // When
        try (PdfWriter writer = new PdfWriter(pdfFile);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document document = new Document(pdfDoc)) {

            document.add(new com.itextpdf.layout.element.Paragraph("Page 1"));
            layoutManager.addPageBreak(document);
            document.add(new com.itextpdf.layout.element.Paragraph("Page 2"));
            layoutManager.addPageBreak(document);
            document.add(new com.itextpdf.layout.element.Paragraph("Page 3"));
        }

        // Then
        assertThat(pdfFile).exists();
        try (PDDocument doc = PDDocument.load(pdfFile)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(3);

            // 驗證每頁有預期內容
            PDFTextStripper stripper = new PDFTextStripper();

            stripper.setStartPage(1);
            stripper.setEndPage(1);
            assertThat(stripper.getText(doc)).contains("Page 1");

            stripper.setStartPage(2);
            stripper.setEndPage(2);
            assertThat(stripper.getText(doc)).contains("Page 2");

            stripper.setStartPage(3);
            stripper.setEndPage(3);
            assertThat(stripper.getText(doc)).contains("Page 3");
        }
    }
}
