package com.github.sonarqube.rules.java;

import com.github.sonarqube.rules.RuleDefinition;
import com.github.sonarqube.rules.RuleDefinition.RuleSeverity;
import com.github.sonarqube.rules.owasp.Owasp2021Category;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JavaSecurityRules 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
class JavaSecurityRulesTest {

    @Test
    void testGetAllRules() {
        List<RuleDefinition> rules = JavaSecurityRules.getAllRules();

        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        assertTrue(rules.size() >= 18, "Should have at least 18 Java security rules");
    }

    @Test
    void testAllRulesHaveRequiredFields() {
        List<RuleDefinition> rules = JavaSecurityRules.getAllRules();

        for (RuleDefinition rule : rules) {
            assertNotNull(rule.getRuleKey(), "Rule key should not be null");
            assertNotNull(rule.getName(), "Rule name should not be null");
            assertNotNull(rule.getDescription(), "Rule description should not be null");
            assertNotNull(rule.getSeverity(), "Severity should not be null");
            assertNotNull(rule.getType(), "Type should not be null");
            assertEquals("java", rule.getLanguage(), "Language should be java");
            assertNotNull(rule.getOwaspCategory(), "OWASP category should not be null");
            assertFalse(rule.getCweIds().isEmpty(), "Should have at least one CWE ID");
        }
    }

    @Test
    void testGetRulesByCategory() {
        List<RuleDefinition> injectionRules = JavaSecurityRules.getRulesByCategory(
            Owasp2021Category.A03_INJECTION
        );

        assertNotNull(injectionRules);
        assertTrue(injectionRules.size() >= 3, "Should have at least 3 injection rules");

        for (RuleDefinition rule : injectionRules) {
            assertEquals(Owasp2021Category.A03_INJECTION, rule.getOwaspCategory());
        }
    }

    @Test
    void testGetRulesByCategoryNull() {
        List<RuleDefinition> rules = JavaSecurityRules.getRulesByCategory(null);
        assertNotNull(rules);
        assertTrue(rules.isEmpty());
    }

    @Test
    void testGetRulesBySeverity() {
        List<RuleDefinition> blockerRules = JavaSecurityRules.getRulesBySeverity(
            RuleSeverity.BLOCKER
        );

        assertNotNull(blockerRules);
        assertFalse(blockerRules.isEmpty());

        for (RuleDefinition rule : blockerRules) {
            assertEquals(RuleSeverity.BLOCKER, rule.getSeverity());
        }
    }

    @Test
    void testGetRulesBySeverityNull() {
        List<RuleDefinition> rules = JavaSecurityRules.getRulesBySeverity(null);
        assertNotNull(rules);
        assertTrue(rules.isEmpty());
    }

    @Test
    void testGetRuleByKey() {
        RuleDefinition rule = JavaSecurityRules.getRuleByKey("java-owasp-a03-001");

        assertNotNull(rule);
        assertEquals("java-owasp-a03-001", rule.getRuleKey());
        assertTrue(rule.getName().contains("SQL"));
        assertEquals(Owasp2021Category.A03_INJECTION, rule.getOwaspCategory());
    }

    @Test
    void testGetRuleByKeyNotFound() {
        RuleDefinition rule = JavaSecurityRules.getRuleByKey("non-existent-rule");
        assertNull(rule);
    }

    @Test
    void testGetRuleByKeyNull() {
        RuleDefinition rule = JavaSecurityRules.getRuleByKey(null);
        assertNull(rule);
    }

    @Test
    void testGetRuleStatistics() {
        Map<Owasp2021Category, Long> stats = JavaSecurityRules.getRuleStatistics();

        assertNotNull(stats);
        assertFalse(stats.isEmpty());

        // 驗證每個分類至少有一個規則
        assertTrue(stats.containsKey(Owasp2021Category.A01_BROKEN_ACCESS_CONTROL));
        assertTrue(stats.containsKey(Owasp2021Category.A02_CRYPTOGRAPHIC_FAILURES));
        assertTrue(stats.containsKey(Owasp2021Category.A03_INJECTION));
        assertTrue(stats.containsKey(Owasp2021Category.A07_IDENTIFICATION_AUTHENTICATION_FAILURES));
        assertTrue(stats.containsKey(Owasp2021Category.A08_SOFTWARE_DATA_INTEGRITY_FAILURES));
        assertTrue(stats.containsKey(Owasp2021Category.A09_SECURITY_LOGGING_MONITORING_FAILURES));
        assertTrue(stats.containsKey(Owasp2021Category.A10_SERVER_SIDE_REQUEST_FORGERY));
    }

