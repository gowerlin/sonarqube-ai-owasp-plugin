package com.github.sonarqube.plugin.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * GitHub Copilot CLI 執行器
 *
 * 整合 GitHub Copilot CLI 進行 AI 安全分析。
 *
 * 功能特性：
 * - 支援 GitHub Copilot Chat
 * - 本地 GitHub 認證（gh auth）
 * - 程式碼建議與分析
 * - 安全最佳實踐建議
 *
 * CLI 安裝：
 * ```bash
 * gh extension install github/gh-copilot
 * gh auth login
 * ```
 *
 * 使用範例：
 * ```bash
 * gh copilot suggest "Analyze this code for security issues..."
 * ```
 *
 * @since 3.0.0 (Epic 9)
 * @author SonarQube AI OWASP Plugin Team
 */
public class CopilotCliExecutor extends CliExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(CopilotCliExecutor.class);

    /**
     * 建立 Copilot CLI 執行器
     *
     * @param cliPath GitHub CLI 路徑（預設: /usr/local/bin/gh）
     * @param timeoutSeconds 超時時間（秒）
     */
    public CopilotCliExecutor(String cliPath, int timeoutSeconds) {
        super(cliPath, timeoutSeconds);
        LOG.info("CopilotCliExecutor 已初始化: cliPath={}, timeout={}s",
                cliPath, timeoutSeconds);
    }

    /**
     * 簡化建構子（使用預設參數）
     */
    public CopilotCliExecutor(String cliPath) {
        this(cliPath, 60);
    }

    @Override
    public String analyze(String prompt, String codeSnippet) throws CliExecutionException {
        LOG.debug("開始 Copilot CLI 分析: codeLength={}", codeSnippet.length());

        // 建構完整提示詞
        String fullPrompt = buildSecurityPrompt(codeSnippet);

        // 建構 CLI 命令
        List<String> command = buildCommand(fullPrompt);

        // 執行命令
        long startTime = System.currentTimeMillis();
        String output = executeCommand(command);
        long duration = System.currentTimeMillis() - startTime;

        LOG.debug("Copilot CLI 分析完成: duration={}ms, outputLength={}",
                duration, output.length());

        return cleanOutput(output);
    }

    /**
     * 建構 Copilot CLI 命令
     */
    private List<String> buildCommand(String prompt) {
        List<String> command = new ArrayList<>();

        command.add(cliPath);
        command.add("copilot");
        command.add("suggest");

        // 使用 --target shell 以獲得更好的輸出格式
        command.add("--target");
        command.add("shell");

        // 提示詞
        command.add(prompt);

        return command;
    }

    /**
     * 建構安全分析提示詞
     */
    private String buildSecurityPrompt(String codeSnippet) {
        return String.format(
                "Security analysis request:\n" +
                        "Analyze the following code for OWASP security vulnerabilities.\n" +
                        "Provide:\n" +
                        "- Vulnerability type (OWASP category)\n" +
                        "- CWE ID\n" +
                        "- Severity (CRITICAL/HIGH/MEDIUM/LOW)\n" +
                        "- Description of the security issue\n" +
                        "- Fix recommendations with code examples\n\n" +
                        "Code:\n%s",
                codeSnippet
        );
    }

    /**
     * 清理 Copilot CLI 輸出
     *
     * GitHub Copilot CLI 可能包含互動式提示和格式化字元，
     * 需要提取純文字內容。
     */
    private String cleanOutput(String rawOutput) {
        // 移除 ANSI 顏色代碼
        String cleaned = rawOutput.replaceAll("\u001B\\[[;\\d]*m", "");

        // 移除常見的 CLI 提示符號
        cleaned = cleaned.replaceAll("^[>?]\\s*", "");

        // 移除多餘的空白行
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        return cleaned.trim();
    }

    /**
     * 檢查 GitHub Copilot 是否已安裝
     */
    @Override
    public boolean isAvailable() {
        try {
            // 檢查 gh CLI 是否可用
            if (!super.isAvailable()) {
                return false;
            }

            // 檢查 Copilot 擴充功能是否已安裝
            List<String> command = new ArrayList<>();
            command.add(cliPath);
            command.add("extension");
            command.add("list");

            String output = executeCommand(command);
            return output.contains("gh-copilot") || output.contains("copilot");

        } catch (Exception e) {
            LOG.debug("檢查 Copilot 可用性失敗", e);
            return false;
        }
    }

    /**
     * 檢查 GitHub Copilot 是否已認證
     */
    public boolean isAuthenticated() {
        try {
            List<String> command = new ArrayList<>();
            command.add(cliPath);
            command.add("auth");
            command.add("status");

            String output = executeCommand(command);
            return output.contains("Logged in") || output.contains("authenticated");

        } catch (Exception e) {
            LOG.debug("檢查 Copilot 認證失敗", e);
            return false;
        }
    }

    /**
     * 取得當前 GitHub 使用者
     */
    public String getAuthenticatedUser() {
        try {
            List<String> command = new ArrayList<>();
            command.add(cliPath);
            command.add("api");
            command.add("user");
            command.add("--jq");
            command.add(".login");

            String output = executeCommand(command);
            return output != null ? output.trim() : "unknown";

        } catch (Exception e) {
            LOG.debug("取得 GitHub 使用者失敗", e);
            return "unknown";
        }
    }

    /**
     * 使用 Copilot Chat 模式進行分析
     *
     * 提供更互動式的分析體驗
     */
    public String analyzeWithChat(String codeSnippet) throws CliExecutionException {
        LOG.debug("開始 Copilot Chat 分析: codeLength={}", codeSnippet.length());

        // 建構提示詞
        String prompt = buildSecurityPrompt(codeSnippet);

        // 使用 gh copilot explain（Copilot Chat 功能）
        List<String> command = new ArrayList<>();
        command.add(cliPath);
        command.add("copilot");
        command.add("explain");
        command.add(prompt);

        // 執行命令
        String output = executeCommand(command);

        return cleanOutput(output);
    }
}
