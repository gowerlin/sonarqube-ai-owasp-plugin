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
import com.github.sonarqube.ai.ratelimit.TokenBucketRateLimiter;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // 正則表達式：解析 Rate Limit 錯誤訊息中的等待時間
    // 範例: "Please try again in 562ms" 或 "Please try again in 1.5s"
    private static final Pattern RETRY_AFTER_PATTERN = Pattern.compile(
        "Please try again in (\\d+(?:\\.\\d+)?)(ms|s)",
        Pattern.CASE_INSENSITIVE
    );

    private final AiConfig config;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AiCacheManager cacheManager;
    private final AiResponseParser responseParser;
    private final TokenBucketRateLimiter rateLimiter; // TPM Rate Limiter

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

        // 初始化 Rate Limiter（如果啟用）
        if (config.isRateLimitEnabled()) {
            this.rateLimiter = new TokenBucketRateLimiter(
                config.getMaxTokensPerMinute(),
                config.getRateLimitBufferRatio()
            );
        } else {
            this.rateLimiter = null;
        }
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
     * 執行 HTTP 請求（帶重試機制 + Rate Limiting）
     */
    private OpenAiApiResponse executeWithRetry(String requestJson) throws IOException, AiException {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < config.getMaxRetries()) {
            // Rate Limiting 檢查（如果啟用）
            if (rateLimiter != null) {
                int estimatedTokens = config.getMaxTokens(); // 使用配置的最大 token 數作為預估
                long waitTime = rateLimiter.getRecommendedWaitTime(estimatedTokens);

                if (waitTime > 0) {
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AiException(
                            "Rate limiting wait interrupted",
                            ie,
                            AiException.ErrorType.UNKNOWN_ERROR,
                            getProviderName()
                        );
                    }
                }

                // 嘗試獲取 token（非阻塞）
                if (!rateLimiter.tryAcquire(estimatedTokens)) {
                    // 等待一段時間後重試
                    try {
                        Thread.sleep(1000);
                        attempts++;
                        continue;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AiException(
                            "Rate limiting retry interrupted",
                            ie,
                            AiException.ErrorType.UNKNOWN_ERROR,
                            getProviderName()
                        );
                    }
                }
            }

            try {
                OpenAiApiResponse response = executeRequest(requestJson);

                // 檢查是否為 Rate Limit 錯誤
                if (response.hasError() && isRateLimitError(response.getError())) {
                    long retryAfterMs = parseRetryAfter(response.getError().getMessage());

                    if (retryAfterMs > 0 && attempts < config.getMaxRetries() - 1) {
                        // 使用 API 建議的等待時間
                        try {
                            Thread.sleep(retryAfterMs);
                            attempts++;
                            continue; // 重試
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new AiException(
                                "Rate limit retry interrupted",
                                ie,
                                AiException.ErrorType.UNKNOWN_ERROR,
                                getProviderName()
                            );
                        }
                    }
                }

                return response;

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
     * 檢查錯誤是否為 Rate Limit 錯誤
     */
    private boolean isRateLimitError(OpenAiApiResponse.Error error) {
        if (error == null || error.getMessage() == null) {
            return false;
        }
        String message = error.getMessage().toLowerCase();
        return message.contains("rate limit") || message.contains("too many requests");
    }

    /**
     * 解析 API 錯誤訊息中的等待時間
     *
     * 範例訊息：
     * "Rate limit reached for gpt-4-turbo-preview... Please try again in 562ms."
     * "Rate limit reached... Please try again in 1.5s."
     *
     * @param errorMessage 錯誤訊息
     * @return 等待時間（毫秒），如果無法解析則返回 0
     */
    private long parseRetryAfter(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return 0;
        }

        Matcher matcher = RETRY_AFTER_PATTERN.matcher(errorMessage);
        if (matcher.find()) {
            try {
                double value = Double.parseDouble(matcher.group(1));
                String unit = matcher.group(2).toLowerCase();

                if ("ms".equals(unit)) {
                    return (long) value;
                } else if ("s".equals(unit)) {
                    return (long) (value * 1000);
                }
            } catch (NumberFormatException e) {
                // 無法解析，返回 0
            }
        }

        return 0; // 無法解析，使用預設重試延遲
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
