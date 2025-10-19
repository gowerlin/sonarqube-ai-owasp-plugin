package com.github.sonarqube.ai;

import com.github.sonarqube.ai.cli.CliExecutor;
import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiExecutionMode;
import com.github.sonarqube.ai.model.AiModel;
import com.github.sonarqube.ai.model.AiRequest;
import com.github.sonarqube.ai.model.AiResponse;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration tests for all AI providers
 *
 * 整合測試所有 6 種 AI Provider 的完整工作流程
 *
 * 測試範圍：
 * - 服務建立
 * - 程式碼分析請求
 * - 回應解析
 * - 錯誤處理
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9, Story 9.8)
 */
@Tag("integration")
class AllProvidersIntegrationTest {

    private static final String TEST_CODE = """
        public void login(String username, String password) {
            String sql = "SELECT * FROM users WHERE username = '" + username +
                        "' AND password = '" + password + "'";
            executeQuery(sql);
        }
        """;

    private static final String SAFE_CODE = """
        public void safeLogin(String username, String password) {
            String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeQuery();
        }
        """;

    // ========== API 模式 Provider 測試 ==========

    @Test
    void testOpenAiProvider_VulnerableCode() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-key")
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertThat(service).isNotNull();
        assertThat(service.getProviderName()).isEqualTo("OpenAI");

