package com.github.sonarqube.ai.provider.gemini;

import com.github.sonarqube.ai.AiException;
import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiModel;
import com.github.sonarqube.ai.model.AiRequest;
import com.github.sonarqube.ai.model.AiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for GeminiApiService
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9, Story 9.2)
 */
class GeminiApiServiceTest {

    private AiConfig config;

    @BeforeEach
    void setUp() {
        config = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .apiKey("test-api-key")
            .temperature(0.3f)
            .maxTokens(2000)
            .build();
    }

    @Test
    void testConstructorWithValidConfig() {
        GeminiApiService service = new GeminiApiService(config);

        assertThat(service).isNotNull();
        assertThat(service.getProviderName()).isEqualTo("Google Gemini");
        assertThat(service.getModelName()).isEqualTo("gemini-1.5-pro");
    }

    @Test
    void testConstructorWithNullConfig() {
        assertThatThrownBy(() -> new GeminiApiService(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid AI configuration");
    }

    @Test
    void testConstructorWithInvalidConfig() {
        AiConfig invalidConfig = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_PRO)
            .build(); // 缺少 API key

        assertThatThrownBy(() -> new GeminiApiService(invalidConfig))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Gemini API key is required");
    }

    @Test
    void testGetProviderName() {
        GeminiApiService service = new GeminiApiService(config);

        assertThat(service.getProviderName()).isEqualTo("Google Gemini");
    }

    @Test
    void testGetModelName() {
        GeminiApiService service = new GeminiApiService(config);

        assertThat(service.getModelName()).isEqualTo("gemini-1.5-pro");
    }

    @Test
    void testGetModelNameWithFlashModel() {
        AiConfig flashConfig = AiConfig.builder()
            .model(AiModel.GEMINI_1_5_FLASH)
            .apiKey("test-api-key")
            .build();

        GeminiApiService service = new GeminiApiService(flashConfig);

        assertThat(service.getModelName()).isEqualTo("gemini-1.5-flash");
    }

    @Test
    void testAnalyzeCodeWithNullRequest() {
        GeminiApiService service = new GeminiApiService(config);

        assertThatThrownBy(() -> service.analyzeCode(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("AiRequest cannot be null");
    }

    @Test
    void testAnalyzeCodeWithEmptyCode() {
        GeminiApiService service = new GeminiApiService(config);

        AiRequest request = AiRequest.builder("")
            .fileName("test.java")
            .build();

        // 應該不會拋出例外，但會返回空結果
        // 實際 API 調用會失敗，這裡只測試參數驗證
        assertThatThrownBy(() -> service.analyzeCode(request))
            .isInstanceOf(AiException.class);
    }

    @Test
    void testClose() {
        GeminiApiService service = new GeminiApiService(config);

        // close() 不應該拋出例外
        service.close();
    }

    @Test
    void testGeminiApiRequestBuilder() {
        GeminiApiRequest request = GeminiApiRequest.builder()
            .addUserMessage("Test message")
            .temperature(0.5f)
            .maxTokens(1000)
            .addSafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_ONLY_HIGH")
            .build();

        assertThat(request).isNotNull();
        assertThat(request.getContents()).hasSize(1);
        assertThat(request.getContents().get(0).getParts()).hasSize(1);
        assertThat(request.getContents().get(0).getParts().get(0).getText()).isEqualTo("Test message");
        assertThat(request.getGenerationConfig()).isNotNull();
        assertThat(request.getGenerationConfig().getTemperature()).isEqualTo(0.5f);
        assertThat(request.getGenerationConfig().getMaxOutputTokens()).isEqualTo(1000);
        assertThat(request.getSafetySettings()).hasSize(1);
    }

    @Test
    void testGeminiApiRequestMultipleMessages() {
        GeminiApiRequest request = GeminiApiRequest.builder()
            .addUserMessage("Message 1")
            .addUserMessage("Message 2")
            .build();

        assertThat(request.getContents()).hasSize(2);
    }

    @Test
    void testGeminiApiResponseGetFirstCandidateText() {
        GeminiApiResponse response = new GeminiApiResponse();

        // 測試空回應
        assertThat(response.getFirstCandidateText()).isNull();

        // 測試有候選回應
        GeminiApiResponse.Candidate candidate = new GeminiApiResponse.Candidate();
        GeminiApiRequest.Content content = new GeminiApiRequest.Content();
        content.addTextPart("Test response");
        candidate.setContent(content);

        List<GeminiApiResponse.Candidate> candidates = new ArrayList<>();
        candidates.add(candidate);
        response.setCandidates(candidates);

        assertThat(response.getFirstCandidateText()).isEqualTo("Test response");
    }

    @Test
    void testGeminiApiResponseMultipleParts() {
        GeminiApiResponse response = new GeminiApiResponse();

        GeminiApiResponse.Candidate candidate = new GeminiApiResponse.Candidate();
        GeminiApiRequest.Content content = new GeminiApiRequest.Content();
        content.addTextPart("Part 1");
        content.addTextPart("Part 2");
        candidate.setContent(content);

        List<GeminiApiResponse.Candidate> candidates = new ArrayList<>();
        candidates.add(candidate);
        response.setCandidates(candidates);

        // 應該合併所有 parts
        assertThat(response.getFirstCandidateText()).isEqualTo("Part 1Part 2");
    }
}
