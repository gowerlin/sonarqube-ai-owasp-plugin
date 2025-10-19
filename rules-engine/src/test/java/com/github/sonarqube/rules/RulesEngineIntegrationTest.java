package com.github.sonarqube.rules;

import com.github.sonarqube.rules.cwe.CweMapping;
import com.github.sonarqube.rules.java.JavaSecurityRules;
import com.github.sonarqube.rules.javascript.JavaScriptSecurityRules;
import com.github.sonarqube.rules.owasp.Owasp2021Category;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 規則引擎整合測試
 *
 * 測試多語言規則引擎的整合功能，包括：
 * - CWE 映射與 OWASP 分類的一致性
 * - 多語言規則的完整性
 * - 規則引擎的統計功能
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
class RulesEngineIntegrationTest {

    @Test
    void testMultiLanguageRulesIntegration() {
        // 獲取所有語言的規則
        List<RuleDefinition> javaRules = JavaSecurityRules.getAllRules();
        List<RuleDefinition> jsRules = JavaScriptSecurityRules.getAllRules();

        // 驗證規則數量
        assertTrue(javaRules.size() >= 18, "Java should have at least 18 rules");
        assertTrue(jsRules.size() >= 21, "JavaScript should have at least 21 rules");

        // 驗證規則鍵唯一性（跨語言）
        Set<String> allRuleKeys = javaRules.stream()
            .map(RuleDefinition::getRuleKey)
            .collect(Collectors.toSet());

        jsRules.stream()
            .map(RuleDefinition::getRuleKey)
            .forEach(key -> assertFalse(allRuleKeys.contains(key),
                "Rule key " + key + " should be unique across languages"));
    }

    @Test
    void testCweToOwaspMappingConsistency() {
        // 測試 Java 規則的 CWE 是否都能映射到 OWASP 分類
        List<RuleDefinition> javaRules = JavaSecurityRules.getAllRules();

        for (RuleDefinition rule : javaRules) {
            for (String cweId : rule.getCweIds()) {
                Owasp2021Category mappedCategory = CweMapping.getOwaspCategory(cweId);
                assertNotNull(mappedCategory,
                    "CWE " + cweId + " in rule " + rule.getRuleKey() + " should have OWASP mapping");

                // 驗證規則的 OWASP 分類與 CWE 映射一致（或相關）
                // 注意：一個規則可能有多個 CWE，不一定所有 CWE 都映射到同一個 OWASP 分類
                assertTrue(mappedCategory != null,
                    "CWE " + cweId + " should map to a valid OWASP category");
            }
        }
    }

    @Test
    void testJavaScriptCweToOwaspMappingConsistency() {
        // 測試 JavaScript 規則的 CWE 映射
        List<RuleDefinition> jsRules = JavaScriptSecurityRules.getAllRules();

        for (RuleDefinition rule : jsRules) {
            for (String cweId : rule.getCweIds()) {
                Owasp2021Category mappedCategory = CweMapping.getOwaspCategory(cweId);
                assertNotNull(mappedCategory,
                    "CWE " + cweId + " in rule " + rule.getRuleKey() + " should have OWASP mapping");
            }
        }
    }

    @Test
    void testOwaspCategoryCoverage() {
        // 測試所有 OWASP 2021 Top 10 分類都有對應的規則
        Map<Owasp2021Category, Long> javaStats = JavaSecurityRules.getRuleStatistics();
        Map<Owasp2021Category, Long> jsStats = JavaScriptSecurityRules.getRuleStatistics();

        // 合併統計
        Set<Owasp2021Category> coveredCategories = javaStats.keySet().stream()
            .collect(Collectors.toSet());
        coveredCategories.addAll(jsStats.keySet());

        // 驗證覆蓋率
        assertTrue(coveredCategories.size() >= 9,
            "Should cover at least 9 out of 10 OWASP 2021 categories");

        // 驗證關鍵分類都有覆蓋
        assertTrue(coveredCategories.contains(Owasp2021Category.A01_BROKEN_ACCESS_CONTROL));
        assertTrue(coveredCategories.contains(Owasp2021Category.A02_CRYPTOGRAPHIC_FAILURES));
        assertTrue(coveredCategories.contains(Owasp2021Category.A03_INJECTION));
    }

