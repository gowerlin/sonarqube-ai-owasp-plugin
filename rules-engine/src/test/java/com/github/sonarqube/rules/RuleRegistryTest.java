package com.github.sonarqube.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RuleRegistry 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.1)
 */
class RuleRegistryTest {

    private RuleRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new RuleRegistry();
    }

    @Test
    void testRegisterRule() {
        OwaspRule rule = createMockRule("rule-001", "A01", "2021", "java", true);
        registry.registerRule(rule);

        assertEquals(1, registry.getRuleCount());
        assertNotNull(registry.getRule("rule-001"));
        assertTrue(registry.isRegistered("rule-001"));
        assertTrue(registry.isEnabled("rule-001"));
    }

    @Test
    void testRegisterNullRule() {
        assertThrows(IllegalArgumentException.class, () -> registry.registerRule(null));
    }

    @Test
    void testRegisterRuleWithNullId() {
        OwaspRule rule = createMockRule(null, "A01", "2021", "java", true);
        assertThrows(IllegalArgumentException.class, () -> registry.registerRule(rule));
    }

    @Test
    void testRegisterMultipleRules() {
        OwaspRule rule1 = createMockRule("rule-001", "A01", "2021", "java", true);
        OwaspRule rule2 = createMockRule("rule-002", "A02", "2021", "javascript", true);
        OwaspRule rule3 = createMockRule("rule-003", "A01", "2025", "java", true);

        registry.registerRules(Arrays.asList(rule1, rule2, rule3));

        assertEquals(3, registry.getRuleCount());
        assertEquals(3, registry.getEnabledRuleCount());
    }

    @Test
    void testUnregisterRule() {
        OwaspRule rule = createMockRule("rule-001", "A01", "2021", "java", true);
        registry.registerRule(rule);

        assertTrue(registry.unregisterRule("rule-001"));
        assertEquals(0, registry.getRuleCount());
        assertFalse(registry.isRegistered("rule-001"));
    }

    @Test
    void testUnregisterNonexistentRule() {
        assertFalse(registry.unregisterRule("nonexistent"));
    }

    @Test
    void testGetRulesByCategory() {
        OwaspRule rule1 = createMockRule("rule-001", "A01", "2021", "java", true);
        OwaspRule rule2 = createMockRule("rule-002", "A01", "2021", "javascript", true);
        OwaspRule rule3 = createMockRule("rule-003", "A02", "2021", "java", true);

        registry.registerRules(Arrays.asList(rule1, rule2, rule3));

        List<OwaspRule> a01Rules = registry.getRulesByCategory("A01");
        assertEquals(2, a01Rules.size());

        List<OwaspRule> a02Rules = registry.getRulesByCategory("A02");
        assertEquals(1, a02Rules.size());
    }

    @Test
    void testGetRulesByLanguage() {
        OwaspRule rule1 = createMockRule("rule-001", "A01", "2021", "java", true);
        OwaspRule rule2 = createMockRule("rule-002", "A02", "2021", "java", true);
        OwaspRule rule3 = createMockRule("rule-003", "A01", "2021", "javascript", true);

        registry.registerRules(Arrays.asList(rule1, rule2, rule3));

        List<OwaspRule> javaRules = registry.getRulesByLanguage("java");
        assertEquals(2, javaRules.size());

        List<OwaspRule> jsRules = registry.getRulesByLanguage("JavaScript"); // 測試大小寫不敏感
        assertEquals(1, jsRules.size());
    }

    @Test
    void testGetRulesByVersion() {
        OwaspRule rule1 = createMockRule("rule-001", "A01", "2021", "java", true);
        OwaspRule rule2 = createMockRule("rule-002", "A02", "2021", "java", true);
        OwaspRule rule3 = createMockRule("rule-003", "A01", "2025", "java", true);

        registry.registerRules(Arrays.asList(rule1, rule2, rule3));

        List<OwaspRule> rules2021 = registry.getRulesByVersion("2021");
        assertEquals(2, rules2021.size());

        List<OwaspRule> rules2025 = registry.getRulesByVersion("2025");
        assertEquals(1, rules2025.size());
    }

    @Test
    void testEnableDisableRule() {
        OwaspRule rule = createMockRule("rule-001", "A01", "2021", "java", false);
        registry.registerRule(rule);

        assertFalse(registry.isEnabled("rule-001"));
        assertEquals(0, registry.getEnabledRuleCount());

        registry.enableRule("rule-001");
        assertTrue(registry.isEnabled("rule-001"));
        assertEquals(1, registry.getEnabledRuleCount());

        registry.disableRule("rule-001");
        assertFalse(registry.isEnabled("rule-001"));
        assertEquals(0, registry.getEnabledRuleCount());
    }

    @Test
    void testGetEnabledRules() {
        OwaspRule rule1 = createMockRule("rule-001", "A01", "2021", "java", true);
        OwaspRule rule2 = createMockRule("rule-002", "A02", "2021", "java", false);
        OwaspRule rule3 = createMockRule("rule-003", "A01", "2021", "java", true);

        registry.registerRules(Arrays.asList(rule1, rule2, rule3));

        List<OwaspRule> enabledRules = registry.getEnabledRules();
        assertEquals(2, enabledRules.size());
    }

    @Test
    void testGetAllRules() {
        OwaspRule rule1 = createMockRule("rule-001", "A01", "2021", "java", true);
        OwaspRule rule2 = createMockRule("rule-002", "A02", "2021", "java", false);

        registry.registerRules(Arrays.asList(rule1, rule2));

        List<OwaspRule> allRules = registry.getAllRules();
        assertEquals(2, allRules.size());
    }

    @Test
    void testClear() {
        OwaspRule rule1 = createMockRule("rule-001", "A01", "2021", "java", true);
        OwaspRule rule2 = createMockRule("rule-002", "A02", "2021", "java", true);

        registry.registerRules(Arrays.asList(rule1, rule2));
        assertEquals(2, registry.getRuleCount());

        registry.clear();
        assertEquals(0, registry.getRuleCount());
        assertEquals(0, registry.getEnabledRuleCount());
    }

    @Test
    void testGetStatistics() {
        OwaspRule rule1 = createMockRule("rule-001", "A01", "2021", "java", true);
        OwaspRule rule2 = createMockRule("rule-002", "A02", "2021", "javascript", true);
        OwaspRule rule3 = createMockRule("rule-003", "A01", "2025", "java", false);

        registry.registerRules(Arrays.asList(rule1, rule2, rule3));

        String stats = registry.getStatistics();
        assertTrue(stats.contains("Total: 3"));
        assertTrue(stats.contains("Enabled: 2"));
        assertTrue(stats.contains("Categories: 2")); // A01, A02
        assertTrue(stats.contains("Languages: 2")); // java, javascript
        assertTrue(stats.contains("Versions: 2")); // 2021, 2025
    }

    @Test
    void testRemoveFromIndexWhenUnregister() {
        OwaspRule rule = createMockRule("rule-001", "A01", "2021", "java", true);
        registry.registerRule(rule);

        assertEquals(1, registry.getRulesByCategory("A01").size());
        assertEquals(1, registry.getRulesByLanguage("java").size());
        assertEquals(1, registry.getRulesByVersion("2021").size());

        registry.unregisterRule("rule-001");

        assertEquals(0, registry.getRulesByCategory("A01").size());
        assertEquals(0, registry.getRulesByLanguage("java").size());
        assertEquals(0, registry.getRulesByVersion("2021").size());
    }

    // === Helper Methods ===

    private OwaspRule createMockRule(String ruleId, String category, String version, String language, boolean enabled) {
        OwaspRule rule = mock(OwaspRule.class);
        when(rule.getRuleId()).thenReturn(ruleId);
        when(rule.getOwaspCategory()).thenReturn(category);
        when(rule.getOwaspVersion()).thenReturn(version);
        when(rule.getSupportedLanguages()).thenReturn(List.of(language));
        when(rule.isEnabled()).thenReturn(enabled);
        return rule;
    }
}
