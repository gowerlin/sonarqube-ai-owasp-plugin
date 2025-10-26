package com.github.sonarqube.ai.ratelimit;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Token Bucket Rate Limiter
 *
 * 實作 Token Bucket 算法，用於控制 API 呼叫速率（TPM - Tokens Per Minute）。
 *
 * 特性：
 * - 允許突發請求（在限制內）
 * - 長期平均速率符合限制
 * - 執行緒安全
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class TokenBucketRateLimiter {

    private final double maxTokens; // Bucket 最大容量（TPM 限制 * buffer ratio）
    private final double refillRate; // 每毫秒補充的 token 數量
    private final Lock lock = new ReentrantLock();

    private double availableTokens; // 目前可用的 token 數量
    private long lastRefillTimestamp; // 上次補充 token 的時間戳記（毫秒）

    /**
     * 建構子
     *
     * @param tokensPerMinute 每分鐘允許的最大 token 數量（例如 OpenAI 的 30000 TPM）
     * @param bufferRatio 緩衝比例（例如 0.9 表示使用 90% 的限制，保留 10% 緩衝）
     */
    public TokenBucketRateLimiter(int tokensPerMinute, double bufferRatio) {
        if (tokensPerMinute <= 0) {
            throw new IllegalArgumentException("tokensPerMinute must be positive");
        }
        if (bufferRatio <= 0 || bufferRatio > 1.0) {
            throw new IllegalArgumentException("bufferRatio must be between 0 and 1");
        }

        this.maxTokens = tokensPerMinute * bufferRatio;
        this.refillRate = this.maxTokens / (60.0 * 1000.0); // 每毫秒補充的 token 數量
        this.availableTokens = this.maxTokens; // 初始時 bucket 是滿的
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

    /**
     * 嘗試獲取指定數量的 token
     *
     * @param tokens 需要的 token 數量（通常是 API 請求預計使用的 token 數）
     * @return 成功獲取時返回 true，否則返回 false
     */
    public boolean tryAcquire(int tokens) {
        lock.lock();
        try {
            refill();

            if (availableTokens >= tokens) {
                availableTokens -= tokens;
                return true;
            }

            return false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 阻塞式獲取 token，等待直到有足夠的 token 可用
     *
     * @param tokens 需要的 token 數量
     * @throws InterruptedException 如果執行緒被中斷
     */
    public void acquire(int tokens) throws InterruptedException {
        lock.lock();
        try {
            while (true) {
                refill();

                if (availableTokens >= tokens) {
                    availableTokens -= tokens;
                    return;
                }

                // 計算需要等待多久才能有足夠的 token
                double tokensNeeded = tokens - availableTokens;
                long waitTimeMs = (long) Math.ceil(tokensNeeded / refillRate);

                // 釋放鎖，等待一段時間後重試
                lock.unlock();
                try {
                    Thread.sleep(waitTimeMs);
                } finally {
                    lock.lock();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 獲取當前建議的等待時間（毫秒）
     *
     * 用於 adaptive 策略：當 Rate Limit 錯誤發生時，動態調整等待時間。
     *
     * @param tokensRequested 請求的 token 數量
     * @return 建議等待的毫秒數
     */
    public long getRecommendedWaitTime(int tokensRequested) {
        lock.lock();
        try {
            refill();

            if (availableTokens >= tokensRequested) {
                return 0; // 無需等待
            }

            double tokensNeeded = tokensRequested - availableTokens;
            return (long) Math.ceil(tokensNeeded / refillRate);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 重新填充 token（基於時間流逝）
     *
     * 此方法應在每次獲取 token 前呼叫，以確保 token 數量反映當前時間。
     */
    private void refill() {
        long now = System.currentTimeMillis();
        long timeSinceLastRefill = now - lastRefillTimestamp;

        if (timeSinceLastRefill > 0) {
            double tokensToAdd = timeSinceLastRefill * refillRate;
            availableTokens = Math.min(availableTokens + tokensToAdd, maxTokens);
            lastRefillTimestamp = now;
        }
    }

    /**
     * 取得目前可用的 token 數量（用於測試和監控）
     *
     * @return 可用 token 數量
     */
    public double getAvailableTokens() {
        lock.lock();
        try {
            refill();
            return availableTokens;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 重置 Rate Limiter（將 bucket 填滿）
     */
    public void reset() {
        lock.lock();
        try {
            this.availableTokens = this.maxTokens;
            this.lastRefillTimestamp = System.currentTimeMillis();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 取得 Rate Limiter 配置資訊
     */
    @Override
    public String toString() {
        return String.format("TokenBucketRateLimiter[maxTokens=%.2f, refillRate=%.4f tokens/ms, available=%.2f]",
            maxTokens, refillRate, getAvailableTokens());
    }
}
