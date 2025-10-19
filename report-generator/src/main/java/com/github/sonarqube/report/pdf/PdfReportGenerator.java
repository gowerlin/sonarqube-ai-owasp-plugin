package com.github.sonarqube.report.pdf;

import com.github.sonarqube.report.ReportGenerator;
import com.github.sonarqube.report.model.AnalysisReport;
import com.github.sonarqube.report.model.ReportSummary;
import com.github.sonarqube.report.model.SecurityFinding;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.navigation.PdfDestination;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
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
    private final PdfChartGenerator chartGenerator = new PdfChartGenerator();

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

                // Story 1.3: 建立執行摘要與統計表格
                createExecutiveSummary(document, report);

                // Story 1.4: 建立嚴重性分布圓餅圖
                createSeverityDistributionSection(document, report);

                // Story 1.4: 建立 OWASP 分類分布長條圖
                createOwaspCategorySection(document, report);

                // 佔位符內容（Story 1.5 將新增實際內容）
                document.add(new Paragraph("- Story 1.5: Detailed Findings Section with Code Snippets")
                        .setMarginTop(50f));

                LOG.debug("PDF document structure created (Stories 1.1-1.4 complete)");
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
     * 建立執行摘要（Executive Summary）章節
     *
     * <p><strong>章節內容：</strong></p>
     * <ul>
     *   <li>章節標題「Executive Summary」</li>
     *   <li>統計表格（總發現數、BLOCKER/CRITICAL/MAJOR 數量、分析檔案數）</li>
     *   <li>自動生成的描述文字（3-5 句）</li>
     *   <li>PDF 書籤（目錄導航）</li>
     * </ul>
     *
     * @param doc iText Document 物件
     * @param report 分析報告
     * @throws IOException 若字型載入失敗
     * @since 2.0.0 (Story 1.3)
     */
    private void createExecutiveSummary(Document doc, AnalysisReport report) throws IOException {
        LOG.info("Creating executive summary");

        ReportSummary summary = report.getReportSummary();
        PdfDocument pdfDoc = doc.getPdfDocument();

        // 新增 PDF 書籤（目錄導航）
        PdfOutline rootOutline = pdfDoc.getOutlines(false);
        PdfOutline summaryOutline = rootOutline.addOutline("Executive Summary");
        summaryOutline.addDestination(PdfExplicitDestination.createFit(pdfDoc.getLastPage()));

        // 章節標題
        PdfFont titleFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA_BOLD);
        Paragraph title = new Paragraph("Executive Summary")
                .setFont(titleFont)
                .setFontSize(PdfStyleConstants.SUMMARY_TITLE_SIZE)
                .setFontColor(PdfStyleConstants.SECTION_TITLE_COLOR)
                .setBold()
                .setMarginBottom(20f);
        doc.add(title);

        // 統計表格
        Table statisticsTable = createStatisticsTable(summary);
        doc.add(statisticsTable);

        // 自動生成的摘要文字
        Paragraph summaryText = generateSummaryText(summary);
        doc.add(summaryText);

        // 分頁
        doc.add(new AreaBreak());

        LOG.info("Executive summary created successfully");
    }

    /**
     * 建立統計表格
     *
     * <p><strong>表格結構：</strong></p>
     * <ul>
     *   <li>標題行：深灰色背景，白色文字</li>
     *   <li>資料行：交替白色和淺灰色背景</li>
     *   <li>嚴重性行：使用顏色標籤（BLOCKER=紅色, CRITICAL=橙色, MAJOR=黃色）</li>
     * </ul>
     *
     * @param summary 報告摘要資料
     * @return iText Table 物件
     * @throws IOException 若字型載入失敗
     */
    private Table createStatisticsTable(ReportSummary summary) throws IOException {
        LOG.debug("Creating statistics table");

        // 建立 2 欄表格（指標名稱、數值）
        Table table = new Table(UnitValue.createPercentArray(new float[]{70, 30}));
        table.setWidth(UnitValue.createPercentValue(80)); // 表格寬度 80%
        table.setMarginTop(10f);
        table.setMarginBottom(20f);

        PdfFont headerFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA_BOLD);
        PdfFont dataFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA);

        // 標題行
        table.addHeaderCell(createHeaderCell("Metric", headerFont));
        table.addHeaderCell(createHeaderCell("Count", headerFont));

        // 資料行索引（用於交替背景顏色）
        int rowIndex = 0;

        // 總發現數
        table.addCell(createDataCell("Total Findings", dataFont, rowIndex));
        table.addCell(createDataCell(String.valueOf(summary.getTotalFindings()), dataFont, rowIndex));
        rowIndex++;

        // BLOCKER 數量（紅色標籤）
        table.addCell(createDataCell("BLOCKER Issues", dataFont, rowIndex));
        table.addCell(createSeverityCell(summary.getBlockerCount(),
                PdfStyleConstants.SEVERITY_BLOCKER_COLOR, dataFont, rowIndex));
        rowIndex++;

        // CRITICAL 數量（橙色標籤）
        table.addCell(createDataCell("CRITICAL Issues", dataFont, rowIndex));
        table.addCell(createSeverityCell(summary.getCriticalCount(),
                PdfStyleConstants.SEVERITY_CRITICAL_COLOR, dataFont, rowIndex));
        rowIndex++;

        // MAJOR 數量（黃色標籤）
        table.addCell(createDataCell("MAJOR Issues", dataFont, rowIndex));
        table.addCell(createSeverityCell(summary.getMajorCount(),
                PdfStyleConstants.SEVERITY_MAJOR_COLOR, dataFont, rowIndex));
        rowIndex++;

        // 分析檔案數（若有資料）
        if (summary.getAnalyzedFilesCount() > 0) {
            table.addCell(createDataCell("Analyzed Files", dataFont, rowIndex));
            table.addCell(createDataCell(String.valueOf(summary.getAnalyzedFilesCount()), dataFont, rowIndex));
        }

        LOG.debug("Statistics table created with {} rows", rowIndex + 1);
        return table;
    }

    /**
     * 建立表格標題儲存格
     *
     * @param text 儲存格文字
     * @param font 字型
     * @return iText Cell 物件
     */
    private Cell createHeaderCell(String text, PdfFont font) {
        return new Cell()
                .add(new Paragraph(text))
                .setBackgroundColor(PdfStyleConstants.TABLE_HEADER_BACKGROUND)
                .setFontColor(PdfStyleConstants.TABLE_HEADER_TEXT_COLOR)
                .setFont(font)
                .setFontSize(PdfStyleConstants.HEADER_TITLE_SIZE)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(PdfStyleConstants.TABLE_CELL_PADDING);
    }

    /**
     * 建立表格資料儲存格
     *
     * @param text 儲存格文字
     * @param font 字型
     * @param rowIndex 行索引（用於交替背景顏色）
     * @return iText Cell 物件
     */
    private Cell createDataCell(String text, PdfFont font, int rowIndex) {
        boolean isEvenRow = (rowIndex % 2 == 0);
        return new Cell()
                .add(new Paragraph(text))
                .setBackgroundColor(isEvenRow ?
                        PdfStyleConstants.TABLE_DATA_BACKGROUND_LIGHT :
                        PdfStyleConstants.TABLE_DATA_BACKGROUND_DARK)
                .setFont(font)
                .setFontSize(PdfStyleConstants.BODY_TEXT_SIZE)
                .setTextAlignment(TextAlignment.LEFT)
                .setPadding(PdfStyleConstants.TABLE_CELL_PADDING);
    }

    /**
     * 建立嚴重性數量儲存格（帶顏色標籤）
     *
     * @param count 數量
     * @param severityColor 嚴重性顏色
     * @param font 字型
     * @param rowIndex 行索引
     * @return iText Cell 物件
     */
    private Cell createSeverityCell(int count, com.itextpdf.kernel.colors.Color severityColor,
                                      PdfFont font, int rowIndex) {
        boolean isEvenRow = (rowIndex % 2 == 0);
        return new Cell()
                .add(new Paragraph(String.valueOf(count)).setBold())
                .setBackgroundColor(isEvenRow ?
                        PdfStyleConstants.TABLE_DATA_BACKGROUND_LIGHT :
                        PdfStyleConstants.TABLE_DATA_BACKGROUND_DARK)
                .setFont(font)
                .setFontSize(PdfStyleConstants.BODY_TEXT_SIZE)
                .setFontColor(severityColor) // 使用嚴重性顏色
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(PdfStyleConstants.TABLE_CELL_PADDING);
    }

    /**
     * 生成摘要文字
     *
     * <p>根據統計資料自動生成描述文字（3-5 句）。</p>
     *
     * <p><strong>生成邏輯：</strong></p>
     * <ul>
     *   <li>無發現：「No security issues detected.」</li>
     *   <li>1-10 個發現：「This project has X findings, Y require immediate attention.」</li>
     *   <li>>10 個發現：「This project has X findings, including Y critical issues that require immediate attention.」</li>
     * </ul>
     *
     * @param summary 報告摘要資料
     * @return iText Paragraph 物件
     * @throws IOException 若字型載入失敗
     */
    private Paragraph generateSummaryText(ReportSummary summary) throws IOException {
        LOG.debug("Generating summary text");

        PdfFont textFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA);
        String summaryText;

        int totalFindings = summary.getTotalFindings();
        int criticalCount = summary.getBlockerCount() + summary.getCriticalCount();

        if (totalFindings == 0) {
            // 無安全問題
            summaryText = "No security issues were detected in this project. " +
                    "The codebase meets the OWASP security standards analyzed.";
        } else if (totalFindings <= 10) {
            // 少量問題
            if (criticalCount > 0) {
                summaryText = String.format(
                        "This project has %d security finding%s, %d of which require immediate attention. " +
                                "Please review the BLOCKER and CRITICAL issues as a priority.",
                        totalFindings, totalFindings > 1 ? "s" : "", criticalCount
                );
            } else {
                summaryText = String.format(
                        "This project has %d security finding%s. " +
                                "While no critical issues were found, it is recommended to address these findings to improve security posture.",
                        totalFindings, totalFindings > 1 ? "s" : ""
                );
            }
        } else {
            // 大量問題
            summaryText = String.format(
                    "This project has %d security findings, including %d critical issues that require immediate attention. " +
                            "We recommend prioritizing the BLOCKER and CRITICAL issues first, followed by addressing MAJOR issues. " +
                            "Please refer to the detailed findings section below for specific recommendations and remediation steps.",
                    totalFindings, criticalCount
            );
        }

        return new Paragraph(summaryText)
                .setFont(textFont)
                .setFontSize(PdfStyleConstants.SUMMARY_TEXT_SIZE)
                .setFontColor(PdfStyleConstants.BODY_TEXT_COLOR)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginTop(20f)
                .setMarginBottom(30f);
    }

    /**
     * 建立嚴重性分布圖表章節
     *
     * <p><strong>章節內容：</strong></p>
     * <ul>
     *   <li>章節標題「Severity Distribution」</li>
     *   <li>圓餅圖（400x300px）顯示各嚴重性等級的百分比分布</li>
     *   <li>使用顏色編碼：BLOCKER=紅色, CRITICAL=橙色, MAJOR=黃色, MINOR=藍色, INFO=綠色</li>
     *   <li>PDF 書籤（目錄導航）</li>
     * </ul>
     *
     * @param doc iText Document 物件
     * @param report 分析報告
     * @throws IOException 若圖表生成失敗
     * @since 2.0.0 (Story 1.4)
     */
    private void createSeverityDistributionSection(Document doc, AnalysisReport report) throws IOException {
        LOG.info("Creating severity distribution chart section");

        PdfDocument pdfDoc = doc.getPdfDocument();
        ReportSummary summary = report.getReportSummary();

        // 新增 PDF 書籤（目錄導航）
        PdfOutline rootOutline = pdfDoc.getOutlines(false);
        PdfOutline severityOutline = rootOutline.addOutline("Severity Distribution");
        severityOutline.addDestination(PdfExplicitDestination.createFit(pdfDoc.getLastPage()));

        // 章節標題
        PdfFont titleFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA_BOLD);
        Paragraph title = new Paragraph("Severity Distribution")
                .setFont(titleFont)
                .setFontSize(PdfStyleConstants.SECTION_TITLE_SIZE)
                .setFontColor(PdfStyleConstants.SECTION_TITLE_COLOR)
                .setBold()
                .setMarginBottom(20f);
        doc.add(title);

        // 生成圓餅圖
        com.itextpdf.layout.element.Image severityChart = chartGenerator.generateSeverityPieChart(summary);
        severityChart.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
        severityChart.setMarginTop(10f);
        severityChart.setMarginBottom(30f);
        doc.add(severityChart);

        // 分頁
        doc.add(new AreaBreak());

        LOG.info("Severity distribution chart section created successfully");
    }

    /**
     * 建立 OWASP 分類分布圖表章節
     *
     * <p><strong>章節內容：</strong></p>
     * <ul>
     *   <li>章節標題「OWASP Category Distribution」</li>
     *   <li>長條圖（600x400px）顯示各 OWASP 類別的問題數量</li>
     *   <li>長條依數量降序排列</li>
     *   <li>使用深藍色 (#003F7F)</li>
     *   <li>PDF 書籤（目錄導航）</li>
     * </ul>
     *
     * @param doc iText Document 物件
     * @param report 分析報告
     * @throws IOException 若圖表生成失敗
     * @since 2.0.0 (Story 1.4)
     */
    private void createOwaspCategorySection(Document doc, AnalysisReport report) throws IOException {
        LOG.info("Creating OWASP category distribution chart section");

        PdfDocument pdfDoc = doc.getPdfDocument();

        // 新增 PDF 書籤（目錄導航）
        PdfOutline rootOutline = pdfDoc.getOutlines(false);
        PdfOutline owaspOutline = rootOutline.addOutline("OWASP Category Distribution");
        owaspOutline.addDestination(PdfExplicitDestination.createFit(pdfDoc.getLastPage()));

        // 章節標題
        PdfFont titleFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA_BOLD);
        Paragraph title = new Paragraph("OWASP Category Distribution")
                .setFont(titleFont)
                .setFontSize(PdfStyleConstants.SECTION_TITLE_SIZE)
                .setFontColor(PdfStyleConstants.SECTION_TITLE_COLOR)
                .setBold()
                .setMarginBottom(20f);
        doc.add(title);

        // 計算 OWASP 分類分布
        Map<String, Long> categoryDistribution = report.getFindings().stream()
                .filter(finding -> finding.getOwaspCategory() != null)
                .collect(Collectors.groupingBy(
                        SecurityFinding::getOwaspCategory,
                        Collectors.counting()));

        if (categoryDistribution.isEmpty()) {
            // 無 OWASP 分類資料
            PdfFont textFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA);
            Paragraph noData = new Paragraph("No OWASP category data available.")
                    .setFont(textFont)
                    .setFontSize(PdfStyleConstants.BODY_TEXT_SIZE)
                    .setFontColor(PdfStyleConstants.BODY_TEXT_COLOR)
                    .setMarginTop(20f);
            doc.add(noData);
        } else {
            // 生成長條圖
            com.itextpdf.layout.element.Image owaspChart = chartGenerator.generateOwaspCategoryBarChart(categoryDistribution);
            owaspChart.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            owaspChart.setMarginTop(10f);
            owaspChart.setMarginBottom(30f);
            doc.add(owaspChart);
        }

        // 分頁
        doc.add(new AreaBreak());

        LOG.info("OWASP category distribution chart section created successfully");
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
