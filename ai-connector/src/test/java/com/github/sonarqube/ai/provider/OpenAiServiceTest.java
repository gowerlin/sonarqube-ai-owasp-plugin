package com.github.sonarqube.ai.provider;

import com.github.sonarqube.ai.AiException;
import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiModel;
import com.github.sonarqube.ai.model.AiRequest;
import com.github.sonarqube.ai.model.AiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OpenAiService 單元測試
 *
 * 注意：實際 API 調用測試需要 OPENAI_API_KEY 環境變數
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
class OpenAiServiceTest {

    private OpenAiService service;
    private AiConfig config;

    @BeforeEach
    void setUp() {
        // 使用測試 API 金鑰（實際測試時需替換）
        config = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("test-api-key")
            .timeoutSeconds(30)
            .maxRetries(2)
            .build();

        service = new OpenAiService(config);
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
        assertEquals("OpenAI", service.getProviderName());
        assertEquals("gpt-4", service.getModelName());
    }

    @Test
    void testConstructorWithNullConfig() {
        assertThrows(IllegalArgumentException.class, () -> {
            new OpenAiService(null);
        });
    }

    @Test
    void testConstructorWithClaudeModel() {
        AiConfig claudeConfig = AiConfig.builder()
            .model(AiModel.CLAUDE_3_OPUS)
            .apiKey("test-api-key")
            .build();

        assertThrows(IllegalArgumentException.class, () -> {
            new OpenAiService(claudeConfig);
        });
    }

    @Test
    void testGetProviderName() {
        assertEquals("OpenAI", service.getProviderName());
    }

    @Test
    void testGetModelName() {
        assertEquals("gpt-4", service.getModelName());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    void testAnalyzeCodeWithRealApi() throws AiException {
        // 僅在設定環境變數時執行
        String apiKey = System.getenv("OPENAI_API_KEY");
        AiConfig realConfig = AiConfig.builder()
            .model(AiModel.GPT_3_5_TURBO) // 使用較便宜的模型測試
            .apiKey(apiKey)
            .maxTokens(100)
            .build();

        OpenAiService realService = new OpenAiService(realConfig);
        AiRequest request = AiRequest.builder("SELECT * FROM users WHERE id = '" + "' + userInput + '")
            .fileName("test.java")
            .language("java")
            .owaspVersion("2021")
            .build();

        AiResponse response = realService.analyzeCode(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertNotNull(response.getAnalysisResult());
        assertTrue(response.getTokensUsed() > 0);
        assertTrue(response.getProcessingTimeMs() > 0);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    void testConnectionWithRealApi() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        AiConfig realConfig = AiConfig.builder()
            .model(AiModel.GPT_3_5_TURBO)
            .apiKey(apiKey)
            .build();

        OpenAiService realService = new OpenAiService(realConfig);
        boolean connected = realService.testConnection();
        assertTrue(connected, "Connection test should succeed with valid API key");
    }

    @Test
    void testAnalyzeCodeWithInvalidApiKey() {
        AiConfig invalidConfig = AiConfig.builder()
            .model(AiModel.GPT_4)
            .apiKey("invalid-api-key-12345")
            .maxRetries(1) // 減少重試次數加快測試
            .build();

        OpenAiService invalidService = new OpenAiService(invalidConfig);
        AiRequest request = AiRequest.builder("public void test() {}")
            .fileName("test.java")
            .build();

        // 預期會拋出 AiException
        assertThrows(AiException.class, () -> {
            invalidService.analyzeCode(request);
        });
    }

    @Test
    void testClose() {
        // 確保 close() 不拋出例外
        assertDoesNotThrow(() -> service.close());

        // 多次呼叫 close() 應該是安全的
        assertDoesNotThrow(() -> service.close());
    }

    @Test
    void testDifferentModels() {
        // 測試不同的 OpenAI 模型
        AiModel[] models = {AiModel.GPT_4, AiModel.GPT_4_TURBO, AiModel.GPT_3_5_TURBO};

        for (AiModel model : models) {
            AiConfig modelConfig = AiConfig.builder()
                .model(model)
                .apiKey("test-api-key")
                .build();

            OpenAiService modelService = new OpenAiService(modelConfig);
            assertEquals("OpenAI", modelService.getProviderName());
            assertEquals(model.getModelId(), modelService.getModelName());
        }
    }
}
