package com.github.sonarqube.ai.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 基於 Process 的 CLI 執行器實作
 *
 * 通用的 CLI 執行器，適用於大多數命令列工具。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9, Story 9.3)
 */
public class ProcessCliExecutor extends AbstractCliExecutor {

    private final List<String> defaultArgs;
    private final String[] versionArgs;

    /**
     * 建構 ProcessCliExecutor
     *
     * @param cliPath        CLI 工具路徑
     * @param timeoutSeconds 超時時間（秒）
     * @param defaultArgs    預設命令列參數
     * @param versionArgs    版本查詢參數（例如 "--version" 或 "-v"）
     */
    public ProcessCliExecutor(String cliPath, int timeoutSeconds, List<String> defaultArgs, String... versionArgs) {
        super(cliPath, timeoutSeconds);
        this.defaultArgs = defaultArgs != null ? new ArrayList<>(defaultArgs) : new ArrayList<>();
        this.versionArgs = versionArgs != null && versionArgs.length > 0
            ? versionArgs
            : new String[]{"--version"};
    }

    /**
     * 建構 ProcessCliExecutor（使用預設超時 60 秒）
     */
    public ProcessCliExecutor(String cliPath, List<String> defaultArgs, String... versionArgs) {
        this(cliPath, 60, defaultArgs, versionArgs);
    }

    /**
     * 建構 ProcessCliExecutor（無預設參數）
     */
    public ProcessCliExecutor(String cliPath) {
        this(cliPath, 60, null);
    }

    @Override
    protected String[] buildVersionCommand() {
        List<String> command = new ArrayList<>();
        command.add(cliPath);
        command.addAll(Arrays.asList(versionArgs));
        return command.toArray(new String[0]);
    }

    /**
     * 建構完整的命令陣列（包含預設參數）
     *
     * @param args 使用者提供的參數
     * @return 完整的命令陣列
     */
    public String[] buildCommand(String... args) {
        List<String> command = new ArrayList<>();
        command.add(cliPath);
        command.addAll(defaultArgs);
        if (args != null && args.length > 0) {
            command.addAll(Arrays.asList(args));
        }
        return command.toArray(new String[0]);
    }

    /**
     * 建構完整的命令陣列（包含預設參數）
     *
     * @param args 使用者提供的參數列表
     * @return 完整的命令陣列
     */
    public String[] buildCommand(List<String> args) {
        List<String> command = new ArrayList<>();
        command.add(cliPath);
        command.addAll(defaultArgs);
        if (args != null && !args.isEmpty()) {
            command.addAll(args);
        }
        return command.toArray(new String[0]);
    }

    /**
     * 取得預設參數
     */
    public List<String> getDefaultArgs() {
        return new ArrayList<>(defaultArgs);
    }

    /**
     * 新增預設參數
     */
    public void addDefaultArg(String arg) {
        if (arg != null && !arg.trim().isEmpty()) {
            defaultArgs.add(arg.trim());
        }
    }

    /**
     * 清除所有預設參數
     */
    public void clearDefaultArgs() {
        defaultArgs.clear();
    }

    /**
     * Builder for fluent API
     */
    public static class Builder {
        private String cliPath;
        private int timeoutSeconds = 60;
        private List<String> defaultArgs = new ArrayList<>();
        private String[] versionArgs = new String[]{"--version"};

        public Builder cliPath(String cliPath) {
            this.cliPath = cliPath;
            return this;
        }

        public Builder timeout(int seconds) {
            this.timeoutSeconds = seconds;
            return this;
        }

        public Builder addDefaultArg(String arg) {
            this.defaultArgs.add(arg);
            return this;
        }

        public Builder defaultArgs(List<String> args) {
            this.defaultArgs = new ArrayList<>(args);
            return this;
        }

        public Builder versionArgs(String... args) {
            this.versionArgs = args;
            return this;
        }

        public ProcessCliExecutor build() {
            if (cliPath == null || cliPath.trim().isEmpty()) {
                throw new IllegalArgumentException("CLI path is required");
            }
            return new ProcessCliExecutor(cliPath, timeoutSeconds, defaultArgs, versionArgs);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
