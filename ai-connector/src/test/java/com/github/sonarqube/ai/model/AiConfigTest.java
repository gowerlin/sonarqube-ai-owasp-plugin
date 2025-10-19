package com.github.sonarqube.ai.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AiConfig 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
class AiConfigTest {

    @Test
    void testBuilderWithValidConfig() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-api-key")
            .timeoutSeconds(30)
            .temperature(0.5)
            .maxTokens(2000)
            .build();

        assertNotNull(config);
        assertEquals(AiModel.GPT_4, config.getModel());
        assertEquals("test-api-key", config.getApiKey());
        assertEquals(30, config.getTimeoutSeconds());
        assertEquals(0.5, config.getTemperature());
        assertEquals(2000, config.getMaxTokens());
        assertTrue(config.isValid());
    }

    @Test
    void testBuilderWithDefaultValues() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-api-key")
            .build();

        assertEquals(60, config.getTimeoutSeconds());
        assertEquals(0.3, config.getTemperature());
        assertEquals(4096, config.getMaxTokens());
        assertEquals(3, config.getMaxRetries());
        assertEquals(1000, config.getRetryDelayMs());
    }

    @Test
    void testAutoEndpointForOpenAI() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-api-key")
            .build();

        assertEquals("https://api.openai.com/v1/chat/completions", config.getApiEndpoint());
    }

    @Test
    void testAutoEndpointForClaude() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .apiKey("test-api-key")
            .build();

        assertEquals("https://api.anthropic.com/v1/messages", config.getApiEndpoint());
    }

    @Test
    void testInvalidConfigThrowsException() {
        assertThrows(IllegalStateException.class, () -> {
            AiConfig.builder()
                .model(AiModel.GPT_4)
                // 缺少 API 金鑰
                .build();
        });
    }

    @Test
    void testInvalidTemperature() {
        assertThrows(IllegalStateException.class, () -> {
            AiConfig.builder()
                .model(AiModel.GPT_4)
                .apiKey("test-api-key")
                .temperature(3.0) // 超出範圍
                .build();
        });
    }

    @Test
    void testToString() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-api-key")
            .build();

        String str = config.toString();
        assertTrue(str.contains("gpt-4"));
        assertTrue(str.contains("60s"));
        assertTrue(str.contains("0.30"));
    }
}
