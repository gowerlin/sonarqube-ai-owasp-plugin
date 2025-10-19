package com.github.sonarqube.ai.provider.cli;

import com.github.sonarqube.ai.AiException;
import com.github.sonarqube.ai.AiService;
import com.github.sonarqube.ai.cli.CliExecutionException;
import com.github.sonarqube.ai.cli.CliExecutor;
import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiRequest;
import com.github.sonarqube.ai.model.AiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * CLI 模式 AI 服務抽象基類
 *
 * 提供 CLI 工具調用的通用實作，子類別需實作：
 * - buildCliCommand(): 建構特定 CLI 工具的命令
 * - parseCliOutput(): 解析 CLI 輸出為 AiResponse
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9, Story 9.3)
 */
public abstract class AbstractCliService implements AiService {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCliService.class);

    protected final AiConfig config;
    protected final CliExecutor executor;

    /**
     * 建構 CLI 服務
     *
     * @param config   AI 配置
     * @param executor CLI 執行器
     */
    protected AbstractCliService(AiConfig config, CliExecutor executor) {
        if (config == null) {
            throw new IllegalArgumentException("AiConfig cannot be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("CliExecutor cannot be null");
        }

        this.config = config;
        this.executor = executor;
    }

    @Override
    public AiResponse analyzeCode(AiRequest request) throws AiException {
        if (request == null) {
            throw new IllegalArgumentException("AiRequest cannot be null");
        }

        try {
            LOG.debug("Analyzing code using CLI: {}", getProviderName());

            // 1. 建構 CLI 命令
            String[] command = buildCliCommand(request);

            LOG.debug("CLI command: {}", String.join(" ", command));

            // 2. 執行 CLI 命令
            String output = executor.executeCommand(command, buildCliInput(request));

            LOG.debug("CLI output received ({} chars)", output.length());

            // 3. 解析 CLI 輸出
            AiResponse response = parseCliOutput(output, request);

            LOG.debug("CLI analysis completed with {} findings", response.getFindings().size());

            return response;

        } catch (CliExecutionException e) {
            LOG.error("CLI execution failed", e);
            throw new AiException(
                "Failed to execute CLI command: " + e.getMessage(),
                e
            );
        } catch (IOException e) {
            LOG.error("CLI I/O error", e);
            throw new AiException(
                "CLI I/O error: " + e.getMessage(),
                e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("CLI execution interrupted", e);
            throw new AiException(
                "CLI execution interrupted: " + e.getMessage(),
                e
            );
        }
    }

    @Override
    public boolean testConnection() {
        try {
            LOG.debug("Testing CLI connection: {}", getProviderName());

            // 檢查 CLI 工具是否可用
            if (!executor.isCliAvailable()) {
                LOG.warn("CLI tool not available at: {}", executor.getCliPath());
                return false;
            }

            // 取得版本資訊以驗證 CLI 工具正常運作
            String version = executor.getCliVersion();
            LOG.info("CLI tool version: {}", version);

            return true;

        } catch (Exception e) {
            LOG.error("CLI connection test failed", e);
            return false;
        }
    }

    @Override
    public void close() {
        LOG.debug("Closing CLI service: {}", getProviderName());
        // CLI 服務通常不需要特殊清理
        // 子類別可覆寫以實作特定清理邏輯
    }

    /**
     * 建構 CLI 命令陣列
     *
     * 子類別必須實作此方法以建構特定 CLI 工具的命令。
     *
     * @param request AI 請求
     * @return CLI 命令陣列
     */
    protected abstract String[] buildCliCommand(AiRequest request);

    /**
     * 建構 CLI 標準輸入內容（可選）
     *
     * 某些 CLI 工具透過標準輸入接收提示詞。
     * 預設實作回傳 null（無標準輸入）。
     *
     * @param request AI 請求
     * @return 標準輸入內容，或 null
     */
    protected String buildCliInput(AiRequest request) {
        return null; // 預設無標準輸入
    }

    /**
     * 解析 CLI 輸出為 AiResponse
     *
     * 子類別必須實作此方法以解析特定 CLI 工具的輸出格式。
     *
     * @param output  CLI 輸出
     * @param request 原始請求（用於上下文資訊）
     * @return 解析後的 AiResponse
     * @throws AiException 當解析失敗時拋出
     */
    protected abstract AiResponse parseCliOutput(String output, AiRequest request) throws AiException;

    /**
     * 建構代碼分析提示詞
     *
     * 通用的代碼分析提示詞模板，子類別可覆寫以自訂格式。
     *
     * @param request AI 請求
     * @return 提示詞字串
     */
    protected String buildAnalysisPrompt(AiRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Please analyze the following code for security vulnerabilities:\n\n");
        prompt.append("```\n");
        prompt.append(request.getCode());
        prompt.append("\n```\n\n");

        prompt.append("Focus on:\n");
        prompt.append("- OWASP Top 10 vulnerabilities\n");
        prompt.append("- Common security issues\n");
        prompt.append("- Best practices violations\n\n");

        prompt.append("For each issue found, provide:\n");
        prompt.append("1. Severity level (BLOCKER, CRITICAL, MAJOR, MINOR, INFO)\n");
        prompt.append("2. OWASP category (e.g., A03:2021-Injection)\n");
        prompt.append("3. CWE IDs\n");
        prompt.append("4. Description of the vulnerability\n");
        prompt.append("5. Suggested fix\n");

        return prompt.toString();
    }

    /**
     * 取得 CLI 執行器
     */
    protected CliExecutor getExecutor() {
        return executor;
    }

    /**
     * 取得配置
     */
    protected AiConfig getConfig() {
        return config;
    }
}
