package com.github.sonarqube.rules.javascript;

import com.github.sonarqube.rules.RuleDefinition;
import com.github.sonarqube.rules.RuleDefinition.RuleSeverity;
import com.github.sonarqube.rules.owasp.Owasp2021Category;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JavaScriptSecurityRules 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
class JavaScriptSecurityRulesTest {

    @Test
    void testGetAllRules() {
        List<RuleDefinition> rules = JavaScriptSecurityRules.getAllRules();

        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        assertTrue(rules.size() >= 21, "Should have at least 21 JavaScript security rules");
    }

    @Test
    void testAllRulesHaveRequiredFields() {
        List<RuleDefinition> rules = JavaScriptSecurityRules.getAllRules();

        for (RuleDefinition rule : rules) {
            assertNotNull(rule.getRuleKey(), "Rule key should not be null");
            assertNotNull(rule.getName(), "Rule name should not be null");
            assertNotNull(rule.getDescription(), "Rule description should not be null");
            assertNotNull(rule.getSeverity(), "Severity should not be null");
            assertNotNull(rule.getType(), "Type should not be null");
            assertEquals("javascript", rule.getLanguage(), "Language should be javascript");
            assertNotNull(rule.getOwaspCategory(), "OWASP category should not be null");
            assertFalse(rule.getCweIds().isEmpty(), "Should have at least one CWE ID");
        }
    }

    @Test
    void testGetRulesByCategory() {
        List<RuleDefinition> injectionRules = JavaScriptSecurityRules.getRulesByCategory(
            Owasp2021Category.A03_INJECTION
        );

        assertNotNull(injectionRules);
        assertTrue(injectionRules.size() >= 4, "Should have at least 4 injection rules");

        for (RuleDefinition rule : injectionRules) {
            assertEquals(Owasp2021Category.A03_INJECTION, rule.getOwaspCategory());
        }
    }

    @Test
    void testGetRulesByCategoryNull() {
        List<RuleDefinition> rules = JavaScriptSecurityRules.getRulesByCategory(null);
        assertNotNull(rules);
        assertTrue(rules.isEmpty());
    }

