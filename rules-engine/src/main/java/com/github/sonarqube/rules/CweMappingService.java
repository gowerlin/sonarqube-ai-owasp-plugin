package com.github.sonarqube.rules;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CWE 映射服務
 *
 * 管理 OWASP 類別與 CWE ID 之間的映射關係。
 * 提供快速查詢功能，支援雙向查詢（OWASP → CWE, CWE → OWASP）。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.12)
 */
public class CweMappingService {

    private final Map<String, Set<String>> owaspToCweMap = new ConcurrentHashMap<>();
    private final Map<String, String> cweToOwaspMap = new ConcurrentHashMap<>();

    /**
     * 預設建構子 - 初始化 OWASP 2021 映射
     */
    public CweMappingService() {
        initializeOwasp2021Mappings();
    }

    /**
     * 根據 OWASP 類別取得 CWE IDs
     *
     * @param owaspCategory OWASP 類別（例如：A01, A02）
     * @return CWE ID 集合
     */
    public Set<String> getCwesByOwasp(String owaspCategory) {
        return owaspToCweMap.getOrDefault(owaspCategory, Collections.emptySet());
    }

    /**
     * 根據 CWE ID 取得所屬 OWASP 類別
     *
     * @param cweId CWE ID（例如：CWE-89）
     * @return OWASP 類別，如果不存在返回 null
     */
    public String getOwaspByCwe(String cweId) {
        return cweToOwaspMap.get(cweId);
    }

    /**
     * 檢查 CWE 是否屬於指定 OWASP 類別
     *
     * @param cweId CWE ID
     * @param owaspCategory OWASP 類別
     * @return true 如果屬於
     */
    public boolean isCweInOwasp(String cweId, String owaspCategory) {
        return owaspCategory.equals(cweToOwaspMap.get(cweId));
    }

    /**
     * 取得所有已註冊的 OWASP 類別
     *
     * @return OWASP 類別集合
     */
    public Set<String> getAllOwaspCategories() {
        return new HashSet<>(owaspToCweMap.keySet());
    }

    /**
     * 取得 CWE 總數
     *
     * @return CWE 總數
     */
    public int getTotalCweCount() {
        return cweToOwaspMap.size();
    }

    /**
     * 註冊映射關係
     *
     * @param owaspCategory OWASP 類別
     * @param cweIds CWE ID 集合
     */
    public void registerMapping(String owaspCategory, Set<String> cweIds) {
        owaspToCweMap.put(owaspCategory, new HashSet<>(cweIds));
        for (String cweId : cweIds) {
            cweToOwaspMap.put(cweId, owaspCategory);
        }
    }

