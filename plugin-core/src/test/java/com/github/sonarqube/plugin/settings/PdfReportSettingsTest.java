package com.github.sonarqube.plugin.settings;

import org.junit.jupiter.api.Test;
import org.sonar.api.config.PropertyDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * PdfReportSettings 單元測試
 *
 * <p>測試 PDF 報表設定屬性定義、驗證邏輯。</p>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.6)
 */
class PdfReportSettingsTest {

    /**
     * 測試：屬性定義清單不為空
     */
    @Test
    void shouldReturnNonEmptyPropertyDefinitions() {
        // When
        List<PropertyDefinition> definitions = PdfReportSettings.getPropertyDefinitions();

        // Then
        assertThat(definitions).isNotNull();
        assertThat(definitions).hasSize(4);  // Logo path, report title, color theme, header/footer enabled
    }

    /**
     * 測試：Logo 路徑屬性定義存在
     */
    @Test
    void shouldDefineLogoPathProperty() {
        // When
        List<PropertyDefinition> definitions = PdfReportSettings.getPropertyDefinitions();

        // Then
        assertThat(definitions).anyMatch(def -> def.key().equals(PdfReportSettings.LOGO_PATH));
        assertThat(definitions).anyMatch(def ->
                def.key().equals(PdfReportSettings.LOGO_PATH) &&
                        def.name().equals("Company Logo Path")
        );
    }

    /**
     * 測試：報表標題屬性定義存在且有預設值
     */
    @Test
    void shouldDefineReportTitlePropertyWithDefaultValue() {
        // When
        List<PropertyDefinition> definitions = PdfReportSettings.getPropertyDefinitions();

        // Then
        PropertyDefinition titleDef = definitions.stream()
                .filter(def -> def.key().equals(PdfReportSettings.REPORT_TITLE))
                .findFirst()
                .orElse(null);

        assertThat(titleDef).isNotNull();
        assertThat(titleDef.name()).isEqualTo("Report Title");
        assertThat(titleDef.defaultValue()).isEqualTo(PdfReportSettings.DEFAULT_REPORT_TITLE);
    }

    /**
     * 測試：色彩主題屬性定義存在且有選項
     */
    @Test
    void shouldDefineColorThemePropertyWithOptions() {
        // When
        List<PropertyDefinition> definitions = PdfReportSettings.getPropertyDefinitions();

        // Then
        PropertyDefinition themeDef = definitions.stream()
                .filter(def -> def.key().equals(PdfReportSettings.COLOR_THEME))
                .findFirst()
                .orElse(null);

        assertThat(themeDef).isNotNull();
        assertThat(themeDef.name()).isEqualTo("Color Theme");
        assertThat(themeDef.defaultValue()).isEqualTo(PdfReportSettings.DEFAULT_COLOR_THEME);
        assertThat(themeDef.options()).contains("Default", "Dark", "Light");
    }

    /**
     * 測試：頁首頁尾啟用屬性定義存在且為布林類型
     */
    @Test
    void shouldDefineHeaderFooterEnabledPropertyAsBoolean() {
        // When
        List<PropertyDefinition> definitions = PdfReportSettings.getPropertyDefinitions();

        // Then
        PropertyDefinition headerFooterDef = definitions.stream()
                .filter(def -> def.key().equals(PdfReportSettings.HEADER_FOOTER_ENABLED))
                .findFirst()
                .orElse(null);

        assertThat(headerFooterDef).isNotNull();
        assertThat(headerFooterDef.name()).isEqualTo("Enable Header/Footer");
        assertThat(headerFooterDef.defaultValue()).isEqualTo("true");
    }

