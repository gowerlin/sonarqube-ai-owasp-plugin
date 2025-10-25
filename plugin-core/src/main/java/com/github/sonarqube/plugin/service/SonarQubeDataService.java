package com.github.sonarqube.plugin.service;

import com.github.sonarqube.report.model.ReportSummary;
import com.github.sonarqube.report.model.SecurityFinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ServerSide;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Issues;
import org.sonarqube.ws.client.HttpConnector;
import org.sonarqube.ws.client.WsClient;
import org.sonarqube.ws.client.WsClientFactories;
import org.sonarqube.ws.client.issues.SearchRequest;
import org.sonarqube.ws.client.sources.RawRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SonarQube 數據查詢服務
 *
 * <p>使用 SonarQube Web Services API 查詢專案的安全問題（Issues）並映射到報告模型。</p>
 *
 * <p><strong>查詢策略：</strong></p>
 * <ul>
 *   <li>查詢所有類型的 Issues (VULNERABILITY, BUG, CODE_SMELL)</li>
 *   <li>過濾包含安全相關 tags (owasp, security, cwe)</li>
 *   <li>僅查詢未解決的 Issues (OPEN, CONFIRMED, REOPENED)</li>
 *   <li>映射到 OWASP Top 10 2021 分類</li>
 * </ul>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.1
 */
@ServerSide
public class SonarQubeDataService {

    private static final Logger LOG = LoggerFactory.getLogger(SonarQubeDataService.class);

    private final Configuration configuration;
    private final WsClient wsClient;

    /**
     * 建構函數
     *
     * @param configuration SonarQube 配置
     */
    public SonarQubeDataService(Configuration configuration) {
        this.configuration = configuration;

        // 創建 WsClient，連接到本地 SonarQube 實例
        // 使用基本認證（插件內部調用本地 API）
        // TODO: 應該從配置中讀取認證資訊或使用 token
        this.wsClient = WsClientFactories.getDefault().newClient(
            HttpConnector.newBuilder()
                .url("http://localhost:9000")
                .credentials("admin", "P@ssw0rd")  // 臨時硬編碼用於測試
                .build()
        );

        LOG.info("SonarQubeDataService initialized with WsClient (using credentials)");
    }

    /**
     * 查詢專案的所有 OWASP 相關安全問題
     *
     * @param projectKey 專案 key (例如: "NCCS2.CallCenterWeb.backend")
     * @return 安全發現列表
     */
    public List<SecurityFinding> getOwaspFindings(String projectKey, String owaspVersion) {
        LOG.info("Querying OWASP findings for project: {} (OWASP {})", projectKey, owaspVersion);

        try {
            // 建構 SearchRequest
            SearchRequest request = new SearchRequest()
                    .setComponentKeys(Collections.singletonList(projectKey))
                    .setTypes(Arrays.asList("VULNERABILITY", "BUG", "CODE_SMELL"))
                    .setStatuses(Arrays.asList("OPEN", "CONFIRMED", "REOPENED"))
                    .setPs("500");

            // 執行查詢
            Issues.SearchWsResponse response = wsClient.issues().search(request);

            LOG.info("Found {} issues for project: {}", response.getIssuesCount(), projectKey);

            // 映射到 SecurityFinding 模型
            final String version = owaspVersion;  // Capture for lambda
        List<SecurityFinding> findings = response.getIssuesList().stream()
                    .filter(this::isSecurityRelated)
                    .map(issue -> mapToSecurityFinding(issue, version))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            LOG.info("Mapped {} issues to SecurityFindings (after OWASP filtering)", findings.size());

            return findings;

        } catch (Exception e) {
            LOG.error("Failed to query OWASP findings for project: {}", projectKey, e);
            return Collections.emptyList();
        }
    }

    /**
     * 計算報告摘要統計
     *
     * @param findings 安全發現列表
     * @return 報告摘要
     */
    public ReportSummary calculateSummary(List<SecurityFinding> findings) {
        return ReportSummary.fromFindings(findings);
    }

    /**
     * 檢查 Issue 是否與安全相關
     *
     * @param issue SonarQube Issue 物件
     * @return true 如果是安全相關的 Issue
     */
    private boolean isSecurityRelated(Issues.Issue issue) {
        // 策略 1: 檢查 Issue 類型
        String type = issue.getType().name();
        if ("VULNERABILITY".equals(type)) {
            return true;
        }

        // 策略 2: 檢查 tags 是否包含安全關鍵字
        if (issue.getTagsList() != null && !issue.getTagsList().isEmpty()) {
            for (String tag : issue.getTagsList()) {
                String lowerTag = tag.toLowerCase();
                if (lowerTag.contains("owasp") ||
                    lowerTag.contains("security") ||
                    lowerTag.contains("cwe") ||
                    lowerTag.contains("cert") ||
                    lowerTag.contains("sans")) {
                    return true;
                }
            }
        }

        // 策略 3: 檢查 rule key 是否為安全規則
        String rule = issue.getRule();
        String lowerRule = rule.toLowerCase();
        return lowerRule.contains("security") ||
               lowerRule.contains("owasp") ||
               lowerRule.contains("injection") ||
               lowerRule.contains("xss") ||
               lowerRule.contains("sql");
    }

