package com.github.sonarqube.ai.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.sonarqube.ai.model.AiRequest;
import com.github.sonarqube.ai.model.AiResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;

/**
 * AI 快取管理器
 *
 * 使用 Caffeine Cache 實現智能快取，避免重複的 AI 分析。
 * 基於檔案 hash 和代碼內容進行快取。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class AiCacheManager {

    /**
     * 預設快取大小
     */
    private static final long DEFAULT_MAX_SIZE = 1000L;

    /**
     * 預設 TTL（小時）
     */
    private static final long DEFAULT_TTL_HOURS = 24L;

    private final Cache<String, AiResponse> cache;
    private final MessageDigest messageDigest;

    /**
     * 預設建構子（使用預設配置）
     */
    public AiCacheManager() {
        this(DEFAULT_MAX_SIZE, DEFAULT_TTL_HOURS);
    }

    /**
     * 自訂配置建構子
     *
     * @param maxSize 最大快取條目數
     * @param ttlHours 快取有效期（小時）
     */
    public AiCacheManager(long maxSize, long ttlHours) {
        this.cache = Caffeine.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(ttlHours, TimeUnit.HOURS)
            .recordStats()
            .build();

        try {
            this.messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 從快取中獲取分析結果
     *
     * @param request AI 請求
     * @return 快取的回應，如果不存在則返回 null
     */
    public AiResponse getFromCache(AiRequest request) {
        if (request == null || request.getCodeSnippet() == null) {
            return null;
        }

        String cacheKey = calculateCacheKey(request);
        return cache.getIfPresent(cacheKey);
    }

    /**
     * 將分析結果存入快取
     *
     * @param request AI 請求
     * @param response AI 回應
     */
    public void putToCache(AiRequest request, AiResponse response) {
        if (request == null || request.getCodeSnippet() == null || response == null) {
            return;
        }

        // 只快取成功的回應
        if (!response.isSuccess()) {
            return;
        }

        String cacheKey = calculateCacheKey(request);
        cache.put(cacheKey, response);
    }

    /**
     * 計算請求的快取鍵
     *
     * 基於代碼內容、檔案名稱、語言和 OWASP 版本計算 SHA-256 hash
     *
     * @param request AI 請求
     * @return 快取鍵（SHA-256 hex 字串）
     */
    public String calculateCacheKey(AiRequest request) {
        if (request == null) {
            return null;
        }

        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(request.getCodeSnippet() != null ? request.getCodeSnippet() : "");
        keyBuilder.append("|");
        keyBuilder.append(request.getFileName() != null ? request.getFileName() : "");
        keyBuilder.append("|");
        keyBuilder.append(request.getLanguage() != null ? request.getLanguage() : "");
        keyBuilder.append("|");
        keyBuilder.append(request.getOwaspVersion() != null ? request.getOwaspVersion() : "");

        byte[] keyBytes = keyBuilder.toString().getBytes(StandardCharsets.UTF_8);
        byte[] hashBytes;

        synchronized (messageDigest) {
            messageDigest.reset();
            hashBytes = messageDigest.digest(keyBytes);
        }

        return HexFormat.of().formatHex(hashBytes);
    }

    /**
     * 清除所有快取
     */
    public void clearCache() {
        cache.invalidateAll();
        cache.cleanUp();
    }

    /**
     * 獲取快取統計資訊
     *
     * @return 快取統計（命中率、大小等）
     */
    public CacheStats getStats() {
        com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = cache.stats();

        CacheStats stats = new CacheStats();
        stats.hitCount = caffeineStats.hitCount();
        stats.missCount = caffeineStats.missCount();
        stats.evictionCount = caffeineStats.evictionCount();
        stats.size = cache.estimatedSize();

        return stats;
    }

    /**
     * 快取統計資料類別
     */
    public static class CacheStats {
        private long hitCount = 0;
        private long missCount = 0;
        private long evictionCount = 0;
        private long size = 0;

        public long getHitCount() {
            return hitCount;
        }

        public long getMissCount() {
            return missCount;
        }

        public long getEvictionCount() {
            return evictionCount;
        }

        public long getSize() {
            return size;
        }

        public double getHitRate() {
            long total = hitCount + missCount;
            return total == 0 ? 0.0 : (double) hitCount / total;
        }

        @Override
        public String toString() {
            return String.format("CacheStats[hits=%d, misses=%d, size=%d, hitRate=%.2f%%]",
                hitCount, missCount, size, getHitRate() * 100);
        }
    }
}
