package com.github.sonarqube.report.pdf;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.sonarqube.report.model.ReportSummary;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.layout.element.Image;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.style.Styler;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.awt.Color;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PDF 圖表生成器
 *
 * <p>負責生成 PDF 報表中的視覺化圖表，包含嚴重性分布圓餅圖和 OWASP 分類分布長條圖。</p>
 *
 * <p><strong>核心功能：</strong></p>
 * <ul>
 *   <li>使用 XChart 函式庫生成 PNG 格式圖表圖片</li>
 *   <li>使用 iText Image API 嵌入 PDF</li>
 *   <li>使用 Caffeine Cache 實作圖表快取機制</li>
 *   <li>支援嚴重性顏色編碼（BLOCKER 紅色、CRITICAL 橙色、MAJOR 黃色、MINOR 藍色、INFO 綠色）</li>
 * </ul>
 *
 * <p><strong>使用範例：</strong></p>
 * <pre>{@code
 * PdfChartGenerator chartGenerator = new PdfChartGenerator();
 *
 * // 生成嚴重性圓餅圖
 * Image severityChart = chartGenerator.generateSeverityPieChart(reportSummary);
 * document.add(severityChart);
 *
 * // 生成 OWASP 分類長條圖
 * Map<String, Long> categoryDistribution = ...;
 * Image owaspChart = chartGenerator.generateOwaspCategoryBarChart(categoryDistribution);
 * document.add(owaspChart);
 * }</pre>
 *
 * <p><strong>圖表規格：</strong></p>
 * <ul>
 *   <li><strong>嚴重性圓餅圖</strong>: 400x300px，顯示各嚴重性等級的百分比分布</li>
 *   <li><strong>OWASP 長條圖</strong>: 600x400px，依問題數量降序排列，顯示各 OWASP 類別的問題數量</li>
 * </ul>
 *
 * <p><strong>快取策略：</strong></p>
 * <ul>
 *   <li>最大快取容量：100 個圖表</li>
 *   <li>過期時間：30 分鐘</li>
 *   <li>快取鍵：基於資料內容雜湊值</li>
 * </ul>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.4)
 */
public class PdfChartGenerator {

    private static final Logger LOG = Loggers.get(PdfChartGenerator.class);

    /**
     * 圖表快取 (Caffeine Cache)
     *
     * <p>快取已生成的圖表圖片，避免相同資料重複生成，提升效能。</p>
     */
    private final Cache<String, Image> chartCache;

    /**
     * 建構子
     *
     * <p>初始化 Caffeine Cache，設定最大容量 100 個圖表，過期時間 30 分鐘。</p>
     */
    public PdfChartGenerator() {
        this.chartCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats()
                .build();
        LOG.info("PdfChartGenerator initialized with Caffeine cache (max 100 entries, 30 min expiry)");
    }

    /**
     * 生成嚴重性分布圓餅圖
     *
     * <p>根據報表摘要資料生成圓餅圖，顯示各嚴重性等級的百分比分布。</p>
     *
     * <p><strong>圖表規格：</strong></p>
     * <ul>
     *   <li>尺寸：400x300 像素</li>
     *   <li>顏色編碼：BLOCKER=紅色, CRITICAL=橙色, MAJOR=黃色, MINOR=藍色, INFO=綠色</li>
     *   <li>顯示標籤：顯示嚴重性名稱和數量，例如 "BLOCKER (5)"</li>
     *   <li>顯示百分比：自動計算並顯示各扇形區塊的百分比</li>
     * </ul>
     *
     * <p><strong>快取機制：</strong></p>
     * <ul>
     *   <li>相同資料的圖表僅生成一次</li>
     *   <li>快取鍵格式："severity-pie-{blockerCount}-{criticalCount}-{majorCount}-{minorCount}-{infoCount}"</li>
     * </ul>
     *
     * @param summary 報表摘要資料
     * @return iText Image 物件，可直接嵌入 PDF
     * @throws IOException 圖表生成失敗時拋出
     */
    public Image generateSeverityPieChart(ReportSummary summary) throws IOException {
        String cacheKey = "severity-pie-" + generateSummaryCacheKey(summary);

        Image cachedImage = chartCache.getIfPresent(cacheKey);
        if (cachedImage != null) {
            LOG.debug("Using cached severity pie chart (key: {})", cacheKey);
            return cachedImage;
        }

        LOG.info("Generating severity pie chart for summary: {}", cacheKey);
        long startTime = System.currentTimeMillis();

        PieChart chart = createSeverityPieChart(summary);
        byte[] pngBytes = BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);

