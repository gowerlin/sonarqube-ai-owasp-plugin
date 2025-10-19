package com.github.sonarqube.report.comparison;

import com.github.sonarqube.report.model.AnalysisReport;
import com.github.sonarqube.report.model.SecurityFinding;
import com.github.sonarqube.report.model.VersionComparisonReport;
import com.github.sonarqube.rules.OwaspVersionMappingService;
import com.github.sonarqube.rules.OwaspVersionMappingService.CategoryMapping;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 版本比較引擎
 *
 * 分析多個 OWASP 版本之間的差異，包含類別映射、新增/移除/變更的發現。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.3.0 (Epic 5, Story 5.4)
 */
public class VersionComparisonEngine {

    private final OwaspVersionMappingService mappingService;

    public VersionComparisonEngine() {
        this.mappingService = new OwaspVersionMappingService();
    }

    /**
     * 建立版本對照報告
     *
     * @param reports 各版本的分析報告 (version → AnalysisReport)
     * @param projectKey 專案鍵值
     * @return 版本對照報告
     */
    public VersionComparisonReport createComparisonReport(Map<String, AnalysisReport> reports, String projectKey) {
        List<String> versions = new ArrayList<>(reports.keySet());
        Collections.sort(versions);

        // 取得版本映射
        List<CategoryMapping> mappings = getRelevantMappings(versions);

        // 分析差異
        VersionComparisonReport.DifferenceAnalysis differenceAnalysis = analyzeDifferences(reports, versions);

        return VersionComparisonReport.builder()
            .owaspVersions(versions)
            .versionReports(reports)
            .differenceAnalysis(differenceAnalysis)
            .categoryMappings(mappings)
            .projectKey(projectKey)
            .analysisTimestamp(System.currentTimeMillis())
            .build();
    }

    /**
     * 取得相關的版本映射
     */
    private List<CategoryMapping> getRelevantMappings(List<String> versions) {
        if (versions.contains("2017") && versions.contains("2021")) {
            return mappingService.get2017To2021Mappings();
        }
        return mappingService.getAllMappings().stream()
            .filter(m -> versions.contains(m.getSourceVersion()) &&
                        (m.getTargetVersion() == null || versions.contains(m.getTargetVersion())))
            .collect(Collectors.toList());
    }

    /**
     * 分析版本間差異
     */
    private VersionComparisonReport.DifferenceAnalysis analyzeDifferences(
            Map<String, AnalysisReport> reports, List<String> versions) {

        VersionComparisonReport.DifferenceAnalysis.Builder builder =
            VersionComparisonReport.DifferenceAnalysis.builder();

        if (versions.size() < 2) {
            return builder.build();
        }

        // 比較相鄰版本
        for (int i = 0; i < versions.size() - 1; i++) {
            String fromVersion = versions.get(i);
            String toVersion = versions.get(i + 1);

            AnalysisReport fromReport = reports.get(fromVersion);
            AnalysisReport toReport = reports.get(toVersion);

            // 計算新增的發現
            int addedCount = calculateAddedFindings(fromReport, toReport);
            builder.addedFindings(Collections.singletonMap(toVersion, addedCount));

            // 計算移除的發現
            int removedCount = calculateRemovedFindings(fromReport, toReport);
            builder.removedFindings(Collections.singletonMap(toVersion, removedCount));

            // 計算變更的發現
            int changedCount = calculateChangedFindings(fromReport, toReport);
            builder.changedFindings(Collections.singletonMap(toVersion, changedCount));

            // 計算合規性變化百分比
            double changePercent = calculateComplianceChange(fromReport, toReport);
            builder.complianceChangePercent(Collections.singletonMap(toVersion, changePercent));

            // 生成遷移建議
            generateMigrationRecommendations(builder, fromVersion, toVersion, addedCount, removedCount);
        }

        return builder.build();
    }

