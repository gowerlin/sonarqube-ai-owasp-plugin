package com.github.sonarqube.report;

import com.github.sonarqube.report.model.AnalysisReport;

/**
 * 報告生成器介面
 *
 * 定義報告生成的統一介面，支援多種格式輸出。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public interface ReportGenerator {

    /**
     * 生成報告
     *
     * @param report 分析報告數據
     * @return 生成的報告內容
     */
    String generate(AnalysisReport report);

    /**
     * 獲取報告格式
     *
     * @return 格式名稱（如 "markdown", "html", "json"）
     */
    String getFormat();

    /**
     * 獲取檔案副檔名
     *
     * @return 副檔名（如 ".md", ".html", ".json"）
     */
    String getFileExtension();
}
