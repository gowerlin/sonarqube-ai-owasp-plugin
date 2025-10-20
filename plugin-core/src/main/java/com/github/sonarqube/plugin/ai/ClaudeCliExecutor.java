package com.github.sonarqube.plugin.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Claude CLI 執行器
 *
 * 整合 Anthropic Claude CLI 進行 AI 安全分析。
 *
 * 功能特性：
 * - 支援 Claude 3 系列模型（Opus, Sonnet, Haiku）
 * - 本地 API 金鑰配置
 * - 長文本分析能力（200K tokens）
 * - 精確的程式碼理解
 *
 * CLI 安裝：
 * ```bash
 * npm install -g @anthropic-ai/claude-cli
 * claude config set api-key <YOUR_API_KEY>
 * ```
 *
 * 使用範例：
 * ```bash
 * claude chat --model=claude-3-sonnet-20240229 "Analyze code..."
 * ```
 *
 * @since 3.0.0 (Epic 9)
 * @author SonarQube AI OWASP Plugin Team
 */
public class ClaudeCliExecutor extends CliExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(ClaudeCliExecutor.class);

    private final String model;
    private final double temperature;
    private final int maxTokens;

    /**
     * 建立 Claude CLI 執行器
     *
     * @param cliPath Claude CLI 路徑（預設: /usr/local/bin/claude）
     * @param model 模型名稱（claude-3-opus, sonnet, haiku）
     * @param temperature 溫度參數（0.0-1.0）
     * @param maxTokens 最大 token 數
     * @param timeoutSeconds 超時時間（秒）
     */
    public ClaudeCliExecutor(String cliPath, String model, double temperature,
                             int maxTokens, int timeoutSeconds) {
        super(cliPath, timeoutSeconds);
        this.model = model != null ? model : "claude-3-sonnet-20240229";
        this.temperature = Math.max(0.0, Math.min(1.0, temperature));
        this.maxTokens = Math.max(256, maxTokens);

        LOG.info("ClaudeCliExecutor 已初始化: model={}, temperature={}, maxTokens={}",
                this.model, this.temperature, this.maxTokens);
    }

    /**
     * 簡化建構子（使用預設參數）
     */
    public ClaudeCliExecutor(String cliPath) {
        this(cliPath, "claude-3-sonnet-20240229", 0.3, 2000, 60);
    }

    @Override
    public String analyze(String prompt, String codeSnippet) throws CliExecutionException {
        LOG.debug("開始 Claude CLI 分析: codeLength={}", codeSnippet.length());

        // 建構完整提示詞
        String fullPrompt = buildPrompt(codeSnippet);

        // 建構 CLI 命令
        List<String> command = buildCommand(fullPrompt);

        // 執行命令
        long startTime = System.currentTimeMillis();
        String output = executeCommand(command);
        long duration = System.currentTimeMillis() - startTime;

        LOG.debug("Claude CLI 分析完成: duration={}ms, outputLength={}",
                duration, output.length());

        return parseOutput(output);
    }

    /**
     * 建構 Claude CLI 命令
     */
    private List<String> buildCommand(String prompt) {
        List<String> command = new ArrayList<>();

        command.add(cliPath);
        command.add("chat");

        // 模型參數
        command.add("--model");
        command.add(model);

        command.add("--temperature");
        command.add(String.valueOf(temperature));

        command.add("--max-tokens");
        command.add(String.valueOf(maxTokens));

        // 輸出格式
        command.add("--format");
        command.add("json");

        // 非互動模式
        command.add("--no-stream");

        // 提示詞
        command.add("--message");
        command.add(prompt);

        return command;
    }

    /**
     * 解析 Claude CLI 輸出
     *
     * Claude CLI JSON 輸出格式：
     * {
     *   "content": [{
     *     "type": "text",
     *     "text": "分析結果..."
     *   }],
     *   "stop_reason": "end_turn"
     * }
     */
    private String parseOutput(String rawOutput) throws CliExecutionException {
        try {
            // 簡化解析：直接尋找 JSON 中的 text 欄位
            // 生產環境建議使用 JSON 函式庫（如 Gson）
            int contentStart = rawOutput.indexOf("\"content\":");
            if (contentStart == -1) {
                throw new CliExecutionException("無法解析 Claude CLI 輸出：找不到 content 欄位");
            }

            // 找到第一個 text 值
            int textStart = rawOutput.indexOf("\"text\":", contentStart);
            if (textStart == -1) {
                throw new CliExecutionException("無法解析 Claude CLI 輸出：找不到 text 欄位");
            }

            // 找到 text 值的起始引號
            int valueStart = rawOutput.indexOf("\"", textStart + 7);
            if (valueStart == -1) {
                throw new CliExecutionException("無法解析 Claude CLI 輸出：text 值格式錯誤");
            }

            // 找到 text 值的結束引號
            int valueEnd = findClosingQuote(rawOutput, valueStart + 1);
            if (valueEnd == -1) {
                throw new CliExecutionException("無法解析 Claude CLI 輸出：找不到 text 結束引號");
            }

            String textContent = rawOutput.substring(valueStart + 1, valueEnd);

            // 移除 JSON 跳脫字元
            return unescapeJson(textContent);

        } catch (Exception e) {
            LOG.error("解析 Claude CLI 輸出失敗", e);
            throw new CliExecutionException("解析 CLI 輸出失敗", e);
        }
    }

    /**
     * 找到 JSON 字串的結束引號（處理跳脫字元）
     */
    private int findClosingQuote(String json, int startIndex) {
        boolean escaped = false;
        for (int i = startIndex; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    /**
     * 移除 JSON 跳脫字元
     */
    private String unescapeJson(String text) {
        return text
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    /**
     * 檢查 Claude CLI 是否已配置 API 金鑰
     */
    public boolean isConfigured() {
        try {
            List<String> command = new ArrayList<>();
            command.add(cliPath);
            command.add("config");
            command.add("get");
            command.add("api-key");

            String output = executeCommand(command);
            return output != null && !output.trim().isEmpty() && !output.contains("not set");

        } catch (Exception e) {
            LOG.debug("檢查 Claude 配置失敗", e);
            return false;
        }
    }

    /**
     * 使用 thinking blocks 模式進行深度分析
     *
     * Claude 的 thinking blocks 提供更詳細的推理過程
     */
    public String analyzeWithThinking(String codeSnippet) throws CliExecutionException {
        LOG.debug("開始 Claude Thinking Blocks 分析: codeLength={}", codeSnippet.length());

        // 建構 thinking prompt
        String thinkingPrompt = String.format(
                "Use thinking blocks to analyze this code for security vulnerabilities.\n\n" +
                        "<thinking>\n" +
                        "Think step-by-step about potential OWASP security issues in this code.\n" +
                        "</thinking>\n\n" +
                        "Code to analyze:\n```\n%s\n```\n\n" +
                        "Provide:\n" +
                        "1. OWASP category\n" +
                        "2. CWE ID\n" +
                        "3. Severity\n" +
                        "4. Description\n" +
                        "5. Fix recommendations",
                codeSnippet
        );

        // 建構命令
        List<String> command = buildCommand(thinkingPrompt);

        // 執行分析
        String output = executeCommand(command);

        return parseOutput(output);
    }

    /**
     * 取得支援的模型列表
     */
    public List<String> getSupportedModels() {
        List<String> models = new ArrayList<>();
        models.add("claude-3-opus-20240229");
        models.add("claude-3-sonnet-20240229");
        models.add("claude-3-haiku-20240307");
        models.add("claude-3-5-sonnet-20240620");
        return models;
    }

    /**
     * 檢查 API 配額
     *
     * 注意：Claude CLI 可能不直接提供配額資訊，
     * 需要透過 Anthropic API 查詢
     */
    public String checkQuota() {
        try {
            List<String> command = new ArrayList<>();
            command.add(cliPath);
            command.add("quota");

            String output = executeCommand(command);
            return output != null ? output.trim() : "unknown";

        } catch (Exception e) {
            LOG.debug("檢查 Claude 配額失敗", e);
            return "unknown";
        }
    }
}
