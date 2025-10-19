package com.github.sonarqube.rules.owasp;

/**
 * OWASP Top 10 2017 分類枚舉
 *
 * 定義 OWASP 2017 版本的 10 大安全風險分類。
 *
 * 參考文件：https://owasp.org/www-project-top-ten/2017/
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public enum Owasp2017Category {

    /**
     * A1:2017 - Injection
     * 注入攻擊
     *
     * SQL、NoSQL、OS 命令注入等，應用程式將不受信任的資料作為命令或查詢的一部分發送給解釋器。
     */
    A1_INJECTION(
        "A1:2017",
        "Injection",
        "注入攻擊",
        "SQL、NoSQL、OS 命令注入等，應用程式將不受信任的資料作為命令或查詢的一部分發送給解釋器。"
    ),

    /**
     * A2:2017 - Broken Authentication
     * 身份驗證失效
     *
     * 與身份驗證和會話管理相關的應用程式功能實施不正確，允許攻擊者破解密碼、金鑰或會話令牌。
     */
    A2_BROKEN_AUTHENTICATION(
        "A2:2017",
        "Broken Authentication",
        "身份驗證失效",
        "與身份驗證和會話管理相關的應用程式功能實施不正確，允許攻擊者破解密碼、金鑰或會話令牌。"
    ),

    /**
     * A3:2017 - Sensitive Data Exposure
     * 敏感資料洩露
     *
     * 許多 Web 應用程式和 API 無法正確保護敏感資料，如財務、醫療保健和個人身份資訊。
     */
    A3_SENSITIVE_DATA_EXPOSURE(
        "A3:2017",
        "Sensitive Data Exposure",
        "敏感資料洩露",
        "許多 Web 應用程式和 API 無法正確保護敏感資料，如財務、醫療保健和個人身份資訊。"
    ),

    /**
     * A4:2017 - XML External Entities (XXE)
     * XML 外部實體注入
     *
     * 許多較舊的或配置不當的 XML 處理器評估 XML 文件中的外部實體引用。
     */
    A4_XML_EXTERNAL_ENTITIES(
        "A4:2017",
        "XML External Entities (XXE)",
        "XML 外部實體注入",
        "許多較舊的或配置不當的 XML 處理器評估 XML 文件中的外部實體引用。"
    ),

    /**
     * A5:2017 - Broken Access Control
     * 存取控制失效
     *
     * 存取控制強制執行政策的限制，使用者無法在其預期權限之外執行操作。
     */
    A5_BROKEN_ACCESS_CONTROL(
        "A5:2017",
        "Broken Access Control",
        "存取控制失效",
        "存取控制強制執行政策的限制，使用者無法在其預期權限之外執行操作。"
    ),

    /**
     * A6:2017 - Security Misconfiguration
     * 安全性設定錯誤
     *
     * 安全性配置錯誤是最常見的問題，通常是不安全的預設配置、不完整或臨時配置的結果。
     */
    A6_SECURITY_MISCONFIGURATION(
        "A6:2017",
        "Security Misconfiguration",
        "安全性設定錯誤",
        "安全性配置錯誤是最常見的問題，通常是不安全的預設配置、不完整或臨時配置的結果。"
    ),

    /**
     * A7:2017 - Cross-Site Scripting (XSS)
     * 跨站腳本攻擊
     *
     * 當應用程式在新網頁中包含不受信任的資料而沒有適當驗證或跳脫時，就會發生 XSS 漏洞。
     */
    A7_CROSS_SITE_SCRIPTING(
        "A7:2017",
        "Cross-Site Scripting (XSS)",
        "跨站腳本攻擊",
        "當應用程式在新網頁中包含不受信任的資料而沒有適當驗證或跳脫時，就會發生 XSS 漏洞。"
    ),

    /**
     * A8:2017 - Insecure Deserialization
     * 不安全的反序列化
     *
     * 不安全的反序列化通常導致遠端程式碼執行，即使反序列化漏洞不會導致遠端程式碼執行。
     */
    A8_INSECURE_DESERIALIZATION(
        "A8:2017",
        "Insecure Deserialization",
        "不安全的反序列化",
        "不安全的反序列化通常導致遠端程式碼執行，即使反序列化漏洞不會導致遠端程式碼執行。"
    ),

    /**
     * A9:2017 - Using Components with Known Vulnerabilities
     * 使用已知漏洞的元件
     *
     * 元件（如函式庫、框架和其他軟體模組）以與應用程式相同的權限執行。
     */
    A9_USING_COMPONENTS_WITH_KNOWN_VULNERABILITIES(
        "A9:2017",
        "Using Components with Known Vulnerabilities",
        "使用已知漏洞的元件",
        "元件（如函式庫、框架和其他軟體模組）以與應用程式相同的權限執行。"
    ),

    /**
     * A10:2017 - Insufficient Logging & Monitoring
     * 不足的日誌記錄和監控
     *
     * 不足的日誌記錄和監控，加上與事件回應整合不足或無效，允許攻擊者進一步攻擊系統。
     */
    A10_INSUFFICIENT_LOGGING_MONITORING(
        "A10:2017",
        "Insufficient Logging & Monitoring",
        "不足的日誌記錄和監控",
        "不足的日誌記錄和監控，加上與事件回應整合不足或無效，允許攻擊者進一步攻擊系統。"
    );

    private final String categoryId;
    private final String nameEn;
    private final String nameZh;
    private final String description;

    Owasp2017Category(String categoryId, String nameEn, String nameZh, String description) {
        this.categoryId = categoryId;
        this.nameEn = nameEn;
        this.nameZh = nameZh;
        this.description = description;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public String getNameEn() {
        return nameEn;
    }

    public String getNameZh() {
        return nameZh;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 獲取完整名稱（雙語）
     *
     * @return 完整名稱
     */
    public String getFullName() {
        return categoryId + " - " + nameEn + " (" + nameZh + ")";
    }

    /**
     * 根據分類 ID 獲取枚舉
     *
     * @param categoryId 分類 ID（如 "A1:2017" 或 "A1"）
     * @return 對應的枚舉，找不到則返回 null
     */
    public static Owasp2017Category fromCategoryId(String categoryId) {
        if (categoryId == null) {
            return null;
        }

        // 正規化 ID（移除 :2017 後綴）
        String normalizedId = categoryId.split(":")[0];

        for (Owasp2017Category category : values()) {
            if (category.getCategoryId().startsWith(normalizedId)) {
                return category;
            }
        }

        return null;
    }

    /**
     * 檢查是否為 2017 版本已移除的分類
     * （2017 相比 2013 版本沒有移除分類，但 2021 移除了 A4 和 A7）
     *
     * @return true 如果是已移除的分類
     */
    public boolean isRemovedIn2021() {
        return this == A4_XML_EXTERNAL_ENTITIES ||
               this == A7_CROSS_SITE_SCRIPTING;
    }

    /**
     * 獲取對應的 2021 分類（如果有映射）
     *
     * @return 對應的 2021 分類，如果無法直接映射則返回 null
     */
    public Owasp2021Category getEquivalent2021Category() {
        switch (this) {
            case A1_INJECTION:
                return Owasp2021Category.A03_INJECTION;
            case A2_BROKEN_AUTHENTICATION:
                return Owasp2021Category.A07_IDENTIFICATION_AUTHENTICATION_FAILURES;
            case A3_SENSITIVE_DATA_EXPOSURE:
                return Owasp2021Category.A02_CRYPTOGRAPHIC_FAILURES;
            case A4_XML_EXTERNAL_ENTITIES:
                // XXE 在 2021 併入 A05 Security Misconfiguration
                return Owasp2021Category.A05_SECURITY_MISCONFIGURATION;
            case A5_BROKEN_ACCESS_CONTROL:
                return Owasp2021Category.A01_BROKEN_ACCESS_CONTROL;
            case A6_SECURITY_MISCONFIGURATION:
                return Owasp2021Category.A05_SECURITY_MISCONFIGURATION;
            case A7_CROSS_SITE_SCRIPTING:
                // XSS 在 2021 併入 A03 Injection
                return Owasp2021Category.A03_INJECTION;
            case A8_INSECURE_DESERIALIZATION:
                return Owasp2021Category.A08_SOFTWARE_DATA_INTEGRITY_FAILURES;
            case A9_USING_COMPONENTS_WITH_KNOWN_VULNERABILITIES:
                return Owasp2021Category.A06_VULNERABLE_OUTDATED_COMPONENTS;
            case A10_INSUFFICIENT_LOGGING_MONITORING:
                return Owasp2021Category.A09_SECURITY_LOGGING_MONITORING_FAILURES;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
