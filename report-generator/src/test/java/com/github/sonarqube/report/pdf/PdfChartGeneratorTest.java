package com.github.sonarqube.report.pdf;

import com.github.sonarqube.report.model.ReportSummary;
import com.itextpdf.layout.element.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * PdfChartGenerator 單元測試
 *
 * <p>測試 PDF 圖表生成功能，包含嚴重性圓餅圖、OWASP 長條圖、快取機制。</p>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.4)
 */
class PdfChartGeneratorTest {

    private PdfChartGenerator chartGenerator;

    @BeforeEach
    void setUp() {
        chartGenerator = new PdfChartGenerator();
    }

    /**
     * 測試：生成嚴重性圓餅圖（混合嚴重性）
     */
    @Test
    void shouldGenerateSeverityPieChartWithMixedSeverities() throws IOException {
        // Given
        ReportSummary summary = ReportSummary.builder()
                .totalFindings(35)
                .blockerCount(5)
                .criticalCount(10)
                .majorCount(15)
                .minorCount(5)
                .infoCount(0)
                .build();

        // When
        Image chart = chartGenerator.generateSeverityPieChart(summary);

        // Then
        assertThat(chart).isNotNull();
        assertThat(chart.getProperty(com.itextpdf.layout.properties.Property.WIDTH))
                .isNotNull();
        assertThat(chart.getProperty(com.itextpdf.layout.properties.Property.HEIGHT))
                .isNotNull();
    }

    /**
     * 測試：生成嚴重性圓餅圖（零發現）
     */
    @Test
    void shouldGenerateSeverityPieChartWithZeroFindings() throws IOException {
        // Given
        ReportSummary summary = ReportSummary.builder()
                .totalFindings(0)
                .blockerCount(0)
                .criticalCount(0)
                .majorCount(0)
                .minorCount(0)
                .infoCount(0)
                .build();

        // When
        Image chart = chartGenerator.generateSeverityPieChart(summary);

        // Then
        assertThat(chart).isNotNull();
    }

    /**
     * 測試：生成嚴重性圓餅圖（單一嚴重性 100%）
     */
    @Test
    void shouldGenerateSeverityPieChartWithSingleSeverity() throws IOException {
        // Given - 僅有 BLOCKER
        ReportSummary summary = ReportSummary.builder()
                .totalFindings(10)
                .blockerCount(10)
                .criticalCount(0)
                .majorCount(0)
                .minorCount(0)
                .infoCount(0)
                .build();

        // When
        Image chart = chartGenerator.generateSeverityPieChart(summary);

        // Then
        assertThat(chart).isNotNull();
    }

    /**
     * 測試：生成 OWASP 分類長條圖
     */
    @Test
    void shouldGenerateOwaspCategoryBarChart() throws IOException {
        // Given
        Map<String, Long> categoryDistribution = new HashMap<>();
        categoryDistribution.put("A01", 10L);
        categoryDistribution.put("A02", 5L);
        categoryDistribution.put("A03", 15L);
        categoryDistribution.put("A04", 8L);
        categoryDistribution.put("A05", 12L);

        // When
        Image chart = chartGenerator.generateOwaspCategoryBarChart(categoryDistribution);

        // Then
        assertThat(chart).isNotNull();
        assertThat(chart.getProperty(com.itextpdf.layout.properties.Property.WIDTH))
                .isNotNull();
        assertThat(chart.getProperty(com.itextpdf.layout.properties.Property.HEIGHT))
                .isNotNull();
    }

    /**
     * 測試：生成 OWASP 分類長條圖（單一分類）
     */
    @Test
    void shouldGenerateOwaspBarChartWithSingleCategory() throws IOException {
        // Given
        Map<String, Long> categoryDistribution = new HashMap<>();
        categoryDistribution.put("A01", 20L);

        // When
        Image chart = chartGenerator.generateOwaspCategoryBarChart(categoryDistribution);

        // Then
        assertThat(chart).isNotNull();
    }

    /**
     * 測試：生成 OWASP 分類長條圖（多於 10 個分類）
     */
    @Test
    void shouldGenerateOwaspBarChartWithMoreThan10Categories() throws IOException {
        // Given - 15 個 OWASP 分類
        Map<String, Long> categoryDistribution = new HashMap<>();
        for (int i = 1; i <= 15; i++) {
            categoryDistribution.put("A" + String.format("%02d", i), (long) (i * 2));
        }

        // When
        Image chart = chartGenerator.generateOwaspCategoryBarChart(categoryDistribution);

        // Then
        assertThat(chart).isNotNull();
    }