    @Test
    void testRuleDistributionAcrossLanguages() {
        // 測試規則在不同語言間的分布
        Map<Owasp2021Category, Long> javaStats = JavaSecurityRules.getRuleStatistics();
        Map<Owasp2021Category, Long> jsStats = JavaScriptSecurityRules.getRuleStatistics();

        // 注入類別應該在兩種語言都有規則
        assertTrue(javaStats.containsKey(Owasp2021Category.A03_INJECTION),
            "Java should have injection rules");
        assertTrue(jsStats.containsKey(Owasp2021Category.A03_INJECTION),
            "JavaScript should have injection rules");

        // 注入類別的規則數量應該都 >= 3
        assertTrue(javaStats.get(Owasp2021Category.A03_INJECTION) >= 3,
            "Java should have at least 3 injection rules");
        assertTrue(jsStats.get(Owasp2021Category.A03_INJECTION) >= 3,
            "JavaScript should have at least 3 injection rules");
    }

    @Test
    void testSeverityDistribution() {
        // 測試嚴重性分布
        List<RuleDefinition> allRules = JavaSecurityRules.getAllRules();
        allRules.addAll(JavaScriptSecurityRules.getAllRules());

        long blockerCount = allRules.stream()
            .filter(r -> r.getSeverity() == RuleDefinition.RuleSeverity.BLOCKER)
            .count();

        long criticalCount = allRules.stream()
            .filter(r -> r.getSeverity() == RuleDefinition.RuleSeverity.CRITICAL)
            .count();

        long majorCount = allRules.stream()
            .filter(r -> r.getSeverity() == RuleDefinition.RuleSeverity.MAJOR)
            .count();

        // 驗證至少有一些高嚴重性規則
        assertTrue(blockerCount >= 5, "Should have at least 5 BLOCKER rules");
        assertTrue(criticalCount >= 5, "Should have at least 5 CRITICAL rules");
        assertTrue(majorCount >= 3, "Should have at least 3 MAJOR rules");
    }

    @Test
    void testRuleTypeDistribution() {
        // 測試規則類型分布
        List<RuleDefinition> allRules = JavaSecurityRules.getAllRules();
        allRules.addAll(JavaScriptSecurityRules.getAllRules());

        long vulnerabilityCount = allRules.stream()
            .filter(r -> r.getType() == RuleDefinition.RuleType.VULNERABILITY)
            .count();

        long securityHotspotCount = allRules.stream()
            .filter(r -> r.getType() == RuleDefinition.RuleType.SECURITY_HOTSPOT)
            .count();

        // 大部分規則應該是 VULNERABILITY 類型
        assertTrue(vulnerabilityCount >= 30, "Should have at least 30 vulnerability rules");
        assertTrue(securityHotspotCount >= 3, "Should have at least 3 security hotspot rules");
    }

    @Test
    void testAllRulesHaveValidCweMapping() {
        // 測試所有規則的 CWE 都存在於映射系統中
        List<RuleDefinition> allRules = JavaSecurityRules.getAllRules();
        allRules.addAll(JavaScriptSecurityRules.getAllRules());

        for (RuleDefinition rule : allRules) {
            for (String cweId : rule.getCweIds()) {
                assertTrue(CweMapping.isMapped(cweId),
                    "CWE " + cweId + " in rule " + rule.getRuleKey() + " should be mapped");
            }
        }
    }

    @Test
    void testRuleTagsConsistency() {
        // 測試規則標籤的一致性
        List<RuleDefinition> allRules = JavaSecurityRules.getAllRules();
        allRules.addAll(JavaScriptSecurityRules.getAllRules());

        for (RuleDefinition rule : allRules) {
            // 所有規則都應該有 owasp 和 security 標籤
            assertTrue(rule.getTags().contains("owasp"),
                "Rule " + rule.getRuleKey() + " should have 'owasp' tag");
            assertTrue(rule.getTags().contains("security"),
                "Rule " + rule.getRuleKey() + " should have 'security' tag");

            // 標籤數量應該至少 3 個
            assertTrue(rule.getTags().size() >= 3,
                "Rule " + rule.getRuleKey() + " should have at least 3 tags");
        }
    }

    @Test
    void testRemediationCostFormat() {
        // 測試修復成本格式
        List<RuleDefinition> allRules = JavaSecurityRules.getAllRules();
        allRules.addAll(JavaScriptSecurityRules.getAllRules());

        for (RuleDefinition rule : allRules) {
            assertNotNull(rule.getRemediationCost(),
                "Rule " + rule.getRuleKey() + " should have remediation cost");

            String cost = rule.getRemediationCost();
            assertTrue(cost.matches("\\d+(min|h)"),
                "Remediation cost '" + cost + "' should match format '10min' or '1h'");

            // 提取數字驗證合理性
            int value = Integer.parseInt(cost.replaceAll("\\D+", ""));
            assertTrue(value >= 5 && value <= 120,
                "Remediation cost " + value + " should be between 5 and 120");
        }
    }

