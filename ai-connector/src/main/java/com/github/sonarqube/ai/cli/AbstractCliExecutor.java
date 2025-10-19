package com.github.sonarqube.ai.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * CLI 執行器抽象基類
 *
 * 提供 CLI 工具執行的通用實作，子類別只需實作特定的命令建構邏輯。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9, Story 9.3)
 */
public abstract class AbstractCliExecutor implements CliExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractCliExecutor.class);

    protected final String cliPath;
    protected int timeoutSeconds;

    /**
     * 建構 CLI 執行器
     *
     * @param cliPath        CLI 工具的檔案系統路徑
     * @param timeoutSeconds 執行超時時間（秒）
     */
    protected AbstractCliExecutor(String cliPath, int timeoutSeconds) {
        if (cliPath == null || cliPath.trim().isEmpty()) {
            throw new IllegalArgumentException("CLI path cannot be null or empty");
        }
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Timeout must be positive: " + timeoutSeconds);
        }

        this.cliPath = cliPath.trim();
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * 建構 CLI 執行器（使用預設 60 秒超時）
     */
    protected AbstractCliExecutor(String cliPath) {
        this(cliPath, 60);
    }

    @Override
    public String executeCommand(String[] command, String input) throws IOException, InterruptedException, CliExecutionException {
        if (command == null || command.length == 0) {
            throw new IllegalArgumentException("Command cannot be null or empty");
        }

        LOG.debug("Executing CLI command: {}", String.join(" ", command));

        // 建立 ProcessBuilder
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false); // 分開處理 stdout 和 stderr

        // 啟動程序
        Process process = pb.start();

        try {
            // 寫入標準輸入（如果有）
            if (input != null && !input.isEmpty()) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(input.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            }

            // 讀取標準輸出和標準錯誤
            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());

            // 等待程序完成（帶超時）
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!completed) {
                process.destroy();
                process.waitFor(5, TimeUnit.SECONDS); // 等待 5 秒讓程序優雅關閉
                if (process.isAlive()) {
                    process.destroyForcibly(); // 強制終止
                }
                throw new CliExecutionException(
                    "CLI execution timeout after " + timeoutSeconds + " seconds",
                    -1,
                    String.join(" ", command)
                );
            }

            int exitCode = process.exitValue();

            LOG.debug("CLI command finished with exit code: {}", exitCode);

            // 檢查退出碼
            if (exitCode != 0) {
                throw new CliExecutionException(
                    "CLI command failed with non-zero exit code",
                    exitCode,
                    String.join(" ", command),
                    stdout,
                    stderr
                );
            }

            return stdout;

        } finally {
            // 確保程序被終止
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    @Override
    public boolean isCliAvailable() {
        try {
            Path path = Paths.get(cliPath);

            // 檢查檔案是否存在
            if (!Files.exists(path)) {
                LOG.warn("CLI tool not found at path: {}", cliPath);
                return false;
            }

            // 檢查是否可執行
            if (!Files.isExecutable(path)) {
                LOG.warn("CLI tool is not executable: {}", cliPath);
                return false;
            }

            // 嘗試執行版本命令
            try {
                getCliVersion();
                return true;
            } catch (Exception e) {
                LOG.warn("Failed to execute CLI tool: {}", e.getMessage());
                return false;
            }

        } catch (Exception e) {
            LOG.error("Error checking CLI availability", e);
            return false;
        }
    }

    @Override
    public String getCliVersion() throws IOException, InterruptedException {
        String[] versionCommand = buildVersionCommand();
        if (versionCommand == null || versionCommand.length == 0) {
            throw new UnsupportedOperationException("Version command not supported for this CLI tool");
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(versionCommand);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = readStream(process.getInputStream());

            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new IOException("Version command timeout");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException("Version command failed with exit code: " + exitCode);
            }

            return output.trim();

        } catch (IOException | InterruptedException e) {
            LOG.error("Failed to get CLI version", e);
            throw e;
        }
    }

    @Override
    public String getCliPath() {
        return cliPath;
    }

    @Override
    public void setTimeoutSeconds(int timeoutSeconds) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("Timeout must be positive: " + timeoutSeconds);
        }
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * 讀取輸入流內容
     *
     * @param inputStream 輸入流
     * @return 輸入流的文字內容
     * @throws IOException 當讀取失敗時拋出
     */
    protected String readStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        return output.toString();
    }

    /**
     * 建構版本查詢命令
     *
     * 子類別可覆寫此方法以提供特定的版本命令（例如 --version, -v 等）
     *
     * @return 版本命令陣列
     */
    protected String[] buildVersionCommand() {
        // 預設使用 --version
        return new String[]{cliPath, "--version"};
    }

    /**
     * 驗證 CLI 輸出格式
     *
     * 子類別可覆寫此方法以實作特定的輸出驗證邏輯
     *
     * @param output CLI 輸出
     * @return true 如果輸出格式有效
     */
    protected boolean validateOutput(String output) {
        return output != null && !output.trim().isEmpty();
    }
}
