package com.github.sonarqube.plugin.service;

import com.github.sonarqube.ai.AiException;
import com.github.sonarqube.ai.AiService;
import com.github.sonarqube.ai.AiServiceFactory;
import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiRequest;
import com.github.sonarqube.ai.model.AiResponse;
import com.github.sonarqube.ai.model.PromptTemplate;
import com.github.sonarqube.ai.model.SecurityIssue;
import com.github.sonarqube.version.VersionManager;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.Closeable;
import java.util.List;

/**
 * AI 建議服務 - 提供按需 AI 修復建議
 *
 * 此服務用於在使用者明確請求時，為特定安全問題生成詳細的 AI 修復建議。
 * 這樣可以節省掃描階段的 Token 消耗，只在需要時才進行完整分析。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class AiSuggestionService implements Closeable {

    private static final Logger LOG = Loggers.get(AiSuggestionService.class);

    private final AiService aiService;

    /**
     * 建構子
     *
     * @param config AI 配置
     */
    public AiSuggestionService(AiConfig config) {
        if (config == null || !config.isValid()) {
            throw new IllegalArgumentException("Invalid AI configuration");
        }
        this.aiService = AiServiceFactory.createService(config);
        LOG.info("AI 建議服務初始化成功 (Provider: {}, Model: {})",
                aiService.getProviderName(), aiService.getModelName());
    }

    /**
     * 為特定安全問題生成詳細的 AI 修復建議
     *
     * 使用完整分析模式，包含：
     * - 詳細的修復步驟
     * - 修復前後的程式碼範例
     * - 工作量評估
     * - 潛在副作用與注意事項
     *
     * @param code 問題代碼片段
     * @param owaspCategory OWASP 類別（例如: A01:2021-Broken Access Control）
     * @param cweId CWE ID（例如: CWE-284）
     * @param language 程式語言（例如: java, javascript）
     * @param fileName 檔案名稱（選填）
     * @param responseLanguage AI 回應的語言偏好（例如: en-US, zh-TW）
     * @return AI 修復建議回應
     * @throws AiException AI 分析失敗時拋出
     */
    public AiResponse generateFixSuggestion(
            String code,
            String owaspCategory,
            String cweId,
            String language,
            String fileName,
            String responseLanguage
    ) throws AiException {
        LOG.info("生成 AI 修復建議: file={}, owasp={}, cwe={}, responseLang={}",
                fileName, owaspCategory, cweId, responseLanguage);

        // 根據語言偏好生成指示
        String languageInstruction = buildLanguageInstruction(responseLanguage);

        // 建立完整分析模式的 AI 請求
        AiRequest request = AiRequest.builder(code)
                .language(language)
                .fileName(fileName)
                .analysisType("full_analysis")  // 使用完整分析模式
                .owaspVersion(VersionManager.getCurrentVersion().getVersion())
                .additionalContext(String.format(
                        "%sFocus on: OWASP %s, CWE %s",
                        languageInstruction, owaspCategory, cweId
                ))
                .build();

        try {
            // 呼叫 AI 服務進行完整分析
            AiResponse response = aiService.analyzeCode(request);

            if (response == null || !response.isSuccess()) {
                LOG.warn("AI 建議生成失敗: {}", response != null ? response.getAnalysisResult() : "No response");
                throw new AiException(
                        "Failed to generate AI suggestion",
                        AiException.ErrorType.UNKNOWN_ERROR,
                        aiService.getProviderName()
                );
            }

            LOG.info("AI 建議生成成功: tokens={}, time={}ms",
                    response.getTokensUsed(), response.getProcessingTimeMs());

            return response;

        } catch (AiException e) {
            LOG.error("AI 建議生成異常: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 為特定安全問題生成簡化的 AI 修復建議（使用專用的修復建議 prompt）
     *
     * 此方法使用 PromptTemplate.createFixSuggestionPrompt() 來生成針對性的修復建議。
     *
     * @param code 問題代碼片段
     * @param owaspCategory OWASP 類別
     * @param cweId CWE ID
     * @return AI 修復建議文字
     * @throws AiException AI 分析失敗時拋出
     */
    public String generateFocusedFixSuggestion(
            String code,
            String owaspCategory,
            String cweId
    ) throws AiException {
        LOG.info("生成聚焦 AI 修復建議: owasp={}, cwe={}", owaspCategory, cweId);

        // 使用專用的修復建議 prompt
        String prompt = PromptTemplate.createFixSuggestionPrompt(code, owaspCategory, cweId);

        // 創建簡化的請求（直接使用 prompt）
        AiRequest request = AiRequest.builder(code)
                .language("unknown")
                .analysisType("fix_suggestion")
                .additionalContext(prompt)
                .build();

        try {
            AiResponse response = aiService.analyzeCode(request);

            if (response == null || !response.isSuccess()) {
                throw new AiException(
                        "Failed to generate focused fix suggestion",
                        AiException.ErrorType.UNKNOWN_ERROR,
                        aiService.getProviderName()
                );
            }

            // 返回 AI 的回應文字
            return response.getAnalysisResult();

        } catch (AiException e) {
            LOG.error("聚焦 AI 建議生成異常: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 測試 AI 服務連接
     *
     * @return true 如果連接成功，否則 false
     */
    public boolean testConnection() {
        try {
            return aiService.testConnection();
        } catch (Exception e) {
            LOG.error("AI 連接測試失敗: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 取得 AI Provider 名稱
     *
     * @return Provider 名稱（例如: OpenAI, Claude）
     */
    public String getProviderName() {
        return aiService.getProviderName();
    }

    /**
     * 取得 AI 模型名稱
     *
     * @return 模型名稱（例如: gpt-4, claude-3）
     */
    public String getModelName() {
        return aiService.getModelName();
    }

    /**
     * 根據語言偏好建立 AI 指示
     *
     * @param responseLanguage 回應語言偏好（en-US 或 zh-TW）
     * @return 語言指示字串
     */
    private String buildLanguageInstruction(String responseLanguage) {
        if (responseLanguage == null || responseLanguage.isEmpty()) {
            responseLanguage = "zh-TW"; // 預設使用繁體中文
        }

        if ("zh-TW".equals(responseLanguage)) {
            return "Please respond in Traditional Chinese (繁體中文). " +
                   "所有分析結果、修復建議、程式碼範例說明等，都請使用繁體中文回應。\n\n";
        } else if ("en-US".equals(responseLanguage)) {
            return "Please respond in English. " +
                   "All analysis results, fix suggestions, and code examples should be in English.\n\n";
        } else {
            // 未知語言，預設使用繁體中文
            LOG.warn("未知的語言偏好: {}, 使用預設值 zh-TW", responseLanguage);
            return "Please respond in Traditional Chinese (繁體中文). " +
                   "所有分析結果、修復建議、程式碼範例說明等，都請使用繁體中文回應。\n\n";
        }
    }

    @Override
    public void close() {
        if (aiService != null) {
            aiService.close();
            LOG.info("AI 建議服務已關閉");
        }
    }
}