    /**
     * 測試：有效報表標題驗證
     */
    @Test
    void shouldValidateReportTitleCorrectly() {
        // Valid titles
        assertThat(PdfReportSettings.isValidReportTitle("OWASP Security Report")).isTrue();
        assertThat(PdfReportSettings.isValidReportTitle("Test")).isTrue();
        assertThat(PdfReportSettings.isValidReportTitle("A".repeat(100))).isTrue();

        // Invalid titles
        assertThat(PdfReportSettings.isValidReportTitle(null)).isFalse();
        assertThat(PdfReportSettings.isValidReportTitle("")).isFalse();
        assertThat(PdfReportSettings.isValidReportTitle("   ")).isFalse();
        assertThat(PdfReportSettings.isValidReportTitle("A".repeat(101))).isFalse();
    }

    /**
     * 測試：有效 Logo 檔案大小驗證
     */
    @Test
    void shouldValidateLogoFileSizeCorrectly() {
        // Valid sizes
        assertThat(PdfReportSettings.isValidLogoFileSize(1L)).isTrue();
        assertThat(PdfReportSettings.isValidLogoFileSize(256000L)).isTrue();  // 250KB
        assertThat(PdfReportSettings.isValidLogoFileSize(512000L)).isTrue();  // 500KB (max)

        // Invalid sizes
        assertThat(PdfReportSettings.isValidLogoFileSize(0L)).isFalse();      // 0 bytes
        assertThat(PdfReportSettings.isValidLogoFileSize(-1L)).isFalse();     // Negative
        assertThat(PdfReportSettings.isValidLogoFileSize(512001L)).isFalse(); // Over limit
        assertThat(PdfReportSettings.isValidLogoFileSize(1024000L)).isFalse(); // 1MB
    }

    /**
     * 測試：有效 Logo 檔案副檔名驗證
     */
    @Test
    void shouldValidateLogoFileExtensionCorrectly() {
        // Valid extensions
        assertThat(PdfReportSettings.isValidLogoFileExtension("logo.png")).isTrue();
        assertThat(PdfReportSettings.isValidLogoFileExtension("logo.jpg")).isTrue();
        assertThat(PdfReportSettings.isValidLogoFileExtension("logo.jpeg")).isTrue();
        assertThat(PdfReportSettings.isValidLogoFileExtension("LOGO.PNG")).isTrue();  // Case insensitive
        assertThat(PdfReportSettings.isValidLogoFileExtension("company-logo.jpg")).isTrue();

        // Invalid extensions
        assertThat(PdfReportSettings.isValidLogoFileExtension(null)).isFalse();
        assertThat(PdfReportSettings.isValidLogoFileExtension("")).isFalse();
        assertThat(PdfReportSettings.isValidLogoFileExtension("   ")).isFalse();
        assertThat(PdfReportSettings.isValidLogoFileExtension("logo.gif")).isFalse();
        assertThat(PdfReportSettings.isValidLogoFileExtension("logo.bmp")).isFalse();
        assertThat(PdfReportSettings.isValidLogoFileExtension("logo.svg")).isFalse();
        assertThat(PdfReportSettings.isValidLogoFileExtension("logo.pdf")).isFalse();
        assertThat(PdfReportSettings.isValidLogoFileExtension("logo")).isFalse();  // No extension
    }

    /**
     * 測試：ColorTheme 列舉正確解析
     */
    @Test
    void shouldParseColorThemeFromKeyCorrectly() {
        // Valid keys
        assertThat(PdfReportSettings.ColorTheme.fromKey("Default"))
                .isEqualTo(PdfReportSettings.ColorTheme.DEFAULT);
        assertThat(PdfReportSettings.ColorTheme.fromKey("Dark"))
                .isEqualTo(PdfReportSettings.ColorTheme.DARK);
        assertThat(PdfReportSettings.ColorTheme.fromKey("Light"))
                .isEqualTo(PdfReportSettings.ColorTheme.LIGHT);

        // Case insensitive
        assertThat(PdfReportSettings.ColorTheme.fromKey("default"))
                .isEqualTo(PdfReportSettings.ColorTheme.DEFAULT);
        assertThat(PdfReportSettings.ColorTheme.fromKey("DARK"))
                .isEqualTo(PdfReportSettings.ColorTheme.DARK);

        // Invalid keys (fallback to DEFAULT)
        assertThat(PdfReportSettings.ColorTheme.fromKey(null))
                .isEqualTo(PdfReportSettings.ColorTheme.DEFAULT);
        assertThat(PdfReportSettings.ColorTheme.fromKey(""))
                .isEqualTo(PdfReportSettings.ColorTheme.DEFAULT);
        assertThat(PdfReportSettings.ColorTheme.fromKey("InvalidTheme"))
                .isEqualTo(PdfReportSettings.ColorTheme.DEFAULT);
    }