        com.itextpdf.io.image.ImageData imageData = ImageDataFactory.create(pngBytes);
        Image image = new Image(imageData).setWidth(400).setHeight(300);

        chartCache.put(cacheKey, image);

        long duration = System.currentTimeMillis() - startTime;
        LOG.info("Severity pie chart generated in {}ms (cached for future use)", duration);

        return image;
    }

    /**
     * 建立嚴重性圓餅圖 (XChart)
     *
     * <p>使用 XChart 函式庫建立圓餅圖，設定圖表樣式、顏色和標籤。</p>
     *
     * @param summary 報表摘要資料
     * @return XChart PieChart 物件
     */
    private PieChart createSeverityPieChart(ReportSummary summary) {
        PieChart chart = new PieChartBuilder()
                .width(400)
                .height(300)
                .title("Severity Distribution")
                .build();

        // 新增資料：僅新增數量 > 0 的嚴重性等級
        if (summary.getBlockerCount() > 0) {
            chart.addSeries("BLOCKER (" + summary.getBlockerCount() + ")",
                    summary.getBlockerCount());
        }
        if (summary.getCriticalCount() > 0) {
            chart.addSeries("CRITICAL (" + summary.getCriticalCount() + ")",
                    summary.getCriticalCount());
        }
        if (summary.getMajorCount() > 0) {
            chart.addSeries("MAJOR (" + summary.getMajorCount() + ")",
                    summary.getMajorCount());
        }
        if (summary.getMinorCount() > 0) {
            chart.addSeries("MINOR (" + summary.getMinorCount() + ")",
                    summary.getMinorCount());
        }
        if (summary.getInfoCount() > 0) {
            chart.addSeries("INFO (" + summary.getInfoCount() + ")",
                    summary.getInfoCount());
        }

        // 套用嚴重性顏色
        chart.getStyler().setSeriesColors(getSeverityColors());
        chart.getStyler().setLegendVisible(true);
        chart.getStyler().setLabelsVisible(true);
        chart.getStyler().setLabelsFontSize(12f);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);

        return chart;
    }

    /**
     * 生成 OWASP 分類分布長條圖
     *
     * <p>根據 OWASP 分類分布資料生成長條圖，顯示各 OWASP 類別的問題數量。</p>
     *
     * <p><strong>圖表規格：</strong></p>
     * <ul>
     *   <li>尺寸：600x400 像素</li>
     *   <li>X 軸：OWASP 類別（如 A01、A02...A10）</li>
     *   <li>Y 軸：問題數量</li>
     *   <li>排序：依問題數量降序排列</li>
     *   <li>顏色：深藍色 (#003F7F)</li>
     *   <li>標籤：每個長條頂部顯示數量</li>
     * </ul>
     *
     * <p><strong>快取機制：</strong></p>
     * <ul>
     *   <li>相同資料的圖表僅生成一次</li>
     *   <li>快取鍵格式："owasp-bar-{categoryDistribution.hashCode()}"</li>
     * </ul>
     *
     * @param categoryDistribution OWASP 分類分布資料（類別代碼 → 問題數量）
     * @return iText Image 物件，可直接嵌入 PDF
     * @throws IOException 圖表生成失敗時拋出
     */
    public Image generateOwaspCategoryBarChart(Map<String, Long> categoryDistribution)
            throws IOException {
        String cacheKey = "owasp-bar-" + categoryDistribution.hashCode();

        Image cachedImage = chartCache.getIfPresent(cacheKey);
        if (cachedImage != null) {
            LOG.debug("Using cached OWASP bar chart (key: {})", cacheKey);
            return cachedImage;
        }

        LOG.info("Generating OWASP category bar chart ({} categories)", categoryDistribution.size());
        long startTime = System.currentTimeMillis();

        CategoryChart chart = createOwaspBarChart(categoryDistribution);
        byte[] pngBytes = BitmapEncoder.getBitmapBytes(chart, BitmapEncoder.BitmapFormat.PNG);

        com.itextpdf.io.image.ImageData imageData = ImageDataFactory.create(pngBytes);
        Image image = new Image(imageData).setWidth(600).setHeight(400);

        chartCache.put(cacheKey, image);

        long duration = System.currentTimeMillis() - startTime;
        LOG.info("OWASP bar chart generated in {}ms (cached for future use)", duration);

        return image;
    }

    /**
     * 建立 OWASP 分類長條圖 (XChart)
     *
     * <p>使用 XChart 函式庫建立長條圖，設定圖表樣式、顏色和標籤。</p>
     *
     * @param categoryDistribution OWASP 分類分布資料
     * @return XChart CategoryChart 物件
     */
    private CategoryChart createOwaspBarChart(Map<String, Long> categoryDistribution) {
        CategoryChart chart = new CategoryChartBuilder()
                .width(600)
                .height(400)
                .title("OWASP Category Distribution")
                .xAxisTitle("OWASP Category")
                .yAxisTitle("Issue Count")
                .build();

        // 依數量降序排列
        List<Map.Entry<String, Long>> sortedEntries = categoryDistribution.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();

        List<String> categories = new ArrayList<>();
        List<Long> counts = new ArrayList<>();

        for (Map.Entry<String, Long> entry : sortedEntries) {
            categories.add(entry.getKey());
            counts.add(entry.getValue());
        }

        chart.addSeries("Issues", categories, counts);

        // 套用深藍色漸層
        chart.getStyler().setSeriesColors(new Color[]{new Color(0, 63, 127)}); // #003F7F
        chart.getStyler().setLegendVisible(false);
        chart.getStyler().setLabelsVisible(true);
        chart.getStyler().setLabelsPosition(0.98);
        chart.getStyler().setChartBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart.getStyler().setPlotGridLinesVisible(true);

        return chart;
    }

    /**
     * 取得嚴重性顏色陣列
     *
     * <p>定義 BLOCKER, CRITICAL, MAJOR, MINOR, INFO 的顏色編碼。</p>
     *
     * @return 顏色陣列（AWT Color）
     */
    private Color[] getSeverityColors() {
        return new Color[]{
            new Color(212, 51, 63),   // BLOCKER - Red (#D4333F)
            new Color(255, 165, 0),   // CRITICAL - Orange (#FFA500)
            new Color(255, 215, 0),   // MAJOR - Yellow (#FFD700)
            new Color(75, 159, 213),  // MINOR - Blue (#4B9FD5)
            new Color(0, 170, 0)      // INFO - Green (#00AA00)
        };
    }

    /**
     * 生成報表摘要快取鍵
     *
     * <p>基於嚴重性計數生成唯一的快取鍵。</p>
     *
     * @param summary 報表摘要資料
     * @return 快取鍵字串
     */
    private String generateSummaryCacheKey(ReportSummary summary) {
        return summary.getBlockerCount() + "-" +
               summary.getCriticalCount() + "-" +
               summary.getMajorCount() + "-" +
               summary.getMinorCount() + "-" +
               summary.getInfoCount();
    }

    /**
     * 清除圖表快取
     *
     * <p>清除所有已快取的圖表圖片。</p>
     */
    public void clearCache() {
        chartCache.invalidateAll();
        LOG.info("Chart cache cleared");
    }

    /**
     * 取得快取統計資訊
     *
     * <p>回傳快取命中率、錯過率等統計資訊。</p>
     *
     * @return 快取統計資訊字串
     */
    public String getCacheStats() {
        return chartCache.stats().toString();
    }
}
