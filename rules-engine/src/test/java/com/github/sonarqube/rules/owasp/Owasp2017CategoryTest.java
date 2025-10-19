package com.github.sonarqube.rules.owasp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Owasp2017Category 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
class Owasp2017CategoryTest {

    @Test
    void testEnumValues() {
        Owasp2017Category[] categories = Owasp2017Category.values();

        assertEquals(10, categories.length, "Should have 10 OWASP 2017 categories");
    }

    @Test
    void testCategoryIds() {
        assertEquals("A1:2017", Owasp2017Category.A1_INJECTION.getCategoryId());
        assertEquals("A2:2017", Owasp2017Category.A2_BROKEN_AUTHENTICATION.getCategoryId());
        assertEquals("A3:2017", Owasp2017Category.A3_SENSITIVE_DATA_EXPOSURE.getCategoryId());
        assertEquals("A4:2017", Owasp2017Category.A4_XML_EXTERNAL_ENTITIES.getCategoryId());
        assertEquals("A5:2017", Owasp2017Category.A5_BROKEN_ACCESS_CONTROL.getCategoryId());
        assertEquals("A6:2017", Owasp2017Category.A6_SECURITY_MISCONFIGURATION.getCategoryId());
        assertEquals("A7:2017", Owasp2017Category.A7_CROSS_SITE_SCRIPTING.getCategoryId());
        assertEquals("A8:2017", Owasp2017Category.A8_INSECURE_DESERIALIZATION.getCategoryId());
        assertEquals("A9:2017", Owasp2017Category.A9_USING_COMPONENTS_WITH_KNOWN_VULNERABILITIES.getCategoryId());
        assertEquals("A10:2017", Owasp2017Category.A10_INSUFFICIENT_LOGGING_MONITORING.getCategoryId());
    }

    @Test
    void testBilingualNames() {
        Owasp2017Category injection = Owasp2017Category.A1_INJECTION;

        assertEquals("Injection", injection.getNameEn());
        assertEquals("注入攻擊", injection.getNameZh());
        assertNotNull(injection.getDescription());
        assertTrue(injection.getDescription().length() > 20);
    }

    @Test
    void testGetFullName() {
        Owasp2017Category category = Owasp2017Category.A1_INJECTION;

        String fullName = category.getFullName();
        assertTrue(fullName.contains("A1:2017"));
        assertTrue(fullName.contains("Injection"));
        assertTrue(fullName.contains("注入攻擊"));
    }

    @Test
    void testFromCategoryId() {
        // 測試完整 ID
        assertEquals(Owasp2017Category.A1_INJECTION,
            Owasp2017Category.fromCategoryId("A1:2017"));

        // 測試簡短 ID
        assertEquals(Owasp2017Category.A1_INJECTION,
            Owasp2017Category.fromCategoryId("A1"));

        // 測試其他分類
        assertEquals(Owasp2017Category.A5_BROKEN_ACCESS_CONTROL,
            Owasp2017Category.fromCategoryId("A5:2017"));

        assertEquals(Owasp2017Category.A10_INSUFFICIENT_LOGGING_MONITORING,
            Owasp2017Category.fromCategoryId("A10"));
    }

    @Test
    void testFromCategoryIdNull() {
        assertNull(Owasp2017Category.fromCategoryId(null));
    }

    @Test
    void testFromCategoryIdInvalid() {
        assertNull(Owasp2017Category.fromCategoryId("A99:2017"));
        assertNull(Owasp2017Category.fromCategoryId("Invalid"));
    }

    @Test
    void testIsRemovedIn2021() {
        // A4 (XXE) 和 A7 (XSS) 在 2021 被併入其他分類
        assertTrue(Owasp2017Category.A4_XML_EXTERNAL_ENTITIES.isRemovedIn2021());
        assertTrue(Owasp2017Category.A7_CROSS_SITE_SCRIPTING.isRemovedIn2021());

        // 其他分類沒有被移除
        assertFalse(Owasp2017Category.A1_INJECTION.isRemovedIn2021());
        assertFalse(Owasp2017Category.A2_BROKEN_AUTHENTICATION.isRemovedIn2021());
        assertFalse(Owasp2017Category.A3_SENSITIVE_DATA_EXPOSURE.isRemovedIn2021());
        assertFalse(Owasp2017Category.A5_BROKEN_ACCESS_CONTROL.isRemovedIn2021());
    }

