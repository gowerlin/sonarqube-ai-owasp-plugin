package com.github.sonarqube.rules.owasp2021;

import com.github.sonarqube.rules.RuleContext;
import com.github.sonarqube.rules.RuleDefinition;
import com.github.sonarqube.rules.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CryptographicFailuresRule 單元測試
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.3)
 */
class CryptographicFailuresRuleTest {

    private CryptographicFailuresRule rule;

    @BeforeEach
    void setUp() {
        rule = new CryptographicFailuresRule();
    }

    @Test
    void testRuleMetadata() {
        assertEquals("owasp-2021-a02-001", rule.getRuleId());
        assertEquals("Cryptographic Failures Detection", rule.getRuleName());
        assertEquals("A02", rule.getOwaspCategory());
        assertEquals("2021", rule.getOwaspVersion());
        assertEquals(RuleDefinition.RuleSeverity.CRITICAL, rule.getDefaultSeverity());
        assertEquals(RuleDefinition.RuleType.VULNERABILITY, rule.getRuleType());
        assertFalse(rule.requiresAi());
    }

    @Test
    void testCweMapping() {
        assertEquals(29, rule.getCweIds().size());
        assertTrue(rule.getCweIds().contains("CWE-327")); // Use of Broken Crypto
        assertTrue(rule.getCweIds().contains("CWE-330")); // Insecure Random
        assertTrue(rule.getCweIds().contains("CWE-319")); // Cleartext Transmission
        assertTrue(rule.getCweIds().contains("CWE-326")); // Inadequate Encryption Strength
        assertTrue(rule.getCweIds().contains("CWE-798")); // Hardcoded Credentials (via CWE-523)
    }

    @Test
    void testDetectWeakAlgorithmDES() {
        String code = "Cipher cipher = Cipher.getInstance(\"DES\");\n" +
                     "cipher.init(Cipher.ENCRYPT_MODE, secretKey);";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());

