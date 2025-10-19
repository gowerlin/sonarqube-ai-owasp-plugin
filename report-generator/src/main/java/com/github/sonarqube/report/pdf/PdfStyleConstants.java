package com.github.sonarqube.report.pdf;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;

/**
 * PDF 樣式常數類別
 *
 * <p>定義 PDF 報表中使用的所有樣式相關常數，包含字型、顏色、尺寸等。</p>
 *
 * <p><strong>使用範例：</strong></p>
 * <pre>{@code
 * Paragraph title = new Paragraph("Report Title")
 *     .setFontSize(PdfStyleConstants.COVER_TITLE_SIZE)
 *     .setFontColor(PdfStyleConstants.HEADER_TEXT_COLOR);
 * }</pre>
 *
 * <p><strong>Story 進度：</strong></p>
 * <ul>
 *   <li>Story 1.2: 封面頁、頁首頁尾樣式常數（當前）</li>
 *   <li>Story 1.3: 表格樣式常數</li>
 *   <li>Story 1.4: 圖表顏色常數</li>
 * </ul>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.2)
 */
public final class PdfStyleConstants {

    // ==================== Font Names ====================

    /**
     * 標準字型名稱：Helvetica
     *
     * <p>用於一般文字內容。</p>
     */
    public static final String FONT_HELVETICA = "Helvetica";

    /**
     * 粗體字型名稱：Helvetica Bold
     *
     * <p>用於標題和強調文字。</p>
     */
    public static final String FONT_HELVETICA_BOLD = "Helvetica-Bold";

    /**
     * 等寬字型名稱：Courier
     *
     * <p>用於程式碼區塊（Story 1.5）。</p>
     */
    public static final String FONT_COURIER = "Courier";

    // ==================== Font Sizes ====================

    /**
     * 封面頁標題字型大小：24pt
     *
     * <p>用於專案名稱、報表主標題。</p>
     *
     * @see #FONT_HELVETICA_BOLD
     */
    public static final float COVER_TITLE_SIZE = 24f;

    /**
     * 封面頁副標題字型大小：18pt
     *
     * <p>用於 OWASP 版本、日期等副標題資訊。</p>
     *
     * @see #FONT_HELVETICA
     */
    public static final float COVER_SUBTITLE_SIZE = 18f;

    /**
     * 頁首標題字型大小：12pt
     *
     * <p>用於頁首中間的報表標題。</p>
     */
    public static final float HEADER_TITLE_SIZE = 12f;

    /**
     * 頁首專案名稱字型大小：10pt
     *
     * <p>用於頁首右上角的專案名稱。</p>
     */
    public static final float HEADER_PROJECT_SIZE = 10f;

    /**
     * 頁尾文字字型大小：8pt
     *
     * <p>用於頁尾生成時間、OWASP 版本等資訊。</p>
     */
    public static final float FOOTER_TEXT_SIZE = 8f;

    /**
     * 頁碼字型大小：9pt
     *
     * <p>用於頁尾中間的「Page X of Y」頁碼顯示。</p>
     */
    public static final float PAGE_NUMBER_SIZE = 9f;

    /**
     * 一般內文字型大小：11pt
     *
     * <p>用於報表內容段落（Story 1.3-1.5）。</p>
     */
    public static final float BODY_TEXT_SIZE = 11f;

    /**
     * 章節標題字型大小：16pt
     *
     * <p>用於「Executive Summary」、「Detailed Findings」等章節標題（Story 1.3）。</p>
     */
    public static final float SECTION_TITLE_SIZE = 16f;

    // ==================== Colors ====================

    /**
     * 頁首文字顏色：黑色 (#000000)
     *
     * <p>高對比度，適合標題和重點文字。</p>
     */
    public static final Color HEADER_TEXT_COLOR = new DeviceRgb(0, 0, 0); // Black

    /**
     * 頁尾文字顏色：中灰色 (#666666)
     *
     * <p>低調顏色，適合次要資訊如日期、頁碼。</p>
     */
    public static final Color FOOTER_TEXT_COLOR = new DeviceRgb(102, 102, 102); // Gray

    /**
     * 封面背景顏色：白色 (#FFFFFF)
     *
     * <p>乾淨專業的背景色。</p>
     */
    public static final Color COVER_BACKGROUND_COLOR = new DeviceRgb(255, 255, 255); // White