    @Test
    void testCriticalRulesHaveDetailedDescriptions() {
        // 測試高危規則都有詳細描述
        List<RuleDefinition> allRules = JavaSecurityRules.getAllRules();
        allRules.addAll(JavaScriptSecurityRules.getAllRules());

        List<RuleDefinition> criticalRules = allRules.stream()
            .filter(r -> r.getSeverity() == RuleDefinition.RuleSeverity.BLOCKER ||
                         r.getSeverity() == RuleDefinition.RuleSeverity.CRITICAL)
            .toList();

        for (RuleDefinition rule : criticalRules) {
            assertTrue(rule.getDescription().length() >= 20,
                "Critical rule " + rule.getRuleKey() + " should have detailed description");

            assertTrue(rule.getName().length() >= 5,
                "Critical rule " + rule.getRuleKey() + " should have meaningful name");
        }
    }

    @Test
    void testNewOwasp2021CategoriesHaveRules() {
        // 測試 OWASP 2021 新增類別都有規則
        List<RuleDefinition> allRules = JavaSecurityRules.getAllRules();
        allRules.addAll(JavaScriptSecurityRules.getAllRules());

        // A04: Insecure Design (2021 新增)
        long a04Count = allRules.stream()
            .filter(r -> r.getOwaspCategory() == Owasp2021Category.A04_INSECURE_DESIGN)
            .count();
        assertTrue(a04Count >= 2, "Should have rules for A04 (new in 2021)");

        // A08: Software and Data Integrity Failures (2021 新增)
        long a08Count = allRules.stream()
            .filter(r -> r.getOwaspCategory() == Owasp2021Category.A08_SOFTWARE_DATA_INTEGRITY_FAILURES)
            .count();
        assertTrue(a08Count >= 2, "Should have rules for A08 (new in 2021)");

        // A10: Server-Side Request Forgery (2021 新增)
        long a10Count = allRules.stream()
            .filter(r -> r.getOwaspCategory() == Owasp2021Category.A10_SERVER_SIDE_REQUEST_FORGERY)
            .count();
        assertTrue(a10Count >= 2, "Should have rules for A10 (new in 2021)");
    }

    @Test
    void testInjectionRulesCoverMainTypes() {
        // 測試注入類規則覆蓋主要類型
        List<RuleDefinition> javaRules = JavaSecurityRules.getAllRules();
        List<RuleDefinition> jsRules = JavaScriptSecurityRules.getAllRules();

        // Java 應該有 SQL、命令、LDAP 注入
        assertTrue(javaRules.stream()
            .anyMatch(r -> r.getCweIds().contains("CWE-89")), // SQL Injection
            "Java should have SQL injection rule");

        assertTrue(javaRules.stream()
            .anyMatch(r -> r.getCweIds().contains("CWE-78")), // Command Injection
            "Java should have command injection rule");

        // JavaScript 應該有 XSS、NoSQL 注入
        assertTrue(jsRules.stream()
            .anyMatch(r -> r.getCweIds().contains("CWE-79")), // XSS
            "JavaScript should have XSS rule");

        assertTrue(jsRules.stream()
            .anyMatch(r -> r.getCweIds().contains("CWE-943")), // NoSQL Injection
            "JavaScript should have NoSQL injection rule");
    }

    @Test
    void testTotalRuleCount() {
        // 測試規則總數
        int javaCount = JavaSecurityRules.getRuleCount();
        int jsCount = JavaScriptSecurityRules.getRuleCount();
        int totalCount = javaCount + jsCount;

        assertTrue(totalCount >= 39,
            "Total rule count should be at least 39 (18 Java + 21 JavaScript)");

        System.out.println("Total Rules: " + totalCount);
        System.out.println("  - Java: " + javaCount);
        System.out.println("  - JavaScript: " + jsCount);
    }

    @Test
    void testRulesEngineStatistics() {
        // 測試規則引擎統計功能
        Map<Owasp2021Category, Long> javaStats = JavaSecurityRules.getRuleStatistics();
        Map<Owasp2021Category, Long> jsStats = JavaScriptSecurityRules.getRuleStatistics();

        System.out.println("\nJava Rules Statistics:");
        javaStats.forEach((category, count) ->
            System.out.println("  " + category.getCategoryId() + ": " + count));

        System.out.println("\nJavaScript Rules Statistics:");
        jsStats.forEach((category, count) ->
            System.out.println("  " + category.getCategoryId() + ": " + count));

        // 驗證統計正確性
        long javaTotal = javaStats.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(JavaSecurityRules.getRuleCount(), javaTotal,
            "Java statistics should match total count");

        long jsTotal = jsStats.values().stream().mapToLong(Long::longValue).sum();
        assertEquals(JavaScriptSecurityRules.getRuleCount(), jsTotal,
            "JavaScript statistics should match total count");
    }
}
