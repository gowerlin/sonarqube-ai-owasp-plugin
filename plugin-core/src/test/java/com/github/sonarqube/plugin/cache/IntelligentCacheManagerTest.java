package com.github.sonarqube.plugin.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Intelligent Cache Manager 單元測試
 *
 * 測試範圍：
 * - 快取存取（put/get）
 * - 檔案 Hash 計算
 * - 快取失效（檔案修改）
 * - LRU 淘汰策略
 * - 統計資訊
 *
 * @since 3.0.0 (Epic 8, Story 8.1)
 */
@DisplayName("IntelligentCacheManager Unit Tests")
public class IntelligentCacheManagerTest {

    @TempDir
    Path tempDir;

    private IntelligentCacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new IntelligentCacheManager(true, 10);
    }

    @AfterEach
    void tearDown() {
        if (cacheManager != null) {
            cacheManager.clear();
        }
    }

    @Test
    @DisplayName("測試快取存取 - put 和 get")
    void testPutAndGet() throws IOException {
        File testFile = createTempFile("test.java", "public class Test {}");
        String filePath = testFile.getAbsolutePath();

        // 存入快取
        cacheManager.put(filePath, "cached value");

        // 取出快取
        String cachedValue = cacheManager.get(filePath);
        assertEquals("cached value", cachedValue);
    }

    @Test
    @DisplayName("測試快取失效 - 檔案內容修改")
    void testCacheInvalidationOnFileChange() throws IOException, InterruptedException {
        File testFile = createTempFile("test.java", "public class Test {}");
        String filePath = testFile.getAbsolutePath();

        // 初始快取
        cacheManager.put(filePath, "original value");
        assertEquals("original value", cacheManager.get(filePath));

        // 修改檔案內容
        Thread.sleep(100); // 確保時間戳不同
        try (FileWriter writer = new FileWriter(testFile, false)) {
            writer.write("public class Test { /* modified */ }");
        }

        // 快取應該失效
        String cachedValue = cacheManager.get(filePath);
        assertNull(cachedValue, "修改後的檔案應該導致快取失效");
    }

    @Test
    @DisplayName("測試快取失效 - 不存在的檔案")
    void testGetNonExistentFile() {
        String nonExistentPath = tempDir.resolve("nonexistent.java").toString();
        String cachedValue = cacheManager.get(nonExistentPath);
        assertNull(cachedValue, "不存在的檔案應該返回 null");
    }

    @Test
    @DisplayName("測試停用快取功能")
    void testDisabledCache() throws IOException {
        IntelligentCacheManager disabledCache = new IntelligentCacheManager(false, 10);

        File testFile = createTempFile("test.java", "public class Test {}");
        String filePath = testFile.getAbsolutePath();

        // 嘗試存入快取
        disabledCache.put(filePath, "value");

        // 應該無法取出（快取已停用）
        String cachedValue = disabledCache.get(filePath);
        assertNull(cachedValue, "停用快取時應該返回 null");
    }

    @Test
    @DisplayName("測試 LRU 淘汰策略")
    void testLruEviction() throws IOException {
        IntelligentCacheManager smallCache = new IntelligentCacheManager(true, 3);

        // 建立 4 個測試檔案
        File file1 = createTempFile("file1.java", "content1");
        File file2 = createTempFile("file2.java", "content2");
        File file3 = createTempFile("file3.java", "content3");
        File file4 = createTempFile("file4.java", "content4");

        // 存入 3 個項目（達到容量上限）
        smallCache.put(file1.getAbsolutePath(), "value1");
        smallCache.put(file2.getAbsolutePath(), "value2");
        smallCache.put(file3.getAbsolutePath(), "value3");

        // 存入第 4 個項目，應該淘汰最舊的項目（file1）
        smallCache.put(file4.getAbsolutePath(), "value4");

        // file1 應該被淘汰
        assertNull(smallCache.get(file1.getAbsolutePath()), "最舊的項目應該被淘汰");

        // file2, file3, file4 應該還在
        assertNotNull(smallCache.get(file2.getAbsolutePath()));
        assertNotNull(smallCache.get(file3.getAbsolutePath()));
        assertNotNull(smallCache.get(file4.getAbsolutePath()));
    }

    @Test
    @DisplayName("測試 clear() 清空快取")
    void testClear() throws IOException {
        File testFile = createTempFile("test.java", "public class Test {}");
        String filePath = testFile.getAbsolutePath();

        cacheManager.put(filePath, "value");
        assertEquals("value", cacheManager.get(filePath));

        // 清空快取
        cacheManager.clear();

        // 快取應該被清空
        assertNull(cacheManager.get(filePath), "清空後應該無法取得快取");
    }

    @Test
    @DisplayName("測試統計資訊 - 命中率計算")
    void testCacheStatistics() throws IOException {
        File file1 = createTempFile("file1.java", "content1");
        File file2 = createTempFile("file2.java", "content2");

        String path1 = file1.getAbsolutePath();
        String path2 = file2.getAbsolutePath();

        // 存入快取
        cacheManager.put(path1, "value1");

        // 命中 1 次
        cacheManager.get(path1);

        // 未命中 1 次
        cacheManager.get(path2);

        // 命中率應該是 50%
        IntelligentCacheManager.CacheStatistics stats = cacheManager.getStatistics();
        assertEquals(1, stats.cacheHits);
        assertEquals(1, stats.cacheMisses);
        assertEquals(50.0, stats.hitRate, 0.1);
    }

    @Test
    @DisplayName("測試統計資訊 - 節省時間計算")
    void testCacheTimeSavings() throws IOException {
        File testFile = createTempFile("test.java", "public class Test {}");
        String filePath = testFile.getAbsolutePath();

        cacheManager.put(filePath, "value");

        // 命中 5 次
        for (int i = 0; i < 5; i++) {
            cacheManager.get(filePath);
        }

        IntelligentCacheManager.CacheStatistics stats = cacheManager.getStatistics();
        assertEquals(5, stats.cacheHits);

        // 節省時間應該約為 5 * 60秒 = 300秒
        assertTrue(stats.timeSavedSeconds >= 250 && stats.timeSavedSeconds <= 350,
                "節省時間計算不正確: " + stats.timeSavedSeconds);
    }

    @Test
    @DisplayName("測試快取大小限制")
    void testCacheSizeLimit() throws IOException {
        IntelligentCacheManager limitedCache = new IntelligentCacheManager(true, 100);

        // 存入 100 個項目
        for (int i = 0; i < 100; i++) {
            File file = createTempFile("file" + i + ".java", "content" + i);
            limitedCache.put(file.getAbsolutePath(), "value" + i);
        }

        IntelligentCacheManager.CacheStatistics stats = limitedCache.getStatistics();
        assertEquals(100, stats.cacheSize, "快取大小應該達到上限");

        // 存入第 101 個項目
        File file101 = createTempFile("file101.java", "content101");
        limitedCache.put(file101.getAbsolutePath(), "value101");

        stats = limitedCache.getStatistics();
        assertEquals(100, stats.cacheSize, "快取大小應該維持上限");
    }

    @Test
    @DisplayName("測試同一檔案多次存取")
    void testMultipleAccessSameFile() throws IOException {
        File testFile = createTempFile("test.java", "public class Test {}");
        String filePath = testFile.getAbsolutePath();

        // 初始存入
        cacheManager.put(filePath, "value1");

        // 第一次取出（命中）
        assertEquals("value1", cacheManager.get(filePath));

        // 再次存入（覆蓋）
        cacheManager.put(filePath, "value2");

        // 第二次取出（應該是新值）
        assertEquals("value2", cacheManager.get(filePath));
    }

    @Test
    @DisplayName("測試空值處理")
    void testNullValueHandling() throws IOException {
        File testFile = createTempFile("test.java", "public class Test {}");
        String filePath = testFile.getAbsolutePath();

        // 存入 null 值（應該被允許）
        cacheManager.put(filePath, null);

        // 取出應該是 null（但不代表未命中）
        assertNull(cacheManager.get(filePath));
    }

    // === Helper Methods ===

    private File createTempFile(String fileName, String content) throws IOException {
        File file = tempDir.resolve(fileName).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }
}
