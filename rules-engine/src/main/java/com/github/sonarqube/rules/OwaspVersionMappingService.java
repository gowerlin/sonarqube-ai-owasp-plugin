package com.github.sonarqube.rules;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OWASP 版本映射服務
 *
 * 管理 OWASP 2017 與 2021 版本之間的類別對應關係和差異說明。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.2.0 (Epic 4, Story 4.3)
 */
public class OwaspVersionMappingService {

    /**
     * 映射類型枚舉
     */
    public enum MappingType {
        DIRECT("直接映射", "Direct mapping"),
        MERGED("合併映射", "Merged from multiple categories"),
        SPLIT("拆分映射", "Split into multiple categories"),
        NEW("新增類別", "New category in target version"),
        REMOVED("移除類別", "Removed in target version");

        private final String zhDescription;
        private final String enDescription;

        MappingType(String zhDescription, String enDescription) {
            this.zhDescription = zhDescription;
            this.enDescription = enDescription;
        }

        public String getZhDescription() { return zhDescription; }
        public String getEnDescription() { return enDescription; }
    }

    /**
     * 類別映射
     */
    public static class CategoryMapping {
        private final String sourceVersion;
        private final String sourceCategory;
        private final String sourceName;
        private final String targetVersion;
        private final String targetCategory;
        private final String targetName;
        private final MappingType mappingType;
        private final String explanation;

        public CategoryMapping(String sourceVersion, String sourceCategory, String sourceName,
                               String targetVersion, String targetCategory, String targetName,
                               MappingType mappingType, String explanation) {
            this.sourceVersion = sourceVersion;
            this.sourceCategory = sourceCategory;
            this.sourceName = sourceName;
            this.targetVersion = targetVersion;
            this.targetCategory = targetCategory;
            this.targetName = targetName;
            this.mappingType = mappingType;
            this.explanation = explanation;
        }

        public String getSourceVersion() { return sourceVersion; }
        public String getSourceCategory() { return sourceCategory; }
        public String getSourceName() { return sourceName; }
        public String getTargetVersion() { return targetVersion; }
        public String getTargetCategory() { return targetCategory; }
        public String getTargetName() { return targetName; }
        public MappingType getMappingType() { return mappingType; }
        public String getExplanation() { return explanation; }

        @Override
        public String toString() {
            return String.format("%s %s (%s) → %s %s (%s) [%s]",
                sourceVersion, sourceCategory, sourceName,
                targetVersion, targetCategory, targetName,
                mappingType.getZhDescription());
        }
    }

    private final Map<String, List<CategoryMapping>> mappings = new ConcurrentHashMap<>();

    /**
     * 預設建構子 - 初始化 2017 ↔ 2021 映射
     */
    public OwaspVersionMappingService() {
        initialize2017To2021Mappings();
    }

    /**
     * 初始化 OWASP 2017 → 2021 映射
     */
    private void initialize2017To2021Mappings() {
        // A1:2017 Injection → A03:2021 Injection (DIRECT)
        addMapping("2017", "A1", "Injection", "2021", "A03", "Injection",
            MappingType.DIRECT, "直接映射：注入攻擊類別在兩版本中保持一致");

        // A2:2017 Broken Authentication → A07:2021 Identification and Authentication Failures (DIRECT)
        addMapping("2017", "A2", "Broken Authentication", "2021", "A07", "Identification and Authentication Failures",
            MappingType.DIRECT, "直接映射：擴展為識別與認證失敗，範圍更廣");

        // A3:2017 Sensitive Data Exposure → A02:2021 Cryptographic Failures (DIRECT)
        addMapping("2017", "A3", "Sensitive Data Exposure", "2021", "A02", "Cryptographic Failures",
            MappingType.DIRECT, "直接映射：聚焦於加密失敗，而非廣泛的資料曝露");

        // A4:2017 XXE → A05:2021 Security Misconfiguration (MERGED)
        addMapping("2017", "A4", "XML External Entities (XXE)", "2021", "A05", "Security Misconfiguration",
            MappingType.MERGED, "合併映射：XXE 被視為安全配置錯誤的一部分");

        // A5:2017 Broken Access Control → A01:2021 Broken Access Control (DIRECT)
        addMapping("2017", "A5", "Broken Access Control", "2021", "A01", "Broken Access Control",
            MappingType.DIRECT, "直接映射：升至第一位，反映其嚴重性與普遍性");

        // A6:2017 Security Misconfiguration → A05:2021 Security Misconfiguration (DIRECT)
        addMapping("2017", "A6", "Security Misconfiguration", "2021", "A05", "Security Misconfiguration",
            MappingType.DIRECT, "直接映射：安全配置錯誤保持一致");

        // A7:2017 XSS → A03:2021 Injection (MERGED)
        addMapping("2017", "A7", "Cross-Site Scripting (XSS)", "2021", "A03", "Injection",
            MappingType.MERGED, "合併映射：XSS 被納入注入攻擊類別");

        // A8:2017 Insecure Deserialization → A08:2021 Software and Data Integrity Failures (DIRECT)
        addMapping("2017", "A8", "Insecure Deserialization", "2021", "A08", "Software and Data Integrity Failures",
            MappingType.DIRECT, "直接映射：擴展為軟體與資料完整性失敗");

        // A9:2017 Using Components with Known Vulnerabilities → A06:2021 Vulnerable and Outdated Components (DIRECT)
        addMapping("2017", "A9", "Using Components with Known Vulnerabilities", "2021", "A06", "Vulnerable and Outdated Components",
            MappingType.DIRECT, "直接映射：重新命名為過時元件");

        // A10:2017 Insufficient Logging & Monitoring → A09:2021 Security Logging and Monitoring Failures (DIRECT)
        addMapping("2017", "A10", "Insufficient Logging & Monitoring", "2021", "A09", "Security Logging and Monitoring Failures",
            MappingType.DIRECT, "直接映射：重新命名為安全日誌與監控失敗");

        // NEW in 2021: A04 Insecure Design
        addMapping("2021", "A04", "Insecure Design", "2017", null, null,
            MappingType.NEW, "新增類別：2021 新增的不安全設計類別");

        // NEW in 2021: A10 Server-Side Request Forgery (SSRF)
        addMapping("2021", "A10", "Server-Side Request Forgery (SSRF)", "2017", null, null,
            MappingType.NEW, "新增類別：2021 新增的 SSRF 類別");
    }

