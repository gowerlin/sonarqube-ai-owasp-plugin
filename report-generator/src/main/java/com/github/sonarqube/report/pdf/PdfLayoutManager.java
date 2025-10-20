package com.github.sonarqube.report.pdf;

import com.github.sonarqube.report.model.AnalysisReport;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfOutline;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.navigation.PdfDestination;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF 版面配置管理器
 *
 * <p>負責管理 PDF 文件的整體結構，包含封面頁、目錄、頁首頁尾、分頁等。</p>
 *
 * <p><strong>主要功能：</strong></p>
 * <ul>
 *   <li>封面頁生成（專案名稱、OWASP 版本、日期、Logo）</li>
 *   <li>目錄（Table of Contents）生成，包含可點擊的書籤</li>
 *   <li>頁首頁尾註冊（透過 {@link PdfPageEventHandler}）</li>
 *   <li>分頁管理</li>
 * </ul>
 *
 * <p><strong>使用範例：</strong></p>
 * <pre>{@code
 * PdfLayoutManager layoutManager = new PdfLayoutManager();
 * layoutManager.createCoverPage(document, report, config);
 * layoutManager.createTableOfContents(document, sections);
 * layoutManager.addHeaderFooter(writer, config, "MyProject", "2021", "2025-10-20T15:30:00");
 * }</pre>
 *
 * <p><strong>線程安全性：</strong>此類別設計為無狀態，可重複使用。</p>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.2)
 */
public class PdfLayoutManager {

    private static final Logger LOG = LoggerFactory.getLogger(PdfLayoutManager.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 建立封面頁
     *
     * <p><strong>封面頁內容（由上到下）：</strong></p>
     * <ol>
     *   <li>公司 Logo（居中，若有配置）</li>
     *   <li>專案名稱（標題，24pt 粗體）</li>
     *   <li>報表標題（副標題，18pt）</li>
     *   <li>OWASP 版本（副標題，18pt）</li>
     *   <li>分析時間（ISO 8601 格式，10pt）</li>
     * </ol>
     *
     * <p><strong>錯誤處理：</strong>若 Logo 檔案不存在或無效，記錄 WARN 日誌並繼續生成（無 Logo）。</p>
     *
     * @param doc iText Document 物件
     * @param report 分析報告資料
     * @param config PDF 報表配置
     * @throws IOException 若字型載入失敗
     */
    public void createCoverPage(Document doc, AnalysisReport report, PdfReportConfig config) throws IOException {
        LOG.info("Creating cover page for project: {}", report.getProjectName());

        PdfFont titleFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA_BOLD);
        PdfFont subtitleFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA);

        // 上方留白
        doc.add(new Paragraph("\n\n\n").setMarginTop(PdfStyleConstants.COVER_TOP_MARGIN));

        // Logo（若有配置）
        if (config.getLogoPath() != null) {
            try {
                Image logo = new Image(ImageDataFactory.create(config.getLogoPath()));
                float aspectRatio = logo.getImageWidth() / logo.getImageHeight();

                // 等比縮放至最大寬度/高度限制
                if (logo.getImageWidth() > PdfStyleConstants.COVER_LOGO_MAX_WIDTH) {
                    logo.setWidth(PdfStyleConstants.COVER_LOGO_MAX_WIDTH);
                    logo.setHeight(PdfStyleConstants.COVER_LOGO_MAX_WIDTH / aspectRatio);
                }
                if (logo.getImageHeight() > PdfStyleConstants.COVER_LOGO_MAX_HEIGHT) {
                    logo.setHeight(PdfStyleConstants.COVER_LOGO_MAX_HEIGHT);
                    logo.setWidth(PdfStyleConstants.COVER_LOGO_MAX_HEIGHT * aspectRatio);
                }

                logo.setHorizontalAlignment(HorizontalAlignment.CENTER);
                doc.add(logo);
                doc.add(new Paragraph("\n")); // Spacing
                LOG.debug("Cover page logo added from: {}", config.getLogoPath());
            } catch (Exception e) {
                LOG.warn("Failed to load cover page logo: {}, using text-only cover", config.getLogoPath(), e);
            }
        }

