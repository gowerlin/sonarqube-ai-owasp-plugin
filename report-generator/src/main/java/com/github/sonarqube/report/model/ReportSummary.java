package com.github.sonarqube.report.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 報告摘要統計
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class ReportSummary {

    private final int totalFindings;
    private final Map<String, Integer> severityCounts;
    private final Map<String, Integer> categoryCounts;
    private final int filesAnalyzed;

    private ReportSummary(Builder builder) {
        this.totalFindings = builder.totalFindings;
        this.severityCounts = Map.copyOf(builder.severityCounts);
        this.categoryCounts = Map.copyOf(builder.categoryCounts);
        this.filesAnalyzed = builder.filesAnalyzed;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int totalFindings;
        private Map<String, Integer> severityCounts = new HashMap<>();
        private Map<String, Integer> categoryCounts = new HashMap<>();
        private int filesAnalyzed;

        public Builder totalFindings(int totalFindings) {
            this.totalFindings = totalFindings;
            return this;
        }

        public Builder severityCounts(Map<String, Integer> severityCounts) {
            this.severityCounts = severityCounts != null ? new HashMap<>(severityCounts) : new HashMap<>();
            return this;
        }

        public Builder categoryCounts(Map<String, Integer> categoryCounts) {
            this.categoryCounts = categoryCounts != null ? new HashMap<>(categoryCounts) : new HashMap<>();
            return this;
        }

        public Builder filesAnalyzed(int filesAnalyzed) {
            this.filesAnalyzed = filesAnalyzed;
            return this;
        }

        public ReportSummary build() {
            return new ReportSummary(this);
        }
    }

    /**
     * 從發現列表自動計算摘要
     */
    public static ReportSummary fromFindings(List<SecurityFinding> findings) {
        if (findings == null || findings.isEmpty()) {
            return builder().totalFindings(0).build();
        }

        Map<String, Integer> severityCounts = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();

        for (SecurityFinding finding : findings) {
            // 統計嚴重性
            String severity = finding.getSeverity();
            severityCounts.put(severity, severityCounts.getOrDefault(severity, 0) + 1);

            // 統計 OWASP 分類
            String category = finding.getOwaspCategory();
            if (category != null) {
                categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
            }
        }

        return builder()
            .totalFindings(findings.size())
            .severityCounts(severityCounts)
            .categoryCounts(categoryCounts)
            .build();
    }

    // Getters
    public int getTotalFindings() { return totalFindings; }
    public Map<String, Integer> getSeverityCounts() { return severityCounts; }
    public Map<String, Integer> getCategoryCounts() { return categoryCounts; }
    public int getFilesAnalyzed() { return filesAnalyzed; }

    public int getBlockerCount() {
        return severityCounts.getOrDefault("BLOCKER", 0);
    }

    public int getCriticalCount() {
        return severityCounts.getOrDefault("CRITICAL", 0);
    }

    public int getMajorCount() {
        return severityCounts.getOrDefault("MAJOR", 0);
    }

    public int getMinorCount() {
        return severityCounts.getOrDefault("MINOR", 0);
    }

    public int getInfoCount() {
        return severityCounts.getOrDefault("INFO", 0);
    }

    public int getHighCount() {
        return severityCounts.getOrDefault("HIGH", 0);
    }

    public int getMediumCount() {
        return severityCounts.getOrDefault("MEDIUM", 0);
    }

    public int getLowCount() {
        return severityCounts.getOrDefault("LOW", 0);
    }

    public int getAnalyzedFilesCount() {
        return filesAnalyzed;
    }
}
