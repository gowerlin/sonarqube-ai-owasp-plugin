package com.github.sonarqube.plugin.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * CLI 執行器介面
 *
 * 定義 AI CLI 工具的標準執行介面，支援：
 * - Gemini CLI (Google)
 * - Copilot CLI (GitHub)
 * - Claude CLI (Anthropic)
 *
 * CLI 模式優勢：
 * - 無需管理 API 金鑰（使用本地認證）
 * - 支援離線/私有部署場景
 * - 整合至開發者本地工具鏈
 * - 成本控制（基於本地配額）
 *
 * @since 3.0.0 (Epic 9)
 * @author SonarQube AI OWASP Plugin Team
 */
public abstract class CliExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(CliExecutor.class);

    protected final String cliPath;
    protected final int timeoutSeconds;

    /**
     * 建立 CLI 執行器
     *
     * @param cliPath CLI 工具的可執行檔路徑
     * @param timeoutSeconds 執行超時時間（秒）
     */
    public CliExecutor(String cliPath, int timeoutSeconds) {
        this.cliPath = cliPath;
        this.timeoutSeconds = Math.max(10, timeoutSeconds);
    }

    /**
     * 執行 AI 分析
     *
     * @param prompt 分析提示詞
     * @param codeSnippet 待分析的程式碼片段
     * @return AI 回應內容
     * @throws CliExecutionException 當執行失敗時拋出
     */
    public abstract String analyze(String prompt, String codeSnippet) throws CliExecutionException;

    /**
     * 檢查 CLI 工具是否可用
     *
     * @return true 如果 CLI 工具可執行
     */
    public boolean isAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(cliPath, "--version");
            Process process = pb.start();
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);

            if (!completed) {
                process.destroy();
                return false;
            }

            return process.exitValue() == 0;

        } catch (Exception e) {
            LOG.debug("CLI 工具不可用: {}", cliPath, e);
            return false;
        }
    }

    /**
     * 取得 CLI 版本資訊
     *
     * @return 版本字串，若無法取得則返回 "unknown"
     */
    public String getVersion() {
        try {
            ProcessBuilder pb = new ProcessBuilder(cliPath, "--version");
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String version = reader.readLine();
                process.waitFor(5, TimeUnit.SECONDS);
                return version != null ? version.trim() : "unknown";
            }

        } catch (Exception e) {
            LOG.debug("無法取得 CLI 版本", e);
            return "unknown";
        }
    }

    /**
     * 執行 CLI 命令
     *
     * @param command 完整的命令列表
     * @return CLI 輸出內容
     * @throws CliExecutionException 當執行失敗時拋出
     */
    protected String executeCommand(List<String> command) throws CliExecutionException {
        LOG.debug("執行 CLI 命令: {}", String.join(" ", command));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            // 讀取輸出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 等待命令完成
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!completed) {
                process.destroy();
                throw new CliExecutionException("CLI 命令超時 (" + timeoutSeconds + "s)");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new CliExecutionException("CLI 命令失敗 (exit code: " + exitCode + ")\n" + output);
            }

            return output.toString();

        } catch (IOException e) {
            throw new CliExecutionException("CLI 執行 IO 錯誤", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CliExecutionException("CLI 執行被中斷", e);
        }
    }

    /**
     * 建構分析提示詞
     *
     * @param codeSnippet 程式碼片段
     * @return 完整的分析提示詞
     */
    protected String buildPrompt(String codeSnippet) {
        return String.format(
                "Analyze the following code for OWASP security vulnerabilities. " +
                        "Provide a detailed analysis including:\n" +
                        "1. Vulnerability type (OWASP category)\n" +
                        "2. CWE ID\n" +
                        "3. Severity level\n" +
                        "4. Detailed description\n" +
                        "5. Fix recommendations with code examples\n\n" +
                        "Code to analyze:\n```\n%s\n```",
                codeSnippet
        );
    }

    /**
     * CLI 執行異常
     */
    public static class CliExecutionException extends Exception {
        public CliExecutionException(String message) {
            super(message);
        }

        public CliExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * CLI 執行結果
     */
    public static class CliResult {
        private final String output;
        private final int exitCode;
        private final long executionTimeMs;

        public CliResult(String output, int exitCode, long executionTimeMs) {
            this.output = output;
            this.exitCode = exitCode;
            this.executionTimeMs = executionTimeMs;
        }

        public String getOutput() {
            return output;
        }

        public int getExitCode() {
            return exitCode;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}
