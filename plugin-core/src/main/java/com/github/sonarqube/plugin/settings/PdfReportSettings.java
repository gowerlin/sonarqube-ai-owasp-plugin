package com.github.sonarqube.plugin.settings;

import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.Arrays;
import java.util.List;

/**
 * PDF Report Settings Property Definitions
 *
 * <p>定義 PDF 報表客製化設定選項，包含公司 Logo、報表標題、色彩主題、頁首頁尾啟用。</p>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.6)
 */
public class PdfReportSettings {

    // ==================== Property Keys ====================

    /**
     * 公司 Logo 檔案路徑
     * <p>儲存於 &lt;sonarqube-data&gt;/owasp-plugin/logo.png</p>
     */
    public static final String LOGO_PATH = "sonar.owasp.pdf.logo.path";

    /**
     * 報表標題
     * <p>預設：「OWASP Security Analysis Report」</p>
     * <p>最大長度：100 字元</p>
     */
    public static final String REPORT_TITLE = "sonar.owasp.pdf.report.title";

    /**
     * 色彩主題
     * <p>選項：Default, Dark, Light</p>
     */
    public static final String COLOR_THEME = "sonar.owasp.pdf.color.theme";

    /**
     * 啟用頁首/頁尾
     * <p>預設：true</p>
     */
    public static final String HEADER_FOOTER_ENABLED = "sonar.owasp.pdf.header.footer.enabled";

    /**
     * Logo 檔案最大大小 (bytes)
     * <p>500KB = 512000 bytes</p>
     */
    public static final long MAX_LOGO_FILE_SIZE = 512000L;

    /**
     * 報表標題最大長度
     */
    public static final int MAX_REPORT_TITLE_LENGTH = 100;

    // ==================== Default Values ====================

    /**
     * 預設報表標題
     */
    public static final String DEFAULT_REPORT_TITLE = "OWASP Security Analysis Report";

    /**
     * 預設色彩主題
     */
    public static final String DEFAULT_COLOR_THEME = "Default";

    /**
     * 預設頁首頁尾啟用狀態
     */
    public static final boolean DEFAULT_HEADER_FOOTER_ENABLED = true;

    // ==================== Color Theme Options ====================

    /**
     * 可用的色彩主題選項
     */
    public enum ColorTheme {
        DEFAULT("Default", "預設主題（藍色系）"),
        DARK("Dark", "深色主題（深灰色系）"),
        LIGHT("Light", "淺色主題（白色系）");

        private final String key;
        private final String description;

        ColorTheme(String key, String description) {
            this.key = key;
            this.description = description;
        }

        public String getKey() {
            return key;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 從字串取得色彩主題
         *
         * @param key 主題鍵值
         * @return 色彩主題列舉，若無效則回傳 DEFAULT
         */
        public static ColorTheme fromKey(String key) {
            if (key == null || key.isEmpty()) {
                return DEFAULT;
            }
            for (ColorTheme theme : values()) {
                if (theme.key.equalsIgnoreCase(key)) {
                    return theme;
                }
            }
            return DEFAULT;
        }
    }

    // ==================== Property Definitions ====================

    /**
     * 取得所有屬性定義清單
     *
     * @return 屬性定義清單
     */
    public static List<PropertyDefinition> getPropertyDefinitions() {
        return Arrays.asList(
                // Logo 檔案路徑
                PropertyDefinition.builder(LOGO_PATH)
                        .name("Company Logo Path")
                        .description("Path to company logo file (PNG/JPG, max 500KB). " +
                                "Stored in <sonarqube-data>/owasp-plugin/logo.png")
                        .category("OWASP AI Plugin")
                        .subCategory("PDF Report Settings")
                        .type(PropertyType.STRING)
                        .onQualifiers(Qualifiers.PROJECT)
                        .index(1)
                        .build(),

                // 報表標題
                PropertyDefinition.builder(REPORT_TITLE)
                        .name("Report Title")
                        .description("Custom title for PDF reports (max 100 characters)")
                        .category("OWASP AI Plugin")
                        .subCategory("PDF Report Settings")
                        .type(PropertyType.STRING)
                        .defaultValue(DEFAULT_REPORT_TITLE)
                        .onQualifiers(Qualifiers.PROJECT)
                        .index(2)
                        .build(),

                // 色彩主題
                PropertyDefinition.builder(COLOR_THEME)
                        .name("Color Theme")
                        .description("PDF report color theme")
                        .category("OWASP AI Plugin")
                        .subCategory("PDF Report Settings")
                        .type(PropertyType.SINGLE_SELECT_LIST)
                        .options("Default", "Dark", "Light")
                        .defaultValue(DEFAULT_COLOR_THEME)
                        .onQualifiers(Qualifiers.PROJECT)
                        .index(3)
                        .build(),

                // 啟用頁首頁尾
                PropertyDefinition.builder(HEADER_FOOTER_ENABLED)
                        .name("Enable Header/Footer")
                        .description("Show header and footer in PDF reports")
                        .category("OWASP AI Plugin")
                        .subCategory("PDF Report Settings")
                        .type(PropertyType.BOOLEAN)
                        .defaultValue(String.valueOf(DEFAULT_HEADER_FOOTER_ENABLED))
                        .onQualifiers(Qualifiers.PROJECT)
                        .index(4)
                        .build()
        );
    }

    /**
     * 驗證報表標題長度
     *
     * @param title 報表標題
     * @return 是否有效
     */
    public static boolean isValidReportTitle(String title) {
        return title != null && !title.trim().isEmpty() &&
                title.length() <= MAX_REPORT_TITLE_LENGTH;
    }

    /**
     * 驗證 Logo 檔案大小
     *
     * @param fileSizeBytes 檔案大小（位元組）
     * @return 是否有效
     */
    public static boolean isValidLogoFileSize(long fileSizeBytes) {
        return fileSizeBytes > 0 && fileSizeBytes <= MAX_LOGO_FILE_SIZE;
    }

    /**
     * 驗證 Logo 檔案副檔名
     *
     * @param filename 檔案名稱
     * @return 是否有效（PNG 或 JPG）
     */
    public static boolean isValidLogoFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return false;
        }
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".png") ||
                lowerFilename.endsWith(".jpg") ||
                lowerFilename.endsWith(".jpeg");
    }

    // 防止實例化
    private PdfReportSettings() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}