    /**
     * 初始化 OWASP 2021 映射
     */
    private void initializeOwasp2021Mappings() {
        // A01: Broken Access Control (33 CWEs)
        registerMapping("A01", new HashSet<>(Arrays.asList(
            "CWE-22", "CWE-23", "CWE-35", "CWE-59", "CWE-200", "CWE-201", "CWE-219", "CWE-264",
            "CWE-275", "CWE-284", "CWE-285", "CWE-352", "CWE-359", "CWE-377", "CWE-402", "CWE-425",
            "CWE-441", "CWE-497", "CWE-538", "CWE-540", "CWE-548", "CWE-552", "CWE-566", "CWE-601",
            "CWE-639", "CWE-651", "CWE-668", "CWE-706", "CWE-862", "CWE-863", "CWE-913", "CWE-922",
            "CWE-1275"
        )));

        // A02: Cryptographic Failures (29 CWEs)
        registerMapping("A02", new HashSet<>(Arrays.asList(
            "CWE-261", "CWE-296", "CWE-310", "CWE-319", "CWE-321", "CWE-322", "CWE-323", "CWE-324",
            "CWE-325", "CWE-326", "CWE-327", "CWE-328", "CWE-329", "CWE-330", "CWE-331", "CWE-335",
            "CWE-336", "CWE-337", "CWE-338", "CWE-340", "CWE-347", "CWE-523", "CWE-720", "CWE-757",
            "CWE-759", "CWE-760", "CWE-780", "CWE-818", "CWE-916"
        )));

        // A03: Injection (33 CWEs)
        registerMapping("A03", new HashSet<>(Arrays.asList(
            "CWE-20", "CWE-74", "CWE-75", "CWE-77", "CWE-78", "CWE-79", "CWE-80", "CWE-83",
            "CWE-87", "CWE-88", "CWE-89", "CWE-90", "CWE-91", "CWE-93", "CWE-94", "CWE-95",
            "CWE-96", "CWE-97", "CWE-98", "CWE-99", "CWE-100", "CWE-113", "CWE-116", "CWE-138",
            "CWE-184", "CWE-470", "CWE-471", "CWE-564", "CWE-610", "CWE-643", "CWE-644", "CWE-652",
            "CWE-917"
        )));

        // A04: Insecure Design (40 CWEs)
        registerMapping("A04", new HashSet<>(Arrays.asList(
            "CWE-73", "CWE-183", "CWE-209", "CWE-213", "CWE-235", "CWE-256", "CWE-257", "CWE-266",
            "CWE-269", "CWE-280", "CWE-311", "CWE-312", "CWE-313", "CWE-316", "CWE-419", "CWE-430",
            "CWE-434", "CWE-444", "CWE-451", "CWE-472", "CWE-501", "CWE-522", "CWE-525", "CWE-539",
            "CWE-579", "CWE-598", "CWE-602", "CWE-642", "CWE-646", "CWE-650", "CWE-653", "CWE-656",
            "CWE-657", "CWE-799", "CWE-807", "CWE-840", "CWE-841", "CWE-927", "CWE-1021", "CWE-1173"
        )));

        // A05: Security Misconfiguration (20 CWEs)
        registerMapping("A05", new HashSet<>(Arrays.asList(
            "CWE-2", "CWE-11", "CWE-13", "CWE-15", "CWE-16", "CWE-260", "CWE-315", "CWE-520",
            "CWE-526", "CWE-537", "CWE-541", "CWE-547", "CWE-611", "CWE-614", "CWE-756", "CWE-776",
            "CWE-942", "CWE-1004", "CWE-1032", "CWE-1174"
        )));

        // A06: Vulnerable and Outdated Components (2 CWEs)
        registerMapping("A06", new HashSet<>(Arrays.asList(
            "CWE-1035", "CWE-1104"
        )));

        // A07: Identification and Authentication Failures (22 CWEs)
        registerMapping("A07", new HashSet<>(Arrays.asList(
            "CWE-255", "CWE-259", "CWE-287", "CWE-288", "CWE-290", "CWE-294", "CWE-295", "CWE-297",
            "CWE-300", "CWE-302", "CWE-304", "CWE-306", "CWE-307", "CWE-346", "CWE-384", "CWE-521",
            "CWE-613", "CWE-620", "CWE-640", "CWE-798", "CWE-940", "CWE-1216"
        )));

        // A08: Software and Data Integrity Failures (10 CWEs)
        registerMapping("A08", new HashSet<>(Arrays.asList(
            "CWE-345", "CWE-353", "CWE-426", "CWE-494", "CWE-502", "CWE-565", "CWE-784", "CWE-829",
            "CWE-830", "CWE-915"
        )));

        // A09: Security Logging and Monitoring Failures (4 CWEs)
        registerMapping("A09", new HashSet<>(Arrays.asList(
            "CWE-117", "CWE-223", "CWE-532", "CWE-778"
        )));

        // A10: Server-Side Request Forgery (1 CWE)
        registerMapping("A10", new HashSet<>(Collections.singletonList(
            "CWE-918"
        )));
    }

    /**
     * 取得統計資訊
     *
     * @return 統計資訊字串
     */
    public String getStatistics() {
        return String.format("CWE Mapping Statistics - OWASP Categories: %d, Total CWEs: %d",
            getAllOwaspCategories().size(),
            getTotalCweCount());
    }
}