    @Test
    void testGetRulesBySeverity() {
        List<RuleDefinition> blockerRules = JavaScriptSecurityRules.getRulesBySeverity(
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
        List<RuleDefinition> rules = JavaScriptSecurityRules.getRulesBySeverity(null);
        assertNotNull(rules);
        assertTrue(rules.isEmpty());
    }

    @Test
    void testGetRuleByKey() {
        RuleDefinition rule = JavaScriptSecurityRules.getRuleByKey("js-owasp-a03-001");

        assertNotNull(rule);
        assertEquals("js-owasp-a03-001", rule.getRuleKey());
        assertTrue(rule.getName().contains("XSS") || rule.getName().contains("跨站"));
        assertEquals(Owasp2021Category.A03_INJECTION, rule.getOwaspCategory());
    }

    @Test
    void testGetRuleByKeyNotFound() {
        RuleDefinition rule = JavaScriptSecurityRules.getRuleByKey("non-existent-rule");
        assertNull(rule);
    }

    @Test
    void testGetRuleByKeyNull() {
        RuleDefinition rule = JavaScriptSecurityRules.getRuleByKey(null);
        assertNull(rule);
    }

    @Test
    void testGetRuleStatistics() {
        Map<Owasp2021Category, Long> stats = JavaScriptSecurityRules.getRuleStatistics();

        assertNotNull(stats);
        assertFalse(stats.isEmpty());

        // 驗證每個主要分類至少有一個規則
        assertTrue(stats.containsKey(Owasp2021Category.A01_BROKEN_ACCESS_CONTROL));
        assertTrue(stats.containsKey(Owasp2021Category.A02_CRYPTOGRAPHIC_FAILURES));
        assertTrue(stats.containsKey(Owasp2021Category.A03_INJECTION));
        assertTrue(stats.containsKey(Owasp2021Category.A07_IDENTIFICATION_AUTHENTICATION_FAILURES));
        assertTrue(stats.containsKey(Owasp2021Category.A08_SOFTWARE_DATA_INTEGRITY_FAILURES));
        assertTrue(stats.containsKey(Owasp2021Category.A10_SERVER_SIDE_REQUEST_FORGERY));
    }

    @Test
    void testGetRuleCount() {
        int count = JavaScriptSecurityRules.getRuleCount();
        assertTrue(count >= 21, "Should have at least 21 rules");
        assertEquals(JavaScriptSecurityRules.getAllRules().size(), count);
    }

    @Test
    void testXssRule() {
        RuleDefinition rule = JavaScriptSecurityRules.getRuleByKey("js-owasp-a03-001");

        assertNotNull(rule);
        assertEquals(RuleSeverity.BLOCKER, rule.getSeverity());
        assertEquals(Owasp2021Category.A03_INJECTION, rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-79"));
        assertTrue(rule.getTags().contains("xss"));
    }

    @Test
    void testNoSqlInjectionRule() {
        RuleDefinition rule = JavaScriptSecurityRules.getRuleByKey("js-owasp-a03-002");

        assertNotNull(rule);
        assertEquals(RuleSeverity.BLOCKER, rule.getSeverity());
        assertEquals(Owasp2021Category.A03_INJECTION, rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-943"));
    }

    @Test
    void testEvalUsageRule() {
        RuleDefinition rule = JavaScriptSecurityRules.getRuleByKey("js-owasp-a03-004");

        assertNotNull(rule);
        assertEquals(RuleSeverity.CRITICAL, rule.getSeverity());
        assertEquals(Owasp2021Category.A03_INJECTION, rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-95"));
    }

    @Test
    void testPrototypePollutionRule() {
        RuleDefinition rule = JavaScriptSecurityRules.getRuleByKey("js-owasp-a04-002");

        assertNotNull(rule);
        assertEquals(Owasp2021Category.A04_INSECURE_DESIGN, rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-1321"));
    }

    @Test
    void testSsrfRule() {
        RuleDefinition rule = JavaScriptSecurityRules.getRuleByKey("js-owasp-a10-001");

        assertNotNull(rule);
        assertEquals(Owasp2021Category.A10_SERVER_SIDE_REQUEST_FORGERY, rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-918"));
    }

    @Test
    void testJwtWeakKeyRule() {
        RuleDefinition rule = JavaScriptSecurityRules.getRuleByKey("js-owasp-a07-001");

        assertNotNull(rule);
        assertEquals(RuleSeverity.BLOCKER, rule.getSeverity());
        assertEquals(Owasp2021Category.A07_IDENTIFICATION_AUTHENTICATION_FAILURES,
            rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-798"));
    }

    @Test
    void testAllRulesHaveUniqueKeys() {
        List<RuleDefinition> rules = JavaScriptSecurityRules.getAllRules();
        long uniqueKeys = rules.stream()
            .map(RuleDefinition::getRuleKey)
            .distinct()
            .count();

        assertEquals(rules.size(), uniqueKeys, "All rule keys should be unique");
    }

    @Test
    void testAllRulesHaveTags() {
        List<RuleDefinition> rules = JavaScriptSecurityRules.getAllRules();

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
        List<RuleDefinition> rules = JavaScriptSecurityRules.getAllRules();

        for (RuleDefinition rule : rules) {
            assertNotNull(rule.getRemediationCost(),
                "Rule " + rule.getRuleKey() + " should have remediation cost");
            assertTrue(rule.getRemediationCost().matches("\\d+(min|h)"),
                "Remediation cost should be in format like '10min' or '1h'");
        }
    }

    @Test
    void testRulesImmutability() {
        List<RuleDefinition> rules1 = JavaScriptSecurityRules.getAllRules();
        List<RuleDefinition> rules2 = JavaScriptSecurityRules.getAllRules();

        // 驗證返回的是不可變列表
        assertThrows(UnsupportedOperationException.class, () -> {
            rules1.add(null);
        });

        // 驗證每次返回相同的規則
        assertEquals(rules1.size(), rules2.size());
    }

    @Test
    void testVulnerableDependenciesRule() {
        RuleDefinition rule = JavaScriptSecurityRules.getRuleByKey("js-owasp-a06-001");

        assertNotNull(rule);
        assertEquals(Owasp2021Category.A06_VULNERABLE_OUTDATED_COMPONENTS,
            rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-1104"));
    }

    @Test
    void testCorsConfigurationRule() {
        RuleDefinition rule = JavaScriptSecurityRules.getRuleByKey("js-owasp-a05-001");

        assertNotNull(rule);
        assertEquals(Owasp2021Category.A05_SECURITY_MISCONFIGURATION,
            rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-942"));
    }

    @Test
    void testDeserializationRule() {
        RuleDefinition rule = JavaScriptSecurityRules.getRuleByKey("js-owasp-a08-001");

        assertNotNull(rule);
        assertEquals(RuleSeverity.BLOCKER, rule.getSeverity());
        assertEquals(Owasp2021Category.A08_SOFTWARE_DATA_INTEGRITY_FAILURES,
            rule.getOwaspCategory());
        assertTrue(rule.getCweIds().contains("CWE-502"));
    }
}
