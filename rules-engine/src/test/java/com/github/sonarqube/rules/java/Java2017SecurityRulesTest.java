package com.github.sonarqube.rules.java;

import com.github.sonarqube.rules.java.Java2017SecurityRules.RuleInfo;
import com.github.sonarqube.rules.owasp.Owasp2017Category;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java2017SecurityRules 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
class Java2017SecurityRulesTest {

    @Test
    void testGetAllRules() {
        List<RuleInfo> rules = Java2017SecurityRules.getAllRules();

        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        assertTrue(rules.size() >= 17, "Should have at least 17 Java 2017 security rules");
    }

    @Test
    void testAllRulesHaveRequiredFields() {
        List<RuleInfo> rules = Java2017SecurityRules.getAllRules();

        for (RuleInfo rule : rules) {
            assertNotNull(rule.getRuleKey(), "Rule key should not be null");
            assertNotNull(rule.getName(), "Rule name should not be null");
            assertNotNull(rule.getDescription(), "Rule description should not be null");
            assertNotNull(rule.getSeverity(), "Severity should not be null");
            assertEquals("java", rule.getLanguage(), "Language should be java");
            assertNotNull(rule.getOwaspCategory(), "OWASP category should not be null");
            assertFalse(rule.getCweIds().isEmpty(), "Should have at least one CWE ID");
            assertTrue(rule.getTags().contains("owasp2017"), "Should have owasp2017 tag");
        }
    }

    @Test
    void testGetRulesByCategory() {
        List<RuleInfo> injectionRules = Java2017SecurityRules.getRulesByCategory(
            Owasp2017Category.A1_INJECTION
        );

        assertNotNull(injectionRules);
        assertTrue(injectionRules.size() >= 3, "Should have at least 3 injection rules");

        for (RuleInfo rule : injectionRules) {
            assertEquals(Owasp2017Category.A1_INJECTION, rule.getOwaspCategory());
        }
    }

    @Test
    void testGetRulesByCategoryNull() {
        List<RuleInfo> rules = Java2017SecurityRules.getRulesByCategory(null);
        assertNotNull(rules);
        assertTrue(rules.isEmpty());
    }

    @Test
    void testGetRuleByKey() {
        RuleInfo rule = Java2017SecurityRules.getRuleByKey("java-owasp2017-a1-001");

        assertNotNull(rule);
        assertEquals("java-owasp2017-a1-001", rule.getRuleKey());
        assertTrue(rule.getName().contains("SQL"));
        assertEquals(Owasp2017Category.A1_INJECTION, rule.getOwaspCategory());
    }

    @Test
    void testGetRuleByKeyNotFound() {
        RuleInfo rule = Java2017SecurityRules.getRuleByKey("non-existent-rule");
        assertNull(rule);
    }

    @Test
    void testGetRuleByKeyNull() {
        RuleInfo rule = Java2017SecurityRules.getRuleByKey(null);
        assertNull(rule);
    }

    @Test
    void testGetRuleStatistics() {
        Map<Owasp2017Category, Long> stats = Java2017SecurityRules.getRuleStatistics();

        assertNotNull(stats);
        assertFalse(stats.isEmpty());

        // 驗證主要分類都有規則
        assertTrue(stats.containsKey(Owasp2017Category.A1_INJECTION));
        assertTrue(stats.containsKey(Owasp2017Category.A2_BROKEN_AUTHENTICATION));
        assertTrue(stats.containsKey(Owasp2017Category.A3_SENSITIVE_DATA_EXPOSURE));
        assertTrue(stats.containsKey(Owasp2017Category.A4_XML_EXTERNAL_ENTITIES));
        assertTrue(stats.containsKey(Owasp2017Category.A5_BROKEN_ACCESS_CONTROL));
    }

    @Test
    void testGetRuleCount() {
        int count = Java2017SecurityRules.getRuleCount();
        assertTrue(count >= 17, "Should have at least 17 rules");
        assertEquals(Java2017SecurityRules.getAllRules().size(), count);
    }

