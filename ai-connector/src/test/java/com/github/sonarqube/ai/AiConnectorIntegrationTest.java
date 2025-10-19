package com.github.sonarqube.ai;

import com.github.sonarqube.ai.analyzer.AiResponseParser;
import com.github.sonarqube.ai.analyzer.FixSuggestionFormatter;
import com.github.sonarqube.ai.cache.AiCacheManager;
import com.github.sonarqube.ai.model.*;
import com.github.sonarqube.ai.provider.ClaudeService;
import com.github.sonarqube.ai.provider.OpenAiService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 連接器整合測試
 *
 * 測試完整的 AI 分析流程：
 * 1. 配置 AI 服務
 * 2. 創建分析請求
 * 3. 模擬 AI 回應解析
 * 4. 快取功能測試
 * 5. 修復建議生成
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
class AiConnectorIntegrationTest {

    @Test
    void testOpenAiServiceConfiguration() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-key")
            .build();

        OpenAiService service = new OpenAiService(config);

        assertNotNull(service);
        assertEquals("OpenAI", service.getProviderName());
        assertEquals("gpt-4", service.getModelName());

        service.close();
    }

    @Test
    void testClaudeServiceConfiguration() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .apiKey("test-key")
            .build();

        ClaudeService service = new ClaudeService(config);

        assertNotNull(service);
        assertEquals("Anthropic", service.getProviderName());
        assertEquals("claude-3-opus-20240229", service.getModelName());

        service.close();
    }

    @Test
    void testAiRequestCreation() {
        AiRequest request = AiRequest.builder("public void unsafeMethod(String input) { System.out.println(input); }")
            .fileName("UnsafeClass.java")
            .language("java")
            .owaspVersion("2021")
            .build();

        assertNotNull(request);
        assertEquals("UnsafeClass.java", request.getFileName());
        assertEquals("java", request.getLanguage());
        assertEquals("2021", request.getOwaspVersion());
        assertTrue(request.getCodeSnippet().contains("unsafeMethod"));
    }

    @Test
    void testResponseParsingWorkflow() {
        // 模擬 AI 回應
        String mockAiResponse = """
            {
              "issues": [
                {
                  "owaspCategory": "A03:2021-Injection",
                  "cweId": "CWE-89",
                  "severity": "HIGH",
                  "description": "Potential SQL injection vulnerability",
                  "lineNumber": 15,
                  "fixSuggestion": "Use parameterized queries instead of string concatenation",
                  "codeExample": {
                    "before": "String query = \\"SELECT * FROM users WHERE id = '\\" + userId + \\"'\\";",
                    "after": "PreparedStatement stmt = conn.prepareStatement(\\"SELECT * FROM users WHERE id = ?\\"); stmt.setString(1, userId);"
                  },
                  "effortEstimate": "Simple (0.5-1 hour)"
                }
              ]
            }
            """;

        AiResponseParser parser = new AiResponseParser();
        List<SecurityIssue> issues = parser.parseSecurityIssues(mockAiResponse);

        assertNotNull(issues);
        assertEquals(1, issues.size());

        SecurityIssue issue = issues.get(0);
        assertEquals("A03:2021-Injection", issue.getOwaspCategory());
        assertEquals("CWE-89", issue.getCweId());
        assertEquals(SecurityIssue.Severity.HIGH, issue.getSeverity());
        assertEquals(15, issue.getLineNumber());
        assertNotNull(issue.getFixSuggestion());
        assertNotNull(issue.getCodeExample());
    }

    @Test
    void testCacheIntegration() {
        AiCacheManager cacheManager = new AiCacheManager(100, 1);

        AiRequest request = AiRequest.builder("public void test() {}")
            .fileName("Test.java")
            .language("java")
            .build();

        // 快取未命中
        AiResponse cachedResponse1 = cacheManager.getFromCache(request);
        assertNull(cachedResponse1);

        // 存入快取
        AiResponse response = AiResponse.success()
            .analysisResult("No issues found")
            .tokensUsed(50)
            .build();

        cacheManager.putToCache(request, response);

        // 快取命中
        AiResponse cachedResponse2 = cacheManager.getFromCache(request);
        assertNotNull(cachedResponse2);
        assertEquals("No issues found", cachedResponse2.getAnalysisResult());

        // 驗證統計
        AiCacheManager.CacheStats stats = cacheManager.getStats();
        assertEquals(1, stats.getHitCount());
        assertEquals(1, stats.getMissCount());
    }

    @Test
    void testServiceWithCacheIntegration() {
        AiCacheManager cacheManager = new AiCacheManager();

        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-key")
            .build();

        OpenAiService service = new OpenAiService(config, cacheManager);

        assertNotNull(service);
        assertEquals("OpenAI", service.getProviderName());

        service.close();
    }

    @Test
    void testFixSuggestionFormatterIntegration() {
        // 創建測試問題
        SecurityIssue issue = new SecurityIssue();
        issue.setOwaspCategory("A03:2021-Injection");
        issue.setCweId("CWE-89");
        issue.setSeverity(SecurityIssue.Severity.HIGH);
        issue.setDescription("SQL Injection vulnerability");
        issue.setLineNumber(42);
        issue.setFixSuggestion("Use prepared statements");

        SecurityIssue.CodeExample example = new SecurityIssue.CodeExample();
        example.setBefore("String query = \"SELECT * FROM users WHERE id = '\" + userId + \"'\";");
        example.setAfter("PreparedStatement stmt = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\");");
        issue.setCodeExample(example);

        // 格式化建議
        FixSuggestionFormatter formatter = new FixSuggestionFormatter();
        String singleIssueReport = formatter.formatSingleIssue(issue);

        assertNotNull(singleIssueReport);
        assertTrue(singleIssueReport.contains("A03:2021-Injection"));
        assertTrue(singleIssueReport.contains("CWE-89"));
        assertTrue(singleIssueReport.contains("HIGH"));
        assertTrue(singleIssueReport.contains("SQL Injection"));
        assertTrue(singleIssueReport.contains("prepared statements"));

        // 完整報告
        List<SecurityIssue> issues = List.of(issue);
        String fullReport = formatter.formatReport(issues);

        assertNotNull(fullReport);
        assertTrue(fullReport.contains("安全分析報告"));
        assertTrue(fullReport.contains("問題摘要"));
        assertTrue(fullReport.contains("高嚴重性問題"));
    }

    @Test
    void testCompleteWorkflow() {
        // 1. 配置
        AiCacheManager cacheManager = new AiCacheManager();
        AiResponseParser parser = new AiResponseParser();
        FixSuggestionFormatter formatter = new FixSuggestionFormatter();

        // 2. 創建請求
        AiRequest request = AiRequest.builder("public void unsafeMethod(String input) { " +
            "String query = \"SELECT * FROM users WHERE name = '\" + input + \"'\"; }")
            .fileName("UnsafeDAO.java")
            .language("java")
            .owaspVersion("2021")
            .build();

        // 3. 模擬 AI 回應
        String mockAiResponse = """
            {
              "issues": [
                {
                  "owaspCategory": "A03:2021-Injection",
                  "cweId": "CWE-89",
                  "severity": "HIGH",
                  "description": "SQL Injection vulnerability detected",
                  "lineNumber": 1,
                  "fixSuggestion": "Use PreparedStatement with parameterized queries"
                }
              ]
            }
            """;

        // 4. 解析回應
        List<SecurityIssue> issues = parser.parseSecurityIssues(mockAiResponse);
        assertNotNull(issues);
        assertEquals(1, issues.size());

        // 5. 創建 AiResponse
        AiResponse response = AiResponse.success()
            .analysisResult(mockAiResponse)
            .issues(issues)
            .tokensUsed(100)
            .processingTimeMs(500L)
            .modelUsed("gpt-4")
            .build();

        // 6. 測試快取
        cacheManager.putToCache(request, response);
        AiResponse cachedResponse = cacheManager.getFromCache(request);
        assertNotNull(cachedResponse);
        assertEquals(1, cachedResponse.getIssues().size());

        // 7. 生成修復建議
        String report = formatter.formatReport(issues);
        assertNotNull(report);
        assertTrue(report.contains("SQL Injection"));
        assertTrue(report.contains("PreparedStatement"));
    }

    @Test
    void testMultipleModelsConfiguration() {
        // 測試所有支援的模型
        AiModel[] allModels = {
            AiModel.GPT_4,
            AiModel.GPT_4_TURBO,
            AiModel.GPT_3_5_TURBO,
            AiModel.CLAUDE_3_OPUS,
            AiModel.CLAUDE_3_SONNET,
            AiModel.CLAUDE_3_HAIKU
        };

        for (AiModel model : allModels) {
            AiConfig config = AiConfig.builder()
                .model(model)
                .apiKey("test-key")
                .build();

            assertNotNull(config);
            assertEquals(model, config.getModel());
            assertTrue(config.isValid());
        }
    }

    @Test
    void testPromptTemplateGeneration() {
        AiRequest request = AiRequest.builder("public void test() {}")
            .fileName("Test.java")
            .language("java")
            .owaspVersion("2021")
            .build();

        String prompt = PromptTemplate.createAnalysisPrompt(request);

        assertNotNull(prompt);
        assertTrue(prompt.contains("Test.java"));
        assertTrue(prompt.contains("java"));
        assertTrue(prompt.contains("2021"));
        assertTrue(prompt.contains("public void test() {}"));
    }

    @Test
    void testErrorHandlingWorkflow() {
        // 測試無效配置
        assertThrows(IllegalArgumentException.class, () -> {
            new OpenAiService(null);
        });

        // 測試模型不匹配
        AiConfig claudeConfig = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .apiKey("test-key")
            .build();

        assertThrows(IllegalArgumentException.class, () -> {
            new OpenAiService(claudeConfig);
        });

        // 測試失敗回應不被快取
        AiCacheManager cacheManager = new AiCacheManager();
        AiRequest request = AiRequest.builder("test code")
            .fileName("test.java")
            .build();

        AiResponse failureResponse = AiResponse.failure("API error")
            .build();

        cacheManager.putToCache(request, failureResponse);
        AiResponse cached = cacheManager.getFromCache(request);
        assertNull(cached, "Failed responses should not be cached");
    }
}