    /**
     * 測試：圖表快取機制（相同資料應回傳快取結果）
     */
    @Test
    void shouldCacheChartForSameData() throws IOException {
        // Given
        ReportSummary summary = ReportSummary.builder()
                .totalFindings(25)
                .blockerCount(5)
                .criticalCount(10)
                .majorCount(10)
                .minorCount(0)
                .infoCount(0)
                .build();

        // When - 生成兩次相同圖表
        Image chart1 = chartGenerator.generateSeverityPieChart(summary);
        Image chart2 = chartGenerator.generateSeverityPieChart(summary);

        // Then - 應該是相同的快取物件
        assertThat(chart1).isSameAs(chart2);
    }

    /**
     * 測試：圖表快取機制（不同資料應生成不同圖表）
     */
    @Test
    void shouldGenerateNewChartForDifferentData() throws IOException {
        // Given
        ReportSummary summary1 = ReportSummary.builder()
                .totalFindings(10)
                .blockerCount(5)
                .criticalCount(5)
                .majorCount(0)
                .minorCount(0)
                .infoCount(0)
                .build();

        ReportSummary summary2 = ReportSummary.builder()
                .totalFindings(20)
                .blockerCount(10)
                .criticalCount(10)
                .majorCount(0)
                .minorCount(0)
                .infoCount(0)
                .build();

        // When
        Image chart1 = chartGenerator.generateSeverityPieChart(summary1);
        Image chart2 = chartGenerator.generateSeverityPieChart(summary2);

        // Then - 應該是不同的物件
        assertThat(chart1).isNotSameAs(chart2);
    }

    /**
     * 測試：圖表生成效能（< 3 秒）
     */
    @Test
    void shouldGenerateChartWithinPerformanceTarget() throws IOException {
        // Given
        ReportSummary summary = ReportSummary.builder()
                .totalFindings(100)
                .blockerCount(20)
                .criticalCount(30)
                .majorCount(40)
                .minorCount(10)
                .infoCount(0)
                .build();

        Map<String, Long> categoryDistribution = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            categoryDistribution.put("A" + String.format("%02d", i), (long) (i * 10));
        }

        // When
        long startTime = System.currentTimeMillis();

        chartGenerator.generateSeverityPieChart(summary);
        chartGenerator.generateOwaspCategoryBarChart(categoryDistribution);

        long duration = System.currentTimeMillis() - startTime;

        // Then - 應在 3 秒內完成
        assertThat(duration).isLessThan(3000);
    }

    /**
     * 測試：清除快取功能
     */
    @Test
    void shouldClearCacheSuccessfully() throws IOException {
        // Given
        ReportSummary summary = ReportSummary.builder()
                .totalFindings(10)
                .blockerCount(10)
                .criticalCount(0)
                .majorCount(0)
                .minorCount(0)
                .infoCount(0)
                .build();

        // 生成圖表並快取
        Image chart1 = chartGenerator.generateSeverityPieChart(summary);

        // When - 清除快取
        chartGenerator.clearCache();

        // Then - 再次生成應該是新物件
        Image chart2 = chartGenerator.generateSeverityPieChart(summary);
        assertThat(chart1).isNotSameAs(chart2);
    }

    /**
     * 測試：取得快取統計資訊
     */
    @Test
    void shouldReturnCacheStats() throws IOException {
        // Given
        ReportSummary summary = ReportSummary.builder()
                .totalFindings(10)
                .blockerCount(10)
                .criticalCount(0)
                .majorCount(0)
                .minorCount(0)
                .infoCount(0)
                .build();

        // When
        chartGenerator.generateSeverityPieChart(summary);
        chartGenerator.generateSeverityPieChart(summary); // Cache hit

        String stats = chartGenerator.getCacheStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats).isNotEmpty();
    }

    /**
     * 測試：空的 OWASP 分類分布
     */
    @Test
    void shouldHandleEmptyOwaspCategoryDistribution() {
        // Given
        Map<String, Long> categoryDistribution = new HashMap<>();

        // When & Then - 應拋出例外或正常處理
        assertThatCode(() -> chartGenerator.generateOwaspCategoryBarChart(categoryDistribution))
                .doesNotThrowAnyException();
    }
}
