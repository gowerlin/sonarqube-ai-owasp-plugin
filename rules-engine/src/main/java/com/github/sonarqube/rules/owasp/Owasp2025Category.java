package com.github.sonarqube.rules.owasp;

/**
 * OWASP Top 10 2025 分類枚舉（預測版本）
 *
 * 基於 2021 版本的演進趨勢設計，涵蓋新興安全威脅。
 *
 * 注意：此為預測性設計，實際 OWASP 2025 可能有所不同。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public enum Owasp2025Category {

    /**
     * A01:2025 - Broken Access Control
     * 存取控制失效（延續 2021）
     */
    A01_BROKEN_ACCESS_CONTROL(
        "A01:2025",
        "Broken Access Control",
        "存取控制失效",
        "未正確實施限制使用者權限的控制措施，攻擊者可執行超出權限的操作。"
    ),

    /**
     * A02:2025 - Cryptographic Failures
     * 加密失效（延續 2021）
     */
    A02_CRYPTOGRAPHIC_FAILURES(
        "A02:2025",
        "Cryptographic Failures",
        "加密失效",
        "與加密相關的失效，導致敏感資料洩露或被篡改。"
    ),

    /**
     * A03:2025 - Injection
     * 注入攻擊（延續 2021）
     */
    A03_INJECTION(
        "A03:2025",
        "Injection",
        "注入攻擊",
        "應用程式將不受信任的資料作為命令或查詢的一部分發送給解釋器。"
    ),

    /**
     * A04:2025 - Insecure Design
     * 不安全的設計（延續 2021）
     */
    A04_INSECURE_DESIGN(
        "A04:2025",
        "Insecure Design",
        "不安全的設計",
        "缺少或無效的安全控制設計，無法防禦特定類型的攻擊。"
    ),

    /**
     * A05:2025 - Security Misconfiguration
     * 安全性設定錯誤（延續 2021）
     */
    A05_SECURITY_MISCONFIGURATION(
        "A05:2025",
        "Security Misconfiguration",
        "安全性設定錯誤",
        "不安全的預設配置、不完整或臨時的配置、開放的雲端儲存等。"
    ),

    /**
     * A06:2025 - Vulnerable and Outdated Components
     * 易受攻擊和過時的元件（延續 2021）
     */
    A06_VULNERABLE_OUTDATED_COMPONENTS(
        "A06:2025",
        "Vulnerable and Outdated Components",
        "易受攻擊和過時的元件",
        "使用已知漏洞的元件、函式庫或框架版本。"
    ),

    /**
     * A07:2025 - Identification and Authentication Failures
     * 識別和身份驗證失效（延續 2021）
     */
    A07_IDENTIFICATION_AUTHENTICATION_FAILURES(
        "A07:2025",
        "Identification and Authentication Failures",
        "識別和身份驗證失效",
        "與使用者身份確認、認證和會話管理相關的安全漏洞。"
    ),

    /**
     * A08:2025 - Software and Data Integrity Failures
     * 軟體和資料完整性失效（延續 2021）
     */
    A08_SOFTWARE_DATA_INTEGRITY_FAILURES(
        "A08:2025",
        "Software and Data Integrity Failures",
        "軟體和資料完整性失效",
        "與軟體更新、關鍵資料和 CI/CD 管道相關的完整性假設。"
    ),

    /**
     * A09:2025 - Security Logging and Monitoring Failures
     * 安全日誌記錄和監控失效（延續 2021）
     */
    A09_SECURITY_LOGGING_MONITORING_FAILURES(
        "A09:2025",
        "Security Logging and Monitoring Failures",
        "安全日誌記錄和監控失效",
        "不足的日誌記錄、偵測、監控和主動回應機制。"
    ),

    /**
     * A10:2025 - Server-Side Request Forgery & AI Security
     * 伺服器端請求偽造與 AI 安全（2025 新增 AI 安全）
     */
    A10_SSRF_AND_AI_SECURITY(
        "A10:2025",
        "Server-Side Request Forgery & AI Security",
        "伺服器端請求偽造與 AI 安全",
        "SSRF 攻擊與新興的 AI/ML 模型安全威脅，包括提示注入、模型投毒等。"
    );

    private final String categoryId;
    private final String nameEn;
    private final String nameZh;
    private final String description;

    Owasp2025Category(String categoryId, String nameEn, String nameZh, String description) {
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

    public String getFullName() {
        return categoryId + " - " + nameEn + " (" + nameZh + ")";
    }

    public static Owasp2025Category fromCategoryId(String categoryId) {
        if (categoryId == null) {
            return null;
        }

        String normalizedId = categoryId.split(":")[0];

        for (Owasp2025Category category : values()) {
            if (category.getCategoryId().startsWith(normalizedId)) {
                return category;
            }
        }

        return null;
    }

    /**
     * 檢查是否為 2025 新增或顯著修改的分類
     */
    public boolean isNewOrModifiedIn2025() {
        // A10 整合了 SSRF 和新興的 AI 安全威脅
        return this == A10_SSRF_AND_AI_SECURITY;
    }

    /**
     * 獲取對應的 2021 分類（如果有映射）
     */
    public Owasp2021Category getEquivalent2021Category() {
        switch (this) {
            case A01_BROKEN_ACCESS_CONTROL:
                return Owasp2021Category.A01_BROKEN_ACCESS_CONTROL;
            case A02_CRYPTOGRAPHIC_FAILURES:
                return Owasp2021Category.A02_CRYPTOGRAPHIC_FAILURES;
            case A03_INJECTION:
                return Owasp2021Category.A03_INJECTION;
            case A04_INSECURE_DESIGN:
                return Owasp2021Category.A04_INSECURE_DESIGN;
            case A05_SECURITY_MISCONFIGURATION:
                return Owasp2021Category.A05_SECURITY_MISCONFIGURATION;
            case A06_VULNERABLE_OUTDATED_COMPONENTS:
                return Owasp2021Category.A06_VULNERABLE_OUTDATED_COMPONENTS;
            case A07_IDENTIFICATION_AUTHENTICATION_FAILURES:
                return Owasp2021Category.A07_IDENTIFICATION_AUTHENTICATION_FAILURES;
            case A08_SOFTWARE_DATA_INTEGRITY_FAILURES:
                return Owasp2021Category.A08_SOFTWARE_DATA_INTEGRITY_FAILURES;
            case A09_SECURITY_LOGGING_MONITORING_FAILURES:
                return Owasp2021Category.A09_SECURITY_LOGGING_MONITORING_FAILURES;
            case A10_SSRF_AND_AI_SECURITY:
                // 基礎部分對應 SSRF
                return Owasp2021Category.A10_SERVER_SIDE_REQUEST_FORGERY;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return getFullName();
    }
}
