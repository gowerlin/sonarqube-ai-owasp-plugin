package com.github.sonarqube.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CweMappingService 單元測試
 */
class CweMappingServiceTest {

    private CweMappingService service;

    @BeforeEach
    void setUp() {
        service = new CweMappingService();
    }

    @Test
    void testGetCwesByOwaspA01() {
        Set<String> cwes = service.getCwesByOwasp("A01");
        assertEquals(33, cwes.size());
        assertTrue(cwes.contains("CWE-22")); // Path Traversal
        assertTrue(cwes.contains("CWE-862")); // Missing Authorization
    }

    @Test
    void testGetCwesByOwaspA02() {
        Set<String> cwes = service.getCwesByOwasp("A02");
        assertEquals(29, cwes.size());
        assertTrue(cwes.contains("CWE-327")); // Broken Crypto
        assertTrue(cwes.contains("CWE-330")); // Weak Random
    }

    @Test
    void testGetCwesByOwaspA03() {
        Set<String> cwes = service.getCwesByOwasp("A03");
        assertEquals(33, cwes.size());
        assertTrue(cwes.contains("CWE-89")); // SQL Injection
        assertTrue(cwes.contains("CWE-79")); // XSS
    }

    @Test
    void testGetOwaspByCwe() {
        assertEquals("A01", service.getOwaspByCwe("CWE-22"));
        assertEquals("A02", service.getOwaspByCwe("CWE-327"));
        assertEquals("A03", service.getOwaspByCwe("CWE-89"));
        assertEquals("A10", service.getOwaspByCwe("CWE-918"));
    }

    @Test
    void testIsCweInOwasp() {
        assertTrue(service.isCweInOwasp("CWE-22", "A01"));
        assertTrue(service.isCweInOwasp("CWE-89", "A03"));
        assertFalse(service.isCweInOwasp("CWE-22", "A02"));
    }

    @Test
    void testGetAllOwaspCategories() {
        Set<String> categories = service.getAllOwaspCategories();
        assertEquals(10, categories.size());
        assertTrue(categories.contains("A01"));
        assertTrue(categories.contains("A10"));
    }

    @Test
    void testGetTotalCweCount() {
        // A01:33 + A02:29 + A03:33 + A04:40 + A05:20 + A06:2 + A07:22 + A08:10 + A09:4 + A10:1 = 194
        assertEquals(194, service.getTotalCweCount());
    }

    @Test
    void testStatistics() {
        String stats = service.getStatistics();
        assertTrue(stats.contains("10")); // 10 categories
        assertTrue(stats.contains("194")); // 194 CWEs
    }
}
