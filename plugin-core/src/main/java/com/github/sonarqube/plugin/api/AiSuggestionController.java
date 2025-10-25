package com.github.sonarqube.plugin.api;

import com.github.sonarqube.ai.AiException;
import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiExecutionMode;
import com.github.sonarqube.ai.model.AiModel;
import com.github.sonarqube.ai.model.AiResponse;
import com.github.sonarqube.plugin.AiOwaspPlugin;
import com.github.sonarqube.plugin.service.AiSuggestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.*;

import java.io.IOException;

/**
 * AI 建議 API 控制器 - 提供按需 AI 修復建議
 *
 * <p>此控制器提供按需生成 AI 修復建議的 API 端點，讓使用者可以針對特定的安全問題，
 * 在需要時才呼叫 AI 生成詳細的修復建議，而不是在掃描時自動生成所有建議（節省 Token）。</p>
 *
 * <p>API 端點: {@code /api/aiowasp/suggest}</p>
 *
 * <p><strong>請求參數：</strong></p>
 * <ul>
 *   <li>code - 問題代碼片段（必填）</li>
 *   <li>owaspCategory - OWASP 類別，例如 A01:2021-Broken Access Control（必填）</li>
 *   <li>cweId - CWE ID，例如 CWE-284（必填）</li>
 *   <li>language - 程式語言，例如 java, javascript（必填）</li>
 *   <li>fileName - 檔案名稱（選填）</li>
 * </ul>
 *
 * <p><strong>回應格式：</strong></p>
 * <pre>
 * {
 *   "success": true,
 *   "fixSuggestion": "詳細的修復步驟...",
 *   "codeExample": {
 *     "before": "修復前的程式碼...",
 *     "after": "修復後的程式碼..."
 *   },
 *   "effortEstimate": "Simple (0.5-1 hour)",
 *   "tokensUsed": 1500,
 *   "processingTimeMs": 2300
 * }
 * </pre>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class AiSuggestionController implements WebService {

    private static final Logger LOG = LoggerFactory.getLogger(AiSuggestionController.class);

    private static final String API_ENDPOINT = "api/aiowasp";
    private static final String ACTION_SUGGEST = "suggest";

    // 請求參數
    private static final String PARAM_CODE = "code";
    private static final String PARAM_OWASP_CATEGORY = "owaspCategory";
    private static final String PARAM_CWE_ID = "cweId";
    private static final String PARAM_LANGUAGE = "language";
    private static final String PARAM_FILE_NAME = "fileName";

    private final Configuration configuration;

    /**
     * 建構子
     *
     * @param configuration SonarQube 配置
     */
    public AiSuggestionController(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void define(Context context) {
        NewController controller = context.createController(API_ENDPOINT)
                .setDescription("AI OWASP Plugin - On-Demand Suggestion API")
                .setSince("1.0.0");

        // 定義 suggest action (使用 GET 避免 CSRF 問題)
        NewAction suggestAction = controller.createAction(ACTION_SUGGEST)
                .setDescription("Generate detailed AI fix suggestion for a specific security issue")
                .setSince("1.0.0")
                .setInternal(true) // 內部 API，不需要特殊權限
                .setHandler(this::handleSuggestRequest);

        // 定義參數
        suggestAction.createParam(PARAM_CODE)
                .setDescription("Code snippet containing the security issue")
                .setRequired(true)
                .setExampleValue("String sql = \"SELECT * FROM users WHERE id = \" + userId;");

        suggestAction.createParam(PARAM_OWASP_CATEGORY)
                .setDescription("OWASP category (e.g., A01:2021-Broken Access Control)")
                .setRequired(true)
                .setExampleValue("A03:2021-Injection");

        suggestAction.createParam(PARAM_CWE_ID)
                .setDescription("CWE ID (e.g., CWE-89)")
                .setRequired(false) // 非必填，某些問題可能沒有 CWE ID
                .setExampleValue("CWE-89");

        suggestAction.createParam(PARAM_LANGUAGE)
                .setDescription("Programming language (e.g., java, javascript)")
                .setRequired(true)
                .setPossibleValues("java", "javascript", "python", "csharp", "php", "ruby", "go")
                .setExampleValue("java");

        suggestAction.createParam(PARAM_FILE_NAME)
                .setDescription("File name (optional)")
                .setRequired(false)
                .setExampleValue("UserService.java");

        controller.done();

        LOG.info("AI Suggestion API Controller 註冊成功");
    }

    /**
     * 處理 AI 建議請求
     *
     * @param request  Web service 請求
     * @param response Web service 回應
     */
    private void handleSuggestRequest(Request request, Response response) {
        String code = request.mandatoryParam(PARAM_CODE);
        String owaspCategory = request.mandatoryParam(PARAM_OWASP_CATEGORY);
        String cweId = request.param(PARAM_CWE_ID); // 改為 param()，因為 CWE ID 非必填
        String language = request.mandatoryParam(PARAM_LANGUAGE);
        String fileName = request.param(PARAM_FILE_NAME);

        LOG.info("AI 建議請求: owasp={}, cwe={}, language={}, file={}",
                owaspCategory, cweId, language, fileName);

        AiSuggestionService suggestionService = null;
        try {
            // 每次請求都重新初始化 AI 建議服務，確保配置變更立即生效
            LOG.info("初始化 AI 建議服務...");
            suggestionService = initializeSuggestionService();

            // 讀取 AI 回應語言偏好
            String responseLanguage = configuration.get(AiOwaspPlugin.PROPERTY_AI_RESPONSE_LANGUAGE)
                    .orElse("zh-TW");

            // 生成 AI 修復建議
            LOG.info("開始生成 AI 修復建議: owasp={}, cwe={}, language={}, file={}, codeLength={}, responseLang={}",
                    owaspCategory, cweId, language, fileName, code != null ? code.length() : 0, responseLanguage);

            AiResponse aiResponse = suggestionService.generateFixSuggestion(
                    code, owaspCategory, cweId, language, fileName, responseLanguage
            );

            LOG.info("AI 建議生成成功: tokens={}, time={}ms",
                    aiResponse.getTokensUsed(), aiResponse.getProcessingTimeMs());

            // 構建 JSON 回應
            writeJsonResponse(response, aiResponse);

        } catch (AiException e) {
            LOG.error("===== AI 建議生成失敗 =====");
            LOG.error("錯誤類型: AiException");
            LOG.error("錯誤訊息: {}", e.getMessage());
            LOG.error("請求參數: owasp={}, cwe={}, language={}, file={}, codeLength={}",
                    owaspCategory, cweId, language, fileName, code != null ? code.length() : 0);
            LOG.error("AI 配置: provider={}, model={}",
                    configuration.get(AiOwaspPlugin.PROPERTY_AI_PROVIDER).orElse("未設定"),
                    configuration.get(AiOwaspPlugin.PROPERTY_AI_MODEL).orElse("未設定"));
            LOG.error("完整堆疊追蹤:", e);
            LOG.error("===========================");
            writeErrorResponse(response, 500, "AI analysis failed: " + e.getMessage());
        } catch (IllegalStateException e) {
            LOG.error("===== AI 配置無效 =====");
            LOG.error("錯誤類型: IllegalStateException");
            LOG.error("錯誤訊息: {}", e.getMessage());
            LOG.error("配置檢查:");
            LOG.error("  - AI Provider: {}", configuration.get(AiOwaspPlugin.PROPERTY_AI_PROVIDER).orElse("未設定"));
            LOG.error("  - AI Model: {}", configuration.get(AiOwaspPlugin.PROPERTY_AI_MODEL).orElse("未設定"));
            LOG.error("  - OpenAI API Key: {}", configuration.get(AiOwaspPlugin.PROPERTY_OPENAI_API_KEY).isPresent() ? "已設定" : "未設定");
            LOG.error("  - Anthropic API Key: {}", configuration.get(AiOwaspPlugin.PROPERTY_ANTHROPIC_API_KEY).isPresent() ? "已設定" : "未設定");
            LOG.error("  - Google API Key: {}", configuration.get(AiOwaspPlugin.PROPERTY_GOOGLE_API_KEY).isPresent() ? "已設定" : "未設定");
            LOG.error("  - AI Temperature: {}", configuration.getDouble(AiOwaspPlugin.PROPERTY_AI_TEMPERATURE).orElse(0.0));
            LOG.error("  - AI Max Tokens: {}", configuration.getInt(AiOwaspPlugin.PROPERTY_AI_MAX_TOKENS).orElse(0));
            LOG.error("  - AI Timeout: {}", configuration.getInt(AiOwaspPlugin.PROPERTY_AI_TIMEOUT).orElse(0));
            LOG.error("完整堆疊追蹤:", e);
            LOG.error("======================");
            writeErrorResponse(response, 503, "AI service not available: " + e.getMessage());
        } catch (Exception e) {
            LOG.error("===== 處理 AI 建議請求時發生異常 =====");
            LOG.error("錯誤類型: {}", e.getClass().getName());
            LOG.error("錯誤訊息: {}", e.getMessage());
            LOG.error("請求參數: owasp={}, cwe={}, language={}, file={}, codeLength={}",
                    owaspCategory, cweId, language, fileName, code != null ? code.length() : 0);
            LOG.error("完整堆疊追蹤:", e);
            LOG.error("=======================================");
            writeErrorResponse(response, 500, "Internal server error: " + e.getMessage());
        } finally {
            // 關閉服務實例，釋放資源
            if (suggestionService != null) {
                try {
                    suggestionService.close();
                    LOG.debug("AI 建議服務已關閉");
                } catch (Exception e) {
                    LOG.warn("關閉 AI 建議服務時發生錯誤: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 初始化 AI 建議服務
     *
     * @return AI 建議服務實例
     * @throws IllegalStateException 如果配置無效
     */
    private AiSuggestionService initializeSuggestionService() {
        // 讀取 AI Provider
        String provider = configuration.get(AiOwaspPlugin.PROPERTY_AI_PROVIDER)
                .orElse("openai");

        // 根據 Provider 選擇對應的 API Key（優先使用專用 Key，回退到通用 Key）
        String apiKey = getApiKeyForProvider(provider);

        String modelId = configuration.get(AiOwaspPlugin.PROPERTY_AI_MODEL)
                .orElse("gpt-4");

        double temperature = configuration.getDouble(AiOwaspPlugin.PROPERTY_AI_TEMPERATURE)
                .orElse(0.3);

        int maxTokens = configuration.getInt(AiOwaspPlugin.PROPERTY_AI_MAX_TOKENS)
                .orElse(2000);

        int timeout = configuration.getInt(AiOwaspPlugin.PROPERTY_AI_TIMEOUT)
                .orElse(60);

        // 根據 provider 推斷 API endpoint
        String apiEndpoint = getApiEndpointForProvider(provider);

        // 建立 AI 配置
        AiConfig config = AiConfig.builder()
                .model(AiModel.fromModelId(modelId))
                .apiKey(apiKey)
                .apiEndpoint(apiEndpoint)
                .timeoutSeconds(timeout)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .maxRetries(3)
                .retryDelayMs(1000L)
                .executionMode(AiExecutionMode.API)
                .build();

        LOG.info("AI 建議服務初始化: provider={}, model={}, endpoint={}",
                provider, modelId, apiEndpoint);

        return new AiSuggestionService(config);
    }

    /**
     * 根據 AI Provider 取得對應的 API Endpoint
     */
    private String getApiEndpointForProvider(String provider) {
        switch (provider.toLowerCase()) {
            case "openai":
                return "https://api.openai.com/v1/chat/completions";
            case "anthropic":
                return "https://api.anthropic.com/v1/messages";
            case "gemini-api":
                return "https://generativelanguage.googleapis.com/v1/models";
            default:
                LOG.warn("未知的 AI Provider: {}, 使用 OpenAI endpoint", provider);
                return "https://api.openai.com/v1/chat/completions";
        }
    }

    /**
     * 根據 AI Provider 取得對應的 API Key
     *
     * @param provider AI Provider 名稱
     * @return API Key
     * @throws IllegalStateException 如果 API Key 未配置
     */
    private String getApiKeyForProvider(String provider) {
        String apiKey = null;
        String providerKeyName = null;

        // 根據 Provider 讀取對應的專用 API Key
        switch (provider.toLowerCase()) {
            case "openai":
                providerKeyName = "OpenAI API Key";
                apiKey = configuration.get(AiOwaspPlugin.PROPERTY_OPENAI_API_KEY).orElse(null);
                break;
            case "anthropic":
                providerKeyName = "Anthropic API Key";
                apiKey = configuration.get(AiOwaspPlugin.PROPERTY_ANTHROPIC_API_KEY).orElse(null);
                break;
            case "gemini-api":
                providerKeyName = "Google API Key";
                apiKey = configuration.get(AiOwaspPlugin.PROPERTY_GOOGLE_API_KEY).orElse(null);
                break;
            default:
                throw new IllegalStateException(String.format(
                    "不支援的 AI Provider: %s。支援的 Provider: openai, anthropic, gemini-api",
                    provider
                ));
        }

        // 檢查 API Key 是否已配置
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException(String.format(
                "%s 未配置: 請在 SonarQube Administration > AI OWASP Plugin > Authentication 中設定",
                providerKeyName
            ));
        }

        LOG.info("使用 {} (Provider: {})", providerKeyName, provider);
        return apiKey;
    }

    /**
     * 寫入 JSON 回應
     *
     * @param response   Web service 回應
     * @param aiResponse AI 回應
     */
    private void writeJsonResponse(Response response, AiResponse aiResponse) {
        response.stream().setMediaType("application/json");
        response.stream().setStatus(200);

        try {
            // 構建簡化的 JSON 回應（包含主要欄位）
            String json = String.format(
                    "{\"success\":%b,\"analysisResult\":\"%s\",\"tokensUsed\":%d,\"processingTimeMs\":%d,\"modelUsed\":\"%s\"}",
                    aiResponse.isSuccess(),
                    escapeJson(aiResponse.getAnalysisResult()),
                    aiResponse.getTokensUsed(),
                    aiResponse.getProcessingTimeMs(),
                    escapeJson(aiResponse.getModelUsed())
            );

            response.stream().output().write(json.getBytes());
            LOG.info("AI 建議回應成功: tokens={}, time={}ms",
                    aiResponse.getTokensUsed(), aiResponse.getProcessingTimeMs());

        } catch (IOException e) {
            LOG.error("寫入 JSON 回應失敗", e);
            writeErrorResponse(response, 500, "Failed to write response");
        }
    }

    /**
     * 寫入錯誤回應
     *
     * @param response Web service 回應
     * @param status   HTTP 狀態碼
     * @param message  錯誤訊息
     */
    private void writeErrorResponse(Response response, int status, String message) {
        response.stream().setMediaType("application/json");
        response.stream().setStatus(status);

        try {
            String json = String.format(
                    "{\"success\":false,\"error\":\"%s\"}",
                    escapeJson(message)
            );
            response.stream().output().write(json.getBytes());
        } catch (IOException e) {
            LOG.error("寫入錯誤回應失敗", e);
        }
    }

    /**
     * 轉義 JSON 字串中的特殊字元
     *
     * @param str 原始字串
     * @return 轉義後的字串
     */
    private String escapeJson(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
