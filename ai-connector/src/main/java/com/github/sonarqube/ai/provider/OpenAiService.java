package com.github.sonarqube.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sonarqube.ai.AiException;
import com.github.sonarqube.ai.AiService;
import com.github.sonarqube.ai.analyzer.AiResponseParser;
import com.github.sonarqube.ai.cache.AiCacheManager;
import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiRequest;
import com.github.sonarqube.ai.model.AiResponse;
import com.github.sonarqube.ai.model.PromptTemplate;
import com.github.sonarqube.ai.model.SecurityIssue;
import com.github.sonarqube.ai.provider.openai.OpenAiApiRequest;
import com.github.sonarqube.ai.provider.openai.OpenAiApiResponse;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI 服務實現
 *
 * 整合 OpenAI GPT-4/GPT-3.5 API 進行代碼安全分析。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class OpenAiService implements AiService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AiConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AiCacheManager cacheManager;
    private final AiResponseParser responseParser;

    public OpenAiService(AiConfig config) {
        this(config, null);
    }

    public OpenAiService(AiConfig config, AiCacheManager cacheManager) {
        if (config == null || !config.isValid()) {
            throw new IllegalArgumentException("Invalid AI configuration");
        }
        if (!config.getModel().isOpenAI()) {
            throw new IllegalArgumentException("Config must be for OpenAI model");
        }
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = createHttpClient();
        this.cacheManager = cacheManager;
        this.responseParser = new AiResponseParser();
    }

    /**
     * 建立 HTTP 客戶端
     */
    private OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
            .connectTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
            .readTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
            .writeTimeout(config.getTimeoutSeconds(), TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }

    @Override
    public AiResponse analyzeCode(AiRequest request) throws AiException {
        // 檢查快取
        if (cacheManager != null) {
            AiResponse cachedResponse = cacheManager.getFromCache(request);
            if (cachedResponse != null) {
                return cachedResponse;
            }
        }

        long startTime = System.currentTimeMillis();

        try {
            // 建立 OpenAI API 請求
            OpenAiApiRequest apiRequest = buildApiRequest(request);
            String requestJson = objectMapper.writeValueAsString(apiRequest);

            // 執行 HTTP 請求（帶重試機制）
            OpenAiApiResponse apiResponse = executeWithRetry(requestJson);

            // 檢查錯誤
            if (apiResponse.hasError()) {
                throw new AiException(
                    "OpenAI API error: " + apiResponse.getError().getMessage(),
                    mapErrorType(apiResponse.getError()),
                    getProviderName()
                );
            }

            // 解析回應
            AiResponse response = parseResponse(apiResponse, System.currentTimeMillis() - startTime);

            // 存入快取
            if (cacheManager != null && response.isSuccess()) {
                cacheManager.putToCache(request, response);
            }

            return response;

        } catch (IOException e) {
            throw new AiException(
                "Failed to communicate with OpenAI API: " + e.getMessage(),
                e,
                AiException.ErrorType.NETWORK_ERROR,
                getProviderName()
            );
        } catch (Exception e) {
            throw new AiException(
                "Unexpected error during OpenAI API call: " + e.getMessage(),
                e,
                AiException.ErrorType.UNKNOWN_ERROR,
                getProviderName()
            );
        }
    }

    /**
     * 建立 OpenAI API 請求
     */
    private OpenAiApiRequest buildApiRequest(AiRequest request) {
        OpenAiApiRequest apiRequest = new OpenAiApiRequest();
        apiRequest.setModel(config.getModel().getModelId());
        apiRequest.setTemperature(config.getTemperature());
        apiRequest.setMaxTokens(config.getMaxTokens());

        // 判斷分析模式（使用 analysisType 欄位）
        boolean isDetectionOnly = "detection".equalsIgnoreCase(request.getAnalysisType());

        // 系統提示（根據模式選擇）
        String systemPrompt = isDetectionOnly
            ? PromptTemplate.SYSTEM_PROMPT_DETECTION_ONLY
            : PromptTemplate.SYSTEM_PROMPT;
        apiRequest.addMessage("system", systemPrompt);

        // 用戶提示（根據模式選擇）
        String userPrompt = isDetectionOnly
            ? PromptTemplate.createDetectionOnlyPrompt(request)
            : PromptTemplate.createAnalysisPrompt(request);
        apiRequest.addMessage("user", userPrompt);

        return apiRequest;
    }

    /**
     * 執行 HTTP 請求（帶重試機制）
     */
    private OpenAiApiResponse executeWithRetry(String requestJson) throws IOException, AiException {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < config.getMaxRetries()) {
            try {
                return executeRequest(requestJson);
            } catch (IOException e) {
                lastException = e;
                attempts++;

                if (attempts < config.getMaxRetries()) {
                    try {
                        Thread.sleep(config.getRetryDelayMs() * attempts); // 指數退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AiException(
                            "Retry interrupted",
                            ie,
                            AiException.ErrorType.UNKNOWN_ERROR,
                            getProviderName()
                        );
                    }
                }
            }
        }

        throw new AiException(
            "Failed after " + config.getMaxRetries() + " retries: " + lastException.getMessage(),
            lastException,
            AiException.ErrorType.NETWORK_ERROR,
            getProviderName()
        );
    }

    /**
     * 執行單次 HTTP 請求
     */
    private OpenAiApiResponse executeRequest(String requestJson) throws IOException {
        RequestBody body = RequestBody.create(requestJson, JSON);
        Request request = new Request.Builder()
            .url(config.getApiEndpoint())
            .addHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + config.getApiKey())
            .post(body)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                // 嘗試解析錯誤回應
                try {
                    OpenAiApiResponse errorResponse = objectMapper.readValue(responseBody, OpenAiApiResponse.class);
                    if (errorResponse.hasError()) {
                        return errorResponse;
                    }
                } catch (Exception ignored) {
                    // 無法解析錯誤，使用原始訊息
                }
                throw new IOException("HTTP " + response.code() + ": " + responseBody);
            }

            return objectMapper.readValue(responseBody, OpenAiApiResponse.class);
        }
    }

    /**
     * 解析 OpenAI 回應為 AiResponse
     */
    private AiResponse parseResponse(OpenAiApiResponse apiResponse, long processingTimeMs) {
        if (apiResponse.getChoices() == null || apiResponse.getChoices().isEmpty()) {
            return AiResponse.failure("No response from OpenAI API")
                .processingTimeMs(processingTimeMs)
                .modelUsed(config.getModel().getModelId())
                .build();
        }

        String content = apiResponse.getChoices().get(0).getMessage().getContent();
        int tokensUsed = apiResponse.getUsage() != null ? apiResponse.getUsage().getTotalTokens() : 0;

        // 解析 JSON 格式的安全問題
        List<SecurityIssue> issues = responseParser.parseSecurityIssues(content);

        return AiResponse.success()
            .analysisResult(content)
            .issues(issues)
            .processingTimeMs(processingTimeMs)
            .tokensUsed(tokensUsed)
            .modelUsed(apiResponse.getModel())
            .timestamp(LocalDateTime.now())
            .build();
    }

    /**
     * 映射 OpenAI 錯誤類型到 AiException.ErrorType
     */
    private AiException.ErrorType mapErrorType(OpenAiApiResponse.Error error) {
        if (error.getCode() == null) {
            return AiException.ErrorType.UNKNOWN_ERROR;
        }

        switch (error.getCode()) {
            case "invalid_api_key":
            case "invalid_request_error":
                return AiException.ErrorType.INVALID_API_KEY;
            case "rate_limit_exceeded":
                return AiException.ErrorType.RATE_LIMIT_EXCEEDED;
            case "timeout":
                return AiException.ErrorType.TIMEOUT;
            default:
                return AiException.ErrorType.UNKNOWN_ERROR;
        }
    }

    @Override
    public boolean testConnection() {
        try {
            // 發送簡單測試請求
            OpenAiApiRequest testRequest = new OpenAiApiRequest();
            testRequest.setModel(config.getModel().getModelId());
            testRequest.setMaxTokens(10);
            testRequest.addMessage("user", PromptTemplate.TEST_PROMPT);

            String requestJson = objectMapper.writeValueAsString(testRequest);
            OpenAiApiResponse response = executeRequest(requestJson);

            return !response.hasError();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "OpenAI";
    }

    @Override
    public String getModelName() {
        return config.getModel().getModelId();
    }

    @Override
    public void close() {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }
}
