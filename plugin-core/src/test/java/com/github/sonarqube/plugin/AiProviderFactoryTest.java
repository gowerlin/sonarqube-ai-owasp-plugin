package com.github.sonarqube.plugin;

import com.github.sonarqube.plugin.ai.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AI Provider Factory 單元測試
 *
 * 測試範圍：
 * - Provider 類型識別
 * - ProviderConfig 建構器
 * - CLI/API 模式判斷
 * - 預設 CLI 路徑
 *
 * @since 3.0.0 (Epic 8, Story 8.1)
 */
@DisplayName("AiProviderFactory Unit Tests")
public class AiProviderFactoryTest {

    @Test
    @DisplayName("測試 ProviderType.fromConfigValue() 正確解析")
    void testProviderTypeFromConfigValue() {
        // API 模式
        assertEquals(AiProviderFactory.ProviderType.OPENAI_API,
                AiProviderFactory.ProviderType.fromConfigValue("openai"));
        assertEquals(AiProviderFactory.ProviderType.ANTHROPIC_API,
                AiProviderFactory.ProviderType.fromConfigValue("anthropic"));
        assertEquals(AiProviderFactory.ProviderType.GEMINI_API,
                AiProviderFactory.ProviderType.fromConfigValue("gemini-api"));

        // CLI 模式
        assertEquals(AiProviderFactory.ProviderType.GEMINI_CLI,
                AiProviderFactory.ProviderType.fromConfigValue("gemini-cli"));
        assertEquals(AiProviderFactory.ProviderType.COPILOT_CLI,
                AiProviderFactory.ProviderType.fromConfigValue("copilot-cli"));
        assertEquals(AiProviderFactory.ProviderType.CLAUDE_CLI,
                AiProviderFactory.ProviderType.fromConfigValue("claude-cli"));
    }

    @Test
    @DisplayName("測試 ProviderType.fromConfigValue() 不區分大小寫")
    void testProviderTypeFromConfigValueCaseInsensitive() {
        assertEquals(AiProviderFactory.ProviderType.OPENAI_API,
                AiProviderFactory.ProviderType.fromConfigValue("OpenAI"));
        assertEquals(AiProviderFactory.ProviderType.GEMINI_CLI,
                AiProviderFactory.ProviderType.fromConfigValue("GEMINI-CLI"));
    }

