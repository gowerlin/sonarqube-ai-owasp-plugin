package com.github.sonarqube.report.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 分析報告模型
 *
 * 包含完整的安全分析結果資訊。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class AnalysisReport {

    private final String projectName;
    private final String owaspVersion;
    private final LocalDateTime analysisTime;
    private final List<SecurityFinding> findings;
    private final ReportSummary summary;
    private final String aiModel;

    private AnalysisReport(Builder builder) {
        this.projectName = builder.projectName;
        this.owaspVersion = builder.owaspVersion;
        this.analysisTime = builder.analysisTime;
        this.findings = Collections.unmodifiableList(new ArrayList<>(builder.findings));
        this.summary = builder.summary;
        this.aiModel = builder.aiModel;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String projectName;
        private String owaspVersion;
        private LocalDateTime analysisTime = LocalDateTime.now();
        private List<SecurityFinding> findings = new ArrayList<>();
        private ReportSummary summary;
        private String aiModel;

        public Builder projectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        public Builder owaspVersion(String owaspVersion) {
            this.owaspVersion = owaspVersion;
            return this;
        }

        public Builder analysisTime(LocalDateTime analysisTime) {
            this.analysisTime = analysisTime;
            return this;
        }

        public Builder findings(List<SecurityFinding> findings) {
            this.findings = findings != null ? new ArrayList<>(findings) : new ArrayList<>();
            return this;
        }

        public Builder addFinding(SecurityFinding finding) {
            if (finding != null) {
                this.findings.add(finding);
            }
            return this;
        }

        public Builder summary(ReportSummary summary) {
            this.summary = summary;
            return this;
        }

        public Builder aiModel(String aiModel) {
            this.aiModel = aiModel;
            return this;
        }

        public AnalysisReport build() {
            if (this.summary == null) {
                this.summary = ReportSummary.fromFindings(this.findings);
            }
            return new AnalysisReport(this);
        }
    }

    // Getters
    public String getProjectName() { return projectName; }
    public String getOwaspVersion() { return owaspVersion; }
    public LocalDateTime getAnalysisTime() { return analysisTime; }
    public List<SecurityFinding> getFindings() { return findings; }
    public ReportSummary getSummary() { return summary; }
    public String getAiModel() { return aiModel; }

    // Convenience methods for backward compatibility
    public int getTotalFindings() {
        return summary != null ? summary.getTotalFindings() : 0;
    }

    public ReportSummary getReportSummary() {
        return summary;
    }
}
