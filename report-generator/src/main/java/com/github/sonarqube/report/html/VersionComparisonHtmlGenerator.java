package com.github.sonarqube.report.html;

import com.github.sonarqube.report.model.AnalysisReport;
import com.github.sonarqube.report.model.VersionComparisonReport;
import com.github.sonarqube.rules.OwaspVersionMappingService.CategoryMapping;
import com.github.sonarqube.rules.OwaspVersionMappingService.MappingType;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 版本對照 HTML 報告生成器
 *
 * 生成包含多版本並排對照、差異高亮顯示和類別映射的 HTML 報告。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.3.0 (Epic 5, Story 5.4)
 */
public class VersionComparisonHtmlGenerator {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

    /**
     * 生成版本對照 HTML 報告
     *
     * @param comparisonReport 版本對照報告
     * @return HTML 字串
     */
    public String generate(VersionComparisonReport comparisonReport) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>\n<html lang=\"zh-TW\">\n<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>OWASP 版本對照報告 - ").append(escapeHtml(comparisonReport.getProjectKey())).append("</title>\n");
        appendStyles(html);
        html.append("</head>\n<body>\n");

        // Header
        appendHeader(html, comparisonReport);

        // Version Summary Table
        appendVersionSummary(html, comparisonReport);

        // Difference Analysis
        appendDifferenceAnalysis(html, comparisonReport);

        // Category Mappings
        appendCategoryMappings(html, comparisonReport);

        // Migration Recommendations
        appendMigrationRecommendations(html, comparisonReport);

        html.append("</body>\n</html>");

        return html.toString();
    }

    private void appendStyles(StringBuilder html) {
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }\n");
        html.append("    .container { max-width: 1200px; margin: 0 auto; background: white; padding: 30px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("    h1 { color: #333; border-bottom: 3px solid #007bff; padding-bottom: 10px; }\n");
        html.append("    h2 { color: #555; margin-top: 30px; border-left: 4px solid #007bff; padding-left: 10px; }\n");
        html.append("    table { width: 100%; border-collapse: collapse; margin: 20px 0; }\n");
        html.append("    th, td { padding: 12px; text-align: left; border: 1px solid #ddd; }\n");
        html.append("    th { background: #007bff; color: white; font-weight: bold; }\n");
        html.append("    tr:nth-child(even) { background: #f9f9f9; }\n");
        html.append("    .added { background: #d4edda; color: #155724; }\n");
        html.append("    .removed { background: #f8d7da; color: #721c24; }\n");
        html.append("    .changed { background: #fff3cd; color: #856404; }\n");
        html.append("    .mapping-direct { color: #28a745; font-weight: bold; }\n");
        html.append("    .mapping-merged { color: #ffc107; font-weight: bold; }\n");
        html.append("    .mapping-new { color: #17a2b8; font-weight: bold; }\n");
        html.append("    .recommendation { background: #e7f3ff; border-left: 4px solid #007bff; padding: 10px; margin: 10px 0; }\n");
        html.append("    .metric { display: inline-block; margin: 10px 20px 10px 0; }\n");
        html.append("    .metric-label { font-weight: bold; color: #666; }\n");
        html.append("    .metric-value { font-size: 1.5em; color: #007bff; }\n");
        html.append("  </style>\n");
    }

    private void appendHeader(StringBuilder html, VersionComparisonReport report) {
        html.append("  <div class=\"container\">\n");
        html.append("    <h1>OWASP 版本對照報告</h1>\n");
        html.append("    <p><strong>專案：</strong>").append(escapeHtml(report.getProjectKey())).append("</p>\n");
        html.append("    <p><strong>分析時間：</strong>")
            .append(FORMATTER.format(Instant.ofEpochMilli(report.getAnalysisTimestamp()))).append("</p>\n");
        html.append("    <p><strong>比較版本：</strong>");

        List<String> versions = report.getOwaspVersions();
        for (int i = 0; i < versions.size(); i++) {
            if (i > 0) html.append(" vs ");
            html.append("<strong>OWASP ").append(versions.get(i)).append("</strong>");
        }
        html.append("</p>\n");
    }

    private void appendVersionSummary(StringBuilder html, VersionComparisonReport report) {
        html.append("    <h2>版本摘要對照</h2>\n");
        html.append("    <table>\n");
        html.append("      <thead>\n        <tr>\n");
        html.append("          <th>版本</th>\n");
        html.append("          <th>總發現數</th>\n");
        html.append("          <th>CRITICAL</th>\n");
        html.append("          <th>HIGH</th>\n");
        html.append("          <th>MEDIUM</th>\n");
        html.append("          <th>LOW</th>\n");
        html.append("        </tr>\n      </thead>\n      <tbody>\n");

        for (String version : report.getOwaspVersions()) {
            AnalysisReport versionReport = report.getVersionReports().get(version);

            html.append("        <tr>\n");
            html.append("          <td><strong>OWASP ").append(version).append("</strong></td>\n");
            html.append("          <td>").append(versionReport.getTotalFindings()).append("</td>\n");
            html.append("          <td>").append(versionReport.getSummary().getCriticalCount()).append("</td>\n");
            html.append("          <td>").append(versionReport.getSummary().getHighCount()).append("</td>\n");
            html.append("          <td>").append(versionReport.getSummary().getMediumCount()).append("</td>\n");
            html.append("          <td>").append(versionReport.getSummary().getLowCount()).append("</td>\n");
            html.append("        </tr>\n");
        }

        html.append("      </tbody>\n    </table>\n");
    }

    private void appendDifferenceAnalysis(StringBuilder html, VersionComparisonReport report) {
        html.append("    <h2>差異分析</h2>\n");

        VersionComparisonReport.DifferenceAnalysis diff = report.getDifferenceAnalysis();

        // Metrics
        for (Map.Entry<String, Integer> entry : diff.getAddedFindings().entrySet()) {
            html.append("    <div class=\"metric added\">\n");
            html.append("      <span class=\"metric-label\">新增發現 (").append(entry.getKey()).append(")：</span>\n");
            html.append("      <span class=\"metric-value\">").append(entry.getValue()).append("</span>\n");
            html.append("    </div>\n");
        }

        for (Map.Entry<String, Integer> entry : diff.getRemovedFindings().entrySet()) {
            html.append("    <div class=\"metric removed\">\n");
            html.append("      <span class=\"metric-label\">移除發現 (").append(entry.getKey()).append(")：</span>\n");
            html.append("      <span class=\"metric-value\">").append(entry.getValue()).append("</span>\n");
            html.append("    </div>\n");
        }

        for (Map.Entry<String, Integer> entry : diff.getChangedFindings().entrySet()) {
            html.append("    <div class=\"metric changed\">\n");
            html.append("      <span class=\"metric-label\">變更分類 (").append(entry.getKey()).append(")：</span>\n");
            html.append("      <span class=\"metric-value\">").append(entry.getValue()).append("</span>\n");
            html.append("    </div>\n");
        }

        for (Map.Entry<String, Double> entry : diff.getComplianceChangePercent().entrySet()) {
            html.append("    <div class=\"metric\">\n");
            html.append("      <span class=\"metric-label\">合規性變化 (").append(entry.getKey()).append(")：</span>\n");
            html.append("      <span class=\"metric-value\">").append(String.format("%.2f", entry.getValue())).append("%</span>\n");
            html.append("    </div>\n");
        }
    }

    private void appendCategoryMappings(StringBuilder html, VersionComparisonReport report) {
        html.append("    <h2>類別映射關係</h2>\n");
        html.append("    <table>\n");
        html.append("      <thead>\n        <tr>\n");
        html.append("          <th>來源版本</th>\n");
        html.append("          <th>來源類別</th>\n");
        html.append("          <th>目標版本</th>\n");
        html.append("          <th>目標類別</th>\n");
        html.append("          <th>映射類型</th>\n");
        html.append("          <th>說明</th>\n");
        html.append("        </tr>\n      </thead>\n      <tbody>\n");

        for (CategoryMapping mapping : report.getCategoryMappings()) {
            html.append("        <tr>\n");
            html.append("          <td>OWASP ").append(mapping.getSourceVersion()).append("</td>\n");
            html.append("          <td><strong>").append(mapping.getSourceCategory()).append("</strong><br>");
            html.append("<small>").append(escapeHtml(mapping.getSourceName())).append("</small></td>\n");

            if (mapping.getTargetVersion() != null) {
                html.append("          <td>OWASP ").append(mapping.getTargetVersion()).append("</td>\n");
                html.append("          <td><strong>").append(mapping.getTargetCategory()).append("</strong><br>");
                html.append("<small>").append(escapeHtml(mapping.getTargetName())).append("</small></td>\n");
            } else {
                html.append("          <td colspan=\"2\" class=\"mapping-new\">新增類別</td>\n");
            }

            String cssClass = getMappingCssClass(mapping.getMappingType());
            html.append("          <td class=\"").append(cssClass).append("\">")
                .append(mapping.getMappingType().getZhDescription()).append("</td>\n");
            html.append("          <td>").append(escapeHtml(mapping.getExplanation())).append("</td>\n");
            html.append("        </tr>\n");
        }

        html.append("      </tbody>\n    </table>\n");
    }

    private void appendMigrationRecommendations(StringBuilder html, VersionComparisonReport report) {
        List<String> recommendations = report.getDifferenceAnalysis().getMigrationRecommendations();

        if (recommendations.isEmpty()) {
            return;
        }

        html.append("    <h2>遷移建議</h2>\n");

        for (String recommendation : recommendations) {
            html.append("    <div class=\"recommendation\">\n");
            html.append("      ").append(escapeHtml(recommendation)).append("\n");
            html.append("    </div>\n");
        }

        html.append("  </div>\n");
    }

    private String getMappingCssClass(MappingType type) {
        switch (type) {
            case DIRECT: return "mapping-direct";
            case MERGED: return "mapping-merged";
            case NEW: return "mapping-new";
            default: return "";
        }
    }

    private String escapeHtml(String str) {
        if (str == null) {
            return "";
        }

        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
