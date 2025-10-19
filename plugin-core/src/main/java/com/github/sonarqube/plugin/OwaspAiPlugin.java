package com.github.sonarqube.plugin;

import com.github.sonarqube.plugin.api.PdfReportApiController;
import com.github.sonarqube.plugin.settings.PdfReportSettings;
import org.sonar.api.Plugin;

/**
 * OWASP AI Security Plugin for SonarQube
 *
 * 主插件類別，註冊所有擴展點和組件。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class OwaspAiPlugin implements Plugin {

    @Override
    public void define(Context context) {
        // 插件基本資訊已在 pom.xml 中定義

        // 註冊組件
        context.addExtensions(
            // 規則定義
            OwaspRulesDefinition.class,

            // 品質設定檔
            OwaspQualityProfile.class,

            // 掃描感測器
            OwaspSensor.class
        );

        // Story 1.6: 註冊 PDF 報表設定屬性定義
        context.addExtensions(PdfReportSettings.getPropertyDefinitions());

        // Story 1.6: 註冊 PDF 報表匯出 API 控制器
        context.addExtension(PdfReportApiController.class);
    }

    /**
     * 獲取插件版本
     *
     * @return 版本字串
     */
    public static String getVersion() {
        return "1.0.0-SNAPSHOT";
    }

    /**
     * 獲取插件名稱
     *
     * @return 插件名稱
     */
    public static String getPluginName() {
        return "OWASP AI Security Plugin";
    }

    /**
     * 獲取插件描述
     *
     * @return 插件描述
     */
    public static String getPluginDescription() {
        return "AI-powered security analysis plugin supporting OWASP Top 10 (2017, 2021, 2025)";
    }
}
