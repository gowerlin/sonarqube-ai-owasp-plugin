package com.github.sonarqube.rules;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * OWASP 規則抽象基類
 *
 * 提供 OWASP 規則的通用實作，使用模板方法模式。
 * 子類別只需實作核心檢查邏輯，基類處理元資料和通用功能。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.1)
 */
public abstract class AbstractOwaspRule implements OwaspRule {

    private final RuleDefinition ruleDefinition;

    /**
     * 建構子
     *
     * @param ruleDefinition 規則定義
     */
    protected AbstractOwaspRule(RuleDefinition ruleDefinition) {
        this.ruleDefinition = Objects.requireNonNull(ruleDefinition, "Rule definition cannot be null");
    }

    @Override
    public String getRuleId() {
        return ruleDefinition.getRuleKey();
    }

    @Override
    public String getRuleName() {
        return ruleDefinition.getName();
    }

    @Override
    public String getOwaspCategory() {
        return ruleDefinition.getOwaspCategory();
    }

    @Override
    public String getOwaspVersion() {
        // 從規則標籤中提取版本，例如 "owasp-2021"
        for (String tag : ruleDefinition.getTags()) {
            if (tag.startsWith("owasp-")) {
                return tag.substring(6); // "owasp-2021" -> "2021"
            }
        }
        return "2021"; // 預設版本
    }

    @Override
    public List<String> getCweIds() {
        return ruleDefinition.getCweIds();
    }

    @Override
    public RuleDefinition.RuleSeverity getDefaultSeverity() {
        return ruleDefinition.getSeverity();
    }

    @Override
    public RuleDefinition.RuleType getRuleType() {
        return ruleDefinition.getType();
    }

    @Override
    public String getDescription() {
        return ruleDefinition.getDescription();
    }

    @Override
    public List<String> getSupportedLanguages() {
        return List.of(ruleDefinition.getLanguage());
    }

    @Override
    public RuleDefinition getRuleDefinition() {
        return ruleDefinition;
    }

    @Override
    public boolean matches(RuleContext context) {
        // 檢查語言是否匹配
        if (!ruleDefinition.getLanguage().equalsIgnoreCase(context.getLanguage())) {
            return false;
        }

        // 檢查 OWASP 版本是否匹配
        String contextVersion = context.getOwaspVersion();
        String ruleVersion = getOwaspVersion();
        if (!ruleVersion.equals(contextVersion)) {
            return false;
        }

        // 檢查是否需要 AI 但沒有 AI 服務
        if (requiresAi() && !context.hasAiService()) {
            return false;
        }

        // 子類別可以覆寫此方法進行更細緻的過濾
        return true;
    }

    @Override
    public RuleResult execute(RuleContext context) {
        long startTime = System.currentTimeMillis();

        try {
            // 模板方法：呼叫子類別實作的核心檢查邏輯
            RuleResult result = doExecute(context);

            // 設定執行時間
            long executionTime = System.currentTimeMillis() - startTime;
            return RuleResult.builder(getRuleId())
                .success(result.isSuccess())
                .violations(result.getViolations())
                .executionTimeMs(executionTime)
                .errorMessage(result.getErrorMessage())
                .build();

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            return RuleResult.builder(getRuleId())
                .success(false)
                .executionTimeMs(executionTime)
                .errorMessage("Rule execution failed: " + e.getMessage())
                .build();
        }
    }

    /**
     * 核心檢查邏輯（由子類別實作）
     *
     * @param context 執行上下文
     * @return 執行結果
     */
    protected abstract RuleResult doExecute(RuleContext context);

    // === 輔助方法 ===

    /**
     * 檢查程式碼是否包含指定模式
     *
     * @param code 程式碼
     * @param pattern 正規表示式模式
     * @return true 如果匹配
     */
    protected boolean containsPattern(String code, Pattern pattern) {
        return pattern.matcher(code).find();
    }

    /**
     * 檢查程式碼是否包含指定字串
     *
     * @param code 程式碼
     * @param keyword 關鍵字
     * @return true 如果包含
     */
    protected boolean containsKeyword(String code, String keyword) {
        return code.contains(keyword);
    }

    /**
     * 尋找所有匹配模式的行號
     *
     * @param code 程式碼
     * @param pattern 正規表示式模式
     * @return 行號列表（從 1 開始）
     */
    protected List<Integer> findMatchingLines(String code, Pattern pattern) {
        String[] lines = code.split("\n");
        List<Integer> matchingLines = new java.util.ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            if (pattern.matcher(lines[i]).find()) {
                matchingLines.add(i + 1); // 行號從 1 開始
            }
        }

        return matchingLines;
    }

    /**
     * 取得指定行號的程式碼片段
     *
     * @param code 程式碼
     * @param lineNumber 行號（從 1 開始）
     * @return 程式碼片段
     */
    protected String getCodeSnippet(String code, int lineNumber) {
        String[] lines = code.split("\n");
        if (lineNumber < 1 || lineNumber > lines.length) {
            return "";
        }
        return lines[lineNumber - 1];
    }

    /**
     * 取得指定行號的上下文（前後各 N 行）
     *
     * @param code 程式碼
     * @param lineNumber 行號（從 1 開始）
     * @param contextLines 上下文行數
     * @return 程式碼上下文
     */
    protected String getCodeContext(String code, int lineNumber, int contextLines) {
        String[] lines = code.split("\n");
        if (lineNumber < 1 || lineNumber > lines.length) {
            return "";
        }

        int startLine = Math.max(1, lineNumber - contextLines);
        int endLine = Math.min(lines.length, lineNumber + contextLines);

        StringBuilder context = new StringBuilder();
        for (int i = startLine - 1; i < endLine; i++) {
            if (i == lineNumber - 1) {
                context.append(">>> "); // 標記問題行
            }
            context.append(lines[i]).append("\n");
        }

        return context.toString();
    }

    /**
     * 建立違規項目
     *
     * @param lineNumber 行號
     * @param message 訊息
     * @param code 程式碼
     * @return 違規項目
     */
    protected RuleResult.RuleViolation createViolation(int lineNumber, String message, String code) {
        return RuleResult.RuleViolation.builder()
            .lineNumber(lineNumber)
            .message(message)
            .severity(getDefaultSeverity())
            .codeSnippet(getCodeSnippet(code, lineNumber))
            .build();
    }

    /**
     * 建立違規項目（帶修復建議）
     *
     * @param lineNumber 行號
     * @param message 訊息
     * @param code 程式碼
     * @param fixSuggestion 修復建議
     * @return 違規項目
     */
    protected RuleResult.RuleViolation createViolation(int lineNumber, String message, String code, String fixSuggestion) {
        return RuleResult.RuleViolation.builder()
            .lineNumber(lineNumber)
            .message(message)
            .severity(getDefaultSeverity())
            .codeSnippet(getCodeSnippet(code, lineNumber))
            .fixSuggestion(fixSuggestion)
            .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractOwaspRule)) return false;
        AbstractOwaspRule that = (AbstractOwaspRule) o;
        return Objects.equals(getRuleId(), that.getRuleId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRuleId());
    }

    @Override
    public String toString() {
        return "OwaspRule{" +
            "id='" + getRuleId() + '\'' +
            ", category='" + getOwaspCategory() + '\'' +
            ", version='" + getOwaspVersion() + '\'' +
            ", language='" + ruleDefinition.getLanguage() + '\'' +
            ", severity=" + getDefaultSeverity() +
            '}';
    }
}
