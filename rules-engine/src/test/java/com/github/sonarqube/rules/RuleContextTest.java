package com.github.sonarqube.rules;

import com.github.sonarqube.ai.AiService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RuleContext 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.1)
 */
class RuleContextTest {

    @Test
    void testBasicBuilder() {
        String code = "public class Test {}";
        String language = "java";

        RuleContext context = RuleContext.builder(code, language).build();

        assertEquals(code, context.getCode());
        assertEquals(language, context.getLanguage());
        assertEquals("2021", context.getOwaspVersion()); // 預設版本
        assertNull(context.getFileName());
        assertNull(context.getFilePath());
        assertNull(context.getAiService());
        assertFalse(context.hasAiService());
    }

    @Test
    void testFullBuilder() {
        String code = "public class Test {}";
        String language = "java";
        String fileName = "Test.java";
        AiService aiService = Mockito.mock(AiService.class);

        RuleContext context = RuleContext.builder(code, language)
            .fileName(fileName)
            .filePath(Paths.get("/path/to/Test.java"))
            .owaspVersion("2025")
            .aiService(aiService)
            .metadata("key1", "value1")
            .metadata("key2", 123)
            .build();

        assertEquals(code, context.getCode());
        assertEquals(language, context.getLanguage());
        assertEquals("2025", context.getOwaspVersion());
        assertEquals(fileName, context.getFileName());
        assertNotNull(context.getFilePath());
        assertEquals(aiService, context.getAiService());
        assertTrue(context.hasAiService());
        assertEquals("value1", context.getMetadata("key1"));
        assertEquals(123, context.getMetadata("key2"));
    }

    @Test
    void testMetadata() {
        RuleContext context = RuleContext.builder("code", "java")
            .metadata("key1", "value1")
            .metadata("key2", 123)
            .build();

        assertEquals("value1", context.getMetadata("key1"));
        assertEquals(123, context.getMetadata("key2"));
        assertNull(context.getMetadata("nonexistent"));
    }

    @Test
    void testMetadataWithDefault() {
        RuleContext context = RuleContext.builder("code", "java")
            .metadata("key1", "value1")
            .build();

        assertEquals("value1", context.getMetadata("key1", "default"));
        assertEquals("default", context.getMetadata("nonexistent", "default"));
        assertEquals(100, context.getMetadata("number", 100));
    }

    @Test
    void testMetadataMap() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", 123);

        RuleContext context = RuleContext.builder("code", "java")
            .metadata(metadata)
            .build();

        assertEquals("value1", context.getMetadata("key1"));
        assertEquals(123, context.getMetadata("key2"));
        assertEquals(2, context.getAllMetadata().size());
    }

    @Test
    void testNullCode() {
        assertThrows(NullPointerException.class, () ->
            RuleContext.builder(null, "java").build()
        );
    }

    @Test
    void testNullLanguage() {
        assertThrows(NullPointerException.class, () ->
            RuleContext.builder("code", null).build()
        );
    }

    @Test
    void testToString() {
        RuleContext context = RuleContext.builder("public class Test {}", "java")
            .fileName("Test.java")
            .owaspVersion("2021")
            .build();

        String str = context.toString();
        assertTrue(str.contains("java"));
        assertTrue(str.contains("Test.java"));
        assertTrue(str.contains("2021"));
    }
}
