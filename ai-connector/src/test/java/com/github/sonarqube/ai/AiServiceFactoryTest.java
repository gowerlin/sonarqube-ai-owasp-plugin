package com.github.sonarqube.ai;

import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiModel;
import com.github.sonarqube.ai.provider.ClaudeService;
import com.github.sonarqube.ai.provider.OpenAiService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AiServiceFactory 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
class AiServiceFactoryTest {

    @Test
    void testCreateOpenAiService() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-api-key")
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertNotNull(service);
        assertTrue(service instanceof OpenAiService);
        assertEquals("OpenAI", service.getProviderName());
        assertEquals("gpt-4", service.getModelName());
    }

    @Test
    void testCreateClaudeService() {
        AiConfig config = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .apiKey("test-api-key")
            .build();

        AiService service = AiServiceFactory.createService(config);

        assertNotNull(service);
        assertTrue(service instanceof ClaudeService);
        assertEquals("Anthropic", service.getProviderName());
        assertEquals("claude-3-opus-20240229", service.getModelName());
    }

    @Test
    void testCreateOpenAiServiceWithFactory() {
        AiService service = AiServiceFactory.createOpenAiService("test-api-key");

        assertNotNull(service);
        assertTrue(service instanceof OpenAiService);
        assertEquals("OpenAI", service.getProviderName());
        assertEquals("gpt-4", service.getModelName());
    }

    @Test
    void testCreateClaudeServiceWithFactory() {
        AiService service = AiServiceFactory.createClaudeService("test-api-key");

        assertNotNull(service);
        assertTrue(service instanceof ClaudeService);
        assertEquals("Anthropic", service.getProviderName());
        assertEquals("claude-3-opus-20240229", service.getModelName());
    }

    @Test
    void testCreateServiceWithNullConfigThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            AiServiceFactory.createService(null);
        });
    }

    @Test
    void testCreateServiceWithInvalidConfigThrowsException() {
        // 建立無效配置（缺少 API 金鑰）
        assertThrows(Exception.class, () -> {
            AiConfig.Builder builder = AiConfig.builder()
                .model(AiModel.GPT_4);
            // 不設定 API 金鑰
            AiConfig config = builder.build();
            AiServiceFactory.createService(config);
        });
    }
}
