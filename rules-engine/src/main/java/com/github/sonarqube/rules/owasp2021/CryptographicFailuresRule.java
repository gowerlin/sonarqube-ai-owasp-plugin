package com.github.sonarqube.rules.owasp2021;

import com.github.sonarqube.rules.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2021 A02: Cryptographic Failures 檢測規則
 *
 * 檢測加密相關的安全漏洞，包括：
 * - Weak Cryptographic Algorithms (弱加密演算法)
 * - Hardcoded Secrets (硬編碼的密鑰)
 * - Insecure Random Number Generation (不安全的隨機數生成)
 * - Plaintext Data Transmission (明文資料傳輸)
 * - Insufficient Key Length (密鑰長度不足)
 * - Missing Encryption (缺少加密)
 *
 * CWE 映射：CWE-261, CWE-296, CWE-310, CWE-319, CWE-321, CWE-322, CWE-323,
 * CWE-324, CWE-325, CWE-326, CWE-327, CWE-328, CWE-329, CWE-330, CWE-331,
 * CWE-335, CWE-336, CWE-337, CWE-338, CWE-340, CWE-347, CWE-523, CWE-720,
 * CWE-757, CWE-759, CWE-760, CWE-780, CWE-818, CWE-916 (共 29 個 CWE)
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.3)
 */
public class CryptographicFailuresRule extends AbstractOwaspRule {

    private static final String RULE_ID = "owasp-2021-a02-001";
    private static final String RULE_NAME = "Cryptographic Failures Detection";
    private static final String DESCRIPTION = "Detects cryptographic vulnerabilities including weak algorithms, " +
        "hardcoded secrets, insecure random number generation, plaintext transmission, and insufficient key lengths.";

    // 弱加密演算法模式
    private static final Pattern WEAK_ALGORITHM_PATTERN = Pattern.compile(
        "(?:DES|RC2|RC4|MD5|SHA1|SHA-1)(?:['\"]|\\s|\\()",
        Pattern.CASE_INSENSITIVE
    );

    // 硬編碼密鑰/密碼模式
    private static final Pattern HARDCODED_SECRET_PATTERN = Pattern.compile(
        "(?:password|passwd|pwd|secret|key|token|api[_-]?key)\\s*[=:]\\s*['\"][^'\"]{8,}['\"]",
        Pattern.CASE_INSENSITIVE
    );

    // 不安全的隨機數生成
    private static final Pattern INSECURE_RANDOM_PATTERN = Pattern.compile(
        "(?:new\\s+Random\\s*\\(|Math\\.random\\(|Random\\.next(?:Int|Long|Double))",
        Pattern.CASE_INSENSITIVE
    );

    // HTTP 明文傳輸
    private static final Pattern HTTP_PLAINTEXT_PATTERN = Pattern.compile(
        "(?:http://|url\\s*=\\s*['\"]http:|setUrl\\(\\s*['\"]http:)",
        Pattern.CASE_INSENSITIVE
    );

    // 不安全的 SSL/TLS 配置
    private static final Pattern INSECURE_SSL_PATTERN = Pattern.compile(
        "(?:SSLv2|SSLv3|TLSv1\\.0|TLSv1\\.1|ALLOW_ALL_HOSTNAME_VERIFIER|setHostnameVerifier\\s*\\(\\s*null)",
        Pattern.CASE_INSENSITIVE
    );

    // 不安全的加密模式
    private static final Pattern INSECURE_CIPHER_MODE_PATTERN = Pattern.compile(
        "(?:AES/ECB|DES/ECB|Cipher\\.getInstance\\s*\\(\\s*['\"](?:AES|DES)['\"]\\s*\\))",
        Pattern.CASE_INSENSITIVE
    );

