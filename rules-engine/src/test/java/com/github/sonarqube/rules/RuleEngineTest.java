package com.github.sonarqube.rules;

import com.github.sonarqube.ai.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RuleEngine 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.1)
 */
class RuleEngineTest {

    private RuleRegistry registry;
    private RuleEngine engine;

    @BeforeEach
    void setUp() {
        registry = new RuleRegistry();
        engine = new RuleEngine(registry, RuleEngine.ExecutionMode.SEQUENTIAL, 4);
    }

    @Test
    void testAnalyzeNoRules() {
        RuleEngine.AnalysisResult result = engine.analyze(
            "public class Test {}",
            "java",
            "2021",
            null
        );

        assertEquals(0, result.getTotalRulesExecuted());
        assertEquals(0, result.getTotalViolations());
        assertTrue(result.getExecutionTimeMs() >= 0);
    }

    @Test
    void testAnalyzeWithMatchingRules() {
        // 建立 mock 規則
        OwaspRule rule1 = createMockRule("rule-001", "java", "2021", true, false);
        OwaspRule rule2 = createMockRule("rule-002", "java", "2021", true, false);

        // 設定規則執行結果
        when(rule1.execute(any(RuleContext.class))).thenReturn(RuleResult.success("rule-001"));
        when(rule2.execute(any(RuleContext.class))).thenReturn(RuleResult.success("rule-002"));

        registry.registerRules(Arrays.asList(rule1, rule2));

        RuleEngine.AnalysisResult result = engine.analyze(
            "public class Test {}",
            "java",
            "2021",
            null
        );

        assertEquals(2, result.getTotalRulesExecuted());
        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        assertEquals(0, result.getTotalViolations());
    }

    @Test
    void testAnalyzeWithViolations() {
        OwaspRule rule = createMockRule("rule-001", "java", "2021", true, false);

        RuleResult.RuleViolation violation = RuleResult.RuleViolation.builder()
            .lineNumber(10)
            .message("Security issue")
            .severity(RuleDefinition.RuleSeverity.CRITICAL)
            .build();

        when(rule.execute(any(RuleContext.class)))
            .thenReturn(RuleResult.success("rule-001", List.of(violation)));

        registry.registerRule(rule);

        RuleEngine.AnalysisResult result = engine.analyze(
            "public class Test {}",
            "java",
            "2021",
            null
        );

        assertEquals(1, result.getTotalRulesExecuted());
        assertEquals(1, result.getTotalViolations());
        assertEquals(1, result.getAllViolations().size());
        assertEquals("Security issue", result.getAllViolations().get(0).getMessage());
    }

    @Test
    void testAnalyzeSkipAiRequiredRulesWithoutAiService() {
        OwaspRule normalRule = createMockRule("rule-001", "java", "2021", true, false);
        OwaspRule aiRule = createMockRule("rule-002", "java", "2021", true, true);

        when(normalRule.execute(any(RuleContext.class))).thenReturn(RuleResult.success("rule-001"));
        when(aiRule.execute(any(RuleContext.class))).thenReturn(RuleResult.success("rule-002"));

        registry.registerRules(Arrays.asList(normalRule, aiRule));

        RuleEngine.AnalysisResult result = engine.analyze(
            "public class Test {}",
            "java",
            "2021",
            null // 沒有 AI 服務
        );

        assertEquals(1, result.getTotalRulesExecuted()); // 只執行 normalRule
        verify(normalRule, times(1)).execute(any(RuleContext.class));
        verify(aiRule, never()).execute(any(RuleContext.class));
    }

    @Test
    void testAnalyzeExecuteAiRequiredRulesWithAiService() {
        AiService aiService = mock(AiService.class);
        OwaspRule aiRule = createMockRule("rule-001", "java", "2021", true, true);

        when(aiRule.execute(any(RuleContext.class))).thenReturn(RuleResult.success("rule-001"));

        registry.registerRule(aiRule);

        RuleEngine.AnalysisResult result = engine.analyze(
            "public class Test {}",
            "java",
            "2021",
            aiService // 有 AI 服務
        );

        assertEquals(1, result.getTotalRulesExecuted());
        verify(aiRule, times(1)).execute(any(RuleContext.class));
    }

    @Test
    void testAnalyzeSkipDisabledRules() {
        OwaspRule rule1 = createMockRule("rule-001", "java", "2021", true, false);
        OwaspRule rule2 = createMockRule("rule-002", "java", "2021", true, false);

        when(rule1.execute(any(RuleContext.class))).thenReturn(RuleResult.success("rule-001"));
        when(rule2.execute(any(RuleContext.class))).thenReturn(RuleResult.success("rule-002"));

        registry.registerRules(Arrays.asList(rule1, rule2));
        registry.disableRule("rule-002");

        RuleEngine.AnalysisResult result = engine.analyze(
            "public class Test {}",
            "java",
            "2021",
            null
        );

        assertEquals(1, result.getTotalRulesExecuted()); // 只執行 rule1
        verify(rule1, times(1)).execute(any(RuleContext.class));
        verify(rule2, never()).execute(any(RuleContext.class));
    }

