package com.github.sonarqube.version;

/**
 * 版本管理服務
 *
 * 管理 OWASP 版本選擇，提供版本切換和查詢功能。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class VersionManager {

    private static OwaspVersion currentVersion = OwaspVersion.getDefault();

    /**
     * 獲取當前 OWASP 版本
     *
     * @return 當前版本
     */
    public static OwaspVersion getCurrentVersion() {
        return currentVersion;
    }

    /**
     * 設定當前 OWASP 版本
     *
     * @param version 目標版本
     */
    public static void setCurrentVersion(OwaspVersion version) {
        if (version != null) {
            currentVersion = version;
        }
    }

    /**
     * 設定當前 OWASP 版本（字串格式）
     *
     * @param versionString 版本字串（如 "2017", "2021"）
     */
    public static void setCurrentVersion(String versionString) {
        currentVersion = OwaspVersion.fromVersion(versionString);
    }

    /**
     * 重置為預設版本
     */
    public static void resetToDefault() {
        currentVersion = OwaspVersion.getDefault();
    }

    /**
     * 檢查當前版本是否為指定版本
     *
     * @param version 要檢查的版本
     * @return true 如果當前版本符合
     */
    public static boolean isVersion(OwaspVersion version) {
        return currentVersion == version;
    }

    /**
     * 檢查是否為 2017 版本
     *
     * @return true 如果當前為 2017 版本
     */
    public static boolean is2017() {
        return currentVersion == OwaspVersion.OWASP_2017;
    }

    /**
     * 檢查是否為 2021 版本
     *
     * @return true 如果當前為 2021 版本
     */
    public static boolean is2021() {
        return currentVersion == OwaspVersion.OWASP_2021;
    }

    /**
     * 獲取所有支援的版本
     *
     * @return 版本陣列
     */
    public static OwaspVersion[] getSupportedVersions() {
        return OwaspVersion.values();
    }

    /**
     * 獲取當前版本資訊
     *
     * @return 版本資訊字串
     */
    public static String getVersionInfo() {
        return String.format("Current OWASP Version: %s (%s)",
            currentVersion.getDisplayName(),
            currentVersion.getVersion());
    }
}
