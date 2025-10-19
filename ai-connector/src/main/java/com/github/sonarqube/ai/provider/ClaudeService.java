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
import com.github.sonarqube.ai.provider.claude.ClaudeApiRequest;
import com.github.sonarqube.ai.provider.claude.ClaudeApiResponse;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Anthropic Claude 服務實現
 *
 * 整合 Anthropic Claude 3 API 進行代碼安全分析。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class ClaudeService implements AiService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String API_KEY_HEADER = "x-api-key";
    private static final String ANTHROPIC_VERSION_HEADER = "anthropic-version";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final AiConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AiCacheManager cacheManager;
    private final AiResponseParser responseParser;

    public ClaudeService(AiConfig config) {
        this(config, null);
    }

    public ClaudeService(AiConfig config, AiCacheManager cacheManager) {
        if (config == null || !config.isValid()) {
            throw new IllegalArgumentException("Invalid AI configuration");
        }
        if (!config.getModel().isClaude()) {
            throw new IllegalArgumentException("Config must be for Claude model");
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
            // 建立 Claude API 請求
            ClaudeApiRequest apiRequest = buildApiRequest(request);
            String requestJson = objectMapper.writeValueAsString(apiRequest);

            // 執行 HTTP 請求（帶重試機制）
            ClaudeApiResponse apiResponse = executeWithRetry(requestJson);

            // 檢查錯誤
            if (apiResponse.hasError()) {
                throw new AiException(
                    "Claude API error: " + apiResponse.getError().getMessage(),
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
                "Failed to communicate with Claude API: " + e.getMessage(),
                e,
                AiException.ErrorType.NETWORK_ERROR,
                getProviderName()
            );
        } catch (Exception e) {
            throw new AiException(
                "Unexpected error during Claude API call: " + e.getMessage(),
                e,
                AiException.ErrorType.UNKNOWN_ERROR,
                getProviderName()
            );
        }
    }

    /**
     * 建立 Claude API 請求
     */
    private ClaudeApiRequest buildApiRequest(AiRequest request) {
        ClaudeApiRequest apiRequest = new ClaudeApiRequest();
        apiRequest.setModel(config.getModel().getModelId());
        apiRequest.setMaxTokens(config.getMaxTokens());
        apiRequest.setTemperature(config.getTemperature());

        // Claude 使用單獨的 system 欄位
        apiRequest.setSystem(PromptTemplate.SYSTEM_PROMPT);

        // 用戶提示
        String userPrompt = PromptTemplate.createAnalysisPrompt(request);
        apiRequest.addMessage("user", userPrompt);

        return apiRequest;
    }

    /**
     * 執行 HTTP 請求（帶重試機制）
     */
    private ClaudeApiResponse executeWithRetry(String requestJson) throws IOException, AiException {
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
    private ClaudeApiResponse executeRequest(String requestJson) throws IOException {
        RequestBody body = RequestBody.create(requestJson, JSON);
        Request request = new Request.Builder()
            .url(config.getApiEndpoint())
            .addHeader(API_KEY_HEADER, config.getApiKey())
            .addHeader(ANTHROPIC_VERSION_HEADER, ANTHROPIC_VERSION)
            .post(body)
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                // 嘗試解析錯誤回應
                try {
                    ClaudeApiResponse errorResponse = objectMapper.readValue(responseBody, ClaudeApiResponse.class);
                    if (errorResponse.hasError()) {
                        return errorResponse;
                    }
                } catch (Exception ignored) {
                    // 無法解析錯誤，使用原始訊息
                }
                throw new IOException("HTTP " + response.code() + ": " + responseBody);
            }

            return objectMapper.readValue(responseBody, ClaudeApiResponse.class);
        }
    }

    /**
     * 解析 Claude 回應為 AiResponse
     */
    private AiResponse parseResponse(ClaudeApiResponse apiResponse, long processingTimeMs) {
        if (apiResponse.getContent() == null || apiResponse.getContent().isEmpty()) {
            return AiResponse.failure("No response from Claude API")
                .processingTimeMs(processingTimeMs)
                .modelUsed(config.getModel().getModelId())
                .build();
        }

        // Claude 回應格式：content 是陣列，取第一個文本內容
        String content = apiResponse.getContent().get(0).getText();
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
     * 映射 Claude 錯誤類型到 AiException.ErrorType
     */
    private AiException.ErrorType mapErrorType(ClaudeApiResponse.Error error) {
        if (error.getType() == null) {
            return AiException.ErrorType.UNKNOWN_ERROR;
        }

        switch (error.getType()) {
            case "authentication_error":
            case "invalid_request_error":
                return AiException.ErrorType.INVALID_API_KEY;
            case "rate_limit_error":
                return AiException.ErrorType.RATE_LIMIT_EXCEEDED;
            case "timeout_error":
                return AiException.ErrorType.TIMEOUT;
            default:
                return AiException.ErrorType.UNKNOWN_ERROR;
        }
    }

    @Override
    public boolean testConnection() {
        try {
            // 發送簡單測試請求
            ClaudeApiRequest testRequest = new ClaudeApiRequest();
            testRequest.setModel(config.getModel().getModelId());
            testRequest.setMaxTokens(10);
            testRequest.addMessage("user", PromptTemplate.TEST_PROMPT);

            String requestJson = objectMapper.writeValueAsString(testRequest);
            ClaudeApiResponse response = executeRequest(requestJson);

            return !response.hasError();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getProviderName() {
        return "Anthropic";
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
