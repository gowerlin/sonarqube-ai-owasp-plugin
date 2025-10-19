package com.github.sonarqube.report.markdown;

import com.github.sonarqube.report.ReportGenerator;
import com.github.sonarqube.report.model.AnalysisReport;
import com.github.sonarqube.report.model.ReportSummary;
import com.github.sonarqube.report.model.SecurityFinding;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Markdown 格式報告生成器
 *
 * 生成友好的 Markdown 格式安全分析報告。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class MarkdownReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String generate(AnalysisReport report) {
        StringBuilder md = new StringBuilder();

        // 標題與基本資訊
        appendHeader(md, report);

        // 執行摘要
        appendSummary(md, report.getSummary());

        // 嚴重性分布
        appendSeverityBreakdown(md, report.getSummary());

        // OWASP 分類分布
        appendCategoryBreakdown(md, report.getSummary());

        // 詳細發現
        appendFindings(md, report.getFindings());

        // 頁尾
        appendFooter(md, report);

        return md.toString();
    }

    private void appendHeader(StringBuilder md, AnalysisReport report) {
        md.append("# 安全分析報告\n\n");
        md.append("## 專案資訊\n\n");
        md.append("| 項目 | 值 |\n");
        md.append("|------|----|\n");
        md.append("| **專案名稱** | ").append(report.getProjectName()).append(" |\n");
        md.append("| **OWASP 版本** | ").append(report.getOwaspVersion()).append(" |\n");
        md.append("| **分析時間** | ").append(report.getAnalysisTime().format(DATE_FORMATTER)).append(" |\n");

        if (report.getAiModel() != null) {
            md.append("| **AI 模型** | ").append(report.getAiModel()).append(" |\n");
        }

        md.append("\n---\n\n");
    }

    private void appendSummary(StringBuilder md, ReportSummary summary) {
        md.append("## 📊 執行摘要\n\n");

        md.append("| 指標 | 數量 |\n");
        md.append("|------|------|\n");
        md.append("| **總發現數** | ").append(summary.getTotalFindings()).append(" |\n");
        md.append("| **🚨 阻斷性 (BLOCKER)** | ").append(summary.getBlockerCount()).append(" |\n");
        md.append("| **🔴 嚴重 (CRITICAL)** | ").append(summary.getCriticalCount()).append(" |\n");
        md.append("| **🟠 主要 (MAJOR)** | ").append(summary.getMajorCount()).append(" |\n");

        if (summary.getFilesAnalyzed() > 0) {
            md.append("| **分析檔案數** | ").append(summary.getFilesAnalyzed()).append(" |\n");
        }

        md.append("\n");
    }

    private void appendSeverityBreakdown(StringBuilder md, ReportSummary summary) {
        md.append("## 🎯 嚴重性分布\n\n");

        Map<String, Integer> counts = summary.getSeverityCounts();
        if (counts.isEmpty()) {
            md.append("*無安全發現*\n\n");
            return;
        }

        // 依嚴重性排序
        List<String> severities = List.of("BLOCKER", "CRITICAL", "MAJOR", "MINOR", "INFO");

        for (String severity : severities) {
            int count = counts.getOrDefault(severity, 0);
            if (count > 0) {
                String emoji = getSeverityEmoji(severity);
                md.append("- ").append(emoji).append(" **").append(severity).append("**: ")
                  .append(count).append(" 個問題\n");
            }
        }

        md.append("\n");
    }

    private void appendCategoryBreakdown(StringBuilder md, ReportSummary summary) {
        md.append("## 🏷️ OWASP 分類分布\n\n");

        Map<String, Integer> counts = summary.getCategoryCounts();
        if (counts.isEmpty()) {
            md.append("*無分類資訊*\n\n");
            return;
        }

        // 按數量降序排列
        counts.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .forEach(entry -> {
                md.append("- **").append(entry.getKey()).append("**: ")
                  .append(entry.getValue()).append(" 個問題\n");
            });

        md.append("\n");
    }

    private void appendFindings(StringBuilder md, List<SecurityFinding> findings) {
        if (findings.isEmpty()) {
            return;
        }

        md.append("## 🔍 詳細發現\n\n");

        // 依嚴重性分組
        Map<String, List<SecurityFinding>> grouped = findings.stream()
            .collect(Collectors.groupingBy(SecurityFinding::getSeverity));

        List<String> severities = List.of("BLOCKER", "CRITICAL", "MAJOR", "MINOR", "INFO");

        for (String severity : severities) {
            List<SecurityFinding> findingsInSeverity = grouped.get(severity);
            if (findingsInSeverity == null || findingsInSeverity.isEmpty()) {
                continue;
            }

            String emoji = getSeverityEmoji(severity);
            md.append("### ").append(emoji).append(" ").append(severity)
              .append(" (").append(findingsInSeverity.size()).append(" 個)\n\n");

            int index = 1;
            for (SecurityFinding finding : findingsInSeverity) {
                appendFinding(md, finding, index++);
            }
        }
    }

    private void appendFinding(StringBuilder md, SecurityFinding finding, int index) {
        md.append("#### ").append(index).append(". ").append(finding.getRuleName()).append("\n\n");

        // 基本資訊
        md.append("**位置**: `").append(finding.getFilePath());
        if (finding.getLineNumber() != null) {
            md.append(":").append(finding.getLineNumber());
        }
        md.append("`\n\n");

        md.append("**規則**: `").append(finding.getRuleKey()).append("`\n\n");
        md.append("**OWASP 分類**: ").append(finding.getOwaspCategory()).append("\n\n");

        if (!finding.getCweIds().isEmpty()) {
            md.append("**CWE**: ").append(String.join(", ", finding.getCweIds())).append("\n\n");
        }

        // 描述
        if (finding.getDescription() != null) {
            md.append("**問題描述**:\n\n");
            md.append(finding.getDescription()).append("\n\n");
        }

        // 代碼片段
        if (finding.getCodeSnippet() != null) {
            md.append("**代碼片段**:\n\n");
            md.append("```java\n");
            md.append(finding.getCodeSnippet()).append("\n");
            md.append("```\n\n");
        }

        // 修復建議
        if (finding.getFixSuggestion() != null) {
            md.append("**💡 修復建議**:\n\n");
            md.append(finding.getFixSuggestion()).append("\n\n");
        }

        md.append("---\n\n");
    }

    private void appendFooter(StringBuilder md, AnalysisReport report) {
        md.append("## 📝 備註\n\n");
        md.append("此報告由 SonarQube AI OWASP Plugin 自動生成。\n\n");
        md.append("- 分析時間: ").append(report.getAnalysisTime().format(DATE_FORMATTER)).append("\n");
        md.append("- OWASP 版本: ").append(report.getOwaspVersion()).append("\n");

        if (report.getAiModel() != null) {
            md.append("- AI 模型: ").append(report.getAiModel()).append("\n");
        }
    }

    private String getSeverityEmoji(String severity) {
        switch (severity) {
            case "BLOCKER": return "🚨";
            case "CRITICAL": return "🔴";
            case "MAJOR": return "🟠";
            case "MINOR": return "🟡";
            case "INFO": return "ℹ️";
            default: return "⚪";
        }
    }

    @Override
    public String getFormat() {
        return "markdown";
    }

    @Override
    public String getFileExtension() {
        return ".md";
    }
}
