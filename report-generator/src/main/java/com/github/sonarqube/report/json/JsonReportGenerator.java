package com.github.sonarqube.report.json;

import com.github.sonarqube.report.ReportGenerator;
import com.github.sonarqube.report.model.AnalysisReport;
import com.github.sonarqube.report.model.ReportSummary;
import com.github.sonarqube.report.model.SecurityFinding;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * JSON 格式報告生成器
 *
 * <p>生成結構化的 JSON 格式安全分析報告，支援 API 整合和自動化處理。</p>
 *
 * <p><strong>功能特性：</strong></p>
 * <ul>
 *   <li>完整的結構化資料，易於程式解析</li>
 *   <li>支援 RESTful API 整合</li>
 *   <li>適用於 CI/CD 自動化流程</li>
 *   <li>JSON 格式符合 RFC 8259 標準</li>
 *   <li>所有字串正確轉義，避免注入攻擊</li>
 * </ul>
 *
 * <p><strong>JSON 結構：</strong></p>
 * <pre>
 * {
 *   "metadata": { ... },
 *   "summary": { ... },
 *   "findings": [ ... ]
 * }
 * </pre>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 5.3 - Epic 5)
 */
public class JsonReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter ISO_FORMATTER =
        DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public String generate(AnalysisReport report) {
        StringBuilder json = new StringBuilder();

        json.append("{\n");

        // Metadata 區塊
        appendMetadata(json, report);
        json.append(",\n");

        // Summary 區塊
        appendSummary(json, report.getSummary());
        json.append(",\n");

        // Findings 區塊
        appendFindings(json, report.getFindings());

        json.append("\n}");

        return json.toString();
    }

    private void appendMetadata(StringBuilder json, AnalysisReport report) {
        json.append("  \"metadata\": {\n");
        json.append("    \"projectName\": ").append(escapeJson(report.getProjectName())).append(",\n");
        json.append("    \"owaspVersion\": ").append(escapeJson(report.getOwaspVersion())).append(",\n");
        json.append("    \"analysisTime\": ").append(escapeJson(report.getAnalysisTime().format(ISO_FORMATTER))).append(",\n");
        json.append("    \"generatedBy\": \"SonarQube AI OWASP Plugin\"");

        if (report.getAiModel() != null) {
            json.append(",\n");
            json.append("    \"aiModel\": ").append(escapeJson(report.getAiModel()));
        }

        json.append("\n  }");
    }

    private void appendSummary(StringBuilder json, ReportSummary summary) {
        json.append("  \"summary\": {\n");
        json.append("    \"totalFindings\": ").append(summary.getTotalFindings()).append(",\n");
        json.append("    \"blockerCount\": ").append(summary.getBlockerCount()).append(",\n");
        json.append("    \"criticalCount\": ").append(summary.getCriticalCount()).append(",\n");
        json.append("    \"majorCount\": ").append(summary.getMajorCount()).append(",\n");
        json.append("    \"minorCount\": ").append(summary.getMinorCount()).append(",\n");
        json.append("    \"infoCount\": ").append(summary.getInfoCount()).append(",\n");
        json.append("    \"filesAnalyzed\": ").append(summary.getFilesAnalyzed()).append(",\n");

        // 嚴重性分布
        json.append("    \"severityCounts\": {\n");
        appendMapAsJson(json, summary.getSeverityCounts(), 6);
        json.append("    },\n");

        // OWASP 分類分布
        json.append("    \"categoryCounts\": {\n");
        appendMapAsJson(json, summary.getCategoryCounts(), 6);
        json.append("    }\n");

        json.append("  }");
    }

    private void appendFindings(StringBuilder json, List<SecurityFinding> findings) {
        json.append("  \"findings\": [\n");

        if (findings.isEmpty()) {
            json.append("  ]");
            return;
        }

        for (int i = 0; i < findings.size(); i++) {
            SecurityFinding finding = findings.get(i);
            appendFinding(json, finding, 4);

            if (i < findings.size() - 1) {
                json.append(",\n");
            } else {
                json.append("\n");
            }
        }

        json.append("  ]");
    }

    private void appendFinding(StringBuilder json, SecurityFinding finding, int indent) {
        String indentStr = " ".repeat(indent);

        json.append(indentStr).append("{\n");

        // 基本資訊
        json.append(indentStr).append("  \"ruleName\": ").append(escapeJson(finding.getRuleName())).append(",\n");
        json.append(indentStr).append("  \"ruleKey\": ").append(escapeJson(finding.getRuleKey())).append(",\n");
        json.append(indentStr).append("  \"severity\": ").append(escapeJson(finding.getSeverity())).append(",\n");
        json.append(indentStr).append("  \"owaspCategory\": ").append(escapeJson(finding.getOwaspCategory())).append(",\n");

        // CWE IDs
        json.append(indentStr).append("  \"cweIds\": [\n");
        List<String> cweIds = finding.getCweIds();
        for (int i = 0; i < cweIds.size(); i++) {
            json.append(indentStr).append("    ").append(escapeJson(cweIds.get(i)));
            if (i < cweIds.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }
        json.append(indentStr).append("  ],\n");

        // 檔案位置
        json.append(indentStr).append("  \"filePath\": ").append(escapeJson(finding.getFilePath())).append(",\n");

        if (finding.getLineNumber() != null) {
            json.append(indentStr).append("  \"lineNumber\": ").append(finding.getLineNumber()).append(",\n");
        }

        // 描述
        if (finding.getDescription() != null) {
            json.append(indentStr).append("  \"description\": ").append(escapeJson(finding.getDescription())).append(",\n");
        }

        // 代碼片段
        if (finding.getCodeSnippet() != null) {
            json.append(indentStr).append("  \"codeSnippet\": ").append(escapeJson(finding.getCodeSnippet())).append(",\n");
        }

        // 修復建議（最後一個欄位，無逗號）
        if (finding.getFixSuggestion() != null) {
            json.append(indentStr).append("  \"fixSuggestion\": ").append(escapeJson(finding.getFixSuggestion())).append("\n");
        } else {
            // 移除最後一個逗號
            int lastComma = json.lastIndexOf(",");
            if (lastComma > 0 && json.substring(lastComma).trim().equals(",")) {
                json.deleteCharAt(lastComma);
                json.append("\n");
            }
        }

        json.append(indentStr).append("}");
    }

    private void appendMapAsJson(StringBuilder json, Map<String, Integer> map, int indent) {
        String indentStr = " ".repeat(indent);

        if (map.isEmpty()) {
            return;
        }

        List<Map.Entry<String, Integer>> entries = map.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .toList();

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            json.append(indentStr).append(escapeJson(entry.getKey())).append(": ").append(entry.getValue());

            if (i < entries.size() - 1) {
                json.append(",\n");
            } else {
                json.append("\n");
            }
        }
    }

    /**
     * 轉義 JSON 字串
     *
     * <p>根據 RFC 8259 規範轉義特殊字元：</p>
     * <ul>
     *   <li>\" (quotation mark)</li>
     *   <li>\\ (reverse solidus)</li>
     *   <li>\/ (solidus)</li>
     *   <li>\b (backspace)</li>
     *   <li>\f (form feed)</li>
     *   <li>\n (line feed)</li>
     *   <li>\r (carriage return)</li>
     *   <li>\t (tab)</li>
     *   <li>\uXXXX (unicode)</li>
     * </ul>
     *
     * @param text 原始字串
     * @return 轉義後的 JSON 字串（包含引號）
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "null";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\"");

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            switch (c) {
                case '\"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '/':
                    sb.append("\\/");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    // 處理控制字元 (0x00-0x1F)
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                    break;
            }
        }

        sb.append("\"");
        return sb.toString();
    }

    @Override
    public String getFormat() {
        return "json";
    }

    @Override
    public String getFileExtension() {
        return ".json";
    }
}
