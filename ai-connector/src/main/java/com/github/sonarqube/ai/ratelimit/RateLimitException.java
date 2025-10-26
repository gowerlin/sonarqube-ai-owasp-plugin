package com.github.sonarqube.ai.ratelimit;

/**
 * Rate Limit 例外
 *
 * 當 API 呼叫超過速率限制時拋出。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class RateLimitException extends Exception {

    private final int tokensRequested;
    private final long retryAfterMs;

    /**
     * 建構子
     *
     * @param message 錯誤訊息
     * @param tokensRequested 請求的 token 數量
     * @param retryAfterMs 建議等待的毫秒數
     */
    public RateLimitException(String message, int tokensRequested, long retryAfterMs) {
        super(message);
        this.tokensRequested = tokensRequested;
        this.retryAfterMs = retryAfterMs;
    }

    /**
     * 建構子（帶原因）
     *
     * @param message 錯誤訊息
     * @param cause 原因
     * @param tokensRequested 請求的 token 數量
     * @param retryAfterMs 建議等待的毫秒數
     */
    public RateLimitException(String message, Throwable cause, int tokensRequested, long retryAfterMs) {
        super(message, cause);
        this.tokensRequested = tokensRequested;
        this.retryAfterMs = retryAfterMs;
    }

    public int getTokensRequested() {
        return tokensRequested;
    }

    public long getRetryAfterMs() {
        return retryAfterMs;
    }

    @Override
    public String toString() {
        return String.format("%s (tokens=%d, retry_after=%dms)",
            getMessage(), tokensRequested, retryAfterMs);
    }
}