    @Test
    void testGetEquivalent2021Category() {
        // 測試 A1 Injection → A03
        assertEquals(Owasp2021Category.A03_INJECTION,
            Owasp2017Category.A1_INJECTION.getEquivalent2021Category());

        // 測試 A2 Broken Authentication → A07
        assertEquals(Owasp2021Category.A07_IDENTIFICATION_AUTHENTICATION_FAILURES,
            Owasp2017Category.A2_BROKEN_AUTHENTICATION.getEquivalent2021Category());

        // 測試 A3 Sensitive Data Exposure → A02
        assertEquals(Owasp2021Category.A02_CRYPTOGRAPHIC_FAILURES,
            Owasp2017Category.A3_SENSITIVE_DATA_EXPOSURE.getEquivalent2021Category());

        // 測試 A4 XXE → A05
        assertEquals(Owasp2021Category.A05_SECURITY_MISCONFIGURATION,
            Owasp2017Category.A4_XML_EXTERNAL_ENTITIES.getEquivalent2021Category());

        // 測試 A5 Broken Access Control → A01
        assertEquals(Owasp2021Category.A01_BROKEN_ACCESS_CONTROL,
            Owasp2017Category.A5_BROKEN_ACCESS_CONTROL.getEquivalent2021Category());

        // 測試 A6 Security Misconfiguration → A05
        assertEquals(Owasp2021Category.A05_SECURITY_MISCONFIGURATION,
            Owasp2017Category.A6_SECURITY_MISCONFIGURATION.getEquivalent2021Category());

        // 測試 A7 XSS → A03
        assertEquals(Owasp2021Category.A03_INJECTION,
            Owasp2017Category.A7_CROSS_SITE_SCRIPTING.getEquivalent2021Category());

        // 測試 A8 Insecure Deserialization → A08
        assertEquals(Owasp2021Category.A08_SOFTWARE_DATA_INTEGRITY_FAILURES,
            Owasp2017Category.A8_INSECURE_DESERIALIZATION.getEquivalent2021Category());

        // 測試 A9 Components → A06
        assertEquals(Owasp2021Category.A06_VULNERABLE_OUTDATED_COMPONENTS,
            Owasp2017Category.A9_USING_COMPONENTS_WITH_KNOWN_VULNERABILITIES.getEquivalent2021Category());

        // 測試 A10 Logging → A09
        assertEquals(Owasp2021Category.A09_SECURITY_LOGGING_MONITORING_FAILURES,
            Owasp2017Category.A10_INSUFFICIENT_LOGGING_MONITORING.getEquivalent2021Category());
    }

    @Test
    void testAllCategoriesHave2021Mapping() {
        // 驗證所有 2017 分類都能映射到 2021
        for (Owasp2017Category category : Owasp2017Category.values()) {
            assertNotNull(category.getEquivalent2021Category(),
                "Category " + category.getCategoryId() + " should have 2021 equivalent");
        }
    }

    @Test
    void testToString() {
        Owasp2017Category category = Owasp2017Category.A1_INJECTION;

        String string = category.toString();
        assertTrue(string.contains("A1:2017"));
        assertTrue(string.contains("Injection"));
        assertTrue(string.contains("注入攻擊"));
    }

    @Test
    void testXxeCategory() {
        // XXE 是 2017 的獨立分類
        Owasp2017Category xxe = Owasp2017Category.A4_XML_EXTERNAL_ENTITIES;

        assertEquals("A4:2017", xxe.getCategoryId());
        assertEquals("XML External Entities (XXE)", xxe.getNameEn());
        assertTrue(xxe.isRemovedIn2021());

        // 在 2021 被併入 Security Misconfiguration
        assertEquals(Owasp2021Category.A05_SECURITY_MISCONFIGURATION,
            xxe.getEquivalent2021Category());
    }

    @Test
    void testXssCategory() {
        // XSS 是 2017 的獨立分類
        Owasp2017Category xss = Owasp2017Category.A7_CROSS_SITE_SCRIPTING;

        assertEquals("A7:2017", xss.getCategoryId());
        assertEquals("Cross-Site Scripting (XSS)", xss.getNameEn());
        assertTrue(xss.isRemovedIn2021());

        // 在 2021 被併入 Injection
        assertEquals(Owasp2021Category.A03_INJECTION,
            xss.getEquivalent2021Category());
    }

    @Test
    void testAllCategoriesHaveDescriptions() {
        for (Owasp2017Category category : Owasp2017Category.values()) {
            assertNotNull(category.getDescription(),
                "Category " + category.getCategoryId() + " should have description");
            assertTrue(category.getDescription().length() >= 20,
                "Description should be detailed");
        }
    }

    @Test
    void testCategoryNaming() {
        // 驗證 2017 分類的命名規則
        for (Owasp2017Category category : Owasp2017Category.values()) {
            assertTrue(category.getCategoryId().matches("A\\d+:2017"),
                "Category ID should match pattern A#:2017");

            assertNotNull(category.getNameEn());
            assertNotNull(category.getNameZh());
            assertTrue(category.getNameEn().length() > 0);
            assertTrue(category.getNameZh().length() > 0);
        }
    }
}
