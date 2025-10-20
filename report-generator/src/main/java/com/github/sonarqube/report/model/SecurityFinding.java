package com.github.sonarqube.report.model;

import java.util.List;

/**
 * 安全發現（漏洞或安全熱點）
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class SecurityFinding {

    private final String ruleKey;
    private final String ruleName;
    private final String owaspCategory;
    private final List<String> cweIds;
    private final String severity;
    private final String filePath;
    private final Integer lineNumber;
    private final String description;
    private final String fixSuggestion;
    private final String codeSnippet;

    private SecurityFinding(Builder builder) {
        this.ruleKey = builder.ruleKey;
        this.ruleName = builder.ruleName;
        this.owaspCategory = builder.owaspCategory;
        this.cweIds = builder.cweIds != null ? List.copyOf(builder.cweIds) : List.of();
        this.severity = builder.severity;
        this.filePath = builder.filePath;
        this.lineNumber = builder.lineNumber;
        this.description = builder.description;
        this.fixSuggestion = builder.fixSuggestion;
        this.codeSnippet = builder.codeSnippet;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String ruleKey;
        private String ruleName;
        private String owaspCategory;
        private List<String> cweIds;
        private String severity;
        private String filePath;
        private Integer lineNumber;
        private String description;
        private String fixSuggestion;
        private String codeSnippet;

        public Builder ruleKey(String ruleKey) {
            this.ruleKey = ruleKey;
            return this;
        }

        public Builder ruleName(String ruleName) {
            this.ruleName = ruleName;
            return this;
        }

        public Builder owaspCategory(String owaspCategory) {
            this.owaspCategory = owaspCategory;
            return this;
        }

        public Builder cweIds(List<String> cweIds) {
            this.cweIds = cweIds;
            return this;
        }

        public Builder severity(String severity) {
            this.severity = severity;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder lineNumber(Integer lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder fixSuggestion(String fixSuggestion) {
            this.fixSuggestion = fixSuggestion;
            return this;
        }

        public Builder codeSnippet(String codeSnippet) {
            this.codeSnippet = codeSnippet;
            return this;
        }

        public SecurityFinding build() {
            return new SecurityFinding(this);
        }
    }

    // Getters
    public String getRuleKey() { return ruleKey; }
    public String getRuleName() { return ruleName; }
    public String getOwaspCategory() { return owaspCategory; }
    public List<String> getCweIds() { return cweIds; }
    public String getSeverity() { return severity; }
    public String getFilePath() { return filePath; }
    public Integer getLineNumber() { return lineNumber; }
    public String getDescription() { return description; }
    public String getFixSuggestion() { return fixSuggestion; }
    public String getCodeSnippet() { return codeSnippet; }

    // Convenience method for backward compatibility
    public String getCweId() {
        return cweIds != null && !cweIds.isEmpty() ? cweIds.get(0) : "";
    }
}
