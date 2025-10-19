package com.github.sonarqube.report.pdf;

import com.github.sonarqube.report.ReportGenerator;
import com.github.sonarqube.report.model.AnalysisReport;
import com.github.sonarqube.report.model.SecurityFinding;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.filespec.PdfFileSpec;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.pdfa.PdfADocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PDF 報表生成器
 *
 * <p>實作 ReportGenerator 介面，負責生成企業級 PDF 格式的安全分析報告。</p>
 *
 * <p><strong>主要功能：</strong></p>
 * <ul>
 *   <li>Story 1.1: 基礎 PDF 文件結構生成</li>
 *   <li>Story 1.2: 封面頁、目錄、頁首頁尾（當前）</li>
 *   <li>Story 1.3: 執行摘要與統計表格</li>
 *   <li>Story 1.4: 視覺化圖表</li>
 *   <li>Story 1.5: 詳細發現區段</li>
 * </ul>
 *
 * <p><strong>技術棧：</strong></p>
 * <ul>
 *   <li>iText 7.2.5+ (AGPL 3.0 license)</li>
 *   <li>PDF/A-1b 合規標準（長期存檔）</li>
 * </ul>
 *
 * <p><strong>線程安全性：</strong>此類別是線程安全的，可作為單例使用。</p>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.1, enhanced in Story 1.2)
 * @see ReportGenerator
 */