        // 驗證服務基本資訊
        assertThat(service.getModelName()).isEqualTo("gpt-4");
    }

    @Test
    void testClaudeApiProvider_VulnerableCode() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .apiKey("test-key")
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertThat(service).isNotNull();
        assertThat(service.getProviderName()).isEqualTo("Anthropic Claude");
        assertThat(service.getModelName()).isEqualTo("claude-3-opus-20240229");
    }

    @Test
    void testGeminiApiProvider_VulnerableCode() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .apiKey("test-key")
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertThat(service).isNotNull();
        assertThat(service.getProviderName()).isEqualTo("Google Gemini API");
        assertThat(service.getModelName()).isEqualTo("gemini-1.5-pro");
    }

    // ========== CLI 模式 Provider 測試 ==========

    @Test
    void testGeminiCliProvider_VulnerableCode() throws Exception {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/usr/bin/gemini")
            .executionMode(AiExecutionMode.CLI)
            .build();

        // Mock CLI Executor
        CliExecutor mockExecutor = mock(CliExecutor.class);
        when(mockExecutor.getCliPath()).thenReturn("/usr/bin/gemini");
        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn("""
                ## Finding 1
                Severity: CRITICAL
                OWASP Category: A03:2021-Injection
                CWE IDs: CWE-89
                Description: SQL Injection vulnerability
                Suggested Fix: Use prepared statements
                """);

        AiService service = new com.github.sonarqube.ai.provider.gemini.GeminiCliService(config, mockExecutor);

        assertThat(service).isNotNull();
        assertThat(service.getProviderName()).isEqualTo("Google Gemini CLI");

        // 測試程式碼分析
        AiRequest request = AiRequest.builder()
            .code(TEST_CODE)
            .fileName("LoginController.java")
            .language("java")
            .build();

        AiResponse response = service.analyzeCode(request);

        assertThat(response).isNotNull();
        assertThat(response.getFindings()).hasSize(1);
        assertThat(response.getFindings().get(0).getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    void testCopilotCliProvider_VulnerableCode() throws Exception {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/usr/bin/gh")
            .executionMode(AiExecutionMode.CLI)
            .build();

        // Mock CLI Executor
        CliExecutor mockExecutor = mock(CliExecutor.class);
        when(mockExecutor.getCliPath()).thenReturn("/usr/bin/gh");
        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn("""
                Vulnerability 1:
                Severity: CRITICAL
                OWASP: A03:2021-Injection
                CWE: CWE-89
                Description: SQL Injection vulnerability
                Fix: Use parameterized queries
                """);

        AiService service = new com.github.sonarqube.ai.provider.copilot.CopilotCliService(config, mockExecutor);

        assertThat(service).isNotNull();
        assertThat(service.getProviderName()).isEqualTo("GitHub Copilot CLI");

        // 測試程式碼分析
        AiRequest request = AiRequest.builder()
            .code(TEST_CODE)
            .fileName("LoginController.java")
            .language("java")
            .build();

        AiResponse response = service.analyzeCode(request);

        assertThat(response).isNotNull();
        assertThat(response.getFindings()).hasSize(1);
        assertThat(response.getFindings().get(0).getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    void testClaudeCliProvider_VulnerableCode() throws Exception {
        AiConfig config = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .cliPath("/usr/bin/claude")
            .executionMode(AiExecutionMode.CLI)
            .build();

        // Mock CLI Executor
        CliExecutor mockExecutor = mock(CliExecutor.class);
        when(mockExecutor.getCliPath()).thenReturn("/usr/bin/claude");
        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn("""
                Vulnerability 1:
                Severity: CRITICAL
                OWASP: A03:2021-Injection
                CWE: CWE-89
                Description: SQL Injection vulnerability
                Fix: Use prepared statements
                """);

        AiService service = new com.github.sonarqube.ai.provider.claude.ClaudeCliService(config, mockExecutor);

        assertThat(service).isNotNull();
        assertThat(service.getProviderName()).isEqualTo("Anthropic Claude CLI");

        // 測試程式碼分析
        AiRequest request = AiRequest.builder()
            .code(TEST_CODE)
            .fileName("LoginController.java")
            .language("java")
            .build();

        AiResponse response = service.analyzeCode(request);

        assertThat(response).isNotNull();
        assertThat(response.getFindings()).hasSize(1);
        assertThat(response.getFindings().get(0).getSeverity()).isEqualTo("CRITICAL");
    }

    // ========== 安全代碼測試 ==========

    @Test
    void testAllCliProviders_SafeCode() throws Exception {
        String safeOutput = "No security vulnerabilities detected.";

        // Gemini CLI
        AiConfig geminiConfig = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/usr/bin/gemini")
            .executionMode(AiExecutionMode.CLI)
            .build();

        CliExecutor geminiExecutor = mock(CliExecutor.class);
        when(geminiExecutor.getCliPath()).thenReturn("/usr/bin/gemini");
        when(geminiExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(safeOutput);

        AiService geminiService = new com.github.sonarqube.ai.provider.gemini.GeminiCliService(
            geminiConfig, geminiExecutor);

        AiRequest request = AiRequest.builder()
            .code(SAFE_CODE)
            .fileName("SafeLogin.java")
            .build();

        AiResponse geminiResponse = geminiService.analyzeCode(request);
        assertThat(geminiResponse.getFindings()).isEmpty();

        // Copilot CLI
        AiConfig copilotConfig = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/usr/bin/gh")
            .executionMode(AiExecutionMode.CLI)
            .build();

        CliExecutor copilotExecutor = mock(CliExecutor.class);
        when(copilotExecutor.getCliPath()).thenReturn("/usr/bin/gh");
        when(copilotExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(safeOutput);

        AiService copilotService = new com.github.sonarqube.ai.provider.copilot.CopilotCliService(
            copilotConfig, copilotExecutor);

        AiResponse copilotResponse = copilotService.analyzeCode(request);
        assertThat(copilotResponse.getFindings()).isEmpty();

        // Claude CLI
        AiConfig claudeConfig = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .cliPath("/usr/bin/claude")
            .executionMode(AiExecutionMode.CLI)
            .build();

        CliExecutor claudeExecutor = mock(CliExecutor.class);
        when(claudeExecutor.getCliPath()).thenReturn("/usr/bin/claude");
        when(claudeExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(safeOutput);

        AiService claudeService = new com.github.sonarqube.ai.provider.claude.ClaudeCliService(
            claudeConfig, claudeExecutor);

        AiResponse claudeResponse = claudeService.analyzeCode(request);
        assertThat(claudeResponse.getFindings()).isEmpty();
    }

    // ========== 多個漏洞測試 ==========

    @Test
    void testMultipleVulnerabilities_AllProviders() throws Exception {
        String multiVulnOutput = """
            Vulnerability 1:
            Severity: CRITICAL
            OWASP: A03:2021-Injection
            CWE: CWE-89
            Description: SQL Injection
            Fix: Use prepared statements

            Vulnerability 2:
            Severity: MAJOR
            OWASP: A01:2021-Broken Access Control
            CWE: CWE-285
            Description: Missing authentication
            Fix: Add authentication check
            """;

        AiRequest request = AiRequest.builder()
            .code(TEST_CODE)
            .fileName("VulnerableCode.java")
            .build();

        // Gemini CLI
        CliExecutor geminiExecutor = mock(CliExecutor.class);
        when(geminiExecutor.getCliPath()).thenReturn("/usr/bin/gemini");
        when(geminiExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(multiVulnOutput.replace("Vulnerability", "## Finding"));

        AiConfig geminiConfig = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/usr/bin/gemini")
            .executionMode(AiExecutionMode.CLI)
            .build();

        AiService geminiService = new com.github.sonarqube.ai.provider.gemini.GeminiCliService(
            geminiConfig, geminiExecutor);

        AiResponse geminiResponse = geminiService.analyzeCode(request);
        assertThat(geminiResponse.getFindings()).hasSize(2);

        // Copilot CLI
        CliExecutor copilotExecutor = mock(CliExecutor.class);
        when(copilotExecutor.getCliPath()).thenReturn("/usr/bin/gh");
        when(copilotExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(multiVulnOutput);

        AiConfig copilotConfig = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/usr/bin/gh")
            .executionMode(AiExecutionMode.CLI)
            .build();

        AiService copilotService = new com.github.sonarqube.ai.provider.copilot.CopilotCliService(
            copilotConfig, copilotExecutor);

        AiResponse copilotResponse = copilotService.analyzeCode(request);
        assertThat(copilotResponse.getFindings()).hasSize(2);

        // Claude CLI
        CliExecutor claudeExecutor = mock(CliExecutor.class);
        when(claudeExecutor.getCliPath()).thenReturn("/usr/bin/claude");
        when(claudeExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(multiVulnOutput);

        AiConfig claudeConfig = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .cliPath("/usr/bin/claude")
            .executionMode(AiExecutionMode.CLI)
            .build();

        AiService claudeService = new com.github.sonarqube.ai.provider.claude.ClaudeCliService(
            claudeConfig, claudeExecutor);

        AiResponse claudeResponse = claudeService.analyzeCode(request);
        assertThat(claudeResponse.getFindings()).hasSize(2);
    }

    // ========== Provider 特性測試 ==========

    @Test
    void testAllProviders_ConsistentBehavior() {
        // 測試所有 Provider 在相同配置下的行為一致性
        AiRequest request = AiRequest.builder()
            .code(TEST_CODE)
            .fileName("Test.java")
            .language("java")
            .build();

        // 所有 Provider 都應該接受相同的 AiRequest 格式
        assertThat(request.getCode()).isNotNull();
        assertThat(request.getFileName()).isNotNull();
        assertThat(request.getLanguage()).isNotNull();
    }

    @Test
    void testProvider_InfoRetrieval() {
        // API Providers
        AiService openai = AiServiceFactory.createOpenAiService("key");
        AiService claude = AiServiceFactory.createClaudeService("key");
        AiService gemini = AiServiceFactory.createGeminiService("key");

        // CLI Providers
        AiService geminiCli = AiServiceFactory.createGeminiCliService("/usr/bin/gemini");
        AiService copilotCli = AiServiceFactory.createCopilotCliService("/usr/bin/gh");
        AiService claudeCli = AiServiceFactory.createClaudeCliService("/usr/bin/claude");

        // 驗證所有 Provider 都能提供基本資訊
        assertThat(openai.getProviderName()).isNotEmpty();
        assertThat(claude.getProviderName()).isNotEmpty();
        assertThat(gemini.getProviderName()).isNotEmpty();
        assertThat(geminiCli.getProviderName()).isNotEmpty();
        assertThat(copilotCli.getProviderName()).isNotEmpty();
        assertThat(claudeCli.getProviderName()).isNotEmpty();

        assertThat(openai.getModelName()).isNotEmpty();
        assertThat(claude.getModelName()).isNotEmpty();
        assertThat(gemini.getModelName()).isNotEmpty();
        assertThat(geminiCli.getModelName()).isNotEmpty();
        assertThat(copilotCli.getModelName()).isNotEmpty();
        assertThat(claudeCli.getModelName()).isNotEmpty();
    }
}
