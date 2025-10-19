package com.github.sonarqube.report.json;

import com.github.sonarqube.report.model.AnalysisReport;
import com.github.sonarqube.report.model.VersionComparisonReport;
import com.github.sonarqube.rules.OwaspVersionMappingService.CategoryMapping;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 版本對照 JSON 報告生成器
 *
 * 生成包含多版本對照、差異分析和類別映射的 JSON 報告。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.3.0 (Epic 5, Story 5.4)
 */
public class VersionComparisonJsonGenerator {

    private static final DateTimeFormatter ISO_FORMATTER =
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault());

    /**
     * 生成版本對照 JSON 報告
     *
     * @param comparisonReport 版本對照報告
     * @return JSON 字串
     */
    public String generate(VersionComparisonReport comparisonReport) {
        StringBuilder json = new StringBuilder();

        json.append("{\n");

        // Metadata
        appendMetadata(json, comparisonReport);
        json.append(",\n");

        // Versions
        appendVersions(json, comparisonReport);
        json.append(",\n");

        // Version Reports Summary
        appendVersionReports(json, comparisonReport);
        json.append(",\n");

        // Comparison
        appendComparison(json, comparisonReport);
        json.append(",\n");

        // Category Mappings
        appendCategoryMappings(json, comparisonReport);

        json.append("\n}");

        return json.toString();
    }

    private void appendMetadata(StringBuilder json, VersionComparisonReport report) {
        json.append("  \"metadata\": {\n");
        json.append("    \"projectKey\": \"").append(escapeJson(report.getProjectKey())).append("\",\n");
        json.append("    \"reportType\": \"version-comparison\",\n");
        json.append("    \"analysisTimestamp\": \"")
            .append(ISO_FORMATTER.format(Instant.ofEpochMilli(report.getAnalysisTimestamp())))
            .append("\",\n");
        json.append("    \"versionsCompared\": ").append(report.getOwaspVersions().size()).append("\n");
        json.append("  }");
    }

    private void appendVersions(StringBuilder json, VersionComparisonReport report) {
        json.append("  \"versions\": [");

        List<String> versions = report.getOwaspVersions();
        for (int i = 0; i < versions.size(); i++) {
            if (i > 0) json.append(", ");
            json.append("\"").append(versions.get(i)).append("\"");
        }

        json.append("]");
    }

    private void appendVersionReports(StringBuilder json, VersionComparisonReport report) {
        json.append("  \"versionReports\": {\n");

        Map<String, AnalysisReport> reports = report.getVersionReports();
        List<String> versions = report.getOwaspVersions();

        for (int i = 0; i < versions.size(); i++) {
            String version = versions.get(i);
            AnalysisReport versionReport = reports.get(version);

            if (i > 0) json.append(",\n");

            json.append("    \"").append(version).append("\": {\n");
            json.append("      \"owaspVersion\": \"").append(version).append("\",\n");
            json.append("      \"totalFindings\": ").append(versionReport.getTotalFindings()).append(",\n");
            json.append("      \"criticalCount\": ").append(versionReport.getSummary().getCriticalCount()).append(",\n");
            json.append("      \"highCount\": ").append(versionReport.getSummary().getHighCount()).append(",\n");
            json.append("      \"mediumCount\": ").append(versionReport.getSummary().getMediumCount()).append(",\n");
            json.append("      \"lowCount\": ").append(versionReport.getSummary().getLowCount()).append("\n");
            json.append("    }");
        }

        json.append("\n  }");
    }

    private void appendComparison(StringBuilder json, VersionComparisonReport report) {
        json.append("  \"comparison\": {\n");

        VersionComparisonReport.DifferenceAnalysis diff = report.getDifferenceAnalysis();

        // Added Findings
        json.append("    \"addedFindings\": ");
        appendMap(json, diff.getAddedFindings());
        json.append(",\n");

        // Removed Findings
        json.append("    \"removedFindings\": ");
        appendMap(json, diff.getRemovedFindings());
        json.append(",\n");

        // Changed Findings
        json.append("    \"changedFindings\": ");
        appendMap(json, diff.getChangedFindings());
        json.append(",\n");

        // Compliance Change Percent
        json.append("    \"complianceChangePercent\": ");
        appendDoubleMap(json, diff.getComplianceChangePercent());
        json.append(",\n");

        // Migration Recommendations
        json.append("    \"migrationRecommendations\": [\n");
        List<String> recommendations = diff.getMigrationRecommendations();
        for (int i = 0; i < recommendations.size(); i++) {
            if (i > 0) json.append(",\n");
            json.append("      \"").append(escapeJson(recommendations.get(i))).append("\"");
        }
        json.append("\n    ]\n");

        json.append("  }");
    }

    private void appendCategoryMappings(StringBuilder json, VersionComparisonReport report) {
        json.append("  \"categoryMappings\": [\n");

        List<CategoryMapping> mappings = report.getCategoryMappings();
        for (int i = 0; i < mappings.size(); i++) {
            CategoryMapping mapping = mappings.get(i);

            if (i > 0) json.append(",\n");

            json.append("    {\n");
            json.append("      \"sourceVersion\": \"").append(mapping.getSourceVersion()).append("\",\n");
            json.append("      \"sourceCategory\": \"").append(mapping.getSourceCategory()).append("\",\n");
            json.append("      \"sourceName\": \"").append(escapeJson(mapping.getSourceName())).append("\",\n");

            if (mapping.getTargetVersion() != null) {
                json.append("      \"targetVersion\": \"").append(mapping.getTargetVersion()).append("\",\n");
                json.append("      \"targetCategory\": \"").append(mapping.getTargetCategory()).append("\",\n");
                json.append("      \"targetName\": \"").append(escapeJson(mapping.getTargetName())).append("\",\n");
            } else {
                json.append("      \"targetVersion\": null,\n");
                json.append("      \"targetCategory\": null,\n");
                json.append("      \"targetName\": null,\n");
            }

            json.append("      \"mappingType\": \"").append(mapping.getMappingType().name()).append("\",\n");
            json.append("      \"explanation\": \"").append(escapeJson(mapping.getExplanation())).append("\"\n");
            json.append("    }");
        }

        json.append("\n  ]");
    }

    private void appendMap(StringBuilder json, Map<String, Integer> map) {
        json.append("{");
        int index = 0;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            if (index > 0) json.append(", ");
            json.append("\"").append(entry.getKey()).append("\": ").append(entry.getValue());
            index++;
        }
        json.append("}");
    }

    private void appendDoubleMap(StringBuilder json, Map<String, Double> map) {
        json.append("{");
        int index = 0;
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            if (index > 0) json.append(", ");
            json.append("\"").append(entry.getKey()).append("\": ").append(String.format("%.2f", entry.getValue()));
            index++;
        }
        json.append("}");
    }

    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }

        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f");
    }
}
