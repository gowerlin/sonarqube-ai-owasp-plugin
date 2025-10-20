package com.github.sonarqube.plugin.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Gemini CLI 執行器
 *
 * 整合 Google Gemini CLI 工具進行 AI 安全分析。
 *
 * 功能特性：
 * - 支援 Gemini 1.5 Pro 與 Flash 模型
 * - 本地 OAuth 認證（gcloud auth）
 * - JSON 輸出格式
 * - 溫度與 token 參數控制
 *
 * CLI 安裝：
 * ```bash
 * gcloud components install gemini
 * gcloud auth login
 * ```
 *
 * 使用範例：
 * ```bash
 * gemini chat --model=gemini-1.5-pro --temperature=0.3 "Analyze code..."
 * ```
 *
 * @since 3.0.0 (Epic 9)
 * @author SonarQube AI OWASP Plugin Team
 */
public class GeminiCliExecutor extends CliExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(GeminiCliExecutor.class);

    private final String model;
    private final double temperature;
    private final int maxTokens;

    /**
     * 建立 Gemini CLI 執行器
     *
     * @param cliPath Gemini CLI 路徑（預設: /usr/local/bin/gemini）
     * @param model 模型名稱（gemini-1.5-pro 或 gemini-1.5-flash）
     * @param temperature 溫度參數（0.0-1.0）
     * @param maxTokens 最大 token 數
     * @param timeoutSeconds 超時時間（秒）
     */
    public GeminiCliExecutor(String cliPath, String model, double temperature,
                             int maxTokens, int timeoutSeconds) {
        super(cliPath, timeoutSeconds);
        this.model = model != null ? model : "gemini-1.5-pro";
        this.temperature = Math.max(0.0, Math.min(1.0, temperature));
        this.maxTokens = Math.max(256, maxTokens);

        LOG.info("GeminiCliExecutor 已初始化: model={}, temperature={}, maxTokens={}",
                this.model, this.temperature, this.maxTokens);
    }

    /**
     * 簡化建構子（使用預設參數）
     */
    public GeminiCliExecutor(String cliPath) {
        this(cliPath, "gemini-1.5-pro", 0.3, 2000, 60);
    }

    @Override
    public String analyze(String prompt, String codeSnippet) throws CliExecutionException {
        LOG.debug("開始 Gemini CLI 分析: codeLength={}", codeSnippet.length());

        // 建構完整提示詞
        String fullPrompt = buildPrompt(codeSnippet);

        // 建構 CLI 命令
        List<String> command = buildCommand(fullPrompt);

        // 執行命令
        long startTime = System.currentTimeMillis();
        String output = executeCommand(command);
        long duration = System.currentTimeMillis() - startTime;

        LOG.debug("Gemini CLI 分析完成: duration={}ms, outputLength={}",
                duration, output.length());

        return parseOutput(output);
    }

    /**
     * 建構 Gemini CLI 命令
     */
    private List<String> buildCommand(String prompt) {
        List<String> command = new ArrayList<>();

        command.add(cliPath);
        command.add("chat");

        // 模型參數
        command.add("--model=" + model);
        command.add("--temperature=" + temperature);
        command.add("--max-tokens=" + maxTokens);

        // 輸出格式
        command.add("--format=json");

        // 提示詞（作為最後參數）
        command.add(prompt);

        return command;
    }

    /**
     * 解析 Gemini CLI 輸出
     *
     * Gemini CLI 輸出格式範例：
     * {
     *   "candidates": [{
     *     "content": {
     *       "parts": [{"text": "分析結果..."}]
     *     }
     *   }]
     * }
     */
    private String parseOutput(String rawOutput) throws CliExecutionException {
        try {
            // 簡化解析：直接尋找 JSON 中的 text 欄位
            // 生產環境建議使用 JSON 函式庫（如 Gson）
            int textStart = rawOutput.indexOf("\"text\":");
            if (textStart == -1) {
                throw new CliExecutionException("無法解析 Gemini CLI 輸出：找不到 text 欄位");
            }

            // 找到 text 值的起始引號
            int valueStart = rawOutput.indexOf("\"", textStart + 7);
            if (valueStart == -1) {
                throw new CliExecutionException("無法解析 Gemini CLI 輸出：text 值格式錯誤");
            }

            // 找到 text 值的結束引號（需處理跳脫字元）
            int valueEnd = findClosingQuote(rawOutput, valueStart + 1);
            if (valueEnd == -1) {
                throw new CliExecutionException("無法解析 Gemini CLI 輸出：找不到 text 結束引號");
            }

            String textContent = rawOutput.substring(valueStart + 1, valueEnd);

            // 移除 JSON 跳脫字元
            return unescapeJson(textContent);

        } catch (Exception e) {
            LOG.error("解析 Gemini CLI 輸出失敗", e);
            throw new CliExecutionException("解析 CLI 輸出失敗", e);
        }
    }

    /**
     * 找到 JSON 字串的結束引號（處理跳脫字元）
     */
    private int findClosingQuote(String json, int startIndex) {
        for (int i = startIndex; i < json.length(); i++) {
            if (json.charAt(i) == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
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
     * 檢查 Gemini CLI 是否已認證
     */
    public boolean isAuthenticated() {
        try {
            List<String> command = new ArrayList<>();
            command.add("gcloud");
            command.add("auth");
            command.add("list");
            command.add("--filter=status:ACTIVE");
            command.add("--format=value(account)");

            String output = executeCommand(command);
            return output != null && !output.trim().isEmpty();

        } catch (Exception e) {
            LOG.debug("檢查 Gemini 認證失敗", e);
            return false;
        }
    }

    /**
     * 取得當前認證帳號
     */
    public String getAuthenticatedAccount() {
        try {
            List<String> command = new ArrayList<>();
            command.add("gcloud");
            command.add("auth");
            command.add("list");
            command.add("--filter=status:ACTIVE");
            command.add("--format=value(account)");

            String output = executeCommand(command);
            return output != null ? output.trim() : "unknown";

        } catch (Exception e) {
            LOG.debug("取得認證帳號失敗", e);
            return "unknown";
        }
    }
}
