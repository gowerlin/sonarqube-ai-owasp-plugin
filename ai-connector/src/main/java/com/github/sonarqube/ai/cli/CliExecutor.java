package com.github.sonarqube.ai.cli;

import java.io.IOException;

/**
 * CLI 執行器介面
 *
 * 定義 CLI 工具執行的統一介面，支援不同的 AI CLI 工具調用。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9)
 */
public interface CliExecutor {

    /**
     * 執行 CLI 命令並獲取輸出
     *
     * @param command CLI 命令陣列（命令與參數）
     * @param input   標準輸入內容（可選，null 表示無輸入）
     * @return CLI 輸出結果
     * @throws IOException           當 CLI 執行失敗時拋出
     * @throws InterruptedException  當執行被中斷時拋出
     * @throws CliExecutionException 當 CLI 返回錯誤時拋出
     */
    String executeCommand(String[] command, String input) throws IOException, InterruptedException, CliExecutionException;

    /**
     * 測試 CLI 工具是否可用
     *
     * @return true 如果 CLI 工具已安裝且可執行，false 否則
     */
    boolean isCliAvailable();

    /**
     * 獲取 CLI 工具版本資訊
     *
     * @return 版本字串，例如 "1.2.3"
     * @throws IOException          當無法獲取版本時拋出
     * @throws InterruptedException 當執行被中斷時拋出
     */
    String getCliVersion() throws IOException, InterruptedException;

    /**
     * 獲取 CLI 工具路徑
     *
     * @return CLI 工具的檔案系統路徑
     */
    String getCliPath();

    /**
     * 設定命令執行超時時間（秒）
     *
     * @param timeoutSeconds 超時時間（秒），預設 60 秒
     */
    void setTimeoutSeconds(int timeoutSeconds);

    /**
     * 獲取當前超時設定
     *
     * @return 超時時間（秒）
     */
    int getTimeoutSeconds();
}
