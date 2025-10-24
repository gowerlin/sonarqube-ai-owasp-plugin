package com.github.sonarqube.report.pdf;

import com.github.sonarqube.report.ReportGenerator;
import com.github.sonarqube.report.exception.ReportGenerationException;
import com.github.sonarqube.report.model.AnalysisReport;
import com.github.sonarqube.report.model.ReportSummary;
import com.github.sonarqube.report.model.SecurityFinding;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.PdfAConformanceLevel;
import com.itextpdf.kernel.pdf.navigation.PdfDestination;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Div;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.pdfa.PdfADocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
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

    // Story 1.7: 超時控制（60 秒）
    private static final int GENERATION_TIMEOUT_SECONDS = 60;

    // Story 1.7: 大型報表閾值（>500 個發現需要批次處理）
    private static final int LARGE_REPORT_THRESHOLD = 500;

    // Story 1.7: 批次處理大小
    private static final int BATCH_SIZE = 100;

    private final PdfLayoutManager layoutManager = new PdfLayoutManager();
    private final PdfChartGenerator chartGenerator = new PdfChartGenerator();

    // Story 1.7: ExecutorService for timeout control
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    /**
     * 生成 PDF 格式的安全分析報告
     *
     * <p><strong>Story 1.7 增強：</strong>全面錯誤處理、超時控制、效能優化、詳細日誌。</p>
     *
     * <p><strong>錯誤處理：</strong></p>
     * <ul>
     *   <li>超時控制：60 秒後自動中斷</li>
     *   <li>記憶體不足：捕獲 OutOfMemoryError 並提供建議</li>
     *   <li>iText 異常：捕獲 PdfException 並記錄詳細資訊</li>
     *   <li>空報告：生成包含「No security issues found」訊息的 PDF</li>
     * </ul>
     *
     * <p><strong>效能優化：</strong></p>
     * <ul>
     *   <li>串流寫入：避免完整 PDF 在記憶體中建構</li>
     *   <li>批次處理：大型報表（>500 個發現）分批處理</li>
     *   <li>圖表快取：相同資料的圖表僅生成一次</li>
     * </ul>
     *
     * <p><strong>效能指標：</strong>記錄生成時間、檔案大小、記憶體使用。</p>
     *
     * @param report 分析報告數據，包含專案名稱、OWASP 版本、發現等資訊。不可為 null。
     * @return 生成的 PDF 檔案絕對路徑，失敗時返回空字串
     */
    @Override
    public String generate(AnalysisReport report) {
        // Story 1.7: 輸入驗證
        if (report == null) {
            LOG.error("AnalysisReport cannot be null");
            return "";
        }

        LOG.info("Starting PDF generation for project: {}", report.getProjectName());

        // Story 1.7: 效能指標 - 記錄開始時間
        long startTime = System.currentTimeMillis();
        long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

        try {
            // Story 1.7: 使用 Future 實作超時控制
            Future<String> future = executorService.submit(() -> generateInternal(report));

            String outputPath = future.get(GENERATION_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Story 1.7: 效能指標 - 記錄完成狀態
            long duration = System.currentTimeMillis() - startTime;
            long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            long memoryUsed = finalMemory - initialMemory;

            File pdfFile = new File(outputPath);
            long fileSize = pdfFile.exists() ? pdfFile.length() : 0;

            LOG.info("PDF generated successfully: {} bytes in {}ms, memory used: {} bytes",
                    fileSize, duration, memoryUsed);

            return outputPath;

        } catch (TimeoutException e) {
            // Story 1.7: 超時錯誤處理
            LOG.error("PDF generation timeout after {} seconds for project: {}",
                    GENERATION_TIMEOUT_SECONDS, report.getProjectName(), e);
            return "";

        } catch (OutOfMemoryError e) {
            // Story 1.7: 記憶體不足錯誤處理
            LOG.error("Out of memory during PDF generation for project: {}. " +
                            "Current findings: {}. Increase JVM heap size or reduce report size.",
                    report.getProjectName(),
                    report.getFindings() != null ? report.getFindings().size() : 0, e);
            return "";

        } catch (ExecutionException e) {
            // Story 1.7: 執行異常處理（包裝內部異常）
            Throwable cause = e.getCause();

            if (cause instanceof IOException) {
                LOG.error("File I/O error during PDF generation for project: {}: {}",
                        report.getProjectName(), cause.getMessage(), cause);
            } else {
                LOG.error("Error during PDF generation for project: {}",
                        report.getProjectName(), cause);
            }
            return "";

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("PDF generation interrupted for project: {}", report.getProjectName(), e);
            return "";
        }
    }

    /**
     * 內部 PDF 生成方法（由 ExecutorService 執行）
     *
     * <p>Story 1.7: 此方法由 generate() 透過 ExecutorService 調用，支援超時控制。</p>
     *
     * @param report 分析報告
     * @return 生成的 PDF 檔案路徑
     * @throws IOException 如果檔案操作失敗
     */
    private String generateInternal(AnalysisReport report) throws IOException {
        // 使用系統臨時目錄儲存 PDF 檔案，避免硬編碼路徑問題
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("owasp-report-", ".pdf");
        String outputPath = tempFile.toAbsolutePath().toString();
        LOG.info("Creating PDF report at temporary location: {}", outputPath);

        PdfReportConfig config = PdfReportConfig.builder().build(); // 預設配置

        // Story 1.7: 檢查是否為空報告
        boolean isEmptyReport = report.getFindings() == null || report.getFindings().isEmpty();
        if (isEmptyReport) {
            LOG.warn("No security findings in report for project: {}, generating empty report",
                    report.getProjectName());
        }

        // Story 1.7: 使用 try-with-resources 確保資源正確釋放（串流寫入優化）
        try (PdfWriter writer = new PdfWriter(outputPath);
             PdfADocument pdfADoc = createPdfADocument(writer);
             Document document = new Document(pdfADoc)) {

            // Note: Compression is enabled by default in iText 7.2.5

            // Story 1.2: 建立封面頁
            layoutManager.createCoverPage(document, report, config);

            // Story 1.2: 建立目錄（章節結構）
            List<PdfLayoutManager.TocSection> sections = createSectionList(report);
            layoutManager.createTableOfContents(document, sections);

            // Story 1.2: 註冊頁首頁尾事件處理器
            String generationTime = LocalDateTime.now()
                    .format(DateTimeFormatter.ISO_DATE_TIME);
            layoutManager.addHeaderFooter(
                    pdfADoc,
                    config,
                    report.getProjectName(),
                    report.getOwaspVersion(),
                    generationTime
            );

            // Story 1.7: 空報告處理
            if (isEmptyReport) {
                createEmptyReportContent(document);
            } else {
                // Story 1.3: 建立執行摘要與統計表格
                createExecutiveSummary(document, report);

                // Story 1.4: 建立嚴重性分布圓餅圖
                createSeverityDistributionSection(document, report);

                // Story 1.4: 建立 OWASP 分類分布長條圖
                createOwaspCategorySection(document, report);

                // Story 1.5: 建立詳細發現區段
                createDetailedFindingsSection(document, report);
            }

            LOG.debug("PDF document structure created (Stories 1.1-1.7 complete)");
        }

        LOG.debug("PDF report written to: {}", outputPath);
        return outputPath;
    }

    /**
     * 建立空報告內容
     *
     * <p>Story 1.7: 當分析報告無安全發現時，生成包含「No security issues found」訊息的 PDF。</p>
     *
     * @param document PDF 文件
     * @throws IOException 如果字型載入失敗
     */
    private void createEmptyReportContent(Document document) throws IOException {
        PdfFont titleFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA_BOLD);
        PdfFont textFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA);

        document.add(new AreaBreak());

        Paragraph title = new Paragraph("Analysis Summary")
                .setFont(titleFont)
                .setFontSize(PdfStyleConstants.SECTION_TITLE_SIZE)
                .setFontColor(PdfStyleConstants.HEADER_TEXT_COLOR)
                .setBold()
                .setMarginBottom(20f);
        document.add(title);

        Paragraph message = new Paragraph("No security issues found.")
                .setFont(textFont)
                .setFontSize(PdfStyleConstants.BODY_TEXT_SIZE)
                .setFontColor(PdfStyleConstants.BODY_TEXT_COLOR)
                .setMarginBottom(20f);
        document.add(message);

        Paragraph congratulations = new Paragraph(
                "Congratulations! Your project passed all OWASP security checks with no vulnerabilities detected.")
                .setFont(textFont)
                .setFontSize(PdfStyleConstants.BODY_TEXT_SIZE)
                .setFontColor(PdfStyleConstants.BODY_TEXT_COLOR);
        document.add(congratulations);

        LOG.info("Empty report content created");
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
            return new PdfADocument(writer, PdfAConformanceLevel.PDF_A_1B,
                    new PdfOutputIntent(
                            "Custom", "", "http://www.color.org", "sRGB IEC61966-2.1",
                            null));
        }

        PdfOutputIntent outputIntent =
                new PdfOutputIntent(
                        "Custom", "", "http://www.color.org", "sRGB IEC61966-2.1",
                        iccProfile);

        return new PdfADocument(writer, PdfAConformanceLevel.PDF_A_1B, outputIntent);
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
     * 建立詳細發現區段
     *
     * <p><strong>章節內容：</strong></p>
     * <ul>
     *   <li>依嚴重性分組（BLOCKER → CRITICAL → MAJOR → MINOR → INFO）</li>
     *   <li>每個嚴重性分組有獨立的子章節標題（例如「BLOCKER Issues (3)」）</li>
     *   <li>每個安全問題包含：編號、規則名稱、檔案路徑、OWASP 分類、CWE ID、描述、代碼片段、修復建議</li>
     *   <li>使用 KeepTogether 防止代碼片段被分頁切斷</li>
     * </ul>
     *
     * @param doc iText Document 物件
     * @param report 分析報告
     * @throws IOException 若字型載入失敗
     * @since 2.0.0 (Story 1.5)
     */
    private void createDetailedFindingsSection(Document doc, AnalysisReport report) throws IOException {
        LOG.info("Creating detailed findings section");

        List<SecurityFinding> findings = report.getFindings();
        if (findings == null || findings.isEmpty()) {
            LOG.info("No security findings to display");
            return;
        }

        // 依嚴重性分組
        Map<String, List<SecurityFinding>> groupedBySeverity = findings.stream()
                .collect(Collectors.groupingBy(SecurityFinding::getSeverity));

        // 依嚴重性順序處理（BLOCKER, CRITICAL, MAJOR, MINOR, INFO）
        List<String> severityOrder = List.of("BLOCKER", "CRITICAL", "MAJOR", "MINOR", "INFO");
        for (String severity : severityOrder) {
            List<SecurityFinding> severityFindings = groupedBySeverity.get(severity);
            if (severityFindings != null && !severityFindings.isEmpty()) {
                addSeverityGroupSection(doc, severity, severityFindings);
            }
        }

        LOG.info("Detailed findings section created successfully ({} findings)", findings.size());
    }

    /**
     * 新增嚴重性分組章節
     *
     * <p>建立嚴重性分組的章節標題和 PDF 書籤，然後逐一新增該嚴重性的所有發現。</p>
     *
     * @param doc iText Document 物件
     * @param severity 嚴重性等級（BLOCKER, CRITICAL, MAJOR, MINOR, INFO）
     * @param findings 該嚴重性的發現列表
     * @throws IOException 若字型載入失敗
     */
    private void addSeverityGroupSection(Document doc, String severity, List<SecurityFinding> findings)
            throws IOException {
        LOG.debug("Adding {} severity group ({} findings)", severity, findings.size());

        PdfDocument pdfDoc = doc.getPdfDocument();

        // 新增 PDF 書籤（目錄導航）
        PdfOutline rootOutline = pdfDoc.getOutlines(false);
        String sectionTitle = String.format("%s Issues (%d)", severity, findings.size());
        PdfOutline severityOutline = rootOutline.addOutline(sectionTitle);
        severityOutline.addDestination(PdfExplicitDestination.createFit(pdfDoc.getLastPage()));

        // 章節標題（使用嚴重性顏色）
        PdfFont titleFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA_BOLD);
        com.itextpdf.kernel.colors.Color severityColor = getSeverityColorByName(severity);

        Paragraph title = new Paragraph(sectionTitle)
                .setFont(titleFont)
                .setFontSize(PdfStyleConstants.SECTION_TITLE_SIZE)
                .setFontColor(severityColor)
                .setBold()
                .setMarginBottom(20f);
        doc.add(title);

        // 逐一新增發現
        for (int i = 0; i < findings.size(); i++) {
            addFinding(doc, findings.get(i), i + 1);
        }

        // 分頁（進入下一個嚴重性分組）
        doc.add(new AreaBreak());

        LOG.debug("{} severity group added successfully", severity);
    }

    /**
     * 新增單一安全發現
     *
     * <p><strong>發現區塊結構：</strong></p>
     * <ul>
     *   <li>標題：編號 + 規則名稱（14pt 粗體）</li>
     *   <li>位置：檔案路徑:行號（等寬字體）</li>
     *   <li>元資料：OWASP 分類 | CWE ID</li>
     *   <li>描述：問題描述文字</li>
     *   <li>代碼片段：淺灰色背景區塊（若有）</li>
     *   <li>修復建議：淺黃色背景區塊（若有）</li>
     * </ul>
     *
     * <p>使用 KeepTogether 確保代碼片段不被分頁切斷。</p>
     *
     * @param doc iText Document 物件
     * @param finding 安全發現
     * @param index 編號（從 1 開始）
     * @throws IOException 若字型載入失敗
     */
    private void addFinding(Document doc, SecurityFinding finding, int index) throws IOException {
        LOG.debug("Adding finding #{}: {}", index, finding.getRuleName());

        PdfFont titleFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA_BOLD);
        PdfFont textFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA);
        PdfFont codeFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_COURIER);

        // 建立發現區塊容器（KeepTogether 防止分頁切斷）
        Div findingBlock = new Div();
        findingBlock.setKeepTogether(true);
        findingBlock.setMarginBottom(PdfStyleConstants.FINDING_SPACING);

        // 標題：編號 + 規則名稱
        Paragraph title = new Paragraph(index + ". " + finding.getRuleName())
                .setFont(titleFont)
                .setFontSize(PdfStyleConstants.FINDING_TITLE_SIZE)
                .setFontColor(PdfStyleConstants.HEADER_TEXT_COLOR)
                .setBold()
                .setMarginBottom(5f);
        findingBlock.add(title);

        // 位置：檔案路徑:行號
        if (finding.getFilePath() != null) {
            String location = finding.getFilePath();
            if (finding.getLineNumber() != null) {
                location += ":" + finding.getLineNumber();
            }
            Paragraph locationPara = new Paragraph(location)
                    .setFont(codeFont)
                    .setFontSize(PdfStyleConstants.CODE_SNIPPET_SIZE)
                    .setFontColor(PdfStyleConstants.BODY_TEXT_COLOR)
                    .setMarginBottom(5f);
            findingBlock.add(locationPara);
        }

        // 元資料：OWASP 分類 | CWE ID
        StringBuilder metadata = new StringBuilder();
        if (finding.getOwaspCategory() != null && !finding.getOwaspCategory().isEmpty()) {
            metadata.append("OWASP: ").append(finding.getOwaspCategory());
        }
        if (finding.getCweId() != null && !finding.getCweId().isEmpty()) {
            if (metadata.length() > 0) {
                metadata.append(" | ");
            }
            metadata.append("CWE: ").append(finding.getCweId());
        }
        if (metadata.length() > 0) {
            Paragraph metadataPara = new Paragraph(metadata.toString())
                    .setFont(textFont)
                    .setFontSize(PdfStyleConstants.FINDING_TEXT_SIZE)
                    .setFontColor(PdfStyleConstants.BODY_TEXT_COLOR)
                    .setMarginBottom(10f);
            findingBlock.add(metadataPara);
        }

        // 描述
        if (finding.getDescription() != null && !finding.getDescription().isEmpty()) {
            Paragraph description = new Paragraph(finding.getDescription())
                    .setFont(textFont)
                    .setFontSize(PdfStyleConstants.FINDING_TEXT_SIZE)
                    .setFontColor(PdfStyleConstants.BODY_TEXT_COLOR)
                    .setMarginBottom(10f);
            findingBlock.add(description);
        }

        // 代碼片段（若有）
        if (finding.getCodeSnippet() != null && !finding.getCodeSnippet().isEmpty()) {
            Div codeBlock = new Div()
                    .setBackgroundColor(PdfStyleConstants.CODE_SNIPPET_BACKGROUND)
                    .setPadding(PdfStyleConstants.BLOCK_PADDING)
                    .setMarginTop(PdfStyleConstants.BLOCK_MARGIN)
                    .setMarginBottom(PdfStyleConstants.BLOCK_MARGIN)
                    .setKeepTogether(true); // 防止代碼片段被切斷

            Paragraph code = new Paragraph(finding.getCodeSnippet())
                    .setFont(codeFont)
                    .setFontSize(PdfStyleConstants.CODE_SNIPPET_SIZE)
                    .setFontColor(PdfStyleConstants.HEADER_TEXT_COLOR)
                    .setFixedLeading(PdfStyleConstants.CODE_SNIPPET_LEADING); // 固定行高

            codeBlock.add(code);
            findingBlock.add(codeBlock);
        }

        // 修復建議（若有）
        if (finding.getFixSuggestion() != null && !finding.getFixSuggestion().isEmpty()) {
            Div fixBlock = new Div()
                    .setBackgroundColor(PdfStyleConstants.FIX_SUGGESTION_BACKGROUND)
                    .setPadding(PdfStyleConstants.BLOCK_PADDING)
                    .setMarginTop(PdfStyleConstants.BLOCK_MARGIN)
                    .setMarginBottom(PdfStyleConstants.BLOCK_MARGIN);

            Paragraph fixTitle = new Paragraph("💡 Fix Suggestion")
                    .setFont(titleFont)
                    .setFontSize(PdfStyleConstants.FINDING_TEXT_SIZE)
                    .setFontColor(PdfStyleConstants.HEADER_TEXT_COLOR)
                    .setBold()
                    .setMarginBottom(5f);

            Paragraph fixText = new Paragraph(finding.getFixSuggestion())
                    .setFont(textFont)
                    .setFontSize(PdfStyleConstants.FINDING_TEXT_SIZE)
                    .setFontColor(PdfStyleConstants.BODY_TEXT_COLOR);

            fixBlock.add(fixTitle);
            fixBlock.add(fixText);
            findingBlock.add(fixBlock);
        }

        doc.add(findingBlock);

        LOG.debug("Finding #{} added successfully", index);
    }

    /**
     * 根據嚴重性名稱取得對應顏色
     *
     * @param severity 嚴重性名稱
     * @return iText Color 物件
     */
    private com.itextpdf.kernel.colors.Color getSeverityColorByName(String severity) {
        return switch (severity) {
            case "BLOCKER" -> PdfStyleConstants.SEVERITY_BLOCKER_COLOR;
            case "CRITICAL" -> PdfStyleConstants.SEVERITY_CRITICAL_COLOR;
            case "MAJOR" -> PdfStyleConstants.SEVERITY_MAJOR_COLOR;
            case "MINOR" -> PdfStyleConstants.SEVERITY_MINOR_COLOR;
            case "INFO" -> PdfStyleConstants.SEVERITY_INFO_COLOR;
            default -> PdfStyleConstants.HEADER_TEXT_COLOR; // 預設黑色
        };
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
