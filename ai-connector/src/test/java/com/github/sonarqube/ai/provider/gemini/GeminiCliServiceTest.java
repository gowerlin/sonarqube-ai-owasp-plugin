package com.github.sonarqube.ai.provider.gemini;

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
 * Unit tests for GeminiCliService
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9, Story 9.4)
 */
class GeminiCliServiceTest {

    private AiConfig config;
    private CliExecutor mockExecutor;
    private GeminiCliService service;

    @BeforeEach
    void setUp() {
        config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/usr/local/bin/gemini")
            .temperature(0.3f)
            .maxTokens(2000)
            .build();

        mockExecutor = mock(CliExecutor.class);
        when(mockExecutor.getCliPath()).thenReturn("/usr/local/bin/gemini");

        service = new GeminiCliService(config, mockExecutor);
    }

    @Test
    void testConstructorWithValidConfig() {
        assertThat(service).isNotNull();
        assertThat(service.getProviderName()).isEqualTo("Google Gemini CLI");
        assertThat(service.getModelName()).isEqualTo("gemini-1.5-pro");
    }

    @Test
    void testConstructorWithNullConfig() {
        assertThatThrownBy(() -> new GeminiCliService(null, mockExecutor))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("AiConfig cannot be null");
    }

    @Test
    void testConstructorWithNullExecutor() {
        assertThatThrownBy(() -> new GeminiCliService(config, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CliExecutor cannot be null");
    }

    @Test
    void testConstructorWithMissingCliPath() {
        AiConfig invalidConfig = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .build();

        assertThatThrownBy(() -> new GeminiCliService(invalidConfig, mockExecutor))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Gemini CLI path is required");
    }

    @Test
    void testGetProviderName() {
        assertThat(service.getProviderName()).isEqualTo("Google Gemini CLI");
    }

    @Test
    void testGetModelName() {
        assertThat(service.getModelName()).isEqualTo("gemini-1.5-pro");
    }

    @Test
    void testGetModelNameWithNullModel() {
        AiConfig configWithoutModel = AiConfig.builder()
            .cliPath("/usr/local/bin/gemini")
            .build();

        GeminiCliService serviceWithoutModel = new GeminiCliService(configWithoutModel, mockExecutor);

        assertThat(serviceWithoutModel.getModelName()).isEqualTo("gemini-1.5-pro");
    }

    @Test
    void testAnalyzeCodeWithStructuredOutput() throws Exception {
        // 模擬 Gemini CLI 輸出（結構化格式）
        String cliOutput = """
            ## Finding 1
            Severity: CRITICAL
            OWASP Category: A03:2021-Injection
            CWE IDs: CWE-89
            Description: SQL Injection vulnerability in user input handling
            Suggested Fix: Use parameterized queries instead of string concatenation

            ## Finding 2
            Severity: MAJOR
            OWASP Category: A01:2021-Broken Access Control
            CWE IDs: CWE-285, CWE-639
            Description: Missing authentication check on sensitive endpoint
            Suggested Fix: Implement proper authentication and authorization checks
            """;

        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(cliOutput);

        AiRequest request = AiRequest.builder("SELECT * FROM users WHERE id = ' + request_id + '")
            .fileName("UserService.java")
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
        assertThat(response.getFindings().get(1).getCweIds()).containsExactly(285, 639);
    }

    @Test
    void testAnalyzeCodeWithNoVulnerabilities() throws Exception {
        String cliOutput = "No security vulnerabilities detected.";

        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(cliOutput);

        AiRequest request = AiRequest.builder("public String safeMethod() { return \"hello\"; }")
            .fileName("SafeClass.java")
            .build();

        AiResponse response = service.analyzeCode(request);

        assertThat(response).isNotNull();
        assertThat(response.getFindings()).isEmpty();
    }

    @Test
    void testAnalyzeCodeWithUnstructuredOutput() throws Exception {
        // 模擬非結構化輸出（備用解析）
        String cliOutput = "The code contains potential security vulnerabilities. " +
                           "Please review the authentication logic and input validation.";

        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(cliOutput);

        AiRequest request = AiRequest.builder("// some code")
            .fileName("test.java")
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

        AiRequest request = AiRequest.builder("// test code")
            .fileName("test.java")
            .language("java")
            .build();

        service.analyzeCode(request);

        // 驗證命令結構
        ArgumentCaptor<String[]> commandCaptor = ArgumentCaptor.forClass(String[].class);
        verify(mockExecutor).executeCommand(commandCaptor.capture(), eq(null));

        String[] command = commandCaptor.getValue();
        assertThat(command).hasSizeGreaterThanOrEqualTo(3);
        assertThat(command[0]).isEqualTo("/usr/local/bin/gemini");
        assertThat(command[1]).isEqualTo("chat");
        assertThat(command[2]).contains("You are a security expert");
        assertThat(command[2]).contains("test.java");
        assertThat(command[2]).contains("// test code");
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

        AiRequest request = AiRequest.builder("// code")
            .fileName("test.java")
            .build();

        assertThatThrownBy(() -> service.analyzeCode(request))
            .isInstanceOf(AiException.class)
            .hasMessageContaining("empty output");
    }

    @Test
    void testAnalyzeCodeWithCliExecutionException() throws Exception {
        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenThrow(new CliExecutionException("CLI execution failed", 1, "gemini chat", "", "error"));

        AiRequest request = AiRequest.builder("// code")
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

        AiRequest request = AiRequest.builder("// code")
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

        AiRequest request = AiRequest.builder("// code")
            .fileName("test.java")
            .build();

        assertThatThrownBy(() -> service.analyzeCode(request))
            .isInstanceOf(AiException.class)
            .hasMessageContaining("CLI execution interrupted");
    }

    @Test
    void testTestConnectionSuccess() throws Exception {
        when(mockExecutor.isCliAvailable()).thenReturn(true);
        when(mockExecutor.getCliVersion()).thenReturn("Gemini CLI v1.0.0");

        boolean result = service.testConnection();

        assertThat(result).isTrue();
        verify(mockExecutor).isCliAvailable();
        verify(mockExecutor).getCliVersion();
    }

    @Test
    void testTestConnectionFailureCliNotAvailable() throws Exception {
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
            ## Finding 1
            SEVERITY: blocker
            Description: Test issue
            """;

        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(cliOutput);

        AiRequest request = AiRequest.builder("// code")
            .fileName("test.java")
            .build();

        AiResponse response = service.analyzeCode(request);

        assertThat(response.getFindings()).hasSize(1);
        assertThat(response.getFindings().get(0).getSeverity()).isEqualTo("BLOCKER");
    }

    @Test
    void testParseMultipleCweIds() throws Exception {
        String cliOutput = """
            ## Finding 1
            Severity: CRITICAL
            CWE IDs: CWE-89, CWE-564, CWE-943
            Description: Multiple CWE IDs test
            """;

        when(mockExecutor.executeCommand(any(String[].class), eq(null)))
            .thenReturn(cliOutput);

        AiRequest request = AiRequest.builder("// code")
            .fileName("test.java")
            .build();

        AiResponse response = service.analyzeCode(request);

        assertThat(response.getFindings()).hasSize(1);
        assertThat(response.getFindings().get(0).getCweIds())
            .containsExactly(89, 564, 943);
    }
}
