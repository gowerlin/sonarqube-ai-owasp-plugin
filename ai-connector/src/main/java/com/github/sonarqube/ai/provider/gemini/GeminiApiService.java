package com.github.sonarqube.ai.provider.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sonarqube.ai.AiException;
import com.github.sonarqube.ai.AiService;
import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiRequest;
import com.github.sonarqube.ai.model.AiResponse;
import com.github.sonarqube.ai.model.SecurityIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Google Gemini API 服務實作
 *
 * 參考文件: https://ai.google.dev/gemini-api/docs
 *
 * 支援模型:
 * - gemini-1.5-pro: 最新專業版
 * - gemini-1.5-flash: 快速版
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9, Story 9.2)
 */
public class GeminiApiService implements AiService {

    private static final Logger LOG = LoggerFactory.getLogger(GeminiApiService.class);

    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final String GENERATE_CONTENT_SUFFIX = ":generateContent";
    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private final AiConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * 建構 Gemini API 服務
     *
     * @param config AI 配置
     */
    public GeminiApiService(AiConfig config) {
        if (config == null || !config.isValid()) {
            throw new IllegalArgumentException("Invalid AI configuration");
        }
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Gemini API key is required");
        }

        this.config = config;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AiResponse analyzeCode(AiRequest request) throws AiException {
        if (request == null) {
            throw new IllegalArgumentException("AiRequest cannot be null");
        }

        try {
            LOG.debug("Analyzing code with Gemini API: {}", getModelName());

            // 1. 建構 Gemini API 請求
            GeminiApiRequest geminiRequest = buildGeminiRequest(request);

            // 2. 發送 HTTP POST 請求
            String responseJson = sendApiRequest(geminiRequest);

            // 3. 解析回應
            GeminiApiResponse geminiResponse = objectMapper.readValue(responseJson, GeminiApiResponse.class);

            // 4. 轉換為統一 AiResponse 格式
            return convertToAiResponse(geminiResponse, request);

        } catch (IOException e) {
            LOG.error("Gemini API I/O error", e);
            throw new AiException("Failed to communicate with Gemini API: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Gemini API request interrupted", e);
            throw new AiException("Gemini API request interrupted: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean testConnection() {
        try {
            LOG.debug("Testing Gemini API connection");

            // 建立簡單的測試請求
            GeminiApiRequest testRequest = GeminiApiRequest.builder()
                .addUserMessage("Hello, Gemini!")
                .temperature(0.1f)
                .maxTokens(10)
                .build();

            // 發送測試請求
            String response = sendApiRequest(testRequest);

            LOG.info("Gemini API connection test successful");
            return response != null && !response.isEmpty();

        } catch (Exception e) {
            LOG.error("Gemini API connection test failed", e);
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "Google Gemini";
    }

    @Override
    public String getModelName() {
        return config.getModel() != null
            ? config.getModel().getModelId()
            : "gemini-1.5-pro"; // 預設模型
    }

    @Override
    public void close() {
        LOG.debug("Closing Gemini API service");
        // HttpClient 會自動管理連接池，不需要特殊清理
    }

    /**
     * 建構 Gemini API 請求
     */
    private GeminiApiRequest buildGeminiRequest(AiRequest request) {
        String prompt = buildSecurityAnalysisPrompt(request);

        GeminiApiRequest.Builder builder = GeminiApiRequest.builder()
            .addUserMessage(prompt);

        // 設定生成參數
        if (config.getTemperature() != null) {
            builder.temperature(config.getTemperature());
        } else {
            builder.temperature(0.3f); // 安全分析建議較低的 temperature
        }

        if (config.getMaxTokens() != null) {
            builder.maxTokens(config.getMaxTokens());
        } else {
            builder.maxTokens(2000); // 預設 2000 tokens
        }

        // 設定安全過濾器（放寬以允許安全分析內容）
        builder.addSafetySetting("HARM_CATEGORY_HARASSMENT", "BLOCK_ONLY_HIGH")
               .addSafetySetting("HARM_CATEGORY_HATE_SPEECH", "BLOCK_ONLY_HIGH")
               .addSafetySetting("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_ONLY_HIGH")
               .addSafetySetting("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_ONLY_HIGH");

        return builder.build();
    }

    /**
     * 建構安全分析提示詞
     */
    private String buildSecurityAnalysisPrompt(AiRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a security analysis expert. ");
        prompt.append("Analyze the following code for security vulnerabilities.\n\n");

        prompt.append("Code to analyze:\n");
        prompt.append("```\n");
        prompt.append(request.getCode());
        prompt.append("\n```\n\n");

        prompt.append("Please identify:\n");
        prompt.append("1. All security vulnerabilities (OWASP Top 10)\n");
        prompt.append("2. Severity level for each issue (BLOCKER, CRITICAL, MAJOR, MINOR, INFO)\n");
        prompt.append("3. OWASP category (e.g., A03:2021-Injection)\n");
        prompt.append("4. Related CWE IDs\n");
        prompt.append("5. Detailed description of each vulnerability\n");
        prompt.append("6. Suggested fixes\n\n");

        prompt.append("Format your response as a structured list of findings.");

        return prompt.toString();
    }

    /**
     * 發送 API 請求
     */
    private String sendApiRequest(GeminiApiRequest request) throws IOException, InterruptedException {
        // 建構 API URL
        String modelName = getModelName();
        String apiUrl = API_BASE_URL + modelName + GENERATE_CONTENT_SUFFIX;
        String urlWithKey = apiUrl + "?key=" + config.getApiKey();

        // 序列化請求
        String requestBody = objectMapper.writeValueAsString(request);

        LOG.debug("Sending request to Gemini API: {}", apiUrl);

        // 建構 HTTP 請求
        HttpRequest httpRequest = HttpRequest.newBuilder()
            .uri(URI.create(urlWithKey))
            .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_SECONDS))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        // 發送請求
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        // 檢查回應狀態
        if (response.statusCode() != 200) {
            LOG.error("Gemini API error: {} - {}", response.statusCode(), response.body());
            throw new IOException("Gemini API request failed with status: " + response.statusCode() + ", body: " + response.body());
        }

        LOG.debug("Gemini API response received ({} chars)", response.body().length());

        return response.body();
    }

    /**
     * 轉換 Gemini 回應為統一的 AiResponse 格式
     */
    private AiResponse convertToAiResponse(GeminiApiResponse geminiResponse, AiRequest originalRequest) {
        List<SecurityIssue> findings = new ArrayList<>();

        // 取得 Gemini 的回應文字
        String responseText = geminiResponse.getFirstCandidateText();

        if (responseText == null || responseText.trim().isEmpty()) {
            LOG.warn("Gemini API returned empty response");
            return AiResponse.builder()
                .findings(findings)
                .rawResponse(responseText)
                .build();
        }

        LOG.debug("Parsing Gemini response ({} chars)", responseText.length());

        // 解析回應文字，提取安全問題
        // 這裡使用簡單的文字解析，實際可能需要更複雜的解析邏輯
        findings = parseSecurityFindings(responseText);

        LOG.info("Gemini API analysis completed: {} findings", findings.size());

        return AiResponse.builder()
            .findings(findings)
            .rawResponse(responseText)
            .confidence(0.85f) // Gemini 的建議信心度
            .build();
    }

    /**
     * 解析安全發現
     *
     * 這是簡化的實作，實際專案中應該有更完善的解析邏輯
     */
    private List<SecurityIssue> parseSecurityFindings(String responseText) {
        List<SecurityIssue> findings = new ArrayList<>();

        // TODO: 實作完整的回應解析邏輯
        // 目前返回空列表，待後續實作詳細的文字解析器

        LOG.debug("Parsing security findings from response text");

        // 暫時返回空列表，實際專案中應該解析 Gemini 的回應
        // 並轉換為 SecurityIssue 物件

        return findings;
    }
}
