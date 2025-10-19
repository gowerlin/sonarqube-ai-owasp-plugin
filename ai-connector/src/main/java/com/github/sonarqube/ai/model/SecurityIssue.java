package com.github.sonarqube.ai.model;

import java.util.Objects;

/**
 * 安全問題資料模型
 *
 * 表示 AI 分析發現的單一安全問題，包含 OWASP 分類、CWE ID、嚴重性等資訊。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class SecurityIssue {

    /**
     * OWASP 分類（如 "A01:2021-Broken Access Control"）
     */
    private String owaspCategory;

    /**
     * CWE ID（如 "CWE-284"）
     */
    private String cweId;

    /**
     * 嚴重性：HIGH, MEDIUM, LOW
     */
    private Severity severity;

    /**
     * 問題描述
     */
    private String description;

    /**
     * 問題所在行號
     */
    private Integer lineNumber;

    /**
     * 修復建議
     */
    private String fixSuggestion;

    /**
     * 修復前後代碼範例
     */
    private CodeExample codeExample;

    /**
     * 修復工時預估
     */
    private String effortEstimate;

    /**
     * 嚴重性枚舉
     */
    public enum Severity {
        HIGH,
        MEDIUM,
        LOW;

        public static Severity fromString(String value) {
            if (value == null) {
                return LOW;
            }
            try {
                return Severity.valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return LOW;
            }
        }
    }

    /**
     * 代碼範例（修復前後對比）
     */
    public static class CodeExample {
        private String before;
        private String after;

        public CodeExample() {
        }

        public CodeExample(String before, String after) {
            this.before = before;
            this.after = after;
        }

        public String getBefore() {
            return before;
        }

        public void setBefore(String before) {
            this.before = before;
        }

        public String getAfter() {
            return after;
        }

        public void setAfter(String after) {
            this.after = after;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CodeExample that = (CodeExample) o;
            return Objects.equals(before, that.before) && Objects.equals(after, that.after);
        }

        @Override
        public int hashCode() {
            return Objects.hash(before, after);
        }
    }

    // Getters and Setters

    public String getOwaspCategory() {
        return owaspCategory;
    }

    public void setOwaspCategory(String owaspCategory) {
        this.owaspCategory = owaspCategory;
    }

    public String getCweId() {
        return cweId;
    }

    public void setCweId(String cweId) {
        this.cweId = cweId;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getFixSuggestion() {
        return fixSuggestion;
    }

    public void setFixSuggestion(String fixSuggestion) {
        this.fixSuggestion = fixSuggestion;
    }

    public CodeExample getCodeExample() {
        return codeExample;
    }

    public void setCodeExample(CodeExample codeExample) {
        this.codeExample = codeExample;
    }

    public String getEffortEstimate() {
        return effortEstimate;
    }

    public void setEffortEstimate(String effortEstimate) {
        this.effortEstimate = effortEstimate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecurityIssue that = (SecurityIssue) o;
        return Objects.equals(owaspCategory, that.owaspCategory) &&
            Objects.equals(cweId, that.cweId) &&
            severity == that.severity &&
            Objects.equals(description, that.description) &&
            Objects.equals(lineNumber, that.lineNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owaspCategory, cweId, severity, description, lineNumber);
    }

    @Override
    public String toString() {
        return "SecurityIssue{" +
            "owaspCategory='" + owaspCategory + '\'' +
            ", cweId='" + cweId + '\'' +
            ", severity=" + severity +
            ", description='" + description + '\'' +
            ", lineNumber=" + lineNumber +
            ", fixSuggestion='" + fixSuggestion + '\'' +
            ", effortEstimate='" + effortEstimate + '\'' +
            '}';
    }
}
