package com.github.sonarqube.rules.owasp2021;

import com.github.sonarqube.rules.RuleContext;
import com.github.sonarqube.rules.RuleDefinition;
import com.github.sonarqube.rules.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BrokenAccessControlRule 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.2)
 */
class BrokenAccessControlRuleTest {

    private BrokenAccessControlRule rule;

    @BeforeEach
    void setUp() {
        rule = new BrokenAccessControlRule();
    }

    @Test
    void testRuleMetadata() {
        assertEquals("owasp-2021-a01-001", rule.getRuleId());
        assertEquals("Broken Access Control Detection", rule.getRuleName());
        assertEquals("A01", rule.getOwaspCategory());
        assertEquals("2021", rule.getOwaspVersion());
        assertEquals(RuleDefinition.RuleSeverity.CRITICAL, rule.getDefaultSeverity());
        assertEquals(RuleDefinition.RuleType.VULNERABILITY, rule.getRuleType());
        assertFalse(rule.requiresAi());
    }

    @Test
    void testCweMapping() {
        assertEquals(33, rule.getCweIds().size());
        assertTrue(rule.getCweIds().contains("CWE-22")); // Path Traversal
        assertTrue(rule.getCweIds().contains("CWE-284")); // Improper Access Control
        assertTrue(rule.getCweIds().contains("CWE-601")); // URL Redirection to Untrusted Site
        assertTrue(rule.getCweIds().contains("CWE-862")); // Missing Authorization
        assertTrue(rule.getCweIds().contains("CWE-863")); // Incorrect Authorization
    }

    @Test
    void testDetectPathTraversal() {
        String code = "String filename = request.getParameter(\"file\");\n" +
                     "File file = new File(\"/uploads/\" + filename); // Vulnerable: ../etc/passwd\n" +
                     "InputStream is = new FileInputStream(file);";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());
        assertTrue(result.getViolationCount() >= 1);