    @Test
    void testSqlInjectionRule() {
        RuleInfo rule = Java2017SecurityRules.getRuleByKey("java-owasp2017-a1-001");

        assertNotNull(rule);
        assertEquals("BLOCKER", rule.getSeverity());
        assertEquals(Owasp2017Category.A1_INJECTION, rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-89"));
        assertTrue(rule.getTags().contains("sql-injection"));
    }

    @Test
    void testXxeRule() {
        RuleInfo rule = Java2017SecurityRules.getRuleByKey("java-owasp2017-a4-001");

        assertNotNull(rule);
        assertEquals(Owasp2017Category.A4_XML_EXTERNAL_ENTITIES, rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-611"));
        assertTrue(rule.getTags().contains("xxe"));
    }

    @Test
    void testXssRule() {
        RuleInfo rule = Java2017SecurityRules.getRuleByKey("java-owasp2017-a7-001");

        assertNotNull(rule);
        assertEquals(Owasp2017Category.A7_CROSS_SITE_SCRIPTING, rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-79"));
        assertTrue(rule.getTags().contains("xss"));
    }

    @Test
    void testDeserializationRule() {
        RuleInfo rule = Java2017SecurityRules.getRuleByKey("java-owasp2017-a8-001");

        assertNotNull(rule);
        assertEquals("BLOCKER", rule.getSeverity());
        assertEquals(Owasp2017Category.A8_INSECURE_DESERIALIZATION, rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-502"));
    }

    @Test
    void testAllRulesHaveUniqueKeys() {
        List<RuleInfo> rules = Java2017SecurityRules.getAllRules();
        long uniqueKeys = rules.stream()
            .map(RuleInfo::getRuleKey)
            .distinct()
            .count();

        assertEquals(rules.size(), uniqueKeys, "All rule keys should be unique");
    }

    @Test
    void testAllRulesHaveTags() {
        List<RuleInfo> rules = Java2017SecurityRules.getAllRules();

        for (RuleInfo rule : rules) {
            assertFalse(rule.getTags().isEmpty(),
                "Rule " + rule.getRuleKey() + " should have tags");
            assertTrue(rule.getTags().contains("owasp"),
                "Rule " + rule.getRuleKey() + " should have 'owasp' tag");
            assertTrue(rule.getTags().contains("security"),
                "Rule " + rule.getRuleKey() + " should have 'security' tag");
            assertTrue(rule.getTags().contains("owasp2017"),
                "Rule " + rule.getRuleKey() + " should have 'owasp2017' tag");
        }
    }

    @Test
    void testAllRulesHaveRemediationCost() {
        List<RuleInfo> rules = Java2017SecurityRules.getAllRules();

        for (RuleInfo rule : rules) {
            assertNotNull(rule.getRemediationCost(),
                "Rule " + rule.getRuleKey() + " should have remediation cost");
            assertTrue(rule.getRemediationCost().matches("\\d+(min|h)"),
                "Remediation cost should be in format like '10min' or '1h'");
        }
    }

    @Test
    void testRulesImmutability() {
        List<RuleInfo> rules1 = Java2017SecurityRules.getAllRules();
        List<RuleInfo> rules2 = Java2017SecurityRules.getAllRules();

        // 驗證返回的是不可變列表
        assertThrows(UnsupportedOperationException.class, () -> {
            rules1.add(null);
        });

        // 驗證每次返回相同的規則
        assertEquals(rules1.size(), rules2.size());
    }

    @Test
    void test2017SpecificCategories() {
        // 測試 2017 特有的分類（A4 XXE 和 A7 XSS）
        List<RuleInfo> xxeRules = Java2017SecurityRules.getRulesByCategory(
            Owasp2017Category.A4_XML_EXTERNAL_ENTITIES
        );
        assertFalse(xxeRules.isEmpty(), "Should have XXE rules in 2017");

        List<RuleInfo> xssRules = Java2017SecurityRules.getRulesByCategory(
            Owasp2017Category.A7_CROSS_SITE_SCRIPTING
        );
        assertFalse(xssRules.isEmpty(), "Should have XSS rules in 2017");
    }

    @Test
    void testCoverageOfTop10Categories() {
        Map<Owasp2017Category, Long> stats = Java2017SecurityRules.getRuleStatistics();

        // 應該覆蓋大部分 OWASP 2017 Top 10 分類
        assertTrue(stats.size() >= 9, "Should cover at least 9 out of 10 categories");
    }
}