    /**
     * 新增映射關係
     */
    private void addMapping(String sourceVersion, String sourceCategory, String sourceName,
                            String targetVersion, String targetCategory, String targetName,
                            MappingType mappingType, String explanation) {
        String key = sourceVersion + ":" + sourceCategory;
        mappings.computeIfAbsent(key, k -> new ArrayList<>())
            .add(new CategoryMapping(sourceVersion, sourceCategory, sourceName,
                targetVersion, targetCategory, targetName, mappingType, explanation));
    }

    /**
     * 取得指定類別的映射
     *
     * @param sourceVersion 來源版本
     * @param sourceCategory 來源類別
     * @return 映射列表
     */
    public List<CategoryMapping> getMappings(String sourceVersion, String sourceCategory) {
        String key = sourceVersion + ":" + sourceCategory;
        return new ArrayList<>(mappings.getOrDefault(key, Collections.emptyList()));
    }

    /**
     * 取得所有映射
     *
     * @return 所有映射列表
     */
    public List<CategoryMapping> getAllMappings() {
        List<CategoryMapping> allMappings = new ArrayList<>();
        for (List<CategoryMapping> categoryMappings : mappings.values()) {
            allMappings.addAll(categoryMappings);
        }
        return allMappings;
    }

    /**
     * 取得 2017 → 2021 的映射
     *
     * @return 映射列表
     */
    public List<CategoryMapping> get2017To2021Mappings() {
        return getAllMappings().stream()
            .filter(m -> "2017".equals(m.getSourceVersion()))
            .toList();
    }

    /**
     * 取得 2021 新增的類別
     *
     * @return 新增類別列表
     */
    public List<CategoryMapping> getNew2021Categories() {
        return getAllMappings().stream()
            .filter(m -> "2021".equals(m.getSourceVersion()) && m.getMappingType() == MappingType.NEW)
            .toList();
    }

    /**
     * 取得差異分析
     *
     * @return 差異分析字串
     */
    public String getDifferenceAnalysis() {
        StringBuilder sb = new StringBuilder();
        sb.append("OWASP 2017 → 2021 差異分析\n");
        sb.append("=".repeat(50)).append("\n\n");

        // 直接映射
        long directMappings = getAllMappings().stream()
            .filter(m -> m.getMappingType() == MappingType.DIRECT)
            .count();
        sb.append("直接映射 (DIRECT): ").append(directMappings).append(" 個\n");

        // 合併映射
        long mergedMappings = getAllMappings().stream()
            .filter(m -> m.getMappingType() == MappingType.MERGED)
            .count();
        sb.append("合併映射 (MERGED): ").append(mergedMappings).append(" 個\n");

        // 新增類別
        long newCategories = getNew2021Categories().size();
        sb.append("新增類別 (NEW): ").append(newCategories).append(" 個\n\n");

        sb.append("詳細映射：\n");
        for (CategoryMapping mapping : get2017To2021Mappings()) {
            sb.append("  ").append(mapping).append("\n");
        }

        return sb.toString();
    }
}
