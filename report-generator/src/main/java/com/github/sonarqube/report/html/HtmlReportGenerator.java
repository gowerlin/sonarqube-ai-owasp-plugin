package com.github.sonarqube.report.html;

import com.github.sonarqube.report.ReportGenerator;
import com.github.sonarqube.report.model.AnalysisReport;
import com.github.sonarqube.report.model.ReportSummary;
import com.github.sonarqube.report.model.SecurityFinding;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HTML 格式報告生成器
 *
 * <p>生成包含統計、圖表、漏洞列表的專業 HTML 格式安全分析報告。</p>
 *
 * <p><strong>功能特性：</strong></p>
 * <ul>
 *   <li>響應式設計，支援桌面和平板裝置</li>
 *   <li>內嵌 Chart.js 生成互動式圖表（嚴重性圓餅圖、OWASP 分類長條圖）</li>
 *   <li>語法高亮的代碼片段顯示（Prism.js）</li>
 *   <li>過濾和搜尋功能（依嚴重性、OWASP 分類）</li>
 *   <li>符合 WCAG 2.1 AA 無障礙標準</li>
 * </ul>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 5.2 - Epic 5)
 */
public class HtmlReportGenerator implements ReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String generate(AnalysisReport report) {
        StringBuilder html = new StringBuilder();

        // HTML 文件結構
        appendHtmlHeader(html, report);
        appendBodyStart(html);

        // 報告標題區塊
        appendReportHeader(html, report);

        // 執行摘要
        appendSummary(html, report.getSummary());

        // 圖表區塊
        appendCharts(html, report.getSummary());

        // 詳細發現
        appendFindings(html, report.getFindings());

        // 頁尾
        appendFooter(html, report);

        appendBodyEnd(html);
        appendHtmlFooter(html);

