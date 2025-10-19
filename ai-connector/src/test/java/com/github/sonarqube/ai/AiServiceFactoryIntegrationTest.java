package com.github.sonarqube.ai;

import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiExecutionMode;
import com.github.sonarqube.ai.model.AiModel;
import com.github.sonarqube.ai.provider.ClaudeService;
import com.github.sonarqube.ai.provider.OpenAiService;
import com.github.sonarqube.ai.provider.claude.ClaudeCliService;
import com.github.sonarqube.ai.provider.copilot.CopilotCliService;
import com.github.sonarqube.ai.provider.gemini.GeminiApiService;
import com.github.sonarqube.ai.provider.gemini.GeminiCliService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for AiServiceFactory
 *
 * 測試 AiServiceFactory 能正確建立所有 6 種 AI Provider 服務實例
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9, Story 9.8)
 */
class AiServiceFactoryIntegrationTest {

    // ========== API 模式測試 ==========

    @Test
    void testCreateOpenAiService() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-api-key")
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertThat(service).isNotNull();
        assertThat(service).isInstanceOf(OpenAiService.class);
        assertThat(service.getProviderName()).isEqualTo("OpenAI");
        assertThat(service.getModelName()).isEqualTo("gpt-4");
    }

    @Test
    void testCreateClaudeApiService() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .apiKey("test-api-key")
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertThat(service).isNotNull();
        assertThat(service).isInstanceOf(ClaudeService.class);
        assertThat(service.getProviderName()).isEqualTo("Anthropic Claude");
        assertThat(service.getModelName()).isEqualTo("claude-3-opus-20240229");
    }

    @Test
    void testCreateGeminiApiService() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .apiKey("test-api-key")
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertThat(service).isNotNull();
        assertThat(service).isInstanceOf(GeminiApiService.class);
        assertThat(service.getProviderName()).isEqualTo("Google Gemini API");
        assertThat(service.getModelName()).isEqualTo("gemini-1.5-pro");
    }

    // ========== CLI 模式測試 ==========

    @Test
    void testCreateGeminiCliService() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/usr/local/bin/gemini")
            .executionMode(AiExecutionMode.CLI)
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertThat(service).isNotNull();
        assertThat(service).isInstanceOf(GeminiCliService.class);
        assertThat(service.getProviderName()).isEqualTo("Google Gemini CLI");
        assertThat(service.getModelName()).isEqualTo("gemini-1.5-pro");
    }

    @Test
    void testCreateCopilotCliService() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO) // 使用通用模型
            .cliPath("/usr/local/bin/gh")
            .executionMode(AiExecutionMode.CLI)
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertThat(service).isNotNull();
        assertThat(service).isInstanceOf(CopilotCliService.class);
        assertThat(service.getProviderName()).isEqualTo("GitHub Copilot CLI");
        assertThat(service.getModelName()).isEqualTo("copilot");
    }

    @Test
    void testCreateClaudeCliService() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .cliPath("/usr/local/bin/claude")
            .executionMode(AiExecutionMode.CLI)
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertThat(service).isNotNull();
        assertThat(service).isInstanceOf(ClaudeCliService.class);
        assertThat(service.getProviderName()).isEqualTo("Anthropic Claude CLI");
        assertThat(service.getModelName()).isEqualTo("claude-3-opus");
    }

    // ========== CLI 路徑智慧偵測測試 ==========

    @Test
    void testCliPathDetection_Gemini() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("C:\\Tools\\gemini.exe") // Windows 路徑
            .executionMode(AiExecutionMode.CLI)
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertThat(service).isInstanceOf(GeminiCliService.class);
    }

    @Test
    void testCliPathDetection_Copilot_gh() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/opt/homebrew/bin/gh") // macOS homebrew 路徑
            .executionMode(AiExecutionMode.CLI)
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertThat(service).isInstanceOf(CopilotCliService.class);
    }

    @Test
    void testCliPathDetection_Copilot_copilot() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/usr/bin/copilot") // 包含 copilot 關鍵字
            .executionMode(AiExecutionMode.CLI)
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertThat(service).isInstanceOf(CopilotCliService.class);
    }

    @Test
    void testCliPathDetection_Claude() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .cliPath("/usr/local/bin/claude")
            .executionMode(AiExecutionMode.CLI)
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertThat(service).isInstanceOf(ClaudeCliService.class);
    }

    // ========== 模型類型備用判斷測試 ==========

    @Test
    void testModelFallback_Gemini() {
        // 當路徑沒有明確關鍵字時，根據模型類型判斷
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/some/custom/path/ai-tool") // 沒有明確關鍵字
            .executionMode(AiExecutionMode.CLI)
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertThat(service).isInstanceOf(GeminiCliService.class);
    }

    @Test
    void testModelFallback_Claude() {
        // 當路徑沒有明確關鍵字時，根據模型類型判斷
        AiConfig config = AiConfig.builder()
            .model(AiModel.CLAUDE_3_SONNET)
            .cliPath("/some/custom/path/ai-tool") // 沒有明確關鍵字
            .executionMode(AiExecutionMode.CLI)
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertThat(service).isInstanceOf(ClaudeCliService.class);
    }

    // ========== 便捷方法測試 ==========

    @Test
    void testConvenienceMethod_createOpenAiService() {
        AiService service = AiServiceFactory.createOpenAiService("test-key");

        assertThat(service).isInstanceOf(OpenAiService.class);
        assertThat(service.getModelName()).isEqualTo("gpt-4");
    }

    @Test
    void testConvenienceMethod_createClaudeService() {
        AiService service = AiServiceFactory.createClaudeService("test-key");

        assertThat(service).isInstanceOf(ClaudeService.class);
        assertThat(service.getModelName()).isEqualTo("claude-3-opus-20240229");
    }

    @Test
    void testConvenienceMethod_createGeminiService() {
        AiService service = AiServiceFactory.createGeminiService("test-key");

        assertThat(service).isInstanceOf(GeminiApiService.class);
        assertThat(service.getModelName()).isEqualTo("gemini-1.5-pro");
    }

    @Test
    void testConvenienceMethod_createGeminiCliService() {
        AiService service = AiServiceFactory.createGeminiCliService("/usr/bin/gemini");

        assertThat(service).isInstanceOf(GeminiCliService.class);
    }

    @Test
    void testConvenienceMethod_createCopilotCliService() {
        AiService service = AiServiceFactory.createCopilotCliService("/usr/bin/gh");

        assertThat(service).isInstanceOf(CopilotCliService.class);
    }

    @Test
    void testConvenienceMethod_createClaudeCliService() {
        AiService service = AiServiceFactory.createClaudeCliService("/usr/bin/claude");

        assertThat(service).isInstanceOf(ClaudeCliService.class);
    }

    // ========== 錯誤處理測試 ==========

    @Test
    void testCreateServiceWithNullConfig() {
        assertThatThrownBy(() -> AiServiceFactory.createService(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid AI configuration");
    }

    @Test
    void testCreateServiceWithInvalidConfig() {
        // API 模式但沒有 API Key
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .build();

        assertThatThrownBy(() -> AiServiceFactory.createService(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid AI configuration");
    }

    @Test
    void testCreateCliServiceWithoutCliPath() {
        // CLI 模式但沒有 CLI Path
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .executionMode(AiExecutionMode.CLI)
            .build();

        assertThatThrownBy(() -> AiServiceFactory.createService(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid AI configuration");
    }

    @Test
    void testCreateCliServiceWithUnsupportedTool() {
        // CLI 模式但工具不支援
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4) // OpenAI 沒有 CLI 模式
            .cliPath("/usr/bin/unsupported-tool")
            .executionMode(AiExecutionMode.CLI)
            .build();

        assertThatThrownBy(() -> AiServiceFactory.createService(config))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unsupported CLI tool");
    }

    // ========== 執行模式切換測試 ==========

    @Test
    void testSwitchMode_ApiToCli_Gemini() {
        // API 模式
        AiConfig apiConfig = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .apiKey("test-key")
            .build();

        AiService apiService = AiServiceFactory.createService(apiConfig);
        assertThat(apiService).isInstanceOf(GeminiApiService.class);

        // 切換到 CLI 模式
        AiConfig cliConfig = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/usr/bin/gemini")
            .executionMode(AiExecutionMode.CLI)
            .build();

        AiService cliService = AiServiceFactory.createService(cliConfig);
        assertThat(cliService).isInstanceOf(GeminiCliService.class);
    }

    @Test
    void testSwitchMode_ApiToCli_Claude() {
        // API 模式
        AiConfig apiConfig = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .apiKey("test-key")
            .build();

        AiService apiService = AiServiceFactory.createService(apiConfig);
        assertThat(apiService).isInstanceOf(ClaudeService.class);

        // 切換到 CLI 模式
        AiConfig cliConfig = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .cliPath("/usr/bin/claude")
            .executionMode(AiExecutionMode.CLI)
            .build();

        AiService cliService = AiServiceFactory.createService(cliConfig);
        assertThat(cliService).isInstanceOf(ClaudeCliService.class);
    }
}
