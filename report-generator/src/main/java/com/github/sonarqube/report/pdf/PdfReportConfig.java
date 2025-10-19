package com.github.sonarqube.report.pdf;

/**
 * PDF 報表配置類別
 *
 * <p>使用 Builder Pattern 建構 PDF 報表的客製化配置選項。</p>
 *
 * <p><strong>支援的配置選項：</strong></p>
 * <ul>
 *   <li><strong>Logo 路徑：</strong>公司標誌檔案路徑（PNG/JPG，最大 500KB）</li>
 *   <li><strong>報表標題：</strong>自訂報表標題（最大 100 字元）</li>
 *   <li><strong>色彩主題：</strong>DEFAULT（藍色系）、DARK（深色系）、LIGHT（淺色系）</li>
 *   <li><strong>頁首/頁尾：</strong>啟用或停用頁首和頁尾</li>
 * </ul>
 *
 * <p><strong>使用範例：</strong></p>
 * <pre>{@code
 * PdfReportConfig config = PdfReportConfig.builder()
 *     .logoPath("/path/to/company-logo.png")
 *     .reportTitle("ABC Corporation Security Report")
 *     .colorTheme(ColorTheme.DARK)
 *     .headerFooterEnabled(true)
 *     .build();
 * }</pre>
 *
 * <p><strong>Story 進度：</strong></p>
 * <ul>
 *   <li>Story 1.1: 基本配置結構（當前）</li>
 *   <li>Story 1.2: Logo 和標題用於封面頁、頁首</li>
 *   <li>Story 1.6: 與 SonarQube Settings API 整合</li>
 * </ul>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.1)
 */
public class PdfReportConfig {

    private final String logoPath;
    private final String reportTitle;
    private final ColorTheme colorTheme;
    private final boolean headerFooterEnabled;

    /**
     * 私有建構子，僅供 Builder 使用
     */
    private PdfReportConfig(Builder builder) {
        this.logoPath = builder.logoPath;
        this.reportTitle = builder.reportTitle;
        this.colorTheme = builder.colorTheme;
        this.headerFooterEnabled = builder.headerFooterEnabled;
    }

    /**
     * 建立 Builder 實例
     *
     * @return 新的 Builder 實例
     */
    public static Builder builder() {
        return new Builder();
    }

    // === Getters ===

    /**
     * 獲取公司標誌檔案路徑
     *
     * @return Logo 檔案路徑，可能為 null（表示無 Logo）
     */
    public String getLogoPath() {
        return logoPath;
    }

    /**
     * 獲取報表標題
     *
     * @return 報表標題，預設為 "OWASP Security Analysis Report"
     */
    public String getReportTitle() {
        return reportTitle;
    }

    /**
     * 獲取色彩主題
     *
     * @return 色彩主題，預設為 DEFAULT
     */
    public ColorTheme getColorTheme() {
        return colorTheme;
    }

    /**
     * 檢查是否啟用頁首/頁尾
     *
     * @return true 表示啟用，false 表示停用。預設為 true
     */
    public boolean isHeaderFooterEnabled() {
        return headerFooterEnabled;
    }

    /**
     * PDF 報表色彩主題列舉
     *
     * <p><strong>主題說明：</strong></p>
     * <ul>
     *   <li><strong>DEFAULT：</strong>專業藍色系（推薦用於商業報告）</li>
     *   <li><strong>DARK：</strong>深色高對比主題（適合夜間閱讀）</li>
     *   <li><strong>LIGHT：</strong>淺色柔和主題（適合列印）</li>
     * </ul>
     *
     * @since 2.0.0 (Story 1.1)
     */
    public enum ColorTheme {
        /**
         * 預設主題（藍色系）
         */
        DEFAULT,

        /**
         * 深色主題（高對比）
         */
        DARK,

        /**
         * 淺色主題（柔和）
         */
        LIGHT
    }

    /**
     * PDF 報表配置建構器
     *
     * <p>使用 Builder Pattern 提供流暢的 API 來建構配置物件。</p>
     *
     * @since 2.0.0 (Story 1.1)
     */
    public static class Builder {
        private String logoPath;
        private String reportTitle = "OWASP Security Analysis Report"; // 預設值
        private ColorTheme colorTheme = ColorTheme.DEFAULT; // 預設值
        private boolean headerFooterEnabled = true; // 預設值

        /**
         * 設定公司標誌檔案路徑
         *
         * @param logoPath Logo 檔案的絕對或相對路徑。
         *                 支援 PNG 和 JPG 格式，建議大小 200x100 像素，
         *                 最大檔案大小 500KB。
         * @return Builder 實例（用於鏈式呼叫）
         */
        public Builder logoPath(String logoPath) {
            this.logoPath = logoPath;
            return this;
        }

        /**
         * 設定報表標題
         *
         * @param reportTitle 自訂報表標題，最大長度 100 字元。
         *                    例如："ABC Corporation Security Analysis"
         * @return Builder 實例（用於鏈式呼叫）
         */
        public Builder reportTitle(String reportTitle) {
            if (reportTitle != null && reportTitle.length() > 100) {
                throw new IllegalArgumentException("Report title must not exceed 100 characters");
            }
            this.reportTitle = reportTitle;
            return this;
        }

        /**
         * 設定色彩主題
         *
         * @param colorTheme 色彩主題（DEFAULT、DARK 或 LIGHT）
         * @return Builder 實例（用於鏈式呼叫）
         */
        public Builder colorTheme(ColorTheme colorTheme) {
            this.colorTheme = colorTheme;
            return this;
        }

        /**
         * 設定是否啟用頁首/頁尾
         *
         * @param headerFooterEnabled true 表示啟用，false 表示停用
         * @return Builder 實例（用於鏈式呼叫）
         */
        public Builder headerFooterEnabled(boolean headerFooterEnabled) {
            this.headerFooterEnabled = headerFooterEnabled;
            return this;
        }

        /**
         * 建構 PdfReportConfig 實例
         *
         * @return 新的 PdfReportConfig 實例
         */
        public PdfReportConfig build() {
            return new PdfReportConfig(this);
        }
    }

    @Override
    public String toString() {
        return "PdfReportConfig{" +
                "logoPath='" + logoPath + '\'' +
                ", reportTitle='" + reportTitle + '\'' +
                ", colorTheme=" + colorTheme +
                ", headerFooterEnabled=" + headerFooterEnabled +
                '}';
    }
}
