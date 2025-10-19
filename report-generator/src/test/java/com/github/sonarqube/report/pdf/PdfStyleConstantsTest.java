package com.github.sonarqube.report.pdf;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * PdfStyleConstants 單元測試
 *
 * <p>驗證 PdfStyleConstants 中所有樣式常數的正確性。</p>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.2)
 */
class PdfStyleConstantsTest {

    /**
     * 測試：所有字型名稱常數不為 null
     */
    @Test
    void shouldHaveValidFontNames() {
        assertThat(PdfStyleConstants.FONT_HELVETICA).isNotNull().isEqualTo("Helvetica");
        assertThat(PdfStyleConstants.FONT_HELVETICA_BOLD).isNotNull().isEqualTo("Helvetica-Bold");
        assertThat(PdfStyleConstants.FONT_COURIER).isNotNull().isEqualTo("Courier");
    }

    /**
     * 測試：所有字型大小常數符合規格
     */
    @Test
    void shouldHaveCorrectFontSizes() {
        // 封面頁字型
        assertThat(PdfStyleConstants.COVER_TITLE_SIZE).isEqualTo(24f);
        assertThat(PdfStyleConstants.COVER_SUBTITLE_SIZE).isEqualTo(18f);

        // 頁首字型
        assertThat(PdfStyleConstants.HEADER_TITLE_SIZE).isEqualTo(12f);
        assertThat(PdfStyleConstants.HEADER_PROJECT_SIZE).isEqualTo(10f);

        // 頁尾字型
        assertThat(PdfStyleConstants.FOOTER_TEXT_SIZE).isEqualTo(8f);
        assertThat(PdfStyleConstants.PAGE_NUMBER_SIZE).isEqualTo(9f);

        // 內文字型
        assertThat(PdfStyleConstants.BODY_TEXT_SIZE).isEqualTo(11f);
        assertThat(PdfStyleConstants.SECTION_TITLE_SIZE).isEqualTo(16f);
    }

    /**
     * 測試：所有顏色常數不為 null
     */
    @Test
    void shouldHaveValidColors() {
        assertThat(PdfStyleConstants.HEADER_TEXT_COLOR).isNotNull();
        assertThat(PdfStyleConstants.FOOTER_TEXT_COLOR).isNotNull();
        assertThat(PdfStyleConstants.COVER_BACKGROUND_COLOR).isNotNull();
        assertThat(PdfStyleConstants.BODY_TEXT_COLOR).isNotNull();
        assertThat(PdfStyleConstants.SECTION_TITLE_COLOR).isNotNull();

        // 主題顏色
        assertThat(PdfStyleConstants.THEME_PRIMARY_COLOR).isNotNull();
        assertThat(PdfStyleConstants.THEME_SECONDARY_COLOR).isNotNull();
        assertThat(PdfStyleConstants.THEME_WARNING_COLOR).isNotNull();
        assertThat(PdfStyleConstants.THEME_ERROR_COLOR).isNotNull();
    }

    /**
     * 測試：圖片尺寸常數符合規格
     */
    @Test
    void shouldHaveCorrectImageSizes() {
        // 封面頁 Logo
        assertThat(PdfStyleConstants.COVER_LOGO_MAX_WIDTH).isEqualTo(200);
        assertThat(PdfStyleConstants.COVER_LOGO_MAX_HEIGHT).isEqualTo(100);

        // 頁首 Logo
        assertThat(PdfStyleConstants.HEADER_LOGO_WIDTH).isEqualTo(50);
        assertThat(PdfStyleConstants.HEADER_LOGO_HEIGHT).isEqualTo(25);
    }

    /**
     * 測試：間距和邊距常數符合規格
     */
    @Test
    void shouldHaveCorrectSpacingValues() {
        assertThat(PdfStyleConstants.COVER_TOP_MARGIN).isEqualTo(100f);
        assertThat(PdfStyleConstants.SECTION_SPACING).isEqualTo(20f);
        assertThat(PdfStyleConstants.PARAGRAPH_SPACING).isEqualTo(10f);
        assertThat(PdfStyleConstants.HEADER_HEIGHT).isEqualTo(40f);
        assertThat(PdfStyleConstants.FOOTER_HEIGHT).isEqualTo(30f);
    }

    /**
     * 測試：線條粗細常數符合規格
     */
    @Test
    void shouldHaveCorrectLineThickness() {
        assertThat(PdfStyleConstants.HEADER_LINE_THICKNESS).isEqualTo(1f);
        assertThat(PdfStyleConstants.FOOTER_LINE_THICKNESS).isEqualTo(1f);
        assertThat(PdfStyleConstants.TABLE_BORDER_THICKNESS).isEqualTo(0.5f);
    }

    /**
     * 測試：PdfStyleConstants 是 Utility 類別，不可實例化
     */
    @Test
    void shouldNotBeInstantiable() {
        assertThatThrownBy(() -> {
            // 使用 Reflection 嘗試呼叫私有建構子
            java.lang.reflect.Constructor<PdfStyleConstants> constructor =
                    PdfStyleConstants.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        })
                .hasCauseInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Utility class cannot be instantiated");
    }
}
