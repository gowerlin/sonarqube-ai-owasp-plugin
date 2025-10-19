package com.github.sonarqube.rules;

import java.util.List;

/**
 * OWASP 規則介面
 *
 * 定義 OWASP 安全規則的執行契約，所有具體規則必須實作此介面。
 * 規則可以是靜態模式匹配、AI 輔助分析或兩者混合。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.1)
 */
public interface OwaspRule {

    /**
     * 獲取規則唯一識別碼
     *
     * @return 規則 ID（例如：owasp-2021-a01-001）
     */
    String getRuleId();

    /**
     * 獲取規則名稱
     *
     * @return 規則名稱
     */
    String getRuleName();

    /**
     * 獲取 OWASP 類別
     *
     * @return OWASP 類別代碼（例如：A01, A02, ..., A10）
     */
    String getOwaspCategory();

    /**
     * 獲取 OWASP 版本
     *
     * @return OWASP 版本（2017, 2021, 2025）
     */
    String getOwaspVersion();

    /**
     * 獲取對應的 CWE ID 列表
     *
     * @return CWE ID 列表（例如：["CWE-89", "CWE-564"]）
     */
    List<String> getCweIds();

    /**
     * 獲取預設嚴重性
     *
     * @return 規則嚴重性
     */
    RuleDefinition.RuleSeverity getDefaultSeverity();

    /**
     * 獲取規則類型
     *
     * @return 規則類型（VULNERABILITY, SECURITY_HOTSPOT, 等）
     */
    RuleDefinition.RuleType getRuleType();

    /**
     * 獲取規則描述
     *
     * @return 規則詳細描述
     */
    String getDescription();

    /**
     * 獲取規則支援的程式語言
     *
     * @return 程式語言列表（例如：["java", "javascript"]）
     */
    List<String> getSupportedLanguages();

    /**
     * 檢查規則是否適用於給定的上下文
     *
     * 快速過濾機制，避免執行不必要的規則。
     * 例如：檢查程式語言、檔案類型、程式碼特徵等。
     *
     * @param context 規則執行上下文
     * @return true 如果規則適用，false 否則
     */
    boolean matches(RuleContext context);

    /**
     * 執行規則檢查
     *
     * 實際執行規則的核心邏輯，可以使用：
     * - 靜態模式匹配（Regex, AST 分析）
     * - AI 輔助分析（透過 AiService）
     * - 混合方法（先靜態過濾，再 AI 分析）
     *
     * @param context 規則執行上下文（包含程式碼、語言、AI 服務等）
     * @return 規則執行結果（包含發現的問題）
     */
    RuleResult execute(RuleContext context);

    /**
     * 獲取規則定義
     *
     * 返回完整的 RuleDefinition 物件，包含所有元資料。
     *
     * @return RuleDefinition 物件
     */
    RuleDefinition getRuleDefinition();

    /**
     * 規則是否啟用
     *
     * @return true 如果規則已啟用，false 否則
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * 規則是否需要 AI 輔助
     *
     * @return true 如果需要 AI 分析，false 如果純靜態檢查即可
     */
    default boolean requiresAi() {
        return false;
    }
}
