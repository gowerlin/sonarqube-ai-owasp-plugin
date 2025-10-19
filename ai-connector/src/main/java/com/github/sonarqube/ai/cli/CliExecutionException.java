package com.github.sonarqube.ai.cli;

/**
 * CLI 執行例外
 *
 * 當 CLI 命令執行失敗時拋出此例外。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9)
 */
public class CliExecutionException extends Exception {

    private final int exitCode;
    private final String command;
    private final String stdOutput;
    private final String stdError;

    /**
     * 建構 CLI 執行例外
     *
     * @param message  錯誤訊息
     * @param exitCode CLI 程序退出碼
     * @param command  執行的命令
     */
    public CliExecutionException(String message, int exitCode, String command) {
        this(message, exitCode, command, null, null);
    }

    /**
     * 建構 CLI 執行例外（包含標準輸出與錯誤）
     *
     * @param message   錯誤訊息
     * @param exitCode  CLI 程序退出碼
     * @param command   執行的命令
     * @param stdOutput 標準輸出內容
     * @param stdError  標準錯誤輸出內容
     */
    public CliExecutionException(String message, int exitCode, String command, String stdOutput, String stdError) {
        super(buildDetailedMessage(message, exitCode, command, stdOutput, stdError));
        this.exitCode = exitCode;
        this.command = command;
        this.stdOutput = stdOutput;
        this.stdError = stdError;
    }

    /**
     * 建構 CLI 執行例外（帶根本原因）
     *
     * @param message  錯誤訊息
     * @param cause    根本原因
     * @param exitCode CLI 程序退出碼
     * @param command  執行的命令
     */
    public CliExecutionException(String message, Throwable cause, int exitCode, String command) {
        super(message, cause);
        this.exitCode = exitCode;
        this.command = command;
        this.stdOutput = null;
        this.stdError = null;
    }

    public int getExitCode() {
        return exitCode;
    }

    public String getCommand() {
        return command;
    }

    public String getStdOutput() {
        return stdOutput;
    }

    public String getStdError() {
        return stdError;
    }

    private static String buildDetailedMessage(String message, int exitCode, String command,
                                                String stdOutput, String stdError) {
        StringBuilder sb = new StringBuilder(message);
        sb.append("\n  Command: ").append(command);
        sb.append("\n  Exit Code: ").append(exitCode);

        if (stdError != null && !stdError.trim().isEmpty()) {
            sb.append("\n  Error Output: ").append(stdError.trim());
        }

        if (stdOutput != null && !stdOutput.trim().isEmpty()) {
            sb.append("\n  Standard Output: ")
              .append(stdOutput.length() > 500
                  ? stdOutput.substring(0, 500) + "..."
                  : stdOutput.trim());
        }

        return sb.toString();
    }
}
