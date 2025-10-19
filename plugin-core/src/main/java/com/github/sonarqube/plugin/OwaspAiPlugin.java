package com.github.sonarqube.plugin;

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

        // TODO: 註冊組件
        // context.addExtensions(
        //     OwaspRulesDefinition.class,
        //     OwaspQualityProfile.class,
        //     OwaspSensor.class
        // );
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
