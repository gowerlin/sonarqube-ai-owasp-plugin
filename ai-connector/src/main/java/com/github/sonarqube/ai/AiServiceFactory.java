package com.github.sonarqube.ai;

import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.provider.ClaudeService;
import com.github.sonarqube.ai.provider.OpenAiService;

/**
 * AI 服務工廠
 *
 * 使用工廠模式建立不同的 AI 服務實例。
 * 根據配置的 AI 模型自動選擇對應的服務提供者。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class AiServiceFactory {

    /**
     * 根據配置建立 AI 服務實例
     *
     * @param config AI 配置
     * @return AI 服務實例
     * @throws IllegalArgumentException 當模型類型不支援時拋出
     */
    public static AiService createService(AiConfig config) {
        if (config == null || !config.isValid()) {
            throw new IllegalArgumentException("Invalid AI configuration");
        }

        if (config.getModel().isOpenAI()) {
            return new OpenAiService(config);
        } else if (config.getModel().isClaude()) {
            return new ClaudeService(config);
        } else {
            throw new IllegalArgumentException(
                "Unsupported AI model: " + config.getModel().getModelId()
            );
        }
    }

    /**
     * 建立預設的 OpenAI 服務實例
     *
     * @param apiKey OpenAI API 金鑰
     * @return OpenAI 服務實例
     */
    public static AiService createOpenAiService(String apiKey) {
        AiConfig config = AiConfig.builder()
            .model(com.github.sonarqube.ai.model.AiModel.GPT_4)
            .apiKey(apiKey)
            .build();
        return new OpenAiService(config);
    }

    /**
     * 建立預設的 Claude 服務實例
     *
     * @param apiKey Anthropic API 金鑰
     * @return Claude 服務實例
     */
    public static AiService createClaudeService(String apiKey) {
        AiConfig config = AiConfig.builder()
            .model(com.github.sonarqube.ai.model.AiModel.CLAUDE_3_OPUS)
            .apiKey(apiKey)
            .build();
        return new ClaudeService(config);
    }

    // 私有建構子，防止實例化
    private AiServiceFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
