package com.github.sonarqube.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 規則執行結果
 *
 * 包含規則檢查的執行結果，包括發現的問題、執行時間、狀態等資訊。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.1)
 */
public class RuleResult {

    private final String ruleId;
    private final boolean success;
    private final List<RuleViolation> violations;
    private final long executionTimeMs;
    private final String errorMessage;

    private RuleResult(Builder builder) {
        this.ruleId = builder.ruleId;
        this.success = builder.success;
        this.violations = Collections.unmodifiableList(new ArrayList<>(builder.violations));
        this.executionTimeMs = builder.executionTimeMs;
        this.errorMessage = builder.errorMessage;
    }

    public String getRuleId() {
        return ruleId;
    }

    public boolean isSuccess() {
        return success;
    }

    public List<RuleViolation> getViolations() {
        return violations;
    }

    public int getViolationCount() {
        return violations.size();
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 是否有發現違規
     *
     * @return true 如果有違規
     */
    public boolean hasViolations() {
        return !violations.isEmpty();
    }

    /**
     * 建立成功結果（無違規）
     *
     * @param ruleId 規則 ID
     * @return RuleResult 實例
     */
    public static RuleResult success(String ruleId) {
        return builder(ruleId)
            .success(true)
            .build();
    }

    /**
     * 建立成功結果（有違規）
     *
     * @param ruleId 規則 ID
     * @param violations 違規列表
     * @return RuleResult 實例
     */
    public static RuleResult success(String ruleId, List<RuleViolation> violations) {
        return builder(ruleId)
            .success(true)
            .violations(violations)
            .build();
    }

    /**
     * 建立失敗結果
     *
     * @param ruleId 規則 ID
     * @param errorMessage 錯誤訊息
     * @return RuleResult 實例
     */
    public static RuleResult failure(String ruleId, String errorMessage) {
        return builder(ruleId)
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }

    /**
     * 建立 Builder
     *
     * @param ruleId 規則 ID
     * @return Builder 實例
     */
    public static Builder builder(String ruleId) {
        return new Builder(ruleId);
    }

    public static class Builder {
        private final String ruleId;
        private boolean success = true;
        private final List<RuleViolation> violations = new ArrayList<>();
        private long executionTimeMs = 0;
        private String errorMessage;

        private Builder(String ruleId) {
            this.ruleId = Objects.requireNonNull(ruleId, "Rule ID cannot be null");
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder violation(RuleViolation violation) {
            if (violation != null) {
                this.violations.add(violation);
            }
            return this;
        }

        public Builder violations(List<RuleViolation> violations) {
            if (violations != null) {
                this.violations.addAll(violations);
            }
            return this;
        }

        public Builder executionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public RuleResult build() {
            return new RuleResult(this);
        }
    }

    @Override
    public String toString() {
        return "RuleResult{" +
            "ruleId='" + ruleId + '\'' +
            ", success=" + success +
            ", violations=" + violations.size() +
            ", executionTimeMs=" + executionTimeMs +
            (errorMessage != null ? ", error='" + errorMessage + '\'' : "") +
            '}';
    }

    /**
     * 規則違規
     *
     * 表示單一違規項目，包含位置、訊息、嚴重性等資訊。
     */
    public static class RuleViolation {
        private final int lineNumber;
        private final String message;
        private final RuleDefinition.RuleSeverity severity;
        private final String codeSnippet;
        private final String fixSuggestion;

        private RuleViolation(ViolationBuilder builder) {
            this.lineNumber = builder.lineNumber;
            this.message = builder.message;
            this.severity = builder.severity;
            this.codeSnippet = builder.codeSnippet;
            this.fixSuggestion = builder.fixSuggestion;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getMessage() {
            return message;
        }

        public RuleDefinition.RuleSeverity getSeverity() {
            return severity;
        }

        public String getCodeSnippet() {
            return codeSnippet;
        }

        public String getFixSuggestion() {
            return fixSuggestion;
        }

        public static ViolationBuilder builder() {
            return new ViolationBuilder();
        }

        public static class ViolationBuilder {
            private int lineNumber = -1;
            private String message;
            private RuleDefinition.RuleSeverity severity = RuleDefinition.RuleSeverity.MAJOR;
            private String codeSnippet;
            private String fixSuggestion;

            public ViolationBuilder lineNumber(int lineNumber) {
                this.lineNumber = lineNumber;
                return this;
            }

            public ViolationBuilder message(String message) {
                this.message = message;
                return this;
            }

            public ViolationBuilder severity(RuleDefinition.RuleSeverity severity) {
                this.severity = severity;
                return this;
            }

            public ViolationBuilder codeSnippet(String codeSnippet) {
                this.codeSnippet = codeSnippet;
                return this;
            }

            public ViolationBuilder fixSuggestion(String fixSuggestion) {
                this.fixSuggestion = fixSuggestion;
                return this;
            }

            public RuleViolation build() {
                Objects.requireNonNull(message, "Violation message cannot be null");
                return new RuleViolation(this);
            }
        }

        @Override
        public String toString() {
            return "Violation{" +
                "line=" + lineNumber +
                ", severity=" + severity +
                ", message='" + message + '\'' +
                '}';
        }
    }
}