    @Test
    void testAnalyzeLanguageFilter() {
        OwaspRule javaRule = createMockRule("rule-001", "java", "2021", true, false);
        OwaspRule jsRule = createMockRule("rule-002", "javascript", "2021", true, false);

        when(javaRule.execute(any(RuleContext.class))).thenReturn(RuleResult.success("rule-001"));
        when(jsRule.execute(any(RuleContext.class))).thenReturn(RuleResult.success("rule-002"));

        registry.registerRules(Arrays.asList(javaRule, jsRule));

        RuleEngine.AnalysisResult result = engine.analyze(
            "public class Test {}",
            "java",
            "2021",
            null
        );

        assertEquals(1, result.getTotalRulesExecuted()); // 只執行 javaRule
        verify(javaRule, times(1)).execute(any(RuleContext.class));
        verify(jsRule, never()).execute(any(RuleContext.class));
    }

    @Test
    void testAnalyzeVersionFilter() {
        OwaspRule rule2021 = createMockRule("rule-001", "java", "2021", true, false);
        OwaspRule rule2025 = createMockRule("rule-002", "java", "2025", true, false);

        when(rule2021.execute(any(RuleContext.class))).thenReturn(RuleResult.success("rule-001"));
        when(rule2025.execute(any(RuleContext.class))).thenReturn(RuleResult.success("rule-002"));

        registry.registerRules(Arrays.asList(rule2021, rule2025));

        RuleEngine.AnalysisResult result = engine.analyze(
            "public class Test {}",
            "java",
            "2021",
            null
        );

        assertEquals(1, result.getTotalRulesExecuted()); // 只執行 rule2021
        verify(rule2021, times(1)).execute(any(RuleContext.class));
        verify(rule2025, never()).execute(any(RuleContext.class));
    }

    @Test
    void testAnalyzeRuleExecutionFailure() {
        OwaspRule rule = createMockRule("rule-001", "java", "2021", true, false);

        when(rule.execute(any(RuleContext.class)))
            .thenThrow(new RuntimeException("Execution error"));

        registry.registerRule(rule);

        RuleEngine.AnalysisResult result = engine.analyze(
            "public class Test {}",
            "java",
            "2021",
            null
        );

        assertEquals(1, result.getTotalRulesExecuted());
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
    }

    @Test
    void testAnalysisResultStatistics() {
        OwaspRule rule1 = createMockRule("rule-001", "java", "2021", true, false);
        OwaspRule rule2 = createMockRule("rule-002", "java", "2021", true, false);

        RuleResult.RuleViolation violation1 = RuleResult.RuleViolation.builder()
            .lineNumber(5).message("Issue 1").build();
        RuleResult.RuleViolation violation2 = RuleResult.RuleViolation.builder()
            .lineNumber(10).message("Issue 2").build();

        when(rule1.execute(any(RuleContext.class)))
            .thenReturn(RuleResult.success("rule-001", List.of(violation1, violation2)));
        when(rule2.execute(any(RuleContext.class)))
            .thenReturn(RuleResult.failure("rule-002", "Error"));

        registry.registerRules(Arrays.asList(rule1, rule2));

        RuleEngine.AnalysisResult result = engine.analyze(
            "public class Test {}",
            "java",
            "2021",
            null
        );

        assertEquals(2, result.getTotalRulesExecuted());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(2, result.getTotalViolations());
    }

    @Test
    void testParallelExecutionMode() {
        RuleEngine parallelEngine = new RuleEngine(registry, RuleEngine.ExecutionMode.PARALLEL, 2);

        OwaspRule rule1 = createMockRule("rule-001", "java", "2021", true, false);
        OwaspRule rule2 = createMockRule("rule-002", "java", "2021", true, false);

        when(rule1.execute(any(RuleContext.class))).thenReturn(RuleResult.success("rule-001"));
        when(rule2.execute(any(RuleContext.class))).thenReturn(RuleResult.success("rule-002"));

        registry.registerRules(Arrays.asList(rule1, rule2));

        RuleEngine.AnalysisResult result = parallelEngine.analyze(
            "public class Test {}",
            "java",
            "2021",
            null
        );

        assertEquals(2, result.getTotalRulesExecuted());
        assertEquals(2, result.getSuccessCount());
    }

    @Test
    void testAnalysisResultToString() {
        RuleEngine.AnalysisResult result = RuleEngine.AnalysisResult.builder()
            .context(RuleContext.builder("code", "java").build())
            .executionTimeMs(100)
            .build();

        String str = result.toString();
        assertTrue(str.contains("totalRules=0"));
        assertTrue(str.contains("violations=0"));
        assertTrue(str.contains("executionTimeMs=100"));
    }

    // === Helper Methods ===

    private OwaspRule createMockRule(String ruleId, String language, String version, boolean matches, boolean requiresAi) {
        OwaspRule rule = mock(OwaspRule.class);
        when(rule.getRuleId()).thenReturn(ruleId);
        when(rule.getSupportedLanguages()).thenReturn(List.of(language));
        when(rule.getOwaspVersion()).thenReturn(version);
        when(rule.matches(any(RuleContext.class))).thenReturn(matches);
        when(rule.requiresAi()).thenReturn(requiresAi);
        when(rule.isEnabled()).thenReturn(true);
        return rule;
    }
}
