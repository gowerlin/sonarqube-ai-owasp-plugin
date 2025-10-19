package com.github.sonarqube.ai.analyzer;

import com.github.sonarqube.ai.model.SecurityIssue;

import java.util.List;

/**
 * 修復建議格式化器
 *
 * 將 AI 分析的安全問題轉換為易讀的修復建議報告。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class FixSuggestionFormatter {

    /**
     * 生成單一問題的修復建議
     *
     * @param issue 安全問題
     * @return 格式化的修復建議文本
     */
    public String formatSingleIssue(SecurityIssue issue) {
        if (issue == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // 標題：OWASP 分類 + CWE ID
        sb.append("## ");
        if (issue.getOwaspCategory() != null) {
            sb.append(issue.getOwaspCategory());
        }
        if (issue.getCweId() != null) {
            sb.append(" (").append(issue.getCweId()).append(")");
        }
        sb.append("\n\n");

        // 嚴重性
        if (issue.getSeverity() != null) {
            sb.append("**嚴重性**: ");
            sb.append(getSeverityBadge(issue.getSeverity()));
            sb.append("\n\n");
        }

        // 位置
        if (issue.getLineNumber() != null) {
            sb.append("**位置**: Line ").append(issue.getLineNumber()).append("\n\n");
        }

        // 描述
        if (issue.getDescription() != null) {
            sb.append("**問題描述**:\n");
            sb.append(issue.getDescription()).append("\n\n");
        }

        // 修復建議
        if (issue.getFixSuggestion() != null) {
            sb.append("**修復建議**:\n");
            sb.append(issue.getFixSuggestion()).append("\n\n");
        }

        // 代碼範例
        if (issue.getCodeExample() != null) {
            sb.append("**修復範例**:\n\n");

            if (issue.getCodeExample().getBefore() != null) {
                sb.append("```java\n");
                sb.append("// ❌ 修復前\n");
                sb.append(issue.getCodeExample().getBefore()).append("\n");
                sb.append("```\n\n");
            }

            if (issue.getCodeExample().getAfter() != null) {
                sb.append("```java\n");
                sb.append("// ✅ 修復後\n");
                sb.append(issue.getCodeExample().getAfter()).append("\n");
                sb.append("```\n\n");
            }
        }

        // 工時預估
        if (issue.getEffortEstimate() != null) {
            sb.append("**預估工時**: ").append(issue.getEffortEstimate()).append("\n");
        }

        return sb.toString();
    }

    /**
     * 生成問題列表的完整修復報告
     *
     * @param issues 安全問題列表
     * @return 格式化的修復報告
     */
    public String formatReport(List<SecurityIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return "✅ 未發現安全問題\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# 安全分析報告\n\n");

        // 摘要統計
        sb.append(generateSummary(issues));
        sb.append("\n---\n\n");

        // 按嚴重性分組
        List<SecurityIssue> highIssues = issues.stream()
            .filter(i -> i.getSeverity() == SecurityIssue.Severity.HIGH)
            .toList();

        List<SecurityIssue> mediumIssues = issues.stream()
            .filter(i -> i.getSeverity() == SecurityIssue.Severity.MEDIUM)
            .toList();

        List<SecurityIssue> lowIssues = issues.stream()
            .filter(i -> i.getSeverity() == SecurityIssue.Severity.LOW)
            .toList();

        // 高嚴重性問題
        if (!highIssues.isEmpty()) {
            sb.append("# 🚨 高嚴重性問題 (").append(highIssues.size()).append(")\n\n");
            for (int i = 0; i < highIssues.size(); i++) {
                sb.append("### ").append(i + 1).append(". ");
                sb.append(formatSingleIssue(highIssues.get(i)));
                sb.append("\n---\n\n");
            }
        }

        // 中嚴重性問題
        if (!mediumIssues.isEmpty()) {
            sb.append("# ⚠️ 中嚴重性問題 (").append(mediumIssues.size()).append(")\n\n");
            for (int i = 0; i < mediumIssues.size(); i++) {
                sb.append("### ").append(i + 1).append(". ");
                sb.append(formatSingleIssue(mediumIssues.get(i)));
                sb.append("\n---\n\n");
            }
        }

        // 低嚴重性問題
        if (!lowIssues.isEmpty()) {
            sb.append("# ℹ️ 低嚴重性問題 (").append(lowIssues.size()).append(")\n\n");
            for (int i = 0; i < lowIssues.size(); i++) {
                sb.append("### ").append(i + 1).append(". ");
                sb.append(formatSingleIssue(lowIssues.get(i)));
                sb.append("\n---\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * 生成摘要統計
     *
     * @param issues 安全問題列表
     * @return 摘要統計文本
     */
    private String generateSummary(List<SecurityIssue> issues) {
        long high = issues.stream()
            .filter(i -> i.getSeverity() == SecurityIssue.Severity.HIGH)
            .count();

        long medium = issues.stream()
            .filter(i -> i.getSeverity() == SecurityIssue.Severity.MEDIUM)
            .count();

        long low = issues.stream()
            .filter(i -> i.getSeverity() == SecurityIssue.Severity.LOW)
            .count();

        StringBuilder sb = new StringBuilder();
        sb.append("## 📊 問題摘要\n\n");
        sb.append("- **總計**: ").append(issues.size()).append(" 個問題\n");
        sb.append("- **高嚴重性**: ").append(high).append(" 🚨\n");
        sb.append("- **中嚴重性**: ").append(medium).append(" ⚠️\n");
        sb.append("- **低嚴重性**: ").append(low).append(" ℹ️\n");

        return sb.toString();
    }

    /**
     * 獲取嚴重性徽章
     *
     * @param severity 嚴重性
     * @return 徽章文本
     */
    private String getSeverityBadge(SecurityIssue.Severity severity) {
        return switch (severity) {
            case HIGH -> "🚨 HIGH";
            case MEDIUM -> "⚠️ MEDIUM";
            case LOW -> "ℹ️ LOW";
        };
    }

    /**
     * 生成簡短摘要（用於 SonarQube issue message）
     *
     * @param issue 安全問題
     * @return 簡短摘要文本
     */
    public String formatShortSummary(SecurityIssue issue) {
        if (issue == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // [OWASP Category] Description
        if (issue.getOwaspCategory() != null) {
            sb.append("[").append(issue.getOwaspCategory()).append("] ");
        }

        if (issue.getDescription() != null) {
            // 限制長度為 200 字元
            String desc = issue.getDescription();
            if (desc.length() > 200) {
                desc = desc.substring(0, 197) + "...";
            }
            sb.append(desc);
        }

        return sb.toString();
    }
}
