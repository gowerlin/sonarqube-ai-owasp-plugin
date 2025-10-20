package com.github.sonarqube.ai.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * CLI 工具掃描結果
 *
 * 代表從 CLI 工具（Gemini CLI、Copilot CLI、Claude CLI）獲得的單個安全發現。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class Finding {

    private final String fileName;
    private final String severity;
    private final String owaspCategory;
    private final List<Integer> cweIds;
    private final String message;
    private final String suggestedFix;
    private final int startLine;
    private final int endLine;
    private final String codeSnippet;
    private final String impact;
    private final Double confidenceScore;

    private Finding(Builder builder) {
        this.fileName = builder.fileName;
        this.severity = builder.severity;
        this.owaspCategory = builder.owaspCategory;
        this.cweIds = builder.cweIds != null ? Collections.unmodifiableList(builder.cweIds) : Collections.emptyList();
        this.message = builder.message;
        this.suggestedFix = builder.suggestedFix;
        this.startLine = builder.startLine;
        this.endLine = builder.endLine;
        this.codeSnippet = builder.codeSnippet;
        this.impact = builder.impact;
        this.confidenceScore = builder.confidenceScore;
    }

    // Getters
    public String getFileName() {
        return fileName;
    }

    public String getSeverity() {
        return severity;
    }

    public String getOwaspCategory() {
        return owaspCategory;
    }

    public List<Integer> getCweIds() {
        return cweIds;
    }

    public String getMessage() {
        return message;
    }

    public String getSuggestedFix() {
        return suggestedFix;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public String getCodeSnippet() {
        return codeSnippet;
    }

    public String getImpact() {
        return impact;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    /**
     * 創建 Builder 實例
     *
     * @return Builder 實例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 轉換為 SecurityIssue
     *
     * @return SecurityIssue 實例
     */
    public SecurityIssue toSecurityIssue() {
        String cweId = null;
        if (cweIds != null && !cweIds.isEmpty()) {
            cweId = "CWE-" + cweIds.get(0);
        }

        SecurityIssue issue = new SecurityIssue();
        issue.setOwaspCategory(owaspCategory != null ? owaspCategory : "Unknown");
        issue.setCweId(cweId);
        issue.setSeverity(SecurityIssue.Severity.fromString(severity != null ? severity : "MEDIUM"));
        issue.setDescription(message != null ? message : "Security issue detected");
        issue.setLineNumber(startLine);
        issue.setFixSuggestion(suggestedFix);

        return issue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Finding finding = (Finding) o;
        return startLine == finding.startLine &&
               endLine == finding.endLine &&
               Objects.equals(fileName, finding.fileName) &&
               Objects.equals(severity, finding.severity) &&
               Objects.equals(owaspCategory, finding.owaspCategory) &&
               Objects.equals(message, finding.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, severity, owaspCategory, message, startLine, endLine);
    }

    @Override
    public String toString() {
        return "Finding{" +
               "fileName='" + fileName + '\'' +
               ", severity='" + severity + '\'' +
               ", owaspCategory='" + owaspCategory + '\'' +
               ", cweIds=" + cweIds +
               ", startLine=" + startLine +
               ", endLine=" + endLine +
               '}';
    }

    /**
     * Finding 建構器
     */
    public static class Builder {
        private String fileName;
        private String severity = "MEDIUM";
        private String owaspCategory = "Unknown";
        private List<Integer> cweIds = new ArrayList<>();
        private String message;
        private String suggestedFix;
        private int startLine = 1;
        private int endLine = 1;
        private String codeSnippet;
        private String impact;
        private Double confidenceScore;

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder severity(String severity) {
            this.severity = severity;
            return this;
        }

        public Builder owaspCategory(String owaspCategory) {
            this.owaspCategory = owaspCategory;
            return this;
        }

        public Builder cweIds(List<Integer> cweIds) {
            this.cweIds = cweIds != null ? new ArrayList<>(cweIds) : new ArrayList<>();
            return this;
        }

        public Builder cweId(int cweId) {
            if (this.cweIds == null) {
                this.cweIds = new ArrayList<>();
            }
            this.cweIds.add(cweId);
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder suggestedFix(String suggestedFix) {
            this.suggestedFix = suggestedFix;
            return this;
        }

        public Builder startLine(int startLine) {
            this.startLine = startLine;
            return this;
        }

        public Builder endLine(int endLine) {
            this.endLine = endLine;
            return this;
        }

        public Builder codeSnippet(String codeSnippet) {
            this.codeSnippet = codeSnippet;
            return this;
        }

        public Builder impact(String impact) {
            this.impact = impact;
            return this;
        }

        public Builder confidenceScore(Double confidenceScore) {
            this.confidenceScore = confidenceScore;
            return this;
        }

        public Finding build() {
            return new Finding(this);
        }
    }
}
