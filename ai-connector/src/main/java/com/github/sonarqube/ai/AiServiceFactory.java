package com.github.sonarqube.ai;

import com.github.sonarqube.ai.cli.CliExecutor;
import com.github.sonarqube.ai.cli.ProcessCliExecutor;
import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiExecutionMode;
import com.github.sonarqube.ai.provider.ClaudeService;
import com.github.sonarqube.ai.provider.OpenAiService;
import com.github.sonarqube.ai.provider.copilot.CopilotCliService;
import com.github.sonarqube.ai.provider.gemini.GeminiApiService;
import com.github.sonarqube.ai.provider.gemini.GeminiCliService;

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
     * 支援 API 模式與 CLI 模式：
     * - API 模式：OpenAI, Claude, Gemini API
     * - CLI 模式：Gemini CLI, Copilot CLI (準備中), Claude CLI (準備中)
     *
     * @param config AI 配置
     * @return AI 服務實例
     * @throws IllegalArgumentException 當模型類型不支援時拋出
     */
    public static AiService createService(AiConfig config) {
        if (config == null || !config.isValid()) {
            throw new IllegalArgumentException("Invalid AI configuration");
        }

        AiExecutionMode mode = config.getExecutionMode();

        // CLI 模式
        if (mode == AiExecutionMode.CLI) {
            return createCliService(config);
        }

        // API 模式
        if (config.getModel().isOpenAI()) {
            return new OpenAiService(config);
        } else if (config.getModel().isClaude()) {
            return new ClaudeService(config);
        } else if (config.getModel().isGemini()) {
            return new GeminiApiService(config);
        } else {
            throw new IllegalArgumentException(
                "Unsupported AI model: " + config.getModel().getModelId()
            );
        }
    }

    /**
     * 建立 CLI 模式 AI 服務
     *
     * @param config AI 配置（必須包含 cliPath）
     * @return CLI AI 服務實例
     * @throws IllegalArgumentException 當 CLI 類型不支援時拋出
     */
    private static AiService createCliService(AiConfig config) {
        if (config.getCliPath() == null || config.getCliPath().trim().isEmpty()) {
            throw new IllegalArgumentException("CLI path is required for CLI mode");
        }

        // 建立 CLI 執行器
        CliExecutor executor = ProcessCliExecutor.builder()
            .cliPath(config.getCliPath())
            .timeout(config.getTimeoutSeconds())
            .build();

        // 根據 CLI 路徑判斷使用哪個 CLI 服務
        // 因為 CLI 模式下，不同工具對應不同服務
        String cliPath = config.getCliPath().toLowerCase();

        if (cliPath.contains("gemini")) {
            return new GeminiCliService(config, executor);
        } else if (cliPath.contains("gh") || cliPath.contains("copilot")) {
            return new CopilotCliService(config, executor);
        } else if (config.getModel().isGemini()) {
            // 備用：根據模型類型判斷
            return new GeminiCliService(config, executor);
        } else {
            throw new IllegalArgumentException(
                "Unsupported CLI tool: " + config.getCliPath() +
                ". Currently supported: Gemini CLI (gemini), GitHub Copilot CLI (gh)"
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

    /**
     * 建立預設的 Gemini API 服務實例
     *
     * @param apiKey Google Gemini API 金鑰
     * @return Gemini API 服務實例
     */
    public static AiService createGeminiService(String apiKey) {
        AiConfig config = AiConfig.builder()
            .model(com.github.sonarqube.ai.model.AiModel.GEMINI_1_5_PRO)
            .apiKey(apiKey)
            .build();
        return new GeminiApiService(config);
    }

    /**
     * 建立預設的 Gemini CLI 服務實例
     *
     * @param cliPath Gemini CLI 工具路徑
     * @return Gemini CLI 服務實例
     */
    public static AiService createGeminiCliService(String cliPath) {
        AiConfig config = AiConfig.builder()
            .model(com.github.sonarqube.ai.model.AiModel.GEMINI_1_5_PRO)
            .cliPath(cliPath)
            .executionMode(AiExecutionMode.CLI)
            .build();

        CliExecutor executor = ProcessCliExecutor.builder()
            .cliPath(cliPath)
            .timeout(60)
            .build();

        return new GeminiCliService(config, executor);
    }

    /**
     * 建立預設的 GitHub Copilot CLI 服務實例
     *
     * @param cliPath GitHub CLI (gh) 工具路徑
     * @return GitHub Copilot CLI 服務實例
     */
    public static AiService createCopilotCliService(String cliPath) {
        AiConfig config = AiConfig.builder()
            .model(com.github.sonarqube.ai.model.AiModel.GEMINI_1_5_PRO) // 使用通用模型
            .cliPath(cliPath)
            .executionMode(AiExecutionMode.CLI)
            .build();

        CliExecutor executor = ProcessCliExecutor.builder()
            .cliPath(cliPath)
            .timeout(60)
            .build();

        return new CopilotCliService(config, executor);
    }

    // 私有建構子，防止實例化
    private AiServiceFactory() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
