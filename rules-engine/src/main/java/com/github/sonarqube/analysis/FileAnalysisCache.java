package com.github.sonarqube.analysis;

import com.github.sonarqube.rules.RuleEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 檔案分析快取
 *
 * 基於檔案 SHA-256 hash 的智能快取策略，避免重複 AI 分析。
 * 支援 TTL（Time-To-Live）過期機制和快取統計。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.4.0 (Epic 6, Story 6.5)
 */
public class FileAnalysisCache {

    private static final Logger logger = LoggerFactory.getLogger(FileAnalysisCache.class);

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final long ttlMillis;
    private final int maxCacheSize;

    // 快取統計
    private long cacheHits = 0;
    private long cacheMisses = 0;
    private long cacheEvictions = 0;

    /**
     * 建構子
     *
     * @param ttlMillis 快取 TTL（毫秒，預設 1 小時）
     * @param maxCacheSize 最大快取項目數（預設 1000）
     */
    public FileAnalysisCache(long ttlMillis, int maxCacheSize) {
        this.ttlMillis = ttlMillis;
        this.maxCacheSize = maxCacheSize;
    }

    /**
     * 建構子（使用預設參數）
     */
    public FileAnalysisCache() {
        this(3600000L, 1000); // 1 hour, 1000 entries
    }

    /**
     * 取得快取的分析結果
     *
     * @param filePath 檔案路徑
     * @param owaspVersion OWASP 版本（作為快取鍵的一部分）
     * @return 快取的分析結果，若無快取則返回 null
     */
    public RuleEngine.AnalysisResult get(Path filePath, String owaspVersion) {
        try {
            String fileHash = calculateFileHash(filePath);
            String cacheKey = buildCacheKey(fileHash, owaspVersion);

            CacheEntry entry = cache.get(cacheKey);

            if (entry == null) {
                cacheMisses++;
                logger.debug("Cache miss: {} (version={})", filePath, owaspVersion);
                return null;
            }

            // 檢查 TTL 是否過期
            if (isExpired(entry)) {
                cache.remove(cacheKey);
                cacheMisses++;
                logger.debug("Cache expired: {} (age={}ms)", filePath,
                    System.currentTimeMillis() - entry.timestamp);
                return null;
            }

            cacheHits++;
            logger.debug("Cache hit: {} (version={}, age={}ms)", filePath, owaspVersion,
                System.currentTimeMillis() - entry.timestamp);

            return entry.result;

        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error("Failed to check cache for file: {}", filePath, e);
            cacheMisses++;
            return null;
        }
    }

    /**
     * 儲存分析結果至快取
     *
     * @param filePath 檔案路徑
     * @param owaspVersion OWASP 版本
     * @param result 分析結果
     */
    public void put(Path filePath, String owaspVersion, RuleEngine.AnalysisResult result) {
        try {
            // 檢查快取大小限制
            if (cache.size() >= maxCacheSize) {
                evictOldestEntries();
            }

            String fileHash = calculateFileHash(filePath);
            String cacheKey = buildCacheKey(fileHash, owaspVersion);

            CacheEntry entry = new CacheEntry(result, fileHash, System.currentTimeMillis());
            cache.put(cacheKey, entry);

            logger.debug("Cache stored: {} (version={}, violations={}, size={})",
                filePath, owaspVersion, result.getTotalViolations(), cache.size());

        } catch (IOException | NoSuchAlgorithmException e) {
            logger.error("Failed to cache result for file: {}", filePath, e);
        }
    }

    /**
     * 計算檔案 SHA-256 hash
     *
     * @param filePath 檔案路徑
     * @return SHA-256 hash 字串
     * @throws IOException 檔案讀取錯誤
     * @throws NoSuchAlgorithmException SHA-256 演算法不可用
     */
    private String calculateFileHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        byte[] fileBytes = Files.readAllBytes(filePath);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(fileBytes);

        // 轉換為十六進位字串
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * 建立快取鍵
     *
     * @param fileHash 檔案 hash
     * @param owaspVersion OWASP 版本
     * @return 快取鍵
     */
    private String buildCacheKey(String fileHash, String owaspVersion) {
        return fileHash + ":" + owaspVersion;
    }

    /**
     * 檢查快取項目是否過期
     *
     * @param entry 快取項目
     * @return true 若已過期
     */
    private boolean isExpired(CacheEntry entry) {
        long age = System.currentTimeMillis() - entry.timestamp;
        return age > ttlMillis;
    }

    /**
     * 清除最舊的快取項目（LRU 策略）
     */
    private void evictOldestEntries() {
        int toRemove = maxCacheSize / 10; // Remove 10% of entries

        cache.entrySet().stream()
            .sorted((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
            .limit(toRemove)
            .map(Map.Entry::getKey)
            .forEach(key -> {
                cache.remove(key);
                cacheEvictions++;
            });

        logger.info("Cache eviction: removed {} oldest entries (cache size: {})", toRemove, cache.size());
    }

    /**
     * 清除所有快取
     */
    public void clear() {
        int previousSize = cache.size();
        cache.clear();
        logger.info("Cache cleared: {} entries removed", previousSize);
    }

    /**
     * 清除過期的快取項目
     */
    public void clearExpired() {
        int removedCount = 0;

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (isExpired(entry.getValue())) {
                cache.remove(entry.getKey());
                removedCount++;
            }
        }

        logger.info("Expired cache cleared: {} entries removed (remaining: {})",
            removedCount, cache.size());
    }

    /**
     * 取得快取統計
     *
     * @return 快取統計資訊
     */
    public CacheStatistics getStatistics() {
        return new CacheStatistics(
            cache.size(),
            cacheHits,
            cacheMisses,
            cacheEvictions,
            calculateHitRate()
        );
    }

    /**
     * 計算快取命中率
     *
     * @return 命中率（0.0 ~ 1.0）
     */
    private double calculateHitRate() {
        long totalRequests = cacheHits + cacheMisses;
        if (totalRequests == 0) {
            return 0.0;
        }
        return (double) cacheHits / totalRequests;
    }

    /**
     * 快取項目
     */
    private static class CacheEntry {
        private final RuleEngine.AnalysisResult result;
        private final String fileHash;
        private final long timestamp;

        public CacheEntry(RuleEngine.AnalysisResult result, String fileHash, long timestamp) {
            this.result = result;
            this.fileHash = fileHash;
            this.timestamp = timestamp;
        }
    }

    /**
     * 快取統計資訊
     */
    public static class CacheStatistics {
        private final int currentSize;
        private final long hits;
        private final long misses;
        private final long evictions;
        private final double hitRate;

        public CacheStatistics(int currentSize, long hits, long misses, long evictions, double hitRate) {
            this.currentSize = currentSize;
            this.hits = hits;
            this.misses = misses;
            this.evictions = evictions;
            this.hitRate = hitRate;
        }

        public int getCurrentSize() {
            return currentSize;
        }

        public long getHits() {
            return hits;
        }

        public long getMisses() {
            return misses;
        }

        public long getEvictions() {
            return evictions;
        }

        public double getHitRate() {
            return hitRate;
        }

        public long getTotalRequests() {
            return hits + misses;
        }

        @Override
        public String toString() {
            return String.format(
                "CacheStatistics{size=%d, hits=%d, misses=%d, evictions=%d, hitRate=%.2f%%, total=%d}",
                currentSize, hits, misses, evictions, hitRate * 100, getTotalRequests()
            );
        }
    }
}