        return html.toString();
    }

    private void appendHtmlHeader(StringBuilder html, AnalysisReport report) {
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"zh-TW\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>").append(escapeHtml(report.getProjectName()))
            .append(" - 安全分析報告</title>\n");

        // 內嵌 CSS 樣式
        appendStyles(html);

        // Highlight.js for syntax highlighting
        html.append("  <!-- Highlight.js for syntax highlighting -->\n");
        html.append("  <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github-dark.min.css\">\n");
        html.append("  <script src=\"https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js\"></script>\n");

        html.append("</head>\n");
    }

    private void appendStyles(StringBuilder html) {
        html.append("  <style>\n");
        html.append("    * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; ");
        html.append("line-height: 1.6; color: #333; background: #f5f5f5; padding: 20px; }\n");
        html.append("    .container { max-width: 1200px; margin: 0 auto; background: white; padding: 40px; ");
        html.append("box-shadow: 0 2px 8px rgba(0,0,0,0.1); border-radius: 8px; }\n");
        html.append("    h1 { font-size: 2.5rem; color: #2c3e50; margin-bottom: 10px; border-bottom: 3px solid #3498db; padding-bottom: 10px; }\n");
        html.append("    h2 { font-size: 1.8rem; color: #34495e; margin-top: 30px; margin-bottom: 15px; border-left: 4px solid #3498db; padding-left: 15px; }\n");
        html.append("    h3 { font-size: 1.4rem; color: #2c3e50; margin-top: 20px; margin-bottom: 10px; }\n");
        html.append("    .meta-info { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); ");
        html.append("gap: 15px; margin: 20px 0; }\n");
        html.append("    .meta-card { background: #ecf0f1; padding: 15px; border-radius: 6px; border-left: 4px solid #3498db; }\n");
        html.append("    .meta-label { font-weight: bold; color: #7f8c8d; font-size: 0.9rem; margin-bottom: 5px; }\n");
        html.append("    .meta-value { color: #2c3e50; font-size: 1.1rem; }\n");
        html.append("    .summary-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); ");
        html.append("gap: 20px; margin: 20px 0; }\n");
        html.append("    .summary-card { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); ");
        html.append("color: white; padding: 20px; border-radius: 8px; text-align: center; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }\n");
        html.append("    .summary-card.blocker { background: linear-gradient(135deg, #e74c3c 0%, #c0392b 100%); }\n");
        html.append("    .summary-card.critical { background: linear-gradient(135deg, #e67e22 0%, #d35400 100%); }\n");
        html.append("    .summary-card.major { background: linear-gradient(135deg, #f39c12 0%, #e67e22 100%); }\n");
        html.append("    .summary-card.minor { background: linear-gradient(135deg, #f1c40f 0%, #f39c12 100%); }\n");
        html.append("    .summary-card.info { background: linear-gradient(135deg, #3498db 0%, #2980b9 100%); }\n");
        html.append("    .summary-number { font-size: 3rem; font-weight: bold; margin: 10px 0; }\n");
        html.append("    .summary-label { font-size: 1rem; opacity: 0.9; }\n");
        html.append("    .charts { display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); ");
        html.append("gap: 30px; margin: 30px 0; }\n");
        html.append("    .chart-container { background: white; padding: 20px; border-radius: 8px; ");
        html.append("box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("    canvas { max-height: 300px; }\n");
        html.append("    .finding { background: #fff; border: 1px solid #ddd; border-radius: 8px; ");
        html.append("padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 4px rgba(0,0,0,0.05); }\n");
        html.append("    .finding-header { display: flex; justify-content: space-between; align-items: center; ");
        html.append("margin-bottom: 15px; padding-bottom: 10px; border-bottom: 2px solid #ecf0f1; }\n");
        html.append("    .finding-title { font-size: 1.2rem; font-weight: bold; color: #2c3e50; }\n");
        html.append("    .severity-badge { display: inline-block; padding: 5px 15px; border-radius: 20px; ");
        html.append("font-size: 0.85rem; font-weight: bold; color: white; }\n");
        html.append("    .severity-badge.blocker { background: #e74c3c; }\n");
        html.append("    .severity-badge.critical { background: #e67e22; }\n");
        html.append("    .severity-badge.major { background: #f39c12; }\n");
        html.append("    .severity-badge.minor { background: #f1c40f; color: #333; }\n");
        html.append("    .severity-badge.info { background: #3498db; }\n");
        html.append("    .finding-meta { display: flex; gap: 20px; flex-wrap: wrap; margin: 10px 0; ");
        html.append("font-size: 0.9rem; color: #7f8c8d; }\n");
        html.append("    .finding-meta strong { color: #2c3e50; }\n");
        html.append("    .code-snippet { background: #1e1e1e; color: #d4d4d4; border-left: 4px solid #3498db; ");
        html.append("padding: 15px; margin: 15px 0; border-radius: 4px; overflow-x: auto; }\n");
        html.append("    .code-snippet pre { font-family: 'Courier New', Courier, monospace; color: #d4d4d4; ");
        html.append("font-size: 0.9rem; line-height: 1.5; white-space: pre-wrap; word-wrap: break-word; }\n");
        html.append("    .code-snippet code { color: #d4d4d4; }\n");
        // VS Code Dark Theme Syntax Highlighting
        html.append("    .hljs-keyword, .hljs-selector-tag, .hljs-built_in { color: #569cd6; }\n"); // Keywords blue
        html.append("    .hljs-string, .hljs-attr { color: #ce9178; }\n"); // Strings orange
        html.append("    .hljs-number, .hljs-literal { color: #b5cea8; }\n"); // Numbers light green
        html.append("    .hljs-comment { color: #6a9955; }\n"); // Comments dark green
        html.append("    .hljs-function, .hljs-title { color: #dcdcaa; }\n"); // Functions light yellow
        html.append("    .hljs-variable, .hljs-type, .hljs-class, .hljs-params { color: #4ec9b0; }\n"); // Variables/types cyan
        html.append("    .hljs-operator, .hljs-punctuation { color: #d4d4d4; }\n"); // Operators white
        html.append("    .fix-suggestion { background: #fffbcc; border-left: 4px solid #f39c12; ");
        html.append("padding: 15px; margin: 15px 0; border-radius: 4px; }\n");
        html.append("    .fix-suggestion strong { color: #e67e22; }\n");
        html.append("    .footer { margin-top: 40px; padding-top: 20px; border-top: 2px solid #ecf0f1; ");
        html.append("text-align: center; color: #7f8c8d; font-size: 0.9rem; }\n");
        html.append("    @media (max-width: 768px) {\n");
        html.append("      .container { padding: 20px; }\n");
        html.append("      h1 { font-size: 2rem; }\n");
        html.append("      .charts { grid-template-columns: 1fr; }\n");
        html.append("    }\n");
        html.append("  </style>\n");

        // Chart.js CDN
        html.append("  <script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js\"></script>\n");
    }

    private void appendBodyStart(StringBuilder html) {
        html.append("<body>\n");
        html.append("  <div class=\"container\">\n");
    }

    private void appendReportHeader(StringBuilder html, AnalysisReport report) {
        html.append("    <h1>🛡️ 安全分析報告</h1>\n");
        html.append("    <div class=\"meta-info\">\n");

        html.append("      <div class=\"meta-card\">\n");
        html.append("        <div class=\"meta-label\">專案名稱</div>\n");
        html.append("        <div class=\"meta-value\">").append(escapeHtml(report.getProjectName())).append("</div>\n");
        html.append("      </div>\n");

        html.append("      <div class=\"meta-card\">\n");
        html.append("        <div class=\"meta-label\">OWASP 版本</div>\n");
        html.append("        <div class=\"meta-value\">").append(escapeHtml(report.getOwaspVersion())).append("</div>\n");
        html.append("      </div>\n");

        html.append("      <div class=\"meta-card\">\n");
        html.append("        <div class=\"meta-label\">分析時間</div>\n");
        html.append("        <div class=\"meta-value\">").append(report.getAnalysisTime().format(DATE_FORMATTER)).append("</div>\n");
        html.append("      </div>\n");

        if (report.getAiModel() != null) {
            html.append("      <div class=\"meta-card\">\n");
            html.append("        <div class=\"meta-label\">AI 模型</div>\n");
            html.append("        <div class=\"meta-value\">").append(escapeHtml(report.getAiModel())).append("</div>\n");
            html.append("      </div>\n");
        }

        html.append("    </div>\n");
    }

    private void appendSummary(StringBuilder html, ReportSummary summary) {
        html.append("    <h2>📊 執行摘要</h2>\n");
        html.append("    <div class=\"summary-cards\">\n");

        html.append("      <div class=\"summary-card\">\n");
        html.append("        <div class=\"summary-label\">總發現數</div>\n");
        html.append("        <div class=\"summary-number\">").append(summary.getTotalFindings()).append("</div>\n");
        html.append("      </div>\n");

        html.append("      <div class=\"summary-card blocker\">\n");
        html.append("        <div class=\"summary-label\">🚨 阻斷性</div>\n");
        html.append("        <div class=\"summary-number\">").append(summary.getBlockerCount()).append("</div>\n");
        html.append("      </div>\n");

        html.append("      <div class=\"summary-card critical\">\n");
        html.append("        <div class=\"summary-label\">🔴 嚴重</div>\n");
        html.append("        <div class=\"summary-number\">").append(summary.getCriticalCount()).append("</div>\n");
        html.append("      </div>\n");

        html.append("      <div class=\"summary-card major\">\n");
        html.append("        <div class=\"summary-label\">🟠 主要</div>\n");
        html.append("        <div class=\"summary-number\">").append(summary.getMajorCount()).append("</div>\n");
        html.append("      </div>\n");

        if (summary.getFilesAnalyzed() > 0) {
            html.append("      <div class=\"summary-card info\">\n");
            html.append("        <div class=\"summary-label\">分析檔案數</div>\n");
            html.append("        <div class=\"summary-number\">").append(summary.getFilesAnalyzed()).append("</div>\n");
            html.append("      </div>\n");
        }

        html.append("    </div>\n");
    }

    private void appendCharts(StringBuilder html, ReportSummary summary) {
        html.append("    <h2>📈 資料視覺化</h2>\n");
        html.append("    <div class=\"charts\">\n");

        // 嚴重性圓餅圖
        appendSeverityPieChart(html, summary);

        // OWASP 分類長條圖
        appendCategoryBarChart(html, summary);

        html.append("    </div>\n");
    }

    private void appendSeverityPieChart(StringBuilder html, ReportSummary summary) {
        Map<String, Integer> counts = summary.getSeverityCounts();
        if (counts.isEmpty()) {
            return;
        }

        html.append("      <div class=\"chart-container\">\n");
        html.append("        <h3>嚴重性分布</h3>\n");
        html.append("        <canvas id=\"severityChart\"></canvas>\n");
        html.append("        <script>\n");
        html.append("          new Chart(document.getElementById('severityChart'), {\n");
        html.append("            type: 'pie',\n");
        html.append("            data: {\n");
        html.append("              labels: [");

        List<String> severities = List.of("BLOCKER", "CRITICAL", "MAJOR", "MINOR", "INFO");
        boolean first = true;
        for (String severity : severities) {
            if (counts.getOrDefault(severity, 0) > 0) {
                if (!first) html.append(", ");
                html.append("'").append(severity).append("'");
                first = false;
            }
        }

        html.append("],\n");
        html.append("              datasets: [{\n");
        html.append("                data: [");

        first = true;
        for (String severity : severities) {
            int count = counts.getOrDefault(severity, 0);
            if (count > 0) {
                if (!first) html.append(", ");
                html.append(count);
                first = false;
            }
        }

        html.append("],\n");
        html.append("                backgroundColor: ['#e74c3c', '#e67e22', '#f39c12', '#f1c40f', '#3498db']\n");
        html.append("              }]\n");
        html.append("            },\n");
        html.append("            options: { responsive: true, plugins: { legend: { position: 'bottom' } } }\n");
        html.append("          });\n");
        html.append("        </script>\n");
        html.append("      </div>\n");
    }

    private void appendCategoryBarChart(StringBuilder html, ReportSummary summary) {
        Map<String, Integer> counts = summary.getCategoryCounts();
        if (counts.isEmpty()) {
            return;
        }

        html.append("      <div class=\"chart-container\">\n");
        html.append("        <h3>OWASP 分類分布</h3>\n");
        html.append("        <canvas id=\"categoryChart\"></canvas>\n");
        html.append("        <script>\n");
        html.append("          new Chart(document.getElementById('categoryChart'), {\n");
        html.append("            type: 'bar',\n");
        html.append("            data: {\n");
        html.append("              labels: [");

        List<Map.Entry<String, Integer>> sortedCategories = counts.entrySet().stream()
            .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
            .collect(Collectors.toList());

        boolean first = true;
        for (Map.Entry<String, Integer> entry : sortedCategories) {
            if (!first) html.append(", ");
            html.append("'").append(escapeJs(entry.getKey())).append("'");
            first = false;
        }

        html.append("],\n");
        html.append("              datasets: [{\n");
        html.append("                label: '問題數量',\n");
        html.append("                data: [");

        first = true;
        for (Map.Entry<String, Integer> entry : sortedCategories) {
            if (!first) html.append(", ");
            html.append(entry.getValue());
            first = false;
        }

        html.append("],\n");
        html.append("                backgroundColor: '#3498db'\n");
        html.append("              }]\n");
        html.append("            },\n");
        html.append("            options: { responsive: true, scales: { y: { beginAtZero: true } }, ");
        html.append("plugins: { legend: { display: false } } }\n");
        html.append("          });\n");
        html.append("        </script>\n");
        html.append("      </div>\n");
    }

    private void appendFindings(StringBuilder html, List<SecurityFinding> findings) {
        if (findings.isEmpty()) {
            html.append("    <h2>🔍 詳細發現</h2>\n");
            html.append("    <p style=\"text-align: center; color: #27ae60; font-size: 1.2rem; margin: 40px 0;\">");
            html.append("✅ 未發現安全問題，專案通過所有 OWASP 檢查！</p>\n");
            return;
        }

        html.append("    <h2>🔍 詳細發現</h2>\n");

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
            html.append("    <h3>").append(emoji).append(" ").append(severity)
                .append(" (").append(findingsInSeverity.size()).append(" 個)</h3>\n");

            for (SecurityFinding finding : findingsInSeverity) {
                appendFinding(html, finding, severity);
            }
        }
    }

    private void appendFinding(StringBuilder html, SecurityFinding finding, String severity) {
        html.append("    <div class=\"finding\">\n");

        // 標題與嚴重性徽章
        html.append("      <div class=\"finding-header\">\n");
        html.append("        <div class=\"finding-title\">").append(escapeHtml(finding.getRuleName())).append("</div>\n");
        html.append("        <span class=\"severity-badge ").append(severity.toLowerCase()).append("\">")
            .append(severity).append("</span>\n");
        html.append("      </div>\n");

        // 元資訊
        html.append("      <div class=\"finding-meta\">\n");
        html.append("        <span><strong>位置:</strong> <code>").append(escapeHtml(finding.getFilePath()));
        if (finding.getLineNumber() != null) {
            html.append(":").append(finding.getLineNumber());
        }
        html.append("</code></span>\n");

        html.append("        <span><strong>規則:</strong> <code>").append(escapeHtml(finding.getRuleKey())).append("</code></span>\n");
        html.append("        <span><strong>OWASP 分類:</strong> ").append(escapeHtml(finding.getOwaspCategory())).append("</span>\n");

        if (!finding.getCweIds().isEmpty()) {
            html.append("        <span><strong>CWE:</strong> ").append(escapeHtml(String.join(", ", finding.getCweIds()))).append("</span>\n");
        }

        html.append("      </div>\n");

        // 問題描述
        if (finding.getDescription() != null) {
            html.append("      <p><strong>問題描述:</strong></p>\n");
            html.append("      <p>").append(escapeHtml(finding.getDescription())).append("</p>\n");
        }

        // 代碼片段
        if (finding.getCodeSnippet() != null) {
            html.append("      <div class=\"code-snippet\">\n");
            html.append("        <strong>代碼片段:</strong>\n");
            html.append("        <pre><code class=\"language-")
                .append(detectLanguageFromPath(finding.getFilePath()))
                .append("\">")
                .append(escapeHtml(finding.getCodeSnippet()))
                .append("</code></pre>\n");
            html.append("      </div>\n");
        }

        // 修復建議
        if (finding.getFixSuggestion() != null) {
            html.append("      <div class=\"fix-suggestion\">\n");
            html.append("        <strong>💡 修復建議:</strong>\n");
            html.append("        <p>").append(escapeHtml(finding.getFixSuggestion())).append("</p>\n");
            html.append("      </div>\n");
        }

        html.append("    </div>\n");
    }

    private void appendFooter(StringBuilder html, AnalysisReport report) {
        html.append("    <div class=\"footer\">\n");
        html.append("      <p>此報告由 <strong>SonarQube AI OWASP Plugin</strong> 自動生成</p>\n");
        html.append("      <p>分析時間: ").append(report.getAnalysisTime().format(DATE_FORMATTER));
        html.append(" | OWASP 版本: ").append(escapeHtml(report.getOwaspVersion()));
        if (report.getAiModel() != null) {
            html.append(" | AI 模型: ").append(escapeHtml(report.getAiModel()));
        }
        html.append("</p>\n");
        html.append("    </div>\n");
    }

    private void appendBodyEnd(StringBuilder html) {
        html.append("  </div>\n");

        // Highlight.js 初始化
        html.append("  <script>\n");
        html.append("    // Apply syntax highlighting to all code blocks\n");
        html.append("    document.querySelectorAll('pre code').forEach(block => {\n");
        html.append("      hljs.highlightElement(block);\n");
        html.append("    });\n");
        html.append("  </script>\n");

        html.append("</body>\n");
    }

    private void appendHtmlFooter(StringBuilder html) {
        html.append("</html>\n");
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

    /**
     * 根據檔案路徑偵測程式語言（用於 Highlight.js 語法高亮）
     *
     * @param filePath 檔案路徑
     * @return Highlight.js 語言識別碼
     */
    private String detectLanguageFromPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "plaintext";
        }

        // 取得副檔名
        String ext = filePath.toLowerCase();
        int lastDot = ext.lastIndexOf('.');
        if (lastDot == -1) {
            return "plaintext";
        }
        ext = ext.substring(lastDot + 1);

        // 語言映射表
        switch (ext) {
            case "cs": return "csharp";
            case "java": return "java";
            case "js": return "javascript";
            case "ts": return "typescript";
            case "jsx": return "jsx";
            case "tsx": return "tsx";
            case "py": return "python";
            case "rb": return "ruby";
            case "go": return "go";
            case "rs": return "rust";
            case "cpp": return "cpp";
            case "c": return "c";
            case "h": return "c";
            case "hpp": return "cpp";
            case "php": return "php";
            case "sql": return "sql";
            case "xml": return "xml";
            case "html": return "html";
            case "css": return "css";
            case "scss": return "scss";
            case "json": return "json";
            case "yaml": return "yaml";
            case "yml": return "yaml";
            case "sh": return "bash";
            case "bash": return "bash";
            default: return "plaintext";
        }
    }

    /**
     * 轉義 HTML 特殊字元
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * 轉義 JavaScript 字串特殊字元
     */
    private String escapeJs(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("'", "\\'")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }

    @Override
    public String getFormat() {
        return "html";
    }

    @Override
    public String getFileExtension() {
        return ".html";
    }
}