    /**
     * 一般文字顏色：深灰色 (#333333)
     *
     * <p>用於內文段落，減少眼睛疲勞（Story 1.3-1.5）。</p>
     */
    public static final Color BODY_TEXT_COLOR = new DeviceRgb(51, 51, 51); // Dark Gray

    /**
     * 章節標題顏色：深藍色 (#003366)
     *
     * <p>專業商務風格，適合章節標題（Story 1.3）。</p>
     */
    public static final Color SECTION_TITLE_COLOR = new DeviceRgb(0, 51, 102); // Dark Blue

    // ==================== Image Sizes ====================

    /**
     * 封面頁 Logo 最大寬度：200 像素
     *
     * <p>超過此寬度的 Logo 將被等比縮放。</p>
     */
    public static final int COVER_LOGO_MAX_WIDTH = 200;

    /**
     * 封面頁 Logo 最大高度：100 像素
     *
     * <p>超過此高度的 Logo 將被等比縮放。</p>
     */
    public static final int COVER_LOGO_MAX_HEIGHT = 100;

    /**
     * 頁首 Logo 寬度：50 像素
     *
     * <p>頁首左上角的縮小版 Logo 固定寬度。</p>
     */
    public static final int HEADER_LOGO_WIDTH = 50;

    /**
     * 頁首 Logo 高度：25 像素
     *
     * <p>頁首左上角的縮小版 Logo 固定高度。</p>
     */
    public static final int HEADER_LOGO_HEIGHT = 25;

    // ==================== Spacing and Margins ====================

    /**
     * 封面頁上方邊距：100 像素
     *
     * <p>封面頁頂部留白，讓標題更突出。</p>
     */
    public static final float COVER_TOP_MARGIN = 100f;

    /**
     * 章節間距：20 像素
     *
     * <p>章節標題與內容之間的垂直間距。</p>
     */
    public static final float SECTION_SPACING = 20f;

    /**
     * 段落間距：10 像素
     *
     * <p>段落之間的垂直間距。</p>
     */
    public static final float PARAGRAPH_SPACING = 10f;

    /**
     * 頁首高度：40 像素
     *
     * <p>頁首區域保留高度。</p>
     */
    public static final float HEADER_HEIGHT = 40f;

    /**
     * 頁尾高度：30 像素
     *
     * <p>頁尾區域保留高度。</p>
     */
    public static final float FOOTER_HEIGHT = 30f;

    // ==================== Line Thickness ====================

    /**
     * 頁首分隔線粗細：1 像素
     *
     * <p>頁首下方的水平分隔線。</p>
     */
    public static final float HEADER_LINE_THICKNESS = 1f;

    /**
     * 頁尾分隔線粗細：1 像素
     *
     * <p>頁尾上方的水平分隔線。</p>
     */
    public static final float FOOTER_LINE_THICKNESS = 1f;

    /**
     * 表格邊框粗細：0.5 像素
     *
     * <p>用於表格邊框（Story 1.3）。</p>
     */
    public static final float TABLE_BORDER_THICKNESS = 0.5f;

    // ==================== Severity Colors (Story 1.3) ====================

    /**
     * 嚴重性顏色：BLOCKER（紅色 #D4333F）
     *
     * <p>用於 BLOCKER 級別問題的背景或標籤顏色。</p>
     *
     * @since 2.0.0 (Story 1.3)
     */
    public static final Color SEVERITY_BLOCKER_COLOR = new DeviceRgb(212, 51, 63); // Red

    /**
     * 嚴重性顏色：CRITICAL（橙色 #FFA500）
     *
     * <p>用於 CRITICAL 級別問題的背景或標籤顏色。</p>
     *
     * @since 2.0.0 (Story 1.3)
     */
    public static final Color SEVERITY_CRITICAL_COLOR = new DeviceRgb(255, 165, 0); // Orange

    /**
     * 嚴重性顏色：MAJOR（黃色 #FFD700）
     *
     * <p>用於 MAJOR 級別問題的背景或標籤顏色。</p>
     *
     * @since 2.0.0 (Story 1.3)
     */
    public static final Color SEVERITY_MAJOR_COLOR = new DeviceRgb(255, 215, 0); // Gold/Yellow

    /**
     * 嚴重性顏色：MINOR（藍色 #4B9FD5）
     *
     * <p>用於 MINOR 級別問題的背景或標籤顏色。</p>
     *
     * @since 2.0.0 (Story 1.3)
     */
    public static final Color SEVERITY_MINOR_COLOR = new DeviceRgb(75, 159, 213); // Blue