    /**
     * 測試：ColorTheme 列舉屬性正確
     */
    @Test
    void shouldHaveCorrectColorThemeProperties() {
        // DEFAULT theme
        assertThat(PdfReportSettings.ColorTheme.DEFAULT.getKey()).isEqualTo("Default");
        assertThat(PdfReportSettings.ColorTheme.DEFAULT.getDescription()).contains("預設主題");

        // DARK theme
        assertThat(PdfReportSettings.ColorTheme.DARK.getKey()).isEqualTo("Dark");
        assertThat(PdfReportSettings.ColorTheme.DARK.getDescription()).contains("深色主題");

        // LIGHT theme
        assertThat(PdfReportSettings.ColorTheme.LIGHT.getKey()).isEqualTo("Light");
        assertThat(PdfReportSettings.ColorTheme.LIGHT.getDescription()).contains("淺色主題");
    }

    /**
     * 測試：所有屬性定義屬於同一 category
     */
    @Test
    void shouldHaveAllPropertiesInSameCategory() {
        // When
        List<PropertyDefinition> definitions = PdfReportSettings.getPropertyDefinitions();

        // Then
        assertThat(definitions).allMatch(def -> def.category().equals("OWASP AI Plugin"));
        assertThat(definitions).allMatch(def -> def.subCategory().equals("PDF Report Settings"));
    }

    /**
     * 測試：屬性定義索引排序正確
     */
    @Test
    void shouldHavePropertiesIndexedCorrectly() {
        // When
        List<PropertyDefinition> definitions = PdfReportSettings.getPropertyDefinitions();

        // Then
        PropertyDefinition logoDef = definitions.stream()
                .filter(def -> def.key().equals(PdfReportSettings.LOGO_PATH))
                .findFirst()
                .orElse(null);
        assertThat(logoDef).isNotNull();
        assertThat(logoDef.index()).isEqualTo(1);

        PropertyDefinition titleDef = definitions.stream()
                .filter(def -> def.key().equals(PdfReportSettings.REPORT_TITLE))
                .findFirst()
                .orElse(null);
        assertThat(titleDef).isNotNull();
        assertThat(titleDef.index()).isEqualTo(2);

        PropertyDefinition themeDef = definitions.stream()
                .filter(def -> def.key().equals(PdfReportSettings.COLOR_THEME))
                .findFirst()
                .orElse(null);
        assertThat(themeDef).isNotNull();
        assertThat(themeDef.index()).isEqualTo(3);

        PropertyDefinition headerFooterDef = definitions.stream()
                .filter(def -> def.key().equals(PdfReportSettings.HEADER_FOOTER_ENABLED))
                .findFirst()
                .orElse(null);
        assertThat(headerFooterDef).isNotNull();
        assertThat(headerFooterDef.index()).isEqualTo(4);
    }

    /**
     * 測試：常數值正確
     */
    @Test
    void shouldHaveCorrectConstantValues() {
        assertThat(PdfReportSettings.MAX_LOGO_FILE_SIZE).isEqualTo(512000L);
        assertThat(PdfReportSettings.MAX_REPORT_TITLE_LENGTH).isEqualTo(100);
        assertThat(PdfReportSettings.DEFAULT_REPORT_TITLE).isEqualTo("OWASP Security Analysis Report");
        assertThat(PdfReportSettings.DEFAULT_COLOR_THEME).isEqualTo("Default");
        assertThat(PdfReportSettings.DEFAULT_HEADER_FOOTER_ENABLED).isTrue();
    }
}
