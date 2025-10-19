package com.github.sonarqube.ai.provider;

import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ClaudeService 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
class ClaudeServiceTest {

    private ClaudeService service;
    private AiConfig config;

    @BeforeEach
    void setUp() {
        config = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .apiKey("test-api-key")
            .timeoutSeconds(30)
            .maxRetries(2)
            .build();

        service = new ClaudeService(config);
    }

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.close();
        }
    }

    @Test
    void testConstructorWithValidConfig() {
        assertNotNull(service);
        assertEquals("Anthropic", service.getProviderName());
        assertEquals("claude-3-opus-20240229", service.getModelName());
    }

    @Test
    void testConstructorWithNullConfig() {
        assertThrows(IllegalArgumentException.class, () -> {
            new ClaudeService(null);
        });
    }

    @Test
    void testConstructorWithOpenAiModel() {
        AiConfig openAiConfig = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-api-key")
            .build();

        assertThrows(IllegalArgumentException.class, () -> {
            new ClaudeService(openAiConfig);
        });
    }

    @Test
    void testGetProviderName() {
        assertEquals("Anthropic", service.getProviderName());
    }

    @Test
    void testGetModelName() {
        assertEquals("claude-3-opus-20240229", service.getModelName());
    }

    @Test
    void testClose() {
        assertDoesNotThrow(() -> service.close());
        assertDoesNotThrow(() -> service.close()); // 多次呼叫安全
    }

    @Test
    void testDifferentModels() {
        AiModel[] models = {
            AiModel.CLAUDE_3_OPUS,
            AiModel.CLAUDE_3_SONNET,
            AiModel.CLAUDE_3_HAIKU
        };

        for (AiModel model : models) {
            AiConfig modelConfig = AiConfig.builder()
                .model(model)
                .apiKey("test-api-key")
                .build();

            try (ClaudeService modelService = new ClaudeService(modelConfig)) {
                assertEquals("Anthropic", modelService.getProviderName());
                assertEquals(model.getModelId(), modelService.getModelName());
            }
        }
    }
}