    /**
     * 計算新增的發現
     */
    private int calculateAddedFindings(AnalysisReport fromReport, AnalysisReport toReport) {
        Set<String> fromCategories = fromReport.getFindings().stream()
            .map(SecurityFinding::getOwaspCategory)
            .collect(Collectors.toSet());

        long addedCount = toReport.getFindings().stream()
            .filter(f -> !fromCategories.contains(f.getOwaspCategory()))
            .count();

        return (int) addedCount;
    }

    /**
     * 計算移除的發現
     */
    private int calculateRemovedFindings(AnalysisReport fromReport, AnalysisReport toReport) {
        Set<String> toCategories = toReport.getFindings().stream()
            .map(SecurityFinding::getOwaspCategory)
            .collect(Collectors.toSet());

        long removedCount = fromReport.getFindings().stream()
            .filter(f -> !toCategories.contains(f.getOwaspCategory()))
            .count();

        return (int) removedCount;
    }

    /**
     * 計算變更的發現
     */
    private int calculateChangedFindings(AnalysisReport fromReport, AnalysisReport toReport) {
        // 計算同一檔案但分類改變的發現
        Map<String, String> fromFileCategories = fromReport.getFindings().stream()
            .collect(Collectors.toMap(
                SecurityFinding::getFilePath,
                SecurityFinding::getOwaspCategory,
                (a, b) -> a
            ));

        long changedCount = toReport.getFindings().stream()
            .filter(f -> {
                String fromCategory = fromFileCategories.get(f.getFilePath());
                return fromCategory != null && !fromCategory.equals(f.getOwaspCategory());
            })
            .count();

        return (int) changedCount;
    }

    /**
     * 計算合規性變化百分比
     */
    private double calculateComplianceChange(AnalysisReport fromReport, AnalysisReport toReport) {
        int fromTotal = fromReport.getTotalFindings();
        int toTotal = toReport.getTotalFindings();

        if (fromTotal == 0) {
            return toTotal > 0 ? 100.0 : 0.0;
        }

        return ((double) (toTotal - fromTotal) / fromTotal) * 100.0;
    }

    /**
     * 生成遷移建議
     */
    private void generateMigrationRecommendations(
            VersionComparisonReport.DifferenceAnalysis.Builder builder,
            String fromVersion, String toVersion, int addedCount, int removedCount) {

        if (addedCount > 0) {
            builder.addRecommendation(String.format(
                "OWASP %s 新增了 %d 個安全發現，建議優先處理這些新識別的風險",
                toVersion, addedCount
            ));
        }

        if (removedCount > 0) {
            builder.addRecommendation(String.format(
                "%d 個 OWASP %s 的發現在 %s 中已被重新分類，請檢查新的分類標準",
                removedCount, fromVersion, toVersion
            ));
        }

        // 2017 → 2021 特定建議
        if ("2017".equals(fromVersion) && "2021".equals(toVersion)) {
            builder.addRecommendation("OWASP 2021 將 XSS (A7:2017) 合併至 Injection (A03:2021)，請更新安全測試策略");
            builder.addRecommendation("OWASP 2021 新增 Insecure Design (A04) 和 SSRF (A10)，建議加強設計階段安全審查");
            builder.addRecommendation("Broken Access Control (A01:2021) 升至第一位，反映其嚴重性與普遍性");
        }
    }

    /**
     * 比較兩個版本的發現
     *
     * @param version1Report 版本 1 報告
     * @param version2Report 版本 2 報告
     * @return 比較結果摘要
     */
    public String compareVersions(AnalysisReport version1Report, AnalysisReport version2Report) {
        int added = calculateAddedFindings(version1Report, version2Report);
        int removed = calculateRemovedFindings(version1Report, version2Report);
        int changed = calculateChangedFindings(version1Report, version2Report);
        double changePercent = calculateComplianceChange(version1Report, version2Report);

        return String.format(
            "版本比較：新增 %d 個發現，移除 %d 個發現，變更 %d 個發現，合規性變化 %.2f%%",
            added, removed, changed, changePercent
        );
    }
}
