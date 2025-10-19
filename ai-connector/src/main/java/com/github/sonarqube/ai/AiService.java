package com.github.sonarqube.ai;

import com.github.sonarqube.ai.model.AiRequest;
import com.github.sonarqube.ai.model.AiResponse;

/**
 * AI 服務統一介面
 *
 * 定義所有 AI 提供者（OpenAI、Claude 等）必須實現的統一介面。
 * 採用策略模式，允許運行時切換不同的 AI 模型。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public interface AiService {

    /**
     * 分析代碼並生成安全建議
     *
     * @param request AI 請求，包含代碼內容和分析參數
     * @return AI 回應，包含分析結果和修復建議
     * @throws AiException 當 AI 服務調用失敗時拋出
     */
    AiResponse analyzeCode(AiRequest request) throws AiException;

    /**
     * 測試 AI 服務連接是否正常
     *
     * @return true 如果連接正常，false 否則
     */
    boolean testConnection();

    /**
     * 獲取 AI 服務提供者名稱
     *
     * @return 提供者名稱（例如："OpenAI", "Claude"）
     */
    String getProviderName();

    /**
     * 獲取 AI 模型名稱
     *
     * @return 模型名稱（例如："gpt-4", "claude-3-opus"）
     */
    String getModelName();

    /**
     * 關閉 AI 服務連接並釋放資源
     */
    void close();
}
