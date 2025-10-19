package com.github.sonarqube.ai.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AiModel 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
class AiModelTest {

    @Test
    void testGpt4Properties() {
        AiModel model = AiModel.GPT_4;

        assertEquals("gpt-4", model.getModelId());
        assertEquals("OpenAI", model.getProvider());
        assertEquals(8192, model.getMaxOutputTokens());
        assertEquals(128000, model.getMaxContextTokens());
        assertTrue(model.isOpenAI());
        assertFalse(model.isClaude());
    }

    @Test
    void testClaude3OpusProperties() {
        AiModel model = AiModel.CLAUDE_3_OPUS;

        assertEquals("claude-3-opus-20240229", model.getModelId());
        assertEquals("Anthropic", model.getProvider());
        assertEquals(4096, model.getMaxOutputTokens());
        assertEquals(200000, model.getMaxContextTokens());
        assertTrue(model.isClaude());
        assertFalse(model.isOpenAI());
    }

    @Test
    void testFromModelIdWithValidId() {
        AiModel model = AiModel.fromModelId("gpt-4");
        assertEquals(AiModel.GPT_4, model);

        model = AiModel.fromModelId("claude-3-opus-20240229");
        assertEquals(AiModel.CLAUDE_3_OPUS, model);
    }

    @Test
    void testFromModelIdWithInvalidId() {
        AiModel model = AiModel.fromModelId("invalid-model");
        assertNull(model);
    }

    @Test
    void testFromModelIdCaseInsensitive() {
        AiModel model = AiModel.fromModelId("GPT-4");
        assertEquals(AiModel.GPT_4, model);

        model = AiModel.fromModelId("CLAUDE-3-OPUS-20240229");
        assertEquals(AiModel.CLAUDE_3_OPUS, model);
    }

    @Test
    void testAllModelsHaveValidProperties() {
        for (AiModel model : AiModel.values()) {
            assertNotNull(model.getModelId());
            assertNotNull(model.getProvider());
            assertTrue(model.getMaxOutputTokens() > 0);
            assertTrue(model.getMaxContextTokens() > 0);
            assertTrue(model.getMaxContextTokens() >= model.getMaxOutputTokens());

            // 每個模型應該是 OpenAI 或 Claude 其中之一
            assertTrue(model.isOpenAI() || model.isClaude());
            assertFalse(model.isOpenAI() && model.isClaude());
        }
    }
}