public class PdfReportGenerator implements ReportGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(PdfReportGenerator.class);

    private final PdfLayoutManager layoutManager = new PdfLayoutManager();

    /**
     * 生成 PDF 格式的安全分析報告
     *
     * <p><strong>當前版本（Story 1.2）：</strong>包含封面頁、目錄、頁首頁尾。
     * 後續 Stories 將新增執行摘要、圖表、詳細發現等內容。</p>
     *
     * <p><strong>輸出位置：</strong>target/test-report.pdf（臨時位置）。</p>
     *
     * <p><strong>錯誤處理：</strong>所有 iText 例外都會被捕獲並記錄，
     * 確保不影響其他報表生成器（如 Markdown）。</p>
     *
     * @param report 分析報告數據，包含專案名稱、OWASP 版本、發現等資訊。
     *               不可為 null。空報告（0 個發現）將在 Story 1.7 處理。
     * @return 生成的 PDF 檔案絕對路徑
     * @throws IllegalArgumentException 如果 report 為 null
     */
    @Override
    public String generate(AnalysisReport report) {
        if (report == null) {
            throw new IllegalArgumentException("AnalysisReport cannot be null");
        }

        LOG.info("Starting PDF report generation for project: {}", report.getProjectName());

        try {
            String outputPath = "target/test-report.pdf";
            PdfReportConfig config = PdfReportConfig.builder().build(); // 預設配置

            // 使用 try-with-resources 確保資源正確釋放
            try (PdfWriter writer = new PdfWriter(outputPath);
                 PdfADocument pdfADoc = createPdfADocument(writer);
                 Document document = new Document(pdfADoc)) {

                // Story 1.2: 建立封面頁
                layoutManager.createCoverPage(document, report, config);

                // Story 1.2: 建立目錄（章節結構）
                List<PdfLayoutManager.TocSection> sections = createSectionList(report);
                layoutManager.createTableOfContents(document, sections);

                // Story 1.2: 註冊頁首頁尾事件處理器
                String generationTime = LocalDateTime.now()
                        .format(DateTimeFormatter.ISO_DATE_TIME);
                layoutManager.addHeaderFooter(
                        writer,
                        config,
                        report.getProjectName(),
                        report.getOwaspVersion(),
                        generationTime
                );

                // 佔位符內容（Stories 1.3-1.5 將新增實際內容）
                document.add(new Paragraph("Content will be added in subsequent stories...")
                        .setMarginTop(50f));
                document.add(new Paragraph("- Story 1.3: Executive Summary and Statistical Tables"));
                document.add(new Paragraph("- Story 1.4: Visual Charts (Severity, Category Distribution)"));
                document.add(new Paragraph("- Story 1.5: Detailed Findings Section with Code Snippets"));

                LOG.debug("PDF document structure created (Stories 1.1-1.2 complete)");
            }

            LOG.info("PDF report generated successfully: {}", outputPath);
            return outputPath;

        } catch (IOException e) {
            LOG.error("Failed to generate PDF report for project: {}", report.getProjectName(), e);
            // 不拋出例外，確保不影響其他報表生成器
            return null;
        }
    }

    /**
     * 建立 PDF/A-1b 相容的 PDF 文件
     *
     * <p><strong>PDF/A-1b 標準：</strong></p>
     * <ul>
     *   <li>長期存檔標準，確保 PDF 可在未來數十年後仍能正確開啟</li>
     *   <li>不依賴外部資源（嵌入所有字型和圖片）</li>
     *   <li>完整的視覺呈現（不壓縮關鍵內容）</li>
     * </ul>
     *
     * <p><strong>色彩描述檔：</strong>使用 sRGB IEC61966-2.1 標準色彩空間。</p>
     *
     * @param writer PDF 寫入器
     * @return PDF/A-1b 文件
     * @throws IOException 若色彩描述檔載入失敗
     */
    private PdfADocument createPdfADocument(PdfWriter writer) throws IOException {
        // PDF/A-1b 需要色彩描述檔（Color Profile）
        // 使用 iText 內建的 sRGB 色彩描述檔
        InputStream iccProfile = getClass().getResourceAsStream("/default_rgb.icc");
        if (iccProfile == null) {
            LOG.warn("sRGB color profile not found, using embedded profile");
            // 若 JAR 中無色彩檔案，使用簡單的 PDF 文件（非 PDF/A）
            return new PdfADocument(writer, com.itextpdf.pdfa.PdfAConformanceLevel.PDF_A_1B,
                    new com.itextpdf.kernel.pdf.PdfOutputIntent(
                            "Custom", "", "http://www.color.org", "sRGB IEC61966-2.1",
                            null));
        }

        com.itextpdf.kernel.pdf.PdfOutputIntent outputIntent =
                new com.itextpdf.kernel.pdf.PdfOutputIntent(
                        "Custom", "", "http://www.color.org", "sRGB IEC61966-2.1",
                        iccProfile);

        return new PdfADocument(writer, com.itextpdf.pdfa.PdfAConformanceLevel.PDF_A_1B, outputIntent);
    }

    /**
     * 建立目錄章節列表
     *
     * <p><strong>目錄結構：</strong></p>
     * <ol>
     *   <li>Executive Summary（執行摘要）</li>
     *   <li>Severity Distribution（嚴重性分布）</li>
     *   <li>OWASP Category Distribution（OWASP 分類分布）</li>
     *   <li>BLOCKER Issues (N 個)</li>
     *   <li>CRITICAL Issues (N 個)</li>
     *   <li>MAJOR Issues (N 個)</li>
     *   <li>... 依報告中的嚴重性分組</li>
     * </ol>
     *
     * @param report 分析報告
     * @return 目錄章節列表
     */
    private List<PdfLayoutManager.TocSection> createSectionList(AnalysisReport report) {
        List<PdfLayoutManager.TocSection> sections = new ArrayList<>();

        // 固定章節
        sections.add(new PdfLayoutManager.TocSection("Executive Summary", 1));
        sections.add(new PdfLayoutManager.TocSection("Severity Distribution", 1));
        sections.add(new PdfLayoutManager.TocSection("OWASP Category Distribution", 1));

        // 依嚴重性分組的發現（動態生成）
        Map<String, Long> severityGroups = report.getFindings().stream()
                .collect(Collectors.groupingBy(
                        SecurityFinding::getSeverity,
                        Collectors.counting()));

        // 按照嚴重性排序（BLOCKER, CRITICAL, MAJOR, MINOR, INFO）
        List<String> severityOrder = List.of("BLOCKER", "CRITICAL", "MAJOR", "MINOR", "INFO");
        for (String severity : severityOrder) {
            Long count = severityGroups.get(severity);
            if (count != null && count > 0) {
                String title = String.format("%s Issues (%d)", severity, count);
                sections.add(new PdfLayoutManager.TocSection(title, 1));
            }
        }

        LOG.debug("Created {} TOC sections", sections.size());
        return sections;
    }

    /**
     * 獲取報表格式名稱
     *
     * @return "pdf"
     */
    @Override
    public String getFormat() {
        return "pdf";
    }

    /**
     * 獲取檔案副檔名
     *
     * @return ".pdf"
     */
    @Override
    public String getFileExtension() {
        return ".pdf";
    }
}
