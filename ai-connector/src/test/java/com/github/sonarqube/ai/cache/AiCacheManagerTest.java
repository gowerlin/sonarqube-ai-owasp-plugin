package com.github.sonarqube.ai.cache;

import com.github.sonarqube.ai.model.AiRequest;
import com.github.sonarqube.ai.model.AiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AiCacheManager 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
class AiCacheManagerTest {

    private AiCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new AiCacheManager();
    }

    @Test
    void testCacheMissWhenEmpty() {
        AiRequest request = AiRequest.builder("public void test() {}")
            .fileName("Test.java")
            .language("java")
            .owaspVersion("2021")
            .build();

        AiResponse cached = cacheManager.getFromCache(request);
        assertNull(cached, "Cache should be empty initially");
    }

    @Test
    void testCacheHitAfterPut() {
        AiRequest request = AiRequest.builder("public void test() {}")
            .fileName("Test.java")
            .language("java")
            .owaspVersion("2021")
            .build();

        AiResponse response = AiResponse.success()
            .analysisResult("No issues found")
            .processingTimeMs(1000L)
            .tokensUsed(50)
            .modelUsed("gpt-4")
            .timestamp(LocalDateTime.now())
            .build();

        cacheManager.putToCache(request, response);

        AiResponse cached = cacheManager.getFromCache(request);
        assertNotNull(cached, "Cache should return stored response");
        assertEquals(response.getAnalysisResult(), cached.getAnalysisResult());
    }

    @Test
    void testCacheKeyCalculation() {
        AiRequest request1 = AiRequest.builder("public void test() {}")
            .fileName("Test.java")
            .language("java")
            .owaspVersion("2021")
            .build();

        AiRequest request2 = AiRequest.builder("public void test() {}")
            .fileName("Test.java")
            .language("java")
            .owaspVersion("2021")
            .build();

        String key1 = cacheManager.calculateCacheKey(request1);
        String key2 = cacheManager.calculateCacheKey(request2);

        assertNotNull(key1);
        assertNotNull(key2);
        assertEquals(key1, key2, "Same request content should produce same cache key");
    }

    @Test
    void testCacheKeyDifferentForDifferentCode() {
        AiRequest request1 = AiRequest.builder("public void test() {}")
            .fileName("Test.java")
            .build();

        AiRequest request2 = AiRequest.builder("public void test2() {}")
            .fileName("Test.java")
            .build();

        String key1 = cacheManager.calculateCacheKey(request1);
        String key2 = cacheManager.calculateCacheKey(request2);

        assertNotEquals(key1, key2, "Different code should produce different cache keys");
    }

    @Test
    void testCacheKeyDifferentForDifferentOwaspVersion() {
        AiRequest request1 = AiRequest.builder("public void test() {}")
            .fileName("Test.java")
            .owaspVersion("2021")
            .build();

        AiRequest request2 = AiRequest.builder("public void test() {}")
            .fileName("Test.java")
            .owaspVersion("2017")
            .build();

        String key1 = cacheManager.calculateCacheKey(request1);
        String key2 = cacheManager.calculateCacheKey(request2);

        assertNotEquals(key1, key2, "Different OWASP version should produce different cache keys");
    }

    @Test
    void testClearCache() {
        AiRequest request = AiRequest.builder("public void test() {}")
            .fileName("Test.java")
            .build();

        AiResponse response = AiResponse.success()
            .analysisResult("No issues")
            .build();

        cacheManager.putToCache(request, response);
        assertNotNull(cacheManager.getFromCache(request));

        cacheManager.clearCache();
        assertNull(cacheManager.getFromCache(request), "Cache should be empty after clear");
    }

    @Test
    void testCacheStats() {
        AiRequest request = AiRequest.builder("public void test() {}")
            .fileName("Test.java")
            .build();

        AiResponse response = AiResponse.success()
            .analysisResult("No issues")
            .build();

        // Miss
        cacheManager.getFromCache(request);

        // Put
        cacheManager.putToCache(request, response);

        // Hit
        cacheManager.getFromCache(request);
        cacheManager.getFromCache(request);

        AiCacheManager.CacheStats stats = cacheManager.getStats();
        assertEquals(2, stats.getHitCount());
        assertEquals(1, stats.getMissCount());
        assertTrue(stats.getHitRate() > 0.0);
    }

    @Test
    void testDoNotCacheFailedResponses() {
        AiRequest request = AiRequest.builder("public void test() {}")
            .fileName("Test.java")
            .build();

        AiResponse failedResponse = AiResponse.failure("API error")
            .build();

        cacheManager.putToCache(request, failedResponse);

        AiResponse cached = cacheManager.getFromCache(request);
        assertNull(cached, "Failed responses should not be cached");
    }

    @Test
    void testCacheWithNullRequest() {
        assertNull(cacheManager.getFromCache(null));
        assertDoesNotThrow(() -> cacheManager.putToCache(null, null));
        assertNull(cacheManager.calculateCacheKey(null));
    }

    @Test
    void testCacheKeyIsHex() {
        AiRequest request = AiRequest.builder("public void test() {}")
            .fileName("Test.java")
            .build();

        String key = cacheManager.calculateCacheKey(request);
        assertNotNull(key);
        assertTrue(key.matches("^[0-9a-f]+$"), "Cache key should be hex string");
        assertEquals(64, key.length(), "SHA-256 hex should be 64 characters");
    }

    @Test
    void testCustomCacheConfiguration() {
        AiCacheManager customCache = new AiCacheManager(100, 1);
        assertNotNull(customCache);

        AiRequest request = AiRequest.builder("public void test() {}")
            .fileName("Test.java")
            .build();

        AiResponse response = AiResponse.success()
            .analysisResult("No issues")
            .build();

        customCache.putToCache(request, response);
        assertNotNull(customCache.getFromCache(request));
    }

    @Test
    void testCacheStatsToString() {
        AiRequest request = AiRequest.builder("public void test() {}")
            .fileName("Test.java")
            .build();

        AiResponse response = AiResponse.success()
            .analysisResult("No issues")
            .build();

        cacheManager.putToCache(request, response);
        cacheManager.getFromCache(request);

        AiCacheManager.CacheStats stats = cacheManager.getStats();
        String statsString = stats.toString();

        assertNotNull(statsString);
        assertTrue(statsString.contains("hits="));
        assertTrue(statsString.contains("misses="));
        assertTrue(statsString.contains("size="));
        assertTrue(statsString.contains("hitRate="));
    }
}
