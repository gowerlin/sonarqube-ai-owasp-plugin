package com.github.sonarqube.ai;

import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiExecutionMode;
import com.github.sonarqube.ai.model.AiModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for AI configuration loading and validation
 *
 * 測試 AI 配置載入、驗證和轉換機制
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9, Story 9.8)
 */
class ConfigurationLoadingTest {

    // ========== API 模式配置測試 ==========

    @Test
    void testLoadOpenAiConfig() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("sk-test-key-123")
            .temperature(0.5)
            .maxTokens(3000)
            .build();

        assertThat(config.isValid()).isTrue();
        assertThat(config.getModel()).isEqualTo(AiModel.GPT_4);
        assertThat(config.getApiKey()).isEqualTo("sk-test-key-123");
        assertThat(config.getApiEndpoint()).isEqualTo("https://api.openai.com/v1/chat/completions");
        assertThat(config.getExecutionMode()).isEqualTo(AiExecutionMode.API);
        assertThat(config.getTemperature()).isEqualTo(0.5);
        assertThat(config.getMaxTokens()).isEqualTo(3000);
    }

    @Test
    void testLoadClaudeConfig() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .apiKey("sk-ant-test-key")
            .temperature(0.3)
            .maxTokens(4096)
            .build();

        assertThat(config.isValid()).isTrue();
        assertThat(config.getModel()).isEqualTo(AiModel.CLAUDE_3_OPUS);
        assertThat(config.getApiKey()).isEqualTo("sk-ant-test-key");
        assertThat(config.getApiEndpoint()).isEqualTo("https://api.anthropic.com/v1/messages");
        assertThat(config.getExecutionMode()).isEqualTo(AiExecutionMode.API);
    }

    @Test
    void testLoadGeminiApiConfig() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .apiKey("AIzaSy-test-key")
            .temperature(0.4)
            .maxTokens(2048)
            .build();

        assertThat(config.isValid()).isTrue();
        assertThat(config.getModel()).isEqualTo(AiModel.GEMINI_1_5_PRO);
        assertThat(config.getApiKey()).isEqualTo("AIzaSy-test-key");
        assertThat(config.getExecutionMode()).isEqualTo(AiExecutionMode.API);
    }

    // ========== CLI 模式配置測試 ==========

    @Test
    void testLoadGeminiCliConfig() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/usr/local/bin/gemini")
            .executionMode(AiExecutionMode.CLI)
            .temperature(0.3)
            .maxTokens(2000)
            .build();

        assertThat(config.isValid()).isTrue();
        assertThat(config.getModel()).isEqualTo(AiModel.GEMINI_1_5_PRO);
        assertThat(config.getCliPath()).isEqualTo("/usr/local/bin/gemini");
        assertThat(config.getExecutionMode()).isEqualTo(AiExecutionMode.CLI);
    }

    @Test
    void testLoadCopilotCliConfig() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/usr/local/bin/gh")
            .executionMode(AiExecutionMode.CLI)
            .build();

        assertThat(config.isValid()).isTrue();
        assertThat(config.getCliPath()).isEqualTo("/usr/local/bin/gh");
        assertThat(config.getExecutionMode()).isEqualTo(AiExecutionMode.CLI);
    }

    @Test
    void testLoadClaudeCliConfig() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .cliPath("/usr/local/bin/claude")
            .executionMode(AiExecutionMode.CLI)
            .build();

        assertThat(config.isValid()).isTrue();
        assertThat(config.getModel()).isEqualTo(AiModel.CLAUDE_3_OPUS);
        assertThat(config.getCliPath()).isEqualTo("/usr/local/bin/claude");
        assertThat(config.getExecutionMode()).isEqualTo(AiExecutionMode.CLI);
    }

    // ========== 預設值測試 ==========

    @Test
    void testConfigDefaults() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-key")
            .build();

        // 驗證預設值
        assertThat(config.getTimeoutSeconds()).isEqualTo(60);
        assertThat(config.getTemperature()).isEqualTo(0.3);
        assertThat(config.getMaxTokens()).isEqualTo(4096);
        assertThat(config.getMaxRetries()).isEqualTo(3);
        assertThat(config.getRetryDelayMs()).isEqualTo(1000);
        assertThat(config.getExecutionMode()).isEqualTo(AiExecutionMode.API);
    }

    @Test
    void testConfigOverrides() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-key")
            .timeoutSeconds(120)
            .temperature(0.7)
            .maxTokens(8192)
            .maxRetries(5)
            .retryDelayMs(2000)
            .build();

        assertThat(config.getTimeoutSeconds()).isEqualTo(120);
        assertThat(config.getTemperature()).isEqualTo(0.7);
        assertThat(config.getMaxTokens()).isEqualTo(8192);
        assertThat(config.getMaxRetries()).isEqualTo(5);
        assertThat(config.getRetryDelayMs()).isEqualTo(2000);
    }

    // ========== 配置驗證測試 ==========

    @Test
    void testValidation_ApiModeRequiresApiKey() {
        AiConfig.Builder builder = AiConfig.builder()
            .model(AiModel.GPT_4);
        // 沒有設定 apiKey

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invalid AI configuration");
    }

    @Test
    void testValidation_ApiModeRequiresEndpoint() {
        // API endpoint 應該由 model 自動設定
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-key")
            .build();

        assertThat(config.getApiEndpoint()).isNotNull();
        assertThat(config.getApiEndpoint()).isNotEmpty();
    }

    @Test
    void testValidation_CliModeRequiresCliPath() {
        AiConfig.Builder builder = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .executionMode(AiExecutionMode.CLI);
        // 沒有設定 cliPath

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invalid AI configuration");
    }

    @Test
    void testValidation_InvalidTemperature() {
        AiConfig.Builder builder = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-key")
            .temperature(3.0); // 超出範圍 (0.0-2.0)

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invalid AI configuration");
    }

    @Test
    void testValidation_NegativeTimeout() {
        AiConfig.Builder builder = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-key")
            .timeoutSeconds(-10);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invalid AI configuration");
    }

    @Test
    void testValidation_NegativeMaxTokens() {
        AiConfig.Builder builder = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-key")
            .maxTokens(-1000);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invalid AI configuration");
    }

    @Test
    void testValidation_NegativeRetries() {
        AiConfig.Builder builder = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-key")
            .maxRetries(-5);

        assertThatThrownBy(builder::build)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Invalid AI configuration");
    }

    // ========== 執行模式切換測試 ==========

    @Test
    void testModeSwitch_DefaultIsApi() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-key")
            .build();

        assertThat(config.getExecutionMode()).isEqualTo(AiExecutionMode.API);
    }

    @Test
    void testModeSwitch_ExplicitCli() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/usr/bin/gemini")
            .executionMode(AiExecutionMode.CLI)
            .build();

        assertThat(config.getExecutionMode()).isEqualTo(AiExecutionMode.CLI);
    }

    @Test
    void testModeSwitch_InvalidApiInCliMode() {
        // CLI 模式不需要 API Key
        AiConfig config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .cliPath("/usr/bin/gemini")
            .executionMode(AiExecutionMode.CLI)
            .build();

        assertThat(config.isValid()).isTrue();
        assertThat(config.getApiKey()).isNull();
    }

    // ========== API Endpoint 自動設定測試 ==========

    @Test
    void testAutoEndpoint_OpenAi() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-key")
            .build();

        assertThat(config.getApiEndpoint())
            .isEqualTo("https://api.openai.com/v1/chat/completions");
    }

    @Test
    void testAutoEndpoint_Claude() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .apiKey("test-key")
            .build();

        assertThat(config.getApiEndpoint())
            .isEqualTo("https://api.anthropic.com/v1/messages");
    }

    @Test
    void testEndpointOverride() {
        String customEndpoint = "https://custom-api.example.com/v1/chat";

        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-key")
            .apiEndpoint(customEndpoint)
            .build();

        assertThat(config.getApiEndpoint()).isEqualTo(customEndpoint);
    }

    // ========== 多種模型配置測試 ==========

    @Test
    void testMultipleModels_OpenAi() {
        AiModel[] openAiModels = {
            AiModel.GPT_4,
            AiModel.GPT_4_TURBO,
            AiModel.GPT_3_5_TURBO
        };

        for (AiModel model : openAiModels) {
            AiConfig config = AiConfig.builder()
                .model(model)
                .apiKey("test-key")
                .build();

            assertThat(config.isValid()).isTrue();
            assertThat(config.getModel()).isEqualTo(model);
            assertThat(config.getModel().isOpenAI()).isTrue();
        }
    }

    @Test
    void testMultipleModels_Claude() {
        AiModel[] claudeModels = {
            AiModel.CLAUDE_3_OPUS,
            AiModel.CLAUDE_3_SONNET,
            AiModel.CLAUDE_3_HAIKU
        };

        for (AiModel model : claudeModels) {
            AiConfig config = AiConfig.builder()
                .model(model)
                .apiKey("test-key")
                .build();

            assertThat(config.isValid()).isTrue();
            assertThat(config.getModel()).isEqualTo(model);
            assertThat(config.getModel().isClaude()).isTrue();
        }
    }

    @Test
    void testMultipleModels_Gemini() {
        AiModel[] geminiModels = {
            AiModel.GEMINI_1_5_PRO,
            AiModel.GEMINI_1_5_FLASH
        };

        for (AiModel model : geminiModels) {
            AiConfig config = AiConfig.builder()
                .model(model)
                .apiKey("test-key")
                .build();

            assertThat(config.isValid()).isTrue();
            assertThat(config.getModel()).isEqualTo(model);
            assertThat(config.getModel().isGemini()).isTrue();
        }
    }

    // ========== 配置序列化測試 (toString) ==========

    @Test
    void testConfigToString() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-key")
            .temperature(0.5)
            .maxTokens(3000)
            .build();

        String configStr = config.toString();

        assertThat(configStr).contains("gpt-4");
        assertThat(configStr).contains("60s"); // timeout
        assertThat(configStr).contains("0.50"); // temperature
        assertThat(configStr).contains("3000"); // maxTokens
    }
}
