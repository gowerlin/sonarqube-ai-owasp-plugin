package com.github.sonarqube.ai.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * AI 回應封裝
 *
 * 包含 AI 分析結果、修復建議、工作量評估等資訊。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class AiResponse {

    private final boolean success;
    private final String analysisResult;
    private final List<SecurityIssue> issues;
    private final String fixSuggestion;
    private final String effortEstimate;
    private final long processingTimeMs;
    private final int tokensUsed;
    private final String modelUsed;
    private final LocalDateTime timestamp;
    private final String errorMessage;

    private AiResponse(Builder builder) {
        this.success = builder.success;
        this.analysisResult = builder.analysisResult;
        this.issues = builder.issues != null ? Collections.unmodifiableList(builder.issues) : Collections.emptyList();
        this.fixSuggestion = builder.fixSuggestion;
        this.effortEstimate = builder.effortEstimate;
        this.processingTimeMs = builder.processingTimeMs;
        this.tokensUsed = builder.tokensUsed;
        this.modelUsed = builder.modelUsed;
        this.timestamp = builder.timestamp;
        this.errorMessage = builder.errorMessage;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getAnalysisResult() {
        return analysisResult;
    }

    public List<SecurityIssue> getIssues() {
        return issues;
    }

    public String getFixSuggestion() {
        return fixSuggestion;
    }

    public String getEffortEstimate() {
        return effortEstimate;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public int getTokensUsed() {
        return tokensUsed;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 建立成功的回應
     *
     * @return Builder 實例
     */
    public static Builder success() {
        return new Builder(true);
    }

    /**
     * 建立失敗的回應
     *
     * @param errorMessage 錯誤訊息
     * @return Builder 實例
     */
    public static Builder failure(String errorMessage) {
        return new Builder(false).errorMessage(errorMessage);
    }

    /**
     * AI 回應建構器
     */
    public static class Builder {
        private final boolean success;
        private String analysisResult;
        private List<SecurityIssue> issues;
        private String fixSuggestion;
        private String effortEstimate;
        private long processingTimeMs;
        private int tokensUsed;
        private String modelUsed;
        private LocalDateTime timestamp = LocalDateTime.now();
        private String errorMessage;

        public Builder(boolean success) {
            this.success = success;
        }

        public Builder analysisResult(String analysisResult) {
            this.analysisResult = analysisResult;
            return this;
        }

        public Builder issues(List<SecurityIssue> issues) {
            this.issues = issues;
            return this;
        }

        public Builder fixSuggestion(String fixSuggestion) {
            this.fixSuggestion = fixSuggestion;
            return this;
        }

        public Builder effortEstimate(String effortEstimate) {
            this.effortEstimate = effortEstimate;
            return this;
        }

        public Builder processingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }

        public Builder tokensUsed(int tokensUsed) {
            this.tokensUsed = tokensUsed;
            return this;
        }

        public Builder modelUsed(String modelUsed) {
            this.modelUsed = modelUsed;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public AiResponse build() {
            return new AiResponse(this);
        }
    }

    /**
     * 安全問題資料類別
     */
    public static class SecurityIssue {
        private final String owaspCategory;
        private final String cweId;
        private final String severity;
        private final String description;
        private final int lineNumber;

        public SecurityIssue(String owaspCategory, String cweId, String severity,
                           String description, int lineNumber) {
            this.owaspCategory = Objects.requireNonNull(owaspCategory);
            this.cweId = cweId;
            this.severity = Objects.requireNonNull(severity);
            this.description = Objects.requireNonNull(description);
            this.lineNumber = lineNumber;
        }

        public String getOwaspCategory() {
            return owaspCategory;
        }

        public String getCweId() {
            return cweId;
        }

        public String getSeverity() {
            return severity;
        }

        public String getDescription() {
            return description;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        @Override
        public String toString() {
            return String.format("SecurityIssue[owasp=%s, cwe=%s, severity=%s, line=%d]",
                owaspCategory, cweId, severity, lineNumber);
        }
    }

    @Override
    public String toString() {
        return String.format("AiResponse[success=%s, issues=%d, tokens=%d, time=%dms]",
            success, issues.size(), tokensUsed, processingTimeMs);
    }
}
