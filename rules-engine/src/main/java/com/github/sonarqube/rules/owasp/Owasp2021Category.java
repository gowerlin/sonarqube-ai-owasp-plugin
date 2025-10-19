package com.github.sonarqube.rules.owasp;

/**
 * OWASP Top 10 2021 分類枚舉
 *
 * 定義 OWASP 2021 版本的 10 大安全風險類別。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public enum Owasp2021Category {

    /**
     * A01:2021 - Broken Access Control（存取控制失效）
     *
     * 上升到第一位（從 2017 的第 5 位）。
     * 包含 CWE-200, CWE-201, CWE-352 等 34 個 CWE。
     */
    A01_BROKEN_ACCESS_CONTROL(
        "A01:2021",
        "Broken Access Control",
        "存取控制失效",
        "未正確實施限制使用者權限的控制措施，可能導致未授權資訊洩露、修改或銷毀所有資料，或執行超出使用者權限的業務功能。"
    ),

    /**
     * A02:2021 - Cryptographic Failures（加密機制失效）
     *
     * 上升一位（從 2017 的第 3 位，原名 Sensitive Data Exposure）。
     * 包含 CWE-259, CWE-327, CWE-331 等。
     */
    A02_CRYPTOGRAPHIC_FAILURES(
        "A02:2021",
        "Cryptographic Failures",
        "加密機制失效",
        "與加密相關的失效導致敏感資料外洩或系統妥協。包括傳輸中或靜止狀態下的敏感資料未加密、使用舊的或弱的加密演算法。"
    ),

    /**
     * A03:2021 - Injection（注入攻擊）
     *
     * 下降到第 3 位（從 2017 的第 1 位）。
     * 包含 CWE-79 (XSS), CWE-89 (SQL Injection), CWE-73 等。
     */
    A03_INJECTION(
        "A03:2021",
        "Injection",
        "注入攻擊",
        "應用程式將不受信任的資料作為命令或查詢的一部分傳送給解譯器。攻擊者的惡意資料可能欺騙解譯器執行非預期的命令或在未經適當授權的情況下存取資料。"
    ),

    /**
     * A04:2021 - Insecure Design（不安全設計）
     *
     * 2021 新增類別，聚焦於設計缺陷。
     * 包含 CWE-209, CWE-256, CWE-501 等。
     */
    A04_INSECURE_DESIGN(
        "A04:2021",
        "Insecure Design",
        "不安全設計",
        "缺少或無效的安全控制設計。不同於不安全的實現，不安全設計是指在設計階段就缺乏業務風險分析和安全設計模式。"
    ),

    /**
     * A05:2021 - Security Misconfiguration（安全配置錯誤）
     *
     * 從 2017 的第 6 位上升。
     * 包含 CWE-16, CWE-611 (XXE) 等。
     */
    A05_SECURITY_MISCONFIGURATION(
        "A05:2021",
        "Security Misconfiguration",
        "安全配置錯誤",
        "缺少適當的安全強化，或為雲端服務配置了不正確的權限，啟用或安裝了不必要的功能，預設帳戶和密碼仍然啟用且未改變等。"
    ),

    /**
     * A06:2021 - Vulnerable and Outdated Components（易受攻擊與過時的元件）
     *
     * 從 2017 的第 9 位上升。
     * 包含 CWE-1104 等。
     */
    A06_VULNERABLE_OUTDATED_COMPONENTS(
        "A06:2021",
        "Vulnerable and Outdated Components",
        "易受攻擊與過時的元件",
        "使用已知存在弱點的元件（函式庫、框架和其他軟體模組）。易受攻擊的元件可能破壞應用程式防禦並啟用各種攻擊和影響。"
    ),

    /**
     * A07:2021 - Identification and Authentication Failures（識別與認證失效）
     *
     * 從 2017 的第 2 位下降（原名 Broken Authentication）。
     * 包含 CWE-297, CWE-287, CWE-384 等。
     */
    A07_IDENTIFICATION_AUTHENTICATION_FAILURES(
        "A07:2021",
        "Identification and Authentication Failures",
        "識別與認證失效",
        "與使用者識別、認證或工作階段管理相關的應用程式功能實作不正確，允許攻擊者破壞密碼、金鑰或工作階段權杖。"
    ),

    /**
     * A08:2021 - Software and Data Integrity Failures（軟體與資料完整性失效）
     *
     * 2021 新增類別。
     * 包含 CWE-502 (不安全的反序列化), CWE-829 等。
     */
    A08_SOFTWARE_DATA_INTEGRITY_FAILURES(
        "A08:2021",
        "Software and Data Integrity Failures",
        "軟體與資料完整性失效",
        "與軟體更新、關鍵資料和 CI/CD pipeline 相關的程式碼和基礎設施未能防護完整性。例如，應用程式依賴於來自不受信任來源的插件、函式庫或模組。"
    ),

    /**
     * A09:2021 - Security Logging and Monitoring Failures（安全記錄與監控失效）
     *
     * 從 2017 的第 10 位上升（原名 Insufficient Logging & Monitoring）。
     * 包含 CWE-778, CWE-117, CWE-223 等。
     */
    A09_SECURITY_LOGGING_MONITORING_FAILURES(
        "A09:2021",
        "Security Logging and Monitoring Failures",
        "安全記錄與監控失效",
        "缺少、不足或無效的日誌記錄、監控和主動回應。沒有這些，無法偵測、升級和回應主動攻擊。"
    ),

    /**
     * A10:2021 - Server-Side Request Forgery (SSRF)（伺服器端請求偽造）
     *
     * 2021 新增類別。
     * 包含 CWE-918 等。
     */
    A10_SERVER_SIDE_REQUEST_FORGERY(
        "A10:2021",
        "Server-Side Request Forgery",
        "伺服器端請求偽造",
        "當 Web 應用程式在未驗證使用者提供的 URL 的情況下擷取遠端資源時發生 SSRF 缺陷。它允許攻擊者強制應用程式傳送精心設計的請求到意外的目的地。"
    );

    private final String categoryId;
    private final String englishName;
    private final String chineseName;
    private final String description;

    Owasp2021Category(String categoryId, String englishName, String chineseName, String description) {
        this.categoryId = categoryId;
        this.englishName = englishName;
        this.chineseName = chineseName;
        this.description = description;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getEnglishName() {
        return englishName;
    }

    public String getChineseName() {
        return chineseName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 獲取完整分類名稱（含 ID 和英文名）
     */
    public String getFullName() {
        return categoryId + "-" + englishName;
    }

    /**
     * 從類別 ID 查找對應的枚舉值
     *
     * @param categoryId 類別 ID（如 "A01:2021" 或 "A01"）
     * @return 對應的枚舉值，如果找不到則返回 null
     */
    public static Owasp2021Category fromCategoryId(String categoryId) {
        if (categoryId == null) {
            return null;
        }

        // 移除可能的版本後綴
        String normalizedId = categoryId.split(":")[0];

        for (Owasp2021Category category : values()) {
            if (category.getCategoryId().startsWith(normalizedId)) {
                return category;
            }
        }

        return null;
    }

    /**
     * 檢查是否為 2021 新增的類別
     */
    public boolean isNewIn2021() {
        return this == A04_INSECURE_DESIGN ||
               this == A08_SOFTWARE_DATA_INTEGRITY_FAILURES ||
               this == A10_SERVER_SIDE_REQUEST_FORGERY;
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
