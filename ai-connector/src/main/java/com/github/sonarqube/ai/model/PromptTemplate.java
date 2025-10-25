package com.github.sonarqube.ai.model;

/**
 * AI Prompt 範本
 *
 * 包含用於 AI 代碼分析的系統提示和用戶提示範本。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class PromptTemplate {

    /**
     * 系統提示範本 - 定義 AI 的角色和行為（完整分析模式）
     */
    public static final String SYSTEM_PROMPT = """
        You are a security analysis expert specializing in OWASP Top 10 vulnerabilities.
        Your task is to analyze code for security issues and provide actionable remediation advice.

        Guidelines:
        1. Identify security vulnerabilities based on OWASP Top 10 standards
        2. Map each issue to the corresponding CWE (Common Weakness Enumeration) ID
        3. Provide clear, step-by-step fix instructions
        4. Include before/after code examples
        5. Estimate the effort required to fix (Simple/Medium/Complex + estimated hours)
        6. Be concise but comprehensive
        7. Focus on practical, implementable solutions

        Response format (JSON):
        {
          "issues": [
            {
              "owaspCategory": "A01:2021-Broken Access Control",
              "cweId": "CWE-284",
              "severity": "HIGH|MEDIUM|LOW",
              "description": "Brief description of the issue",
              "lineNumber": 42,
              "fixSuggestion": "Step-by-step instructions",
              "codeExample": {
                "before": "vulnerable code",
                "after": "secure code"
              },
              "effortEstimate": "Simple (0.5-1 hour)"
            }
          ],
          "summary": "Overall security assessment"
        }
        """;

    /**
     * 系統提示範本 - 只檢測模式（節省 Token）
     */
    public static final String SYSTEM_PROMPT_DETECTION_ONLY = """
        You are a security analysis expert specializing in OWASP Top 10 vulnerabilities.
        Your task is to ONLY detect and classify security issues. Do NOT provide fix suggestions.

        Guidelines:
        1. Identify security vulnerabilities based on OWASP Top 10 standards
        2. Map each issue to the corresponding CWE (Common Weakness Enumeration) ID
        3. Provide brief description ONLY (no fix suggestions, no code examples, no effort estimates)
        4. Be concise and focus on issue detection

        Response format (JSON):
        {
          "issues": [
            {
              "owaspCategory": "A01:2021-Broken Access Control",
              "cweId": "CWE-284",
              "severity": "HIGH|MEDIUM|LOW",
              "description": "Brief description of the issue",
              "lineNumber": 42
            }
          ]
        }
        """;

    /**
     * 用戶提示範本 - 代碼分析請求（完整模式）
     */
    public static final String USER_PROMPT_TEMPLATE = """
        Analyze the following %s code for OWASP Top 10 %s security vulnerabilities:

        File: %s

        Code:
        ```%s
        %s
        ```

        %s

        Please identify all security issues and provide detailed remediation advice.
        """;

    /**
     * 用戶提示範本 - 只檢測模式（節省 Token）
     */
    public static final String USER_PROMPT_TEMPLATE_DETECTION_ONLY = """
        Detect OWASP Top 10 %s security vulnerabilities in the following %s code:

        File: %s

        Code:
        ```%s
        %s
        ```

        Only identify and classify issues. Do NOT provide fix suggestions.
        """;

    /**
     * 建立分析代碼的用戶提示（完整模式 - 包含修復建議）
     *
     * @param request AI 請求
     * @return 格式化的用戶提示
     */
    public static String createAnalysisPrompt(AiRequest request) {
        String additionalContext = request.getAdditionalContext() != null
            ? "Additional context:\n" + request.getAdditionalContext()
            : "";

        return String.format(
            USER_PROMPT_TEMPLATE,
            request.getLanguage(),
            request.getOwaspVersion(),
            request.getFileName() != null ? request.getFileName() : "unknown",
            request.getLanguage(),
            request.getCode(),
            additionalContext
        ).trim();
    }

    /**
     * 建立只檢測模式的用戶提示（不包含修復建議，節省 Token）
     *
     * @param request AI 請求
     * @return 格式化的用戶提示
     */
    public static String createDetectionOnlyPrompt(AiRequest request) {
        return String.format(
            USER_PROMPT_TEMPLATE_DETECTION_ONLY,
            request.getOwaspVersion(),
            request.getLanguage(),
            request.getFileName() != null ? request.getFileName() : "unknown",
            request.getLanguage(),
            request.getCode()
        ).trim();
    }

    /**
     * 測試連接的簡單提示
     */
    public static final String TEST_PROMPT = "Hello! Please respond with 'OK' to confirm the connection.";

    /**
     * 建立修復建議提示
     *
     * @param code 問題代碼
     * @param owaspCategory OWASP 類別
     * @param cweId CWE ID
     * @return 格式化的修復建議提示
     */
    public static String createFixSuggestionPrompt(String code, String owaspCategory, String cweId) {
        return String.format("""
            Provide a detailed fix for the following security issue:

            OWASP Category: %s
            CWE ID: %s

            Vulnerable code:
            ```
            %s
            ```

            Please provide:
            1. Step-by-step remediation instructions
            2. Secure code example (before/after)
            3. Effort estimate (Simple/Medium/Complex + hours)
            4. Potential side effects or considerations
            """,
            owaspCategory,
            cweId,
            code
        );
    }

    /**
     * 建立工作量評估提示
     *
     * @param issueDescription 問題描述
     * @param codeContext 代碼上下文
     * @return 格式化的工作量評估提示
     */
    public static String createEffortEstimatePrompt(String issueDescription, String codeContext) {
        return String.format("""
            Estimate the effort required to fix this security issue:

            Issue: %s

            Code context:
            ```
            %s
            ```

            Provide estimate in format: "Category (hours)"
            Where Category is: Simple, Medium, or Complex
            Example: "Simple (0.5-1 hour)"
            """,
            issueDescription,
            codeContext
        );
    }

    // 私有建構子，防止實例化
    private PromptTemplate() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
