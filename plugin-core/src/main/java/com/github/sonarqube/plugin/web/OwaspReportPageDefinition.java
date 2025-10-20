package com.github.sonarqube.plugin.web;

import org.sonar.api.web.page.Context;
import org.sonar.api.web.page.Page;
import org.sonar.api.web.page.PageDefinition;

/**
 * OWASP 報告查看頁面定義
 *
 * 註冊 OWASP Security Report 頁面到 SonarQube Web UI，
 * 整合至專案級別導航選單，提供互動式報告查看功能。
 *
 * 功能特性：
 * - 整合 SonarQube Web Extension 框架
 * - 過濾功能（嚴重性、OWASP 類別、檔案路徑）
 * - 全文搜尋與高亮顯示
 * - 詳情查看（代碼片段、修復建議）
 * - 響應式設計（桌面/平板/手機）
 *
 * @since 2.8.0 (Epic 5, Story 5.6)
 * @author SonarQube AI OWASP Plugin Team
 */
public class OwaspReportPageDefinition implements PageDefinition {

    @Override
    public void define(Context context) {
        // 定義 OWASP 報告查看頁面
        context
            .addPage(Page.builder("owasp-security/report")
                .setName("OWASP Security Report")
                .setScope(Page.Scope.COMPONENT)
                .build());
    }
}