    @Test
    void testGetRuleCount() {
        int count = JavaSecurityRules.getRuleCount();
        assertTrue(count >= 18, "Should have at least 18 rules");
        assertEquals(JavaSecurityRules.getAllRules().size(), count);
    }

    @Test
    void testSqlInjectionRule() {
        RuleDefinition rule = JavaSecurityRules.getRuleByKey("java-owasp-a03-001");

        assertNotNull(rule);
        assertEquals(RuleSeverity.BLOCKER, rule.getSeverity());
        assertEquals(Owasp2021Category.A03_INJECTION, rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-89"));
        assertTrue(rule.getTags().contains("sql-injection"));
    }

    @Test
    void testHardcodedPasswordRule() {
        RuleDefinition rule = JavaSecurityRules.getRuleByKey("java-owasp-a07-001");

        assertNotNull(rule);
        assertEquals(RuleSeverity.BLOCKER, rule.getSeverity());
        assertEquals(Owasp2021Category.A07_IDENTIFICATION_AUTHENTICATION_FAILURES,
            rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-259"));
    }

    @Test
    void testDeserializationRule() {
        RuleDefinition rule = JavaSecurityRules.getRuleByKey("java-owasp-a08-001");

        assertNotNull(rule);
        assertEquals(RuleSeverity.BLOCKER, rule.getSeverity());
        assertEquals(Owasp2021Category.A08_SOFTWARE_DATA_INTEGRITY_FAILURES,
            rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-502"));
    }

    @Test
    void testXxeRule() {
        RuleDefinition rule = JavaSecurityRules.getRuleByKey("java-owasp-a05-001");

        assertNotNull(rule);
        assertEquals(Owasp2021Category.A05_SECURITY_MISCONFIGURATION,
            rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-611"));
    }

    @Test
    void testSsrfRule() {
        RuleDefinition rule = JavaSecurityRules.getRuleByKey("java-owasp-a10-001");

        assertNotNull(rule);
        assertEquals(Owasp2021Category.A10_SERVER_SIDE_REQUEST_FORGERY,
            rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-918"));
    }

    @Test
    void testAllRulesHaveUniqueKeys() {
        List<RuleDefinition> rules = JavaSecurityRules.getAllRules();
        long uniqueKeys = rules.stream()
            .map(RuleDefinition::getRuleKey)
            .distinct()
            .count();

        assertEquals(rules.size(), uniqueKeys, "All rule keys should be unique");
    }

    @Test
    void testAllRulesHaveTags() {
        List<RuleDefinition> rules = JavaSecurityRules.getAllRules();

        for (RuleDefinition rule : rules) {
            assertFalse(rule.getTags().isEmpty(),
                "Rule " + rule.getRuleKey() + " should have tags");
            assertTrue(rule.getTags().contains("owasp"),
                "Rule " + rule.getRuleKey() + " should have 'owasp' tag");
            assertTrue(rule.getTags().contains("security"),
                "Rule " + rule.getRuleKey() + " should have 'security' tag");
        }
    }

    @Test
    void testAllRulesHaveRemediationCost() {
        List<RuleDefinition> rules = JavaSecurityRules.getAllRules();

        for (RuleDefinition rule : rules) {
            assertNotNull(rule.getRemediationCost(),
                "Rule " + rule.getRuleKey() + " should have remediation cost");
            assertTrue(rule.getRemediationCost().matches("\\d+(min|h)"),
                "Remediation cost should be in format like '10min' or '1h'");
        }
    }

    @Test
    void testRulesImmutability() {
        List<RuleDefinition> rules1 = JavaSecurityRules.getAllRules();
        List<RuleDefinition> rules2 = JavaSecurityRules.getAllRules();

        // 驗證返回的是不可變列表
        assertThrows(UnsupportedOperationException.class, () -> {
            rules1.add(null);
        });

        // 驗證每次返回相同的規則
        assertEquals(rules1.size(), rules2.size());
    }
}