        boolean hasPathTraversalViolation = result.getViolations().stream()
            .anyMatch(v -> v.getMessage().contains("Path Traversal") ||
                          v.getMessage().contains("Unsafe file operation"));
        assertTrue(hasPathTraversalViolation);
    }

    @Test
    void testDetectEncodedPathTraversal() {
        String code = "String path = \"/uploads/\" + params.get(\"file\"); // %2e%2e%2f encoded\n" +
                     "if (path.contains(\"%2e%2e%2f\") || path.contains(\"..%2F\")) {\n" +
                     "    return new File(path);\n" +
                     "}";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());
    }

    @Test
    void testDetectUnsafeFileOperations() {
        String code = "@GetMapping(\"/download\")\n" +
                     "public void downloadFile(HttpServletRequest request, HttpServletResponse response) {\n" +
                     "    String filename = request.getParameter(\"file\");\n" +
                     "    File file = new File(\"/data/\" + filename); // Vulnerable\n" +
                     "    Files.copy(file.toPath(), response.getOutputStream());\n" +
                     "}";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());

        boolean hasFileOpViolation = result.getViolations().stream()
            .anyMatch(v -> v.getMessage().contains("Unsafe file operation") ||
                          v.getMessage().contains("user input"));
        assertTrue(hasFileOpViolation);
    }

    @Test
    void testDetectInsecureDirectObjectReference() {
        String code = "@GetMapping(\"/user/{id}\")\n" +
                     "public User getUser(@PathVariable String id) {\n" +
                     "    String sql = \"SELECT * FROM users WHERE id = '\" + id + \"'\"; // Vulnerable IDOR\n" +
                     "    return jdbc.query(sql, userMapper);\n" +
                     "}";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());

        boolean hasIdorViolation = result.getViolations().stream()
            .anyMatch(v -> v.getMessage().contains("Insecure Direct Object Reference") ||
                          v.getMessage().contains("authorization check"));
        assertTrue(hasIdorViolation);
    }

    @Test
    void testDetectMissingAuthorization() {
        String code = "@PostMapping(\"/admin/deleteUser\")\n" + // Missing @PreAuthorize
                     "public ResponseEntity deleteUser(@RequestParam Long userId) {\n" +
                     "    userService.delete(userId);\n" +
                     "    return ResponseEntity.ok().build();\n" +
                     "}";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());

        boolean hasMissingAuthViolation = result.getViolations().stream()
            .anyMatch(v -> v.getMessage().contains("Missing authorization check") ||
                          v.getMessage().contains("@PreAuthorize"));
        assertTrue(hasMissingAuthViolation);
    }

    @Test
    void testNoViolationWithProperAuthorization() {
        String code = "@PreAuthorize(\"hasRole('ADMIN')\")\n" +
                     "@PostMapping(\"/admin/deleteUser\")\n" +
                     "public ResponseEntity deleteUser(@RequestParam Long userId) {\n" +
                     "    userService.delete(userId);\n" +
                     "    return ResponseEntity.ok().build();\n" +
                     "}";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        // Should have no violations because @PreAuthorize is present
        long authViolations = result.getViolations().stream()
            .filter(v -> v.getMessage().contains("Missing authorization"))
            .count();
        assertEquals(0, authViolations);
    }

    @Test
    void testDetectUnsafeRedirect() {
        String code = "@GetMapping(\"/redirect\")\n" +
                     "public void redirect(HttpServletRequest request, HttpServletResponse response) {\n" +
                     "    String url = request.getParameter(\"url\");\n" +
                     "    response.sendRedirect(url); // Vulnerable open redirect\n" +
                     "}";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());

        boolean hasRedirectViolation = result.getViolations().stream()
            .anyMatch(v -> v.getMessage().contains("Unsafe redirect") ||
                          v.getMessage().contains("CWE-601"));
        assertTrue(hasRedirectViolation);
    }

    @Test
    void testCleanCodeNoViolations() {
        String code = "public class SecureFileHandler {\n" +
                     "    private static final Path UPLOAD_DIR = Paths.get(\"/secure/uploads\");\n" +
                     "    \n" +
                     "    @PreAuthorize(\"hasRole('USER')\")\n" +
                     "    @GetMapping(\"/download\")\n" +
                     "    public void downloadFile(@RequestParam String fileId) {\n" +
                     "        // Lookup file by ID, not by user-controlled path\n" +
                     "        FileMetadata metadata = fileRepository.findById(fileId);\n" +
                     "        if (metadata != null && hasPermission(metadata)) {\n" +
                     "            Path file = UPLOAD_DIR.resolve(metadata.getStorageName());\n" +
                     "            // Secure file handling\n" +
                     "        }\n" +
                     "    }\n" +
                     "}";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getViolationCount());
    }

    @Test
    void testMultipleViolationsInSameCode() {
        String code = "@GetMapping(\"/api/file\")\n" +
                     "public void processFile(HttpServletRequest request, HttpServletResponse response) {\n" +
                     "    String filename = request.getParameter(\"file\");\n" +
                     "    File file = new File(\"/data/\" + filename); // Violation 1: Unsafe file operation\n" +
                     "    String userId = request.getParameter(\"uid\");\n" +
                     "    String sql = \"SELECT * FROM users WHERE id = \" + userId; // Violation 2: IDOR\n" +
                     "    String redirectUrl = request.getParameter(\"next\");\n" +
                     "    response.sendRedirect(redirectUrl); // Violation 3: Unsafe redirect\n" +
                     "}";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());
        assertTrue(result.getViolationCount() >= 3); // At least 3 different violation types
    }

    @Test
    void testMatchesReturnsTrueForRelevantCode() {
        String code = "File file = new File(request.getParameter(\"path\"));";
        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        assertTrue(rule.matches(context));
    }

    @Test
    void testMatchesReturnsFalseForIrrelevantCode() {
        String code = "int x = 1 + 2;\n" +
                     "System.out.println(\"Hello World\");";
        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        assertFalse(rule.matches(context));
    }

    @Test
    void testMatchesReturnsFalseForWrongLanguage() {
        String code = "File file = new File(request.getParameter(\"path\"));";
        RuleContext context = RuleContext.builder(code, "javascript")
            .owaspVersion("2021")
            .build();

        assertFalse(rule.matches(context));
    }

    @Test
    void testMatchesReturnsFalseForWrongOwaspVersion() {
        String code = "File file = new File(request.getParameter(\"path\"));";
        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2017")
            .build();

        assertFalse(rule.matches(context));
    }

    @Test
    void testViolationContainsFixSuggestion() {
        String code = "File file = new File(\"/uploads/\" + request.getParameter(\"file\"));";
        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.hasViolations());
        RuleResult.RuleViolation violation = result.getViolations().get(0);
        assertNotNull(violation.getFixSuggestion());
        assertFalse(violation.getFixSuggestion().isEmpty());
    }

    @Test
    void testViolationContainsCodeSnippet() {
        String code = "File file = new File(\"/uploads/\" + request.getParameter(\"file\"));";
        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.hasViolations());
        RuleResult.RuleViolation violation = result.getViolations().get(0);
        assertNotNull(violation.getCodeSnippet());
        assertFalse(violation.getCodeSnippet().isEmpty());
    }

    @Test
    void testToString() {
        String str = rule.toString();
        assertTrue(str.contains("owasp-2021-a01-001"));
        assertTrue(str.contains("A01"));
        assertTrue(str.contains("33")); // CWE count
    }
}