    /**
     * 嚴重性顏色：INFO（綠色 #00AA00）
     *
     * <p>用於 INFO 級別問題的背景或標籤顏色。</p>
     *
     * @since 2.0.0 (Story 1.3)
     */
    public static final Color SEVERITY_INFO_COLOR = new DeviceRgb(0, 170, 0); // Green

    // ==================== Table Styles (Story 1.3) ====================

    /**
     * 表格標題行背景顏色：深灰色 (#333333)
     *
     * <p>用於表格標題行，提供高對比度。</p>
     *
     * @since 2.0.0 (Story 1.3)
     */
    public static final Color TABLE_HEADER_BACKGROUND = new DeviceRgb(51, 51, 51); // Dark Gray

    /**
     * 表格標題行文字顏色：白色 (#FFFFFF)
     *
     * <p>用於表格標題行文字，搭配深灰色背景。</p>
     *
     * @since 2.0.0 (Story 1.3)
     */
    public static final Color TABLE_HEADER_TEXT_COLOR = new DeviceRgb(255, 255, 255); // White

    /**
     * 表格資料行背景顏色（淺色）：白色 (#FFFFFF)
     *
     * <p>用於表格交替行背景（奇數行）。</p>
     *
     * @since 2.0.0 (Story 1.3)
     */
    public static final Color TABLE_DATA_BACKGROUND_LIGHT = new DeviceRgb(255, 255, 255); // White

    /**
     * 表格資料行背景顏色（深色）：淺灰色 (#F5F5F5)
     *
     * <p>用於表格交替行背景（偶數行），提升可讀性。</p>
     *
     * @since 2.0.0 (Story 1.3)
     */
    public static final Color TABLE_DATA_BACKGROUND_DARK = new DeviceRgb(245, 245, 245); // Light Gray

    /**
     * 表格邊框顏色：中灰色 (#CCCCCC)
     *
     * <p>用於表格外框和內部分隔線。</p>
     *
     * @since 2.0.0 (Story 1.3)
     */
    public static final Color TABLE_BORDER_COLOR = new DeviceRgb(204, 204, 204); // Gray

    /**
     * 表格儲存格內邊距：10 像素
     *
     * <p>儲存格內容與邊框之間的空白。</p>
     *
     * @since 2.0.0 (Story 1.3)
     */
    public static final float TABLE_CELL_PADDING = 10f;

    // ==================== Summary Section Fonts (Story 1.3) ====================

    /**
     * 執行摘要標題字型大小：16pt
     *
     * <p>用於「Executive Summary」章節標題。</p>
     *
     * @since 2.0.0 (Story 1.3)
     */
    public static final float SUMMARY_TITLE_SIZE = 16f;

    /**
     * 執行摘要內文字型大小：12pt
     *
     * <p>用於執行摘要的描述文字。</p>
     *
     * @since 2.0.0 (Story 1.3)
     */
    public static final float SUMMARY_TEXT_SIZE = 12f;

    // ==================== Color Theme Support (Future Extension) ====================

    /**
     * 主題顏色：預設藍色 (#0066CC)
     *
     * <p>用於圖表、強調元素（Story 1.4）。</p>
     */
    public static final Color THEME_PRIMARY_COLOR = new DeviceRgb(0, 102, 204); // Blue

    /**
     * 主題顏色：次要綠色 (#009933)
     *
     * <p>用於成功狀態、正向指標（Story 1.4）。</p>
     */
    public static final Color THEME_SECONDARY_COLOR = new DeviceRgb(0, 153, 51); // Green

    /**
     * 主題顏色：警告橘色 (#FF9900)
     *
     * <p>用於警告狀態、中度風險（Story 1.4）。</p>
     */
    public static final Color THEME_WARNING_COLOR = new DeviceRgb(255, 153, 0); // Orange

    /**
     * 主題顏色：錯誤紅色 (#CC0000)
     *
     * <p>用於錯誤狀態、高度風險（Story 1.4）。</p>
     */
    public static final Color THEME_ERROR_COLOR = new DeviceRgb(204, 0, 0); // Red

    // ==================== Private Constructor ====================

    /**
     * 私有建構子，防止實例化
     *
     * <p>此類別僅提供靜態常數，不應被實例化。</p>
     *
     * @throws UnsupportedOperationException 嘗試實例化時拋出
     */
    private PdfStyleConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