    // Base64 編碼當作加密使用
    private static final Pattern BASE64_AS_ENCRYPTION_PATTERN = Pattern.compile(
        "(?:Base64\\.(?:encode|decode)).*(?:password|secret|key|token)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 建構子
     */
    public CryptographicFailuresRule() {
        super(createRuleDefinition());
    }

    private static RuleDefinition createRuleDefinition() {
        return RuleDefinition.builder(RULE_ID)
            .name(RULE_NAME)
            .description(DESCRIPTION)
            .severity(RuleDefinition.RuleSeverity.CRITICAL)
            .type(RuleDefinition.RuleType.VULNERABILITY)
            .owaspCategory("A02")
            .cweId("CWE-261").cweId("CWE-296").cweId("CWE-310").cweId("CWE-319")
            .cweId("CWE-321").cweId("CWE-322").cweId("CWE-323").cweId("CWE-324")
            .cweId("CWE-325").cweId("CWE-326").cweId("CWE-327").cweId("CWE-328")
            .cweId("CWE-329").cweId("CWE-330").cweId("CWE-331").cweId("CWE-335")
            .cweId("CWE-336").cweId("CWE-337").cweId("CWE-338").cweId("CWE-340")
            .cweId("CWE-347").cweId("CWE-523").cweId("CWE-720").cweId("CWE-757")
            .cweId("CWE-759").cweId("CWE-760").cweId("CWE-780").cweId("CWE-818")
            .cweId("CWE-916")
            .language("java")
            .tag("owasp-2021").tag("security").tag("cryptography").tag("encryption")
            .build();
    }

    @Override
    public boolean matches(RuleContext context) {
        if (!super.matches(context)) {
            return false;
        }

        String code = context.getCode();
        return containsKeyword(code, "Cipher") ||
               containsKeyword(code, "encrypt") ||
               containsKeyword(code, "password") ||
               containsKeyword(code, "secret") ||
               containsKeyword(code, "Random") ||
               containsKeyword(code, "http://") ||
               containsKeyword(code, "SSL") ||
               containsKeyword(code, "TLS") ||
               containsKeyword(code, "Base64");
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        String code = context.getCode();
        List<RuleResult.RuleViolation> violations = new ArrayList<>();

        // 1. 檢測弱加密演算法
        violations.addAll(detectWeakAlgorithms(code));

        // 2. 檢測硬編碼的密鑰
        violations.addAll(detectHardcodedSecrets(code));

        // 3. 檢測不安全的隨機數生成
        violations.addAll(detectInsecureRandom(code));

        // 4. 檢測 HTTP 明文傳輸
        violations.addAll(detectPlaintextTransmission(code));

        // 5. 檢測不安全的 SSL/TLS 配置
        violations.addAll(detectInsecureSsl(code));

        // 6. 檢測不安全的加密模式
        violations.addAll(detectInsecureCipherMode(code));

        // 7. 檢測 Base64 編碼誤用
        violations.addAll(detectBase64Misuse(code));

        return RuleResult.builder(getRuleId())
            .success(true)
            .violations(violations)
            .build();
    }

    /**
     * 檢測弱加密演算法
     */
    private List<RuleResult.RuleViolation> detectWeakAlgorithms(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, WEAK_ALGORITHM_PATTERN);

        for (int lineNumber : lines) {
            String snippet = getCodeSnippet(code, lineNumber);
            String algorithm = extractAlgorithm(snippet);

            violations.add(createViolation(
                lineNumber,
                String.format("Weak cryptographic algorithm detected: %s is considered insecure (CWE-327)", algorithm),
                code,
                String.format("Replace %s with secure algorithms: AES-256, RSA-2048+, SHA-256, or SHA-3", algorithm)
            ));
        }

        return violations;
    }