    @Test
    @DisplayName("測試 ProviderType.fromConfigValue() 無效值拋出異常")
    void testProviderTypeFromConfigValueInvalid() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            AiProviderFactory.ProviderType.fromConfigValue("invalid-provider");
        });
        assertTrue(exception.getMessage().contains("Unknown provider type"));
    }

    @Test
    @DisplayName("測試 isCli() 正確判斷 CLI 模式")
    void testIsCliMethod() {
        // CLI 模式應為 true
        assertTrue(AiProviderFactory.ProviderType.GEMINI_CLI.isCli());
        assertTrue(AiProviderFactory.ProviderType.COPILOT_CLI.isCli());
        assertTrue(AiProviderFactory.ProviderType.CLAUDE_CLI.isCli());

        // API 模式應為 false
        assertFalse(AiProviderFactory.ProviderType.OPENAI_API.isCli());
        assertFalse(AiProviderFactory.ProviderType.ANTHROPIC_API.isCli());
        assertFalse(AiProviderFactory.ProviderType.GEMINI_API.isCli());
    }

    @Test
    @DisplayName("測試 isApi() 正確判斷 API 模式")
    void testIsApiMethod() {
        // API 模式應為 true
        assertTrue(AiProviderFactory.ProviderType.OPENAI_API.isApi());
        assertTrue(AiProviderFactory.ProviderType.ANTHROPIC_API.isApi());
        assertTrue(AiProviderFactory.ProviderType.GEMINI_API.isApi());

        // CLI 模式應為 false
        assertFalse(AiProviderFactory.ProviderType.GEMINI_CLI.isApi());
        assertFalse(AiProviderFactory.ProviderType.COPILOT_CLI.isApi());
        assertFalse(AiProviderFactory.ProviderType.CLAUDE_CLI.isApi());
    }

    @Test
    @DisplayName("測試 ProviderConfig.Builder 建構 API 配置")
    void testProviderConfigBuilderForApi() {
        AiProviderFactory.ProviderConfig config = AiProviderFactory.ProviderConfig
                .builder(AiProviderFactory.ProviderType.OPENAI_API)
                .apiKey("sk-test-key")
                .model("gpt-4")
                .temperature(0.5)
                .maxTokens(3000)
                .timeoutSeconds(90)
                .build();

        assertEquals(AiProviderFactory.ProviderType.OPENAI_API, config.getType());
        assertEquals("sk-test-key", config.getApiKey());
        assertEquals("gpt-4", config.getModel());
        assertEquals(0.5, config.getTemperature(), 0.001);
        assertEquals(3000, config.getMaxTokens());
        assertEquals(90, config.getTimeoutSeconds());
    }

    @Test
    @DisplayName("測試 ProviderConfig.Builder 建構 CLI 配置")
    void testProviderConfigBuilderForCli() {
        AiProviderFactory.ProviderConfig config = AiProviderFactory.ProviderConfig
                .builder(AiProviderFactory.ProviderType.GEMINI_CLI)
                .cliPath("/usr/local/bin/gemini")
                .model("gemini-1.5-pro")
                .temperature(0.3)
                .maxTokens(2000)
                .timeoutSeconds(60)
                .build();

        assertEquals(AiProviderFactory.ProviderType.GEMINI_CLI, config.getType());
        assertEquals("/usr/local/bin/gemini", config.getCliPath());
        assertEquals("gemini-1.5-pro", config.getModel());
        assertEquals(0.3, config.getTemperature(), 0.001);
        assertEquals(2000, config.getMaxTokens());
        assertEquals(60, config.getTimeoutSeconds());
    }

    @Test
    @DisplayName("測試 ProviderConfig.Builder 預設值")
    void testProviderConfigBuilderDefaults() {
        AiProviderFactory.ProviderConfig config = AiProviderFactory.ProviderConfig
                .builder(AiProviderFactory.ProviderType.OPENAI_API)
                .build();

        // 預設值檢查
        assertEquals(0.3, config.getTemperature(), 0.001);
        assertEquals(2000, config.getMaxTokens());
        assertEquals(60, config.getTimeoutSeconds());
    }

    @Test
    @DisplayName("測試 getDefaultCliPath() 返回正確路徑")
    void testGetDefaultCliPath() {
        assertEquals("/usr/local/bin/gemini",
                AiProviderFactory.getDefaultCliPath(AiProviderFactory.ProviderType.GEMINI_CLI));
        assertEquals("/usr/local/bin/gh",
                AiProviderFactory.getDefaultCliPath(AiProviderFactory.ProviderType.COPILOT_CLI));
        assertEquals("/usr/local/bin/claude",
                AiProviderFactory.getDefaultCliPath(AiProviderFactory.ProviderType.CLAUDE_CLI));
    }

    @Test
    @DisplayName("測試 getDefaultCliPath() 對 API 模式返回 null")
    void testGetDefaultCliPathForApiReturnsNull() {
        assertNull(AiProviderFactory.getDefaultCliPath(AiProviderFactory.ProviderType.OPENAI_API));
        assertNull(AiProviderFactory.getDefaultCliPath(AiProviderFactory.ProviderType.ANTHROPIC_API));
        assertNull(AiProviderFactory.getDefaultCliPath(AiProviderFactory.ProviderType.GEMINI_API));
    }

    @Test
    @DisplayName("測試 createExecutor() 對 API 模式返回 null")
    void testCreateExecutorForApiReturnsNull() {
        AiProviderFactory.ProviderConfig config = AiProviderFactory.ProviderConfig
                .builder(AiProviderFactory.ProviderType.OPENAI_API)
                .apiKey("sk-test")
                .model("gpt-4")
                .build();

        CliExecutor executor = AiProviderFactory.createExecutor(config);
        assertNull(executor, "API 模式不應該建立 CLI executor");
    }

    @Test
    @DisplayName("測試 getAuthenticationStatus() 對 API 模式返回 N/A")
    void testGetAuthenticationStatusForApi() {
        String status = AiProviderFactory.getAuthenticationStatus(
                AiProviderFactory.ProviderType.OPENAI_API,
                null
        );
        assertEquals("N/A (API mode)", status);
    }

    @Test
    @DisplayName("測試 getCliVersion() 對 API 模式返回 N/A")
    void testGetCliVersionForApi() {
        String version = AiProviderFactory.getCliVersion(
                AiProviderFactory.ProviderType.ANTHROPIC_API,
                null
        );
        assertEquals("N/A (API mode)", version);
    }
}
