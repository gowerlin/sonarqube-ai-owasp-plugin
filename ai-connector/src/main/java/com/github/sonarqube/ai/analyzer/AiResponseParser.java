package com.github.sonarqube.ai.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sonarqube.ai.model.SecurityIssue;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 回應解析器
 *
 * 將 AI 返回的 JSON 格式分析結果解析為結構化的安全問題列表。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class AiResponseParser {

    private final ObjectMapper objectMapper;

    public AiResponseParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 解析 AI 回應內容為安全問題列表
     *
     * @param analysisResult AI 分析結果（JSON 字串）
     * @return 安全問題列表
     */
    public List<SecurityIssue> parseSecurityIssues(String analysisResult) {
        List<SecurityIssue> issues = new ArrayList<>();

        if (analysisResult == null || analysisResult.trim().isEmpty()) {
            return issues;
        }

        try {
            // 嘗試提取 JSON 部分（可能包含其他文字）
            String jsonContent = extractJsonContent(analysisResult);

            if (jsonContent == null) {
                return issues;
            }

            JsonNode rootNode = objectMapper.readTree(jsonContent);
            JsonNode issuesNode = rootNode.get("issues");

            if (issuesNode == null || !issuesNode.isArray()) {
                return issues;
            }

            for (JsonNode issueNode : issuesNode) {
                SecurityIssue issue = parseSecurityIssue(issueNode);
                if (issue != null) {
                    issues.add(issue);
                }
            }

        } catch (Exception e) {
            // 無法解析 JSON，返回空列表
            // 可以記錄日誌以供診斷
        }

        return issues;
    }

    /**
     * 從可能包含其他文字的回應中提取 JSON 內容
     *
     * @param content 原始內容
     * @return JSON 字串，如果無法提取則返回 null
     */
    private String extractJsonContent(String content) {
        if (content == null) {
            return null;
        }

        // 尋找第一個 { 和最後一個 }
        int startIndex = content.indexOf('{');
        int endIndex = content.lastIndexOf('}');

        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            return null;
        }

        return content.substring(startIndex, endIndex + 1);
    }

    /**
     * 解析單一安全問題
     *
     * @param issueNode JSON 節點
     * @return SecurityIssue 物件，如果解析失敗則返回 null
     */
    private SecurityIssue parseSecurityIssue(JsonNode issueNode) {
        try {
            SecurityIssue issue = new SecurityIssue();

            // OWASP 分類
            JsonNode owaspNode = issueNode.get("owaspCategory");
            if (owaspNode != null && !owaspNode.isNull()) {
                issue.setOwaspCategory(owaspNode.asText());
            }

            // CWE ID
            JsonNode cweNode = issueNode.get("cweId");
            if (cweNode != null && !cweNode.isNull()) {
                issue.setCweId(cweNode.asText());
            }

            // 嚴重性
            JsonNode severityNode = issueNode.get("severity");
            if (severityNode != null && !severityNode.isNull()) {
                issue.setSeverity(SecurityIssue.Severity.fromString(severityNode.asText()));
            }

            // 描述
            JsonNode descNode = issueNode.get("description");
            if (descNode != null && !descNode.isNull()) {
                issue.setDescription(descNode.asText());
            }

            // 行號
            JsonNode lineNode = issueNode.get("lineNumber");
            if (lineNode != null && !lineNode.isNull() && lineNode.isNumber()) {
                issue.setLineNumber(lineNode.asInt());
            }

            // 修復建議
            JsonNode fixNode = issueNode.get("fixSuggestion");
            if (fixNode != null && !fixNode.isNull()) {
                issue.setFixSuggestion(fixNode.asText());
            }

            // 代碼範例
            JsonNode exampleNode = issueNode.get("codeExample");
            if (exampleNode != null && !exampleNode.isNull()) {
                SecurityIssue.CodeExample example = parseCodeExample(exampleNode);
                issue.setCodeExample(example);
            }

            // 工時預估
            JsonNode effortNode = issueNode.get("effortEstimate");
            if (effortNode != null && !effortNode.isNull()) {
                issue.setEffortEstimate(effortNode.asText());
            }

            return issue;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析代碼範例
     *
     * @param exampleNode JSON 節點
     * @return CodeExample 物件
     */
    private SecurityIssue.CodeExample parseCodeExample(JsonNode exampleNode) {
        SecurityIssue.CodeExample example = new SecurityIssue.CodeExample();

        JsonNode beforeNode = exampleNode.get("before");
        if (beforeNode != null && !beforeNode.isNull()) {
            example.setBefore(beforeNode.asText());
        }

        JsonNode afterNode = exampleNode.get("after");
        if (afterNode != null && !afterNode.isNull()) {
            example.setAfter(afterNode.asText());
        }

        return example;
    }

    /**
     * 驗證解析結果是否有效
     *
     * @param issue 安全問題
     * @return 是否有效（至少包含 OWASP 分類和描述）
     */
    public boolean isValidIssue(SecurityIssue issue) {
        return issue != null &&
            issue.getOwaspCategory() != null && !issue.getOwaspCategory().trim().isEmpty() &&
            issue.getDescription() != null && !issue.getDescription().trim().isEmpty();
    }
}