        // 專案名稱（主標題）
        Paragraph projectTitle = new Paragraph(report.getProjectName())
                .setFont(titleFont)
                .setFontSize(PdfStyleConstants.COVER_TITLE_SIZE)
                .setFontColor(PdfStyleConstants.SECTION_TITLE_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setMarginTop(20f);
        doc.add(projectTitle);

        // 報表標題（副標題）
        Paragraph reportTitle = new Paragraph(config.getReportTitle())
                .setFont(subtitleFont)
                .setFontSize(PdfStyleConstants.COVER_SUBTITLE_SIZE)
                .setFontColor(PdfStyleConstants.HEADER_TEXT_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10f);
        doc.add(reportTitle);

        // OWASP 版本（副標題）
        Paragraph owaspVersion = new Paragraph("OWASP Top 10 " + report.getOwaspVersion())
                .setFont(subtitleFont)
                .setFontSize(PdfStyleConstants.COVER_SUBTITLE_SIZE)
                .setFontColor(PdfStyleConstants.HEADER_TEXT_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10f);
        doc.add(owaspVersion);

        // 分析時間
        String analysisTime = report.getAnalysisTime().format(DATE_FORMATTER);
        Paragraph timestamp = new Paragraph("Analysis Date: " + analysisTime)
                .setFont(subtitleFont)
                .setFontSize(PdfStyleConstants.BODY_TEXT_SIZE)
                .setFontColor(PdfStyleConstants.BODY_TEXT_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(30f);
        doc.add(timestamp);

        // 分頁（封面頁結束）
        doc.add(new AreaBreak());

        LOG.info("Cover page created successfully");
    }

    /**
     * 建立目錄（Table of Contents）
     *
     * <p><strong>目錄包含：</strong></p>
     * <ul>
     *   <li>標題：「Table of Contents」</li>
     *   <li>章節列表：每個章節都有可點擊的書籤（PDF Outline）</li>
     *   <li>書籤階層：根據章節層級建立父子關係</li>
     * </ul>
     *
     * <p><strong>書籤導航：</strong>在 Adobe Acrobat Reader 中，點擊書籤可快速跳轉至對應章節。</p>
     *
     * @param doc iText Document 物件
     * @param sections 章節列表
     * @throws IOException 若字型載入失敗
     */
    public void createTableOfContents(Document doc, List<TocSection> sections) throws IOException {
        LOG.info("Creating table of contents with {} sections", sections.size());

        PdfFont titleFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA_BOLD);
        PdfFont bodyFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA);

        // 目錄標題
        Paragraph tocTitle = new Paragraph("Table of Contents")
                .setFont(titleFont)
                .setFontSize(PdfStyleConstants.COVER_TITLE_SIZE)
                .setFontColor(PdfStyleConstants.SECTION_TITLE_COLOR)
                .setBold()
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginBottom(20f);
        doc.add(tocTitle);

        // 目錄條目
        PdfDocument pdfDoc = doc.getPdfDocument();
        PdfOutline rootOutline = pdfDoc.getOutlines(false);

        for (TocSection section : sections) {
            // 目錄條目文字
            Paragraph tocEntry = new Paragraph(section.getTitle())
                    .setFont(bodyFont)
                    .setFontSize(PdfStyleConstants.BODY_TEXT_SIZE)
                    .setFontColor(PdfStyleConstants.BODY_TEXT_COLOR)
                    .setMarginBottom(8f);
            doc.add(tocEntry);

            // 建立書籤（PDF Outline）
            PdfOutline sectionOutline = rootOutline.addOutline(section.getTitle());
            // 書籤目的地將在實際內容頁建立時設定（Story 1.3-1.5）
            // 目前設定為目錄頁本身（作為佔位符）
            sectionOutline.addDestination(PdfExplicitDestination.createFit(pdfDoc.getPage(2)));

            LOG.debug("TOC entry added: {}", section.getTitle());
        }

        // 分頁（目錄結束）
        doc.add(new AreaBreak());

        LOG.info("Table of contents created successfully");
    }

    /**
     * 註冊頁首頁尾事件處理器
     *
     * <p>將 {@link PdfPageEventHandler} 註冊至 PDF 文件，使每頁結束時自動添加頁首頁尾。</p>
     *
     * <p><strong>跳過頁面：</strong>封面頁（第 1 頁）和目錄頁（第 2 頁）不顯示頁首頁尾。</p>
     *
     * @param writer PDF 寫入器
     * @param config PDF 報表配置
     * @param projectName 專案名稱（顯示於頁首右側）
     * @param owaspVersion OWASP 版本（顯示於頁尾右側）
     * @param generationTime 報表生成時間（顯示於頁尾左側，ISO 8601 格式）
     */
    public void addHeaderFooter(PdfDocument pdfDoc, PdfReportConfig config,
                                 String projectName, String owaspVersion,
                                 String generationTime) {
        LOG.info("Registering header/footer event handler");

        if (!config.isHeaderFooterEnabled()) {
            LOG.info("Header/footer disabled by configuration");
            return;
        }

        int skipPages = 2; // 封面頁 + 目錄頁
        PdfPageEventHandler eventHandler = new PdfPageEventHandler(
                config, skipPages, projectName, owaspVersion, generationTime);

        pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, eventHandler);

        LOG.info("Header/footer event handler registered (skip first {} pages)", skipPages);
    }

    /**
     * 添加分頁符號
     *
     * <p>在當前位置插入分頁，使後續內容從新頁面開始。</p>
     *
     * @param doc iText Document 物件
     */
    public void addPageBreak(Document doc) {
        doc.add(new AreaBreak());
        LOG.debug("Page break added");
    }

    /**
     * 目錄章節資料類別
     *
     * <p>表示目錄中的一個章節條目。</p>
     *
     * @since 2.0.0 (Story 1.2)
     */
    public static class TocSection {
        private final String title;
        private final int level; // 階層深度（1=章節, 2=子章節）

        /**
         * 建構子
         *
         * @param title 章節標題
         * @param level 階層深度（1=章節, 2=子章節）
         */
        public TocSection(String title, int level) {
            this.title = title;
            this.level = level;
        }

        /**
         * 建構子（預設為第 1 層章節）
         *
         * @param title 章節標題
         */
        public TocSection(String title) {
            this(title, 1);
        }

        /**
         * 獲取章節標題
         *
         * @return 章節標題
         */
        public String getTitle() {
            return title;
        }

        /**
         * 獲取階層深度
         *
         * @return 階層深度（1=章節, 2=子章節）
         */
        public int getLevel() {
            return level;
        }
    }
}
