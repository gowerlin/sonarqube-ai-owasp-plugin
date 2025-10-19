package com.github.sonarqube.ai.provider.copilot;

import com.github.sonarqube.ai.AiException;
import com.github.sonarqube.ai.cli.CliExecutionException;
import com.github.sonarqube.ai.cli.CliExecutor;
import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiModel;
import com.github.sonarqube.ai.model.AiRequest;
import com.github.sonarqube.ai.model.AiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CopilotCliService
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9, Story 9.5)
 */
class CopilotCliServiceTest {

    private AiConfig config;
    private CliExecutor mockExecutor;
    private CopilotCliService service;

    @BeforeEach
    void setUp() {
        config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO) // GitHub Copilot 使用內建模型，但需要設定
            .cliPath("/usr/local/bin/gh")
            .temperature(0.3f)
            .maxTokens(2000)
            .build();

        mockExecutor = mock(CliExecutor.class);
        when(mockExecutor.getCliPath()).thenReturn("/usr/local/bin/gh");

        service = new CopilotCliService(config, mockExecutor);
    }

    @Test
    void testConstructorWithValidConfig() {
        assertThat(service).isNotNull();
        assertThat(service.getProviderName()).isEqualTo("GitHub Copilot CLI");
        assertThat(service.getModelName()).isEqualTo("copilot");
    }

    @Test
    void testConstructorWithNullConfig() {
        assertThatThrownBy(() -> new CopilotCliService(null, mockExecutor))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("AiConfig cannot be null");
    }

    @Test
    void testConstructorWithNullExecutor() {
        assertThatThrownBy(() -> new CopilotCliService(config, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CliExecutor cannot be null");
    }

    @Test
    void testConstructorWithMissingCliPath() {
        AiConfig invalidConfig = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .build();

        assertThatThrownBy(() -> new CopilotCliService(invalidConfig, mockExecutor))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("GitHub Copilot CLI path is required");
    }

    @Test
    void testGetProviderName() {
        assertThat(service.getProviderName()).isEqualTo("GitHub Copilot CLI");
    }

    @Test
    void testGetModelName() {
        assertThat(service.getModelName()).isEqualTo("copilot");
    }

    @Test
    void testAnalyzeCodeWithStructuredOutput() throws Exception {
        // 模擬 GitHub Copilot CLI 輸出（結構化格式）
        String cliOutput = """
            Vulnerability 1:
            Severity: CRITICAL
            OWASP: A03:2021-Injection
            CWE: CWE-89
            Description: SQL Injection vulnerability due to unsanitized user input
            Fix: Use parameterized queries or prepared statements

            Vulnerability 2:
            Severity: MAJOR
            OWASP: A01:2021-Broken Access Control
            CWE: CWE-285
            Description: Missing authentication check allows unauthorized access
            Fix: Implement proper authentication and authorization middleware
            """;

        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(cliOutput);

        AiRequest request = AiRequest.builder()
            .code("SELECT * FROM users WHERE id = " + userId)
            .fileName("UserController.java")
            .language("java")
            .build();

        AiResponse response = service.analyzeCode(request);

        assertThat(response).isNotNull();
        assertThat(response.getFindings()).hasSize(2);

        // 驗證第一個發現
        assertThat(response.getFindings().get(0).getSeverity()).isEqualTo("CRITICAL");
        assertThat(response.getFindings().get(0).getOwaspCategory()).isEqualTo("A03:2021-Injection");
        assertThat(response.getFindings().get(0).getCweIds()).containsExactly(89);
        assertThat(response.getFindings().get(0).getMessage()).contains("SQL Injection");

        // 驗證第二個發現
        assertThat(response.getFindings().get(1).getSeverity()).isEqualTo("MAJOR");
        assertThat(response.getFindings().get(1).getOwaspCategory()).isEqualTo("A01:2021-Broken Access Control");
        assertThat(response.getFindings().get(1).getCweIds()).containsExactly(285);
    }

    @Test
    void testAnalyzeCodeWithAlternativeFormat() throws Exception {
        // 測試 "Issue N:" 格式
        String cliOutput = """
            Issue 1:
            Severity: BLOCKER
            OWASP: A02:2021-Cryptographic Failures
            CWE: CWE-326
            Description: Weak cryptographic algorithm used
            Fix: Use strong encryption algorithms like AES-256
            """;

        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(cliOutput);

        AiRequest request = AiRequest.builder()
            .code("// crypto code")
            .fileName("CryptoUtils.java")
            .build();

        AiResponse response = service.analyzeCode(request);

        assertThat(response).isNotNull();
        assertThat(response.getFindings()).hasSize(1);
        assertThat(response.getFindings().get(0).getSeverity()).isEqualTo("BLOCKER");
        assertThat(response.getFindings().get(0).getOwaspCategory()).isEqualTo("A02:2021-Cryptographic Failures");
    }

    @Test
    void testAnalyzeCodeWithNoVulnerabilities() throws Exception {
        String cliOutput = "No security vulnerabilities detected.";

        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(cliOutput);

        AiRequest request = AiRequest.builder()
            .code("public String safeMethod() { return \"safe\"; }")
            .fileName("SafeClass.java")
            .build();

        AiResponse response = service.analyzeCode(request);

        assertThat(response).isNotNull();
        assertThat(response.getFindings()).isEmpty();
    }

    @Test
    void testAnalyzeCodeWithNoIssuesFound() throws Exception {
        String cliOutput = "Analysis complete. No issues found.";

        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(cliOutput);

        AiRequest request = AiRequest.builder()
            .code("// safe code")
            .fileName("test.java")
            .build();

        AiResponse response = service.analyzeCode(request);

        assertThat(response).isNotNull();
        assertThat(response.getFindings()).isEmpty();
    }

    @Test
    void testAnalyzeCodeWithUnstructuredOutput() throws Exception {
        // 模擬非結構化輸出（備用解析）
        String cliOutput = "The code contains potential security vulnerabilities in authentication handling. " +
                           "Please review access control mechanisms and input validation. " +
                           "Consider implementing rate limiting and session management.";

        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(cliOutput);

        AiRequest request = AiRequest.builder()
            .code("// auth code")
            .fileName("AuthService.java")
            .build();

        AiResponse response = service.analyzeCode(request);

        assertThat(response).isNotNull();
        assertThat(response.getFindings()).hasSize(1);
        assertThat(response.getFindings().get(0).getSeverity()).isEqualTo("MAJOR");
        assertThat(response.getFindings().get(0).getMessage()).contains("security vulnerabilities");
    }

    @Test
    void testAnalyzeCodeCommandBuilding() throws Exception {
        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn("No security vulnerabilities detected.");

        AiRequest request = AiRequest.builder()
            .code("// test code")
            .fileName("test.java")
            .language("java")
            .build();

        service.analyzeCode(request);

        // 驗證命令結構
        ArgumentCaptor<String[]> commandCaptor = ArgumentCaptor.forClass(String[].class);
        verify(mockExecutor).executeCommand(commandCaptor.capture(), eq(null));

        String[] command = commandCaptor.getValue();
        assertThat(command).hasSizeGreaterThanOrEqualTo(6);
        assertThat(command[0]).isEqualTo("/usr/local/bin/gh");
        assertThat(command[1]).isEqualTo("copilot");
        assertThat(command[2]).isEqualTo("suggest");
        assertThat(command[3]).isEqualTo("-t");
        assertThat(command[4]).isEqualTo("security");
        assertThat(command[5]).contains("Security Analysis Request");
        assertThat(command[5]).contains("test.java");
        assertThat(command[5]).contains("// test code");
    }

    @Test
    void testAnalyzeCodeWithNullRequest() {
        assertThatThrownBy(() -> service.analyzeCode(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("AiRequest cannot be null");
    }

    @Test
    void testAnalyzeCodeWithEmptyOutput() throws Exception {
        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn("");

        AiRequest request = AiRequest.builder()
            .code("// code")
            .fileName("test.java")
            .build();

        assertThatThrownBy(() -> service.analyzeCode(request))
            .isInstanceOf(AiException.class)
            .hasMessageContaining("empty output");
    }

    @Test
    void testAnalyzeCodeWithCliExecutionException() throws Exception {
        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenThrow(new CliExecutionException("CLI execution failed", 1, "gh copilot", "", "error"));

        AiRequest request = AiRequest.builder()
            .code("// code")
            .fileName("test.java")
            .build();

        assertThatThrownBy(() -> service.analyzeCode(request))
            .isInstanceOf(AiException.class)
            .hasMessageContaining("Failed to execute CLI command");
    }

    @Test
    void testAnalyzeCodeWithIOException() throws Exception {
        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenThrow(new IOException("I/O error"));

        AiRequest request = AiRequest.builder()
            .code("// code")
            .fileName("test.java")
            .build();

        assertThatThrownBy(() -> service.analyzeCode(request))
            .isInstanceOf(AiException.class)
            .hasMessageContaining("CLI I/O error");
    }

    @Test
    void testAnalyzeCodeWithInterruptedException() throws Exception {
        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenThrow(new InterruptedException("Interrupted"));

        AiRequest request = AiRequest.builder()
            .code("// code")
            .fileName("test.java")
            .build();

        assertThatThrownBy(() -> service.analyzeCode(request))
            .isInstanceOf(AiException.class)
            .hasMessageContaining("CLI execution interrupted");
    }

    @Test
    void testTestConnectionSuccess() throws Exception {
        when(mockExecutor.isCliAvailable()).thenReturn(true);
        when(mockExecutor.getCliVersion()).thenReturn("GitHub CLI v2.40.0");

        boolean result = service.testConnection();

        assertThat(result).isTrue();
        verify(mockExecutor).isCliAvailable();
        verify(mockExecutor).getCliVersion();
    }

    @Test
    void testTestConnectionFailureCliNotAvailable() {
        when(mockExecutor.isCliAvailable()).thenReturn(false);

        boolean result = service.testConnection();

        assertThat(result).isFalse();
        verify(mockExecutor).isCliAvailable();
        verify(mockExecutor, never()).getCliVersion();
    }

    @Test
    void testTestConnectionFailureException() throws Exception {
        when(mockExecutor.isCliAvailable()).thenReturn(true);
        when(mockExecutor.getCliVersion()).thenThrow(new IOException("Failed to get version"));

        boolean result = service.testConnection();

        assertThat(result).isFalse();
    }

    @Test
    void testClose() {
        // close() 不應該拋出例外
        service.close();
    }

    @Test
    void testParseSeverityVariations() throws Exception {
        // 測試不同格式的 Severity 標記
        String cliOutput = """
            Vulnerability 1:
            SEVERITY: info
            Description: Test issue with lowercase severity
            """;

        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(cliOutput);

        AiRequest request = AiRequest.builder()
            .code("// code")
            .fileName("test.java")
            .build();

        AiResponse response = service.analyzeCode(request);

        assertThat(response.getFindings()).hasSize(1);
        assertThat(response.getFindings().get(0).getSeverity()).isEqualTo("INFO");
    }

    @Test
    void testParseMultipleCweIds() throws Exception {
        String cliOutput = """
            Vulnerability 1:
            Severity: CRITICAL
            CWE: CWE-79, CWE-80, CWE-85
            Description: Multiple CWE IDs for XSS vulnerabilities
            """;

        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(cliOutput);

        AiRequest request = AiRequest.builder()
            .code("// code")
            .fileName("test.java")
            .build();

        AiResponse response = service.analyzeCode(request);

        assertThat(response.getFindings()).hasSize(1);
        assertThat(response.getFindings().get(0).getCweIds())
            .containsExactly(79, 80, 85);
    }

    @Test
    void testParseFindingFormat() throws Exception {
        // 測試 "Finding N:" 格式
        String cliOutput = """
            Finding 1:
            Severity: MAJOR
            OWASP: A05:2021-Security Misconfiguration
            CWE: CWE-16
            Description: Configuration vulnerability detected
            Fix: Review security configuration settings
            """;

        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(cliOutput);

        AiRequest request = AiRequest.builder()
            .code("// config code")
            .fileName("Config.java")
            .build();

        AiResponse response = service.analyzeCode(request);

        assertThat(response.getFindings()).hasSize(1);
        assertThat(response.getFindings().get(0).getSeverity()).isEqualTo("MAJOR");
        assertThat(response.getFindings().get(0).getOwaspCategory()).isEqualTo("A05:2021-Security Misconfiguration");
    }
}
