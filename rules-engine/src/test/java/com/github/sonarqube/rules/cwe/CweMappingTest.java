package com.github.sonarqube.rules.cwe;

import com.github.sonarqube.rules.owasp.Owasp2021Category;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CweMapping 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
class CweMappingTest {

    @Test
    void testSqlInjectionMapping() {
        Owasp2021Category category = CweMapping.getOwaspCategory("CWE-89");
        assertEquals(Owasp2021Category.A03_INJECTION, category);
    }

    @Test
    void testXssMapping() {
        Owasp2021Category category = CweMapping.getOwaspCategory("CWE-79");
        assertEquals(Owasp2021Category.A03_INJECTION, category);
    }

    @Test
    void testBrokenAccessControlMapping() {
        Owasp2021Category category = CweMapping.getOwaspCategory("CWE-284");
        assertEquals(Owasp2021Category.A01_BROKEN_ACCESS_CONTROL, category);
    }

    @Test
    void testCryptographicFailuresMapping() {
        Owasp2021Category category = CweMapping.getOwaspCategory("CWE-327");
        assertEquals(Owasp2021Category.A02_CRYPTOGRAPHIC_FAILURES, category);
    }

    @Test
    void testCweIdWithoutPrefix() {
        Owasp2021Category category = CweMapping.getOwaspCategory("89");
        assertEquals(Owasp2021Category.A03_INJECTION, category);
    }

    @Test
    void testCweIdLowerCase() {
        Owasp2021Category category = CweMapping.getOwaspCategory("cwe-89");
        assertEquals(Owasp2021Category.A03_INJECTION, category);
    }

    @Test
    void testUnmappedCwe() {
        Owasp2021Category category = CweMapping.getOwaspCategory("CWE-99999");
        assertNull(category);
    }

    @Test
    void testNullCweId() {
        Owasp2021Category category = CweMapping.getOwaspCategory(null);
        assertNull(category);
    }

    @Test
    void testGetCwesByCategory() {
        List<String> injectionCwes = CweMapping.getCwesByCategory(Owasp2021Category.A03_INJECTION);

        assertNotNull(injectionCwes);
        assertFalse(injectionCwes.isEmpty());
        assertTrue(injectionCwes.contains("CWE-79")); // XSS
        assertTrue(injectionCwes.contains("CWE-89")); // SQL Injection
        assertTrue(injectionCwes.contains("CWE-78")); // OS Command Injection
    }

    @Test
    void testGetCwesByCategoryNull() {
        List<String> cwes = CweMapping.getCwesByCategory(null);
        assertNotNull(cwes);
        assertTrue(cwes.isEmpty());
    }

    @Test
    void testGetAllMappedCwes() {
        List<String> allCwes = CweMapping.getAllMappedCwes();

        assertNotNull(allCwes);
        assertFalse(allCwes.isEmpty());
        assertTrue(allCwes.size() > 100, "Should have over 100 CWE mappings");

        // 驗證排序
        for (int i = 0; i < allCwes.size() - 1; i++) {
            assertTrue(allCwes.get(i).compareTo(allCwes.get(i + 1)) <= 0,
                "List should be sorted");
        }
    }

    @Test
    void testGetMappingCount() {
        int count = CweMapping.getMappingCount();
        assertTrue(count > 100, "Should have over 100 mappings");
        assertTrue(count < 300, "Should have less than 300 mappings");
    }

    @Test
    void testIsMapped() {
        assertTrue(CweMapping.isMapped("CWE-89"));
        assertTrue(CweMapping.isMapped("89"));
        assertFalse(CweMapping.isMapped("CWE-99999"));
        assertFalse(CweMapping.isMapped(null));
    }

    @Test
    void testAllOwasp2021CategoriesHaveMappings() {
        for (Owasp2021Category category : Owasp2021Category.values()) {
            List<String> cwes = CweMapping.getCwesByCategory(category);
            assertFalse(cwes.isEmpty(),
                "Category " + category.getCategoryId() + " should have CWE mappings");
        }
    }

    @Test
    void testSsrfMapping() {
        // SSRF 是 2021 新增類別
        Owasp2021Category category = CweMapping.getOwaspCategory("CWE-918");
        assertEquals(Owasp2021Category.A10_SERVER_SIDE_REQUEST_FORGERY, category);
    }

    @Test
    void testDeserializationMapping() {
        // 不安全的反序列化歸類到 A08
        Owasp2021Category category = CweMapping.getOwaspCategory("CWE-502");
        assertEquals(Owasp2021Category.A08_SOFTWARE_DATA_INTEGRITY_FAILURES, category);
    }

    @Test
    void testXxeMapping() {
        // XXE 歸類到 A05
        Owasp2021Category category = CweMapping.getOwaspCategory("CWE-611");
        assertEquals(Owasp2021Category.A05_SECURITY_MISCONFIGURATION, category);
    }

    @Test
    void testCsrfMapping() {
        // CSRF 歸類到 A01
        Owasp2021Category category = CweMapping.getOwaspCategory("CWE-352");
        assertEquals(Owasp2021Category.A01_BROKEN_ACCESS_CONTROL, category);
    }

    @Test
    void testInsufficientLoggingMapping() {
        Owasp2021Category category = CweMapping.getOwaspCategory("CWE-778");
        assertEquals(Owasp2021Category.A09_SECURITY_LOGGING_MONITORING_FAILURES, category);
    }

    @Test
    void testVulnerableComponentsMapping() {
        Owasp2021Category category = CweMapping.getOwaspCategory("CWE-1104");
        assertEquals(Owasp2021Category.A06_VULNERABLE_OUTDATED_COMPONENTS, category);
    }

    @Test
    void testWeakPasswordMapping() {
        Owasp2021Category category = CweMapping.getOwaspCategory("CWE-521");
        assertEquals(Owasp2021Category.A07_IDENTIFICATION_AUTHENTICATION_FAILURES, category);
    }

    @Test
    void testInsecureDesignMapping() {
        // 測試 A04 的代表性 CWE
        Owasp2021Category category = CweMapping.getOwaspCategory("CWE-209");
        assertEquals(Owasp2021Category.A04_INSECURE_DESIGN, category);
    }

    @Test
    void testCategoryWithMostCwes() {
        int maxCount = 0;
        Owasp2021Category categoryWithMost = null;

        for (Owasp2021Category category : Owasp2021Category.values()) {
            int count = CweMapping.getCwesByCategory(category).size();
            if (count > maxCount) {
                maxCount = count;
                categoryWithMost = category;
            }
        }

        assertNotNull(categoryWithMost);
        assertTrue(maxCount > 10, "The category with most CWEs should have more than 10");
    }
}
