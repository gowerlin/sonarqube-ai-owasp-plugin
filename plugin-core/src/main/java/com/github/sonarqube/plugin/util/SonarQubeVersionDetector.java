package com.github.sonarqube.plugin.util;

import org.sonar.api.SonarRuntime;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.utils.Version;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

/**
 * SonarQube 版本檢測工具
 *
 * 用於檢測 SonarQube API 版本，判斷是否支援特定功能（如 Issue Attributes）。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0 (Story 10.2)
 */
public class SonarQubeVersionDetector {

    private static final Logger LOG = Loggers.get(SonarQubeVersionDetector.class);

    /**
     * Issue Attributes API 最低支援版本 (SonarQube 9.0)
     */
    private static final int ATTRIBUTES_MIN_MAJOR_VERSION = 9;
    private static final int ATTRIBUTES_MIN_MINOR_VERSION = 0;

    /**
     * 快取的版本檢測結果（避免重複查詢）
     */
    private static Boolean supportsAttributesCache = null;

    /**
     * 私有建構函數（工具類別）
     */
    private SonarQubeVersionDetector() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 檢測 SonarQube 是否支援 Issue Attributes API
     *
     * @param context SensorContext
     * @return true 如果支援 Attributes API (SonarQube ≥ 9.0)
     */
    public static boolean supportsAttributes(SensorContext context) {
        if (supportsAttributesCache != null) {
            return supportsAttributesCache;
        }

        try {
            SonarRuntime runtime = context.runtime();
            Version apiVersion = runtime.getApiVersion();

            // Attributes API 從 SonarQube 9.0 開始支援
            boolean supports = apiVersion.isGreaterThanOrEqual(
                    Version.create(ATTRIBUTES_MIN_MAJOR_VERSION, ATTRIBUTES_MIN_MINOR_VERSION)
            );

            LOG.info("SonarQube API 版本: {}, Attributes API 支援: {}",
                    apiVersion.toString(), supports ? "是" : "否");

            supportsAttributesCache = supports;
            return supports;

        } catch (Exception e) {
            LOG.warn("檢測 SonarQube 版本時發生錯誤，預設為不支援 Attributes API", e);
            supportsAttributesCache = false;
            return false;
        }
    }

    /**
     * 重置快取（主要用於測試）
     */
    public static void resetCache() {
        supportsAttributesCache = null;
    }

    /**
     * 取得快取的檢測結果（用於測試驗證）
     *
     * @return 快取的檢測結果，如果尚未檢測則返回 null
     */
    public static Boolean getCachedResult() {
        return supportsAttributesCache;
    }
}