        boolean hasWeakAlgoViolation = result.getViolations().stream()
            .anyMatch(v -> v.getMessage().contains("DES") && v.getMessage().contains("insecure"));
        assertTrue(hasWeakAlgoViolation);
    }

    @Test
    void testDetectWeakAlgorithmMD5() {
        String code = "MessageDigest md = MessageDigest.getInstance(\"MD5\");\n" +
                     "byte[] hash = md.digest(password.getBytes());";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());

        boolean hasMd5Violation = result.getViolations().stream()
            .anyMatch(v -> v.getMessage().contains("MD5") && v.getMessage().contains("CWE-327"));
        assertTrue(hasMd5Violation);
    }

    @Test
    void testDetectWeakAlgorithmSHA1() {
        String code = "MessageDigest sha1 = MessageDigest.getInstance(\"SHA-1\");\n" +
                     "byte[] digest = sha1.digest(data);";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());

        boolean hasSha1Violation = result.getViolations().stream()
            .anyMatch(v -> v.getMessage().contains("SHA-1"));
        assertTrue(hasSha1Violation);
    }

    @Test
    void testDetectHardcodedPassword() {
        String code = "String password = \"MySecretPassword123\";\n" +
                     "String apiKey = \"sk-1234567890abcdef\";\n" +
                     "connection.setPassword(password);";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());

        boolean hasHardcodedViolation = result.getViolations().stream()
            .anyMatch(v -> v.getMessage().contains("Hardcoded secret") ||
                          v.getMessage().contains("CWE-798"));
        assertTrue(hasHardcodedViolation);
    }

    @Test
    void testDetectHardcodedApiKey() {
        String code = "private static final String API_KEY = \"sk-proj-1234567890abcdefghij\";\n" +
                     "request.setHeader(\"Authorization\", \"Bearer \" + API_KEY);";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());
    }

    @Test
    void testDetectInsecureRandom() {
        String code = "Random random = new Random();\n" +
                     "int token = random.nextInt();\n" +
                     "String sessionId = String.valueOf(token);";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());

        boolean hasInsecureRandomViolation = result.getViolations().stream()
            .anyMatch(v -> v.getMessage().contains("Insecure random") &&
                          v.getMessage().contains("CWE-330"));
        assertTrue(hasInsecureRandomViolation);
    }

    @Test
    void testDetectMathRandom() {
        String code = "double randomValue = Math.random();\n" +
                     "String token = String.valueOf(randomValue * 1000000);";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());
    }

    @Test
    void testDetectHttpPlaintext() {
        String code = "String url = \"http://api.example.com/sensitive-data\";\n" +
                     "HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());

        boolean hasPlaintextViolation = result.getViolations().stream()
            .anyMatch(v -> v.getMessage().contains("Plaintext HTTP") &&
                          v.getMessage().contains("CWE-319"));
        assertTrue(hasPlaintextViolation);
    }

    @Test
    void testDetectInsecureSslProtocol() {
        String code = "SSLContext context = SSLContext.getInstance(\"SSLv3\");\n" +
                     "context.init(null, trustAllCerts, new SecureRandom());";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());

        boolean hasSslViolation = result.getViolations().stream()
            .anyMatch(v -> v.getMessage().contains("SSLv3") &&
                          v.getMessage().contains("deprecated"));
        assertTrue(hasSslViolation);
    }

    @Test
    void testDetectInsecureTlsVersion() {
        String code = "SSLContext sslContext = SSLContext.getInstance(\"TLSv1.0\");\n" +
                     "HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());

        boolean hasTlsViolation = result.getViolations().stream()
            .anyMatch(v -> v.getMessage().contains("TLSv1.0"));
        assertTrue(hasTlsViolation);
    }

    @Test
    void testDetectEcbMode() {
        String code = "Cipher cipher = Cipher.getInstance(\"AES/ECB/PKCS5Padding\");\n" +
                     "cipher.init(Cipher.ENCRYPT_MODE, secretKey);";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());

        boolean hasEcbViolation = result.getViolations().stream()
            .anyMatch(v -> v.getMessage().contains("ECB mode") &&
                          v.getMessage().contains("semantic security"));
        assertTrue(hasEcbViolation);
    }

    @Test
    void testDetectBase64Misuse() {
        String code = "String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());\n" +
                     "database.savePassword(encodedPassword); // Not encryption!";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());

        boolean hasBase64Violation = result.getViolations().stream()
            .anyMatch(v -> v.getMessage().contains("Base64 is not encryption"));
        assertTrue(hasBase64Violation);
    }

    @Test
    void testSecureCodeNoViolations() {
        String code = "// Secure cryptography implementation\n" +
                     "SecureRandom secureRandom = SecureRandom.getInstanceStrong();\n" +
                     "KeyGenerator keyGen = KeyGenerator.getInstance(\"AES\");\n" +
                     "keyGen.init(256, secureRandom);\n" +
                     "SecretKey secretKey = keyGen.generateKey();\n" +
                     "Cipher cipher = Cipher.getInstance(\"AES/GCM/NoPadding\");\n" +
                     "cipher.init(Cipher.ENCRYPT_MODE, secretKey);\n" +
                     "String url = \"https://api.example.com/data\";\n" +
                     "SSLContext sslContext = SSLContext.getInstance(\"TLSv1.3\");";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getViolationCount());
    }

    @Test
    void testMultipleViolationsInSameCode() {
        String code = "// Multiple crypto violations\n" +
                     "Cipher cipher = Cipher.getInstance(\"DES\"); // Weak algorithm\n" +
                     "String password = \"hardcoded123\"; // Hardcoded secret\n" +
                     "Random random = new Random(); // Insecure random\n" +
                     "String url = \"http://example.com\"; // HTTP plaintext\n" +
                     "SSLContext ctx = SSLContext.getInstance(\"SSLv3\"); // Insecure SSL\n" +
                     "Cipher aes = Cipher.getInstance(\"AES/ECB/PKCS5Padding\"); // ECB mode\n" +
                     "String encoded = Base64.encode(password); // Base64 misuse";

        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.isSuccess());
        assertTrue(result.hasViolations());
        assertTrue(result.getViolationCount() >= 5); // At least 5 different violation types
    }

    @Test
    void testMatchesReturnsTrueForRelevantCode() {
        String code = "Cipher cipher = Cipher.getInstance(\"AES\");";
        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        assertTrue(rule.matches(context));
    }

    @Test
    void testMatchesReturnsFalseForIrrelevantCode() {
        String code = "int sum = a + b;\n" +
                     "System.out.println(sum);";
        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        assertFalse(rule.matches(context));
    }

    @Test
    void testMatchesReturnsFalseForWrongLanguage() {
        String code = "Cipher cipher = Cipher.getInstance(\"AES\");";
        RuleContext context = RuleContext.builder(code, "javascript")
            .owaspVersion("2021")
            .build();

        assertFalse(rule.matches(context));
    }

    @Test
    void testViolationContainsFixSuggestion() {
        String code = "Cipher cipher = Cipher.getInstance(\"DES\");";
        RuleContext context = RuleContext.builder(code, "java")
            .owaspVersion("2021")
            .build();

        RuleResult result = rule.execute(context);

        assertTrue(result.hasViolations());
        RuleResult.RuleViolation violation = result.getViolations().get(0);
        assertNotNull(violation.getFixSuggestion());
        assertTrue(violation.getFixSuggestion().contains("AES-256") ||
                  violation.getFixSuggestion().contains("secure"));
    }

    @Test
    void testToString() {
        String str = rule.toString();
        assertTrue(str.contains("owasp-2021-a02-001"));
        assertTrue(str.contains("A02"));
        assertTrue(str.contains("29")); // CWE count
    }
}