    /**
     * 映射 SonarQube Issue 到 SecurityFinding 模型
     *
     * @param issue SonarQube Issue 物件
     * @return SecurityFinding 或 null（若無法映射到 OWASP 分類）
     */
    private SecurityFinding mapToSecurityFinding(Issues.Issue issue, String owaspVersion) {
        try {
            // 提取 tags
            List<String> tags = issue.getTagsList();

            // 映射到 OWASP 分類
            String ruleKey = issue.getRule();
            String owaspCategory = OwaspCategoryMapper.mapToOwaspCategory(tags, ruleKey, owaspVersion);

            if (owaspCategory == null) {
                LOG.debug("Issue {} could not be mapped to OWASP category, using tags as fallback",
                        issue.getKey());
                owaspCategory = getDefaultCategoryByType(issue.getType().name(), owaspVersion);
            }

            // 提取 CWE IDs
            List<String> cweIds = extractCweIds(tags);

            // 映射嚴重性
            String severity = mapSeverity(issue.getSeverity().name());

            // 獲取代碼片段
            String codeSnippet = null;
            if (issue.hasLine()) {
                codeSnippet = getCodeSnippet(issue.getComponent(), issue.getLine());
            }

            // 建構 SecurityFinding
            return SecurityFinding.builder()
                    .ruleKey(ruleKey)
                    .ruleName(extractRuleName(ruleKey))
                    .owaspCategory(owaspCategory)
                    .cweIds(cweIds)
                    .severity(severity)
                    .filePath(extractFilePath(issue.getComponent()))
                    .lineNumber(issue.hasLine() ? issue.getLine() : null)
                    .description(issue.getMessage())
                    .fixSuggestion(null)
                    .codeSnippet(codeSnippet)
                    .build();

        } catch (Exception e) {
            LOG.error("Failed to map issue {} to SecurityFinding",
                    issue.getKey(), e);
            return null;
        }
    }

    /**
     * 根據 Issue 類型獲取預設的 OWASP 分類
     */
    private String getDefaultCategoryByType(String type, String owaspVersion) {
        switch (type) {
            case "VULNERABILITY":
                if ("2017".equals(owaspVersion)) {
                    return "A1:2017-Injection";
                } else if ("2025".equals(owaspVersion)) {
                    return "A03:2025-Injection";
                }
                return "A03:2021-Injection";
            case "SECURITY_HOTSPOT":
                if ("2017".equals(owaspVersion)) {
                    return "A6:2017-Security Misconfiguration";
                } else if ("2025".equals(owaspVersion)) {
                    return "A05:2025-Security Misconfiguration";
                }
                return "A05:2021-Security Misconfiguration";
            case "BUG":
                if ("2017".equals(owaspVersion)) {
                    return "A10:2017-Insufficient Logging and Monitoring";
                } else if ("2025".equals(owaspVersion)) {
                    return "A04:2025-Insecure Design";
                }
                return "A04:2021-Insecure Design";
            default:
                if ("2017".equals(owaspVersion)) {
                    return "A10:2017-Insufficient Logging and Monitoring";
                } else if ("2025".equals(owaspVersion)) {
                    return "A09:2025-Security Logging and Monitoring Failures";
                }
                return "A09:2021-Security Logging and Monitoring Failures";
        }
    }

    /**
     * 從 tags 中提取 CWE IDs
     *
     * @param tags Issue tags
     * @return CWE IDs 列表
     */
    private List<String> extractCweIds(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }

        return tags.stream()
                .filter(tag -> tag.toLowerCase().startsWith("cwe-"))
                .map(String::toUpperCase)
                .collect(Collectors.toList());
    }

    /**
     * 提取規則名稱
     *
     * @param ruleKey SonarQube rule key
     * @return 規則名稱
     */
    private String extractRuleName(String ruleKey) {
        if (ruleKey != null && ruleKey.contains(":")) {
            String[] parts = ruleKey.split(":");
            return parts.length > 1 ? parts[1] : ruleKey;
        }
        return ruleKey;
    }

    /**
     * 提取檔案路徑
     *
     * @param component 元件字串
     * @return 檔案路徑
     */
    private String extractFilePath(String component) {
        if (component == null || component.isEmpty()) {
            return "";
        }

        int colonIndex = component.indexOf(':');
        if (colonIndex > 0 && colonIndex < component.length() - 1) {
            return component.substring(colonIndex + 1);
        }

        return component;
    }

    /**
     * 映射 SonarQube 嚴重性到報告嚴重性
     *
     * @param sonarSeverity SonarQube 嚴重性
     * @return 報告嚴重性
     */
    private String mapSeverity(String sonarSeverity) {
        if (sonarSeverity == null || sonarSeverity.isEmpty()) {
            return "INFO";
        }

        switch (sonarSeverity.toUpperCase()) {
            case "BLOCKER":
                return "BLOCKER";
            case "CRITICAL":
                return "CRITICAL";
            case "MAJOR":
                return "MAJOR";
            case "MINOR":
                return "MINOR";
            case "INFO":
            default:
                return "INFO";
        }
    }

    /**
     * 獲取代碼片段
     *
     * @param component 元件 key
     * @param line 行號
     * @return 代碼片段（前後各 3 行），失敗則返回 null
     */
    private String getCodeSnippet(String component, int line) {
        try {
            // 計算開始和結束行（前後各 3 行）
            int fromLine = Math.max(1, line - 3);
            int toLine = line + 3;

            // 使用 WsClient 的 sources().raw() API
            String rawSource = wsClient.sources().raw(new RawRequest().setKey(component));

            if (rawSource == null || rawSource.isEmpty()) {
                LOG.debug("No source code found for component: {}", component);
                return null;
            }

            // 分割成行
            String[] lines = rawSource.split("\r?\n");

            // 提取相關行
            StringBuilder snippet = new StringBuilder();
            for (int i = fromLine - 1; i < Math.min(toLine, lines.length); i++) {
                if (i >= 0 && i < lines.length) {
                    snippet.append(lines[i]).append("\n");
                }
            }

            return snippet.toString().trim();

        } catch (Exception e) {
            LOG.warn("Failed to get code snippet for component {} at line {}: {}", 
                    component, line, e.getMessage());
            return null;
        }
    }
}
