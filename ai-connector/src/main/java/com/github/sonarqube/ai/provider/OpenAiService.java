package com.github.sonarqube.ai.provider;

import com.github.sonarqube.ai.AiException;
import com.github.sonarqube.ai.AiService;
import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiRequest;
import com.github.sonarqube.ai.model.AiResponse;

/**
 * OpenAI 服務實現
 *
 * 整合 OpenAI GPT-4/GPT-3.5 API 進行代碼安全分析。
 * 完整實現將在 Story 2.2 中完成。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class OpenAiService implements AiService {

    private final AiConfig config;

    public OpenAiService(AiConfig config) {
        if (config == null || !config.isValid()) {
            throw new IllegalArgumentException("Invalid AI configuration");
        }
        if (!config.getModel().isOpenAI()) {
            throw new IllegalArgumentException("Config must be for OpenAI model");
        }
        this.config = config;
    }

    @Override
    public AiResponse analyzeCode(AiRequest request) throws AiException {
        // TODO: Story 2.2 - 實現完整的 OpenAI API 整合
        throw new UnsupportedOperationException("Will be implemented in Story 2.2");
    }

    @Override
    public boolean testConnection() {
        // TODO: Story 2.2 - 實現連接測試
        return false;
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
        // TODO: Story 2.2 - 實現資源清理
    }
}
