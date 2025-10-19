package com.github.sonarqube.report.pdf;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * PDF 頁面事件處理器
 *
 * <p>實作 iText {@link IEventHandler}，負責在每頁添加頁首（Header）和頁尾（Footer）。</p>
 *
 * <p><strong>主要功能：</strong></p>
 * <ul>
 *   <li>頁首：Logo（左）、報表標題（中）、專案名稱（右）</li>
 *   <li>頁尾：生成時間（左）、頁碼「Page X of Y」（中）、OWASP 版本（右）</li>
 *   <li>跳過封面頁和目錄頁（前 2 頁），不顯示頁首頁尾</li>
 * </ul>
 *
 * <p><strong>使用範例：</strong></p>
 * <pre>{@code
 * PdfPageEventHandler eventHandler = new PdfPageEventHandler(
 *     config, 2, "MyProject", "OWASP 2021", "2025-10-20T15:30:00");
 * pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, eventHandler);
 * }</pre>
 *
 * <p><strong>線程安全性：</strong>此類別在單一 PDF 文件生成期間使用，設計為線程安全。</p>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.2)
 * @see IEventHandler
 * @see PdfDocumentEvent
 */
public class PdfPageEventHandler implements IEventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PdfPageEventHandler.class);

    private final PdfReportConfig config;
    private final int skipPages;
    private final String projectName;
    private final String owaspVersion;
    private final String generationTime;

    private PdfFont headerFont;
    private PdfFont footerFont;

    /**
     * 建構子
     *
     * @param config PDF 報表配置，包含 Logo 路徑、報表標題、頁首頁尾開關等
     * @param skipPages 跳過的頁數（通常為 2：封面頁 + 目錄頁）
     * @param projectName 專案名稱，顯示於頁首右側
     * @param owaspVersion OWASP 版本（如 "2021"），顯示於頁尾右側
     * @param generationTime 報表生成時間（ISO 8601 格式），顯示於頁尾左側
     */
    public PdfPageEventHandler(PdfReportConfig config, int skipPages,
                                String projectName, String owaspVersion,
                                String generationTime) {
        this.config = config;
        this.skipPages = skipPages;
        this.projectName = projectName;
        this.owaspVersion = owaspVersion;
        this.generationTime = generationTime;

        try {
            this.headerFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA);
            this.footerFont = PdfFontFactory.createFont(PdfStyleConstants.FONT_HELVETICA);
        } catch (IOException e) {
            LOG.error("Failed to load fonts for header/footer", e);
            throw new RuntimeException("Font loading failed", e);
        }
    }

    /**
     * 處理頁面事件（每頁結束時觸發）
     *
     * <p>檢查是否需要跳過頁首頁尾（封面頁、目錄頁），然後添加頁首和頁尾。</p>
     *
     * @param event PDF 文件事件
     */
    @Override
    public void handleEvent(Event event) {
        PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
        PdfDocument pdfDoc = docEvent.getDocument();
        PdfPage page = docEvent.getPage();
        int pageNum = pdfDoc.getPageNumber(page);

        // 跳過封面頁、目錄頁，或使用者停用頁首頁尾
        if (pageNum <= skipPages || !config.isHeaderFooterEnabled()) {
            LOG.debug("Skipping header/footer for page {}", pageNum);
            return;
        }

        try {
            PdfCanvas pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);
            Rectangle pageSize = page.getPageSize();

            addHeader(pdfCanvas, pageSize);
            addFooter(pdfCanvas, pdfDoc, pageSize, pageNum);

            LOG.debug("Header/footer added to page {}", pageNum);
        } catch (Exception e) {
            LOG.error("Failed to add header/footer to page {}", pageNum, e);
            // 不拋出例外，確保 PDF 生成繼續進行
        }
    }

    /**
     * 添加頁首
     *
     * <p><strong>頁首佈局：</strong></p>
     * <ul>
     *   <li>左：Logo（若有配置且檔案存在）</li>
     *   <li>中：報表標題（{@link PdfReportConfig#getReportTitle()}）</li>
     *   <li>右：專案名稱</li>
     * </ul>
     *
     * @param pdfCanvas PDF 畫布
     * @param pageSize 頁面尺寸
     */
    private void addHeader(PdfCanvas pdfCanvas, Rectangle pageSize) {
        float headerY = pageSize.getTop() - 30; // 距離頁面頂部 30px
        float leftX = pageSize.getLeft() + 36; // 左邊距 36px
        float centerX = pageSize.getWidth() / 2;
        float rightX = pageSize.getRight() - 36; // 右邊距 36px

        try (Canvas canvas = new Canvas(pdfCanvas, pageSize)) {
            // 左側：Logo（若有）
            if (config.getLogoPath() != null) {
                try {
                    Image logo = new Image(ImageDataFactory.create(config.getLogoPath()));
                    logo.setWidth(PdfStyleConstants.HEADER_LOGO_WIDTH);
                    logo.setHeight(PdfStyleConstants.HEADER_LOGO_HEIGHT);
                    logo.setFixedPosition(leftX, headerY - PdfStyleConstants.HEADER_LOGO_HEIGHT);
                    canvas.add(logo);
                    LOG.debug("Header logo added from: {}", config.getLogoPath());
                } catch (Exception e) {
                    LOG.warn("Failed to load header logo: {}, using text-only header", config.getLogoPath());
                }
            }

            // 中間：報表標題
            Paragraph title = new Paragraph(config.getReportTitle())
                    .setFont(headerFont)
                    .setFontSize(PdfStyleConstants.HEADER_TITLE_SIZE)
                    .setFontColor(PdfStyleConstants.HEADER_TEXT_COLOR)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFixedPosition(centerX - 150, headerY, 300);
            canvas.add(title);

            // 右側：專案名稱
            Paragraph project = new Paragraph(projectName)
                    .setFont(headerFont)
                    .setFontSize(PdfStyleConstants.HEADER_PROJECT_SIZE)
                    .setFontColor(PdfStyleConstants.HEADER_TEXT_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFixedPosition(rightX - 150, headerY, 150);
            canvas.add(project);

            // 頁首分隔線
            pdfCanvas.saveState()
                    .setStrokeColor(PdfStyleConstants.FOOTER_TEXT_COLOR)
                    .setLineWidth(PdfStyleConstants.HEADER_LINE_THICKNESS)
                    .moveTo(leftX, headerY - 10)
                    .lineTo(rightX, headerY - 10)
                    .stroke()
                    .restoreState();
        }
    }

    /**
     * 添加頁尾
     *
     * <p><strong>頁尾佈局：</strong></p>
     * <ul>
     *   <li>左：生成時間（ISO 8601 格式）</li>
     *   <li>中：頁碼「Page X of Y」（X 和 Y 扣除封面和目錄頁）</li>
     *   <li>右：OWASP 版本（如 "OWASP 2021"）</li>
     * </ul>
     *
     * @param pdfCanvas PDF 畫布
     * @param pdfDoc PDF 文件
     * @param pageSize 頁面尺寸
     * @param pageNum 當前頁碼（從 1 開始）
     */
    private void addFooter(PdfCanvas pdfCanvas, PdfDocument pdfDoc, Rectangle pageSize, int pageNum) {
        float footerY = pageSize.getBottom() + 20; // 距離頁面底部 20px
        float leftX = pageSize.getLeft() + 36; // 左邊距 36px
        float centerX = pageSize.getWidth() / 2;
        float rightX = pageSize.getRight() - 36; // 右邊距 36px

        try (Canvas canvas = new Canvas(pdfCanvas, pageSize)) {
            // 頁尾分隔線
            pdfCanvas.saveState()
                    .setStrokeColor(PdfStyleConstants.FOOTER_TEXT_COLOR)
                    .setLineWidth(PdfStyleConstants.FOOTER_LINE_THICKNESS)
                    .moveTo(leftX, footerY + 15)
                    .lineTo(rightX, footerY + 15)
                    .stroke()
                    .restoreState();

            // 左側：生成時間
            Paragraph timestamp = new Paragraph(generationTime)
                    .setFont(footerFont)
                    .setFontSize(PdfStyleConstants.FOOTER_TEXT_SIZE)
                    .setFontColor(PdfStyleConstants.FOOTER_TEXT_COLOR)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setFixedPosition(leftX, footerY, 200);
            canvas.add(timestamp);

            // 中間：頁碼（扣除封面和目錄頁）
            int contentPageNum = pageNum - skipPages;
            int totalContentPages = pdfDoc.getNumberOfPages() - skipPages;
            String pageText = String.format("Page %d of %d", contentPageNum, totalContentPages);

            Paragraph pageNumber = new Paragraph(pageText)
                    .setFont(footerFont)
                    .setFontSize(PdfStyleConstants.PAGE_NUMBER_SIZE)
                    .setFontColor(PdfStyleConstants.FOOTER_TEXT_COLOR)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFixedPosition(centerX - 50, footerY, 100);
            canvas.add(pageNumber);

            // 右側：OWASP 版本
            Paragraph owasp = new Paragraph("OWASP " + owaspVersion)
                    .setFont(footerFont)
                    .setFontSize(PdfStyleConstants.FOOTER_TEXT_SIZE)
                    .setFontColor(PdfStyleConstants.FOOTER_TEXT_COLOR)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFixedPosition(rightX - 100, footerY, 100);
            canvas.add(owasp);
        }
    }

    /**
     * 獲取配置的跳過頁數
     *
     * @return 跳過頁數（通常為 2：封面頁 + 目錄頁）
     */
    public int getSkipPages() {
        return skipPages;
    }

    /**
     * 獲取專案名稱
     *
     * @return 專案名稱
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * 獲取 OWASP 版本
     *
     * @return OWASP 版本（如 "2021"）
     */
    public String getOwaspVersion() {
        return owaspVersion;
    }

    /**
     * 獲取生成時間
     *
     * @return 生成時間（ISO 8601 格式）
     */
    public String getGenerationTime() {
        return generationTime;
    }
}
