package com.github.sonarqube.report.model;

import com.github.sonarqube.rules.OwaspVersionMappingService.CategoryMapping;

import java.util.*;

/**
 * 版本對照報告資料模型
 *
 * 包含多個 OWASP 版本的分析結果、差異分析和類別映射。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.3.0 (Epic 5, Story 5.4)
 */
public class VersionComparisonReport {

    private final List<String> owaspVersions;
    private final Map<String, AnalysisReport> versionReports;
    private final DifferenceAnalysis differenceAnalysis;
    private final List<CategoryMapping> categoryMappings;
    private final String projectKey;
    private final long analysisTimestamp;

    private VersionComparisonReport(Builder builder) {
        this.owaspVersions = Collections.unmodifiableList(new ArrayList<>(builder.owaspVersions));
        this.versionReports = Collections.unmodifiableMap(new HashMap<>(builder.versionReports));
        this.differenceAnalysis = builder.differenceAnalysis;
        this.categoryMappings = Collections.unmodifiableList(new ArrayList<>(builder.categoryMappings));
        this.projectKey = builder.projectKey;
        this.analysisTimestamp = builder.analysisTimestamp;
    }

    // Getters
    public List<String> getOwaspVersions() { return owaspVersions; }
    public Map<String, AnalysisReport> getVersionReports() { return versionReports; }
    public DifferenceAnalysis getDifferenceAnalysis() { return differenceAnalysis; }
    public List<CategoryMapping> getCategoryMappings() { return categoryMappings; }
    public String getProjectKey() { return projectKey; }
    public long getAnalysisTimestamp() { return analysisTimestamp; }

    /**
     * 取得指定版本的報告
     */
    public AnalysisReport getReportForVersion(String version) {
        return versionReports.get(version);
    }

    /**
     * 檢查是否包含指定版本
     */
    public boolean hasVersion(String version) {
        return owaspVersions.contains(version);
    }

    /**
     * 差異分析資料模型
     */
    public static class DifferenceAnalysis {
        private final Map<String, Integer> addedFindings;
        private final Map<String, Integer> removedFindings;
        private final Map<String, Integer> changedFindings;
        private final Map<String, Double> complianceChangePercent;
        private final List<String> migrationRecommendations;

        private DifferenceAnalysis(Builder builder) {
            this.addedFindings = Collections.unmodifiableMap(new HashMap<>(builder.addedFindings));
            this.removedFindings = Collections.unmodifiableMap(new HashMap<>(builder.removedFindings));
            this.changedFindings = Collections.unmodifiableMap(new HashMap<>(builder.changedFindings));
            this.complianceChangePercent = Collections.unmodifiableMap(new HashMap<>(builder.complianceChangePercent));
            this.migrationRecommendations = Collections.unmodifiableList(new ArrayList<>(builder.migrationRecommendations));
        }

        public Map<String, Integer> getAddedFindings() { return addedFindings; }
        public Map<String, Integer> getRemovedFindings() { return removedFindings; }
        public Map<String, Integer> getChangedFindings() { return changedFindings; }
        public Map<String, Double> getComplianceChangePercent() { return complianceChangePercent; }
        public List<String> getMigrationRecommendations() { return migrationRecommendations; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private Map<String, Integer> addedFindings = new HashMap<>();
            private Map<String, Integer> removedFindings = new HashMap<>();
            private Map<String, Integer> changedFindings = new HashMap<>();
            private Map<String, Double> complianceChangePercent = new HashMap<>();
            private List<String> migrationRecommendations = new ArrayList<>();

            public Builder addedFindings(Map<String, Integer> addedFindings) {
                this.addedFindings = addedFindings;
                return this;
            }

            public Builder removedFindings(Map<String, Integer> removedFindings) {
                this.removedFindings = removedFindings;
                return this;
            }

            public Builder changedFindings(Map<String, Integer> changedFindings) {
                this.changedFindings = changedFindings;
                return this;
            }

            public Builder complianceChangePercent(Map<String, Double> complianceChangePercent) {
                this.complianceChangePercent = complianceChangePercent;
                return this;
            }

            public Builder migrationRecommendations(List<String> migrationRecommendations) {
                this.migrationRecommendations = migrationRecommendations;
                return this;
            }

            public Builder addRecommendation(String recommendation) {
                this.migrationRecommendations.add(recommendation);
                return this;
            }

            public DifferenceAnalysis build() {
                return new DifferenceAnalysis(this);
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<String> owaspVersions = new ArrayList<>();
        private Map<String, AnalysisReport> versionReports = new HashMap<>();
        private DifferenceAnalysis differenceAnalysis;
        private List<CategoryMapping> categoryMappings = new ArrayList<>();
        private String projectKey;
        private long analysisTimestamp = System.currentTimeMillis();

        public Builder owaspVersions(List<String> owaspVersions) {
            this.owaspVersions = owaspVersions;
            return this;
        }

        public Builder addVersion(String version) {
            this.owaspVersions.add(version);
            return this;
        }

        public Builder versionReports(Map<String, AnalysisReport> versionReports) {
            this.versionReports = versionReports;
            return this;
        }

        public Builder addVersionReport(String version, AnalysisReport report) {
            this.versionReports.put(version, report);
            return this;
        }

        public Builder differenceAnalysis(DifferenceAnalysis differenceAnalysis) {
            this.differenceAnalysis = differenceAnalysis;
            return this;
        }

        public Builder categoryMappings(List<CategoryMapping> categoryMappings) {
            this.categoryMappings = categoryMappings;
            return this;
        }

        public Builder addCategoryMapping(CategoryMapping mapping) {
            this.categoryMappings.add(mapping);
            return this;
        }

        public Builder projectKey(String projectKey) {
            this.projectKey = projectKey;
            return this;
        }

        public Builder analysisTimestamp(long analysisTimestamp) {
            this.analysisTimestamp = analysisTimestamp;
            return this;
        }

        public VersionComparisonReport build() {
            if (owaspVersions.isEmpty()) {
                throw new IllegalStateException("At least one OWASP version is required");
            }
            if (differenceAnalysis == null) {
                throw new IllegalStateException("DifferenceAnalysis is required");
            }
            return new VersionComparisonReport(this);
        }
    }
}
