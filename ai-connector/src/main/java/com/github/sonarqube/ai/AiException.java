package com.github.sonarqube.ai;

/**
 * AI 服務相關例外
 *
 * 當 AI 服務調用失敗時拋出，包含詳細的錯誤資訊和原因。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class AiException extends Exception {

    private final ErrorType errorType;
    private final String providerName;

    /**
     * AI 錯誤類型列舉
     */
    public enum ErrorType {
        /** API 金鑰無效或缺失 */
        INVALID_API_KEY,
        /** 網路連接失敗 */
        NETWORK_ERROR,
        /** 請求超時 */
        TIMEOUT,
        /** API 速率限制 */
        RATE_LIMIT_EXCEEDED,
        /** API 回應格式錯誤 */
        INVALID_RESPONSE,
        /** 配置錯誤 */
        CONFIGURATION_ERROR,
        /** 未知錯誤 */
        UNKNOWN_ERROR
    }

    public AiException(String message, ErrorType errorType, String providerName) {
        super(message);
        this.errorType = errorType;
        this.providerName = providerName;
    }

    public AiException(String message, Throwable cause, ErrorType errorType, String providerName) {
        super(message, cause);
        this.errorType = errorType;
        this.providerName = providerName;
    }

    /**
     * 簡化建構子（使用預設錯誤類型和 provider）
     *
     * @param message 錯誤訊息
     */
    public AiException(String message) {
        super(message);
        this.errorType = ErrorType.UNKNOWN_ERROR;
        this.providerName = "Unknown";
    }

    /**
     * 簡化建構子（包含原因）
     *
     * @param message 錯誤訊息
     * @param cause 原始例外
     */
    public AiException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = cause instanceof java.io.IOException ? ErrorType.NETWORK_ERROR :
                        cause instanceof java.util.concurrent.TimeoutException ? ErrorType.TIMEOUT :
                        ErrorType.UNKNOWN_ERROR;
        this.providerName = "Unknown";
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getProviderName() {
        return providerName;
    }

    @Override
    public String toString() {
        return String.format("AiException [provider=%s, type=%s, message=%s]",
            providerName, errorType, getMessage());
    }
}