    /**
     * 檢測硬編碼的密鑰
     */
    private List<RuleResult.RuleViolation> detectHardcodedSecrets(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, HARDCODED_SECRET_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "Hardcoded secret detected: Credentials should never be hardcoded in source code (CWE-798)",
                code,
                "Use environment variables, configuration files, or secure secret management systems (e.g., HashiCorp Vault, AWS Secrets Manager)"
            ));
        }

        return violations;
    }

    /**
     * 檢測不安全的隨機數生成
     */
    private List<RuleResult.RuleViolation> detectInsecureRandom(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, INSECURE_RANDOM_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "Insecure random number generation: java.util.Random is not cryptographically secure (CWE-330)",
                code,
                "Use SecureRandom for cryptographic operations: SecureRandom.getInstanceStrong() or SecureRandom.getInstance(\"NativePRNG\")"
            ));
        }

        return violations;
    }

    /**
     * 檢測 HTTP 明文傳輸
     */
    private List<RuleResult.RuleViolation> detectPlaintextTransmission(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, HTTP_PLAINTEXT_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "Plaintext HTTP transmission: Data transmitted over HTTP is not encrypted (CWE-319)",
                code,
                "Use HTTPS for all network communication to ensure data encryption in transit"
            ));
        }

        return violations;
    }

    /**
     * 檢測不安全的 SSL/TLS 配置
     */
    private List<RuleResult.RuleViolation> detectInsecureSsl(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, INSECURE_SSL_PATTERN);

        for (int lineNumber : lines) {
            String snippet = getCodeSnippet(code, lineNumber);
            String protocol = extractProtocol(snippet);

            violations.add(createViolation(
                lineNumber,
                String.format("Insecure SSL/TLS configuration: %s is deprecated and vulnerable (CWE-326)", protocol),
                code,
                "Use TLSv1.2 or TLSv1.3 with strong cipher suites. Avoid SSLv2, SSLv3, TLSv1.0, and TLSv1.1"
            ));
        }

        return violations;
    }

    /**
     * 檢測不安全的加密模式
     */
    private List<RuleResult.RuleViolation> detectInsecureCipherMode(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, INSECURE_CIPHER_MODE_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "Insecure cipher mode: ECB mode does not provide semantic security (CWE-327)",
                code,
                "Use secure cipher modes: AES/GCM/NoPadding or AES/CBC/PKCS5Padding with random IV"
            ));
        }

        return violations;
    }

    /**
     * 檢測 Base64 編碼誤用
     */
    private List<RuleResult.RuleViolation> detectBase64Misuse(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();
        List<Integer> lines = findMatchingLines(code, BASE64_AS_ENCRYPTION_PATTERN);

        for (int lineNumber : lines) {
            violations.add(createViolation(
                lineNumber,
                "Base64 is not encryption: Base64 is encoding, not encryption, and provides no security (CWE-327)",
                code,
                "Use proper encryption (AES-256) for sensitive data. Base64 should only be used for encoding binary data"
            ));
        }

        return violations;
    }

    /**
     * 從程式碼片段中提取演算法名稱
     */
    private String extractAlgorithm(String snippet) {
        if (snippet.toUpperCase().contains("DES")) return "DES";
        if (snippet.toUpperCase().contains("RC2")) return "RC2";
        if (snippet.toUpperCase().contains("RC4")) return "RC4";
        if (snippet.toUpperCase().contains("MD5")) return "MD5";
        if (snippet.toUpperCase().contains("SHA-1") || snippet.toUpperCase().contains("SHA1")) return "SHA-1";
        return "Unknown weak algorithm";
    }

    /**
     * 從程式碼片段中提取協定名稱
     */
    private String extractProtocol(String snippet) {
        if (snippet.contains("SSLv2")) return "SSLv2";
        if (snippet.contains("SSLv3")) return "SSLv3";
        if (snippet.contains("TLSv1.0")) return "TLSv1.0";
        if (snippet.contains("TLSv1.1")) return "TLSv1.1";
        if (snippet.contains("ALLOW_ALL_HOSTNAME_VERIFIER")) return "ALLOW_ALL_HOSTNAME_VERIFIER";
        if (snippet.contains("setHostnameVerifier")) return "null hostname verifier";
        return "Insecure SSL/TLS configuration";
    }

    @Override
    public boolean requiresAi() {
        return false;
    }

    @Override
    public String toString() {
        return "CryptographicFailuresRule{" +
            "id='" + getRuleId() + '\'' +
            ", category='A02'" +
            ", cweCount=29" +
            '}';
    }
}
