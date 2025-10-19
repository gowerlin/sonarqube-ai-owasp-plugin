package com.github.sonarqube.rules.cwe;

import com.github.sonarqube.rules.owasp.Owasp2021Category;

import java.util.*;

/**
 * CWE 到 OWASP 2021 的映射關係
 *
 * 提供 CWE (Common Weakness Enumeration) ID 與 OWASP Top 10 2021 分類之間的映射。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class CweMapping {

    /**
     * CWE 到 OWASP 2021 的映射表
     */
    private static final Map<String, Owasp2021Category> CWE_TO_OWASP_2021 = new HashMap<>();

    static {
        // A01:2021 - Broken Access Control
        mapToA01("CWE-22");   // Path Traversal
        mapToA01("CWE-23");   // Relative Path Traversal
        mapToA01("CWE-35");   // Path Traversal: '.../...//'
        mapToA01("CWE-59");   // Link Following
        mapToA01("CWE-200");  // Exposure of Sensitive Information
        mapToA01("CWE-201");  // Insertion of Sensitive Information Into Sent Data
        mapToA01("CWE-219");  // Storage in a Directory with Incorrect Permissions
        mapToA01("CWE-264");  // Permissions, Privileges, and Access Controls
        mapToA01("CWE-275");  // Permission Issues
        mapToA01("CWE-276");  // Incorrect Default Permissions
        mapToA01("CWE-284");  // Improper Access Control
        mapToA01("CWE-285");  // Improper Authorization
        mapToA01("CWE-352");  // Cross-Site Request Forgery (CSRF)
        mapToA01("CWE-359");  // Exposure of Private Personal Information
        mapToA01("CWE-377");  // Insecure Temporary File
        mapToA01("CWE-402");  // Transmission of Private Resources into a New Sphere
        mapToA01("CWE-425");  // Direct Request
        mapToA01("CWE-441");  // Unintended Proxy or Intermediary
        mapToA01("CWE-497");  // Exposure of Sensitive System Information
        mapToA01("CWE-538");  // Insertion of Sensitive Information into Externally-Accessible File
        mapToA01("CWE-540");  // Inclusion of Sensitive Information in Source Code
        mapToA01("CWE-548");  // Exposure of Information Through Directory Listing
        mapToA01("CWE-552");  // Files or Directories Accessible to External Parties
        mapToA01("CWE-566");  // Authorization Bypass Through User-Controlled SQL Primary Key
        mapToA01("CWE-601");  // URL Redirection to Untrusted Site
        mapToA01("CWE-639");  // Authorization Bypass Through User-Controlled Key
        mapToA01("CWE-651");  // Exposure of WSDL File
        mapToA01("CWE-668");  // Exposure of Resource to Wrong Sphere
        mapToA01("CWE-706");  // Use of Incorrectly-Resolved Name or Reference
        mapToA01("CWE-862");  // Missing Authorization
        mapToA01("CWE-863");  // Incorrect Authorization
        mapToA01("CWE-913");  // Improper Control of Dynamically-Managed Code Resources
        mapToA01("CWE-922");  // Insecure Storage of Sensitive Information
        mapToA01("CWE-1275"); // Sensitive Cookie with Improper SameSite Attribute

        // A02:2021 - Cryptographic Failures
        mapToA02("CWE-261");  // Weak Encoding for Password
        mapToA02("CWE-296");  // Improper Following of Chain of Trust for Certificate Validation
        mapToA02("CWE-310");  // Cryptographic Issues
        mapToA02("CWE-319");  // Cleartext Transmission of Sensitive Information
        mapToA02("CWE-321");  // Use of Hard-coded Cryptographic Key
        mapToA02("CWE-322");  // Key Exchange without Entity Authentication
        mapToA02("CWE-323");  // Reusing a Nonce, Key Pair in Encryption
        mapToA02("CWE-324");  // Use of a Key Past its Expiration Date
        mapToA02("CWE-325");  // Missing Required Cryptographic Step
        mapToA02("CWE-326");  // Inadequate Encryption Strength
        mapToA02("CWE-327");  // Use of a Broken or Risky Cryptographic Algorithm
        mapToA02("CWE-328");  // Reversible One-Way Hash
        mapToA02("CWE-329");  // Not Using a Random IV with CBC Mode
        mapToA02("CWE-330");  // Use of Insufficiently Random Values
        mapToA02("CWE-331");  // Insufficient Entropy
        mapToA02("CWE-335");  // Incorrect Usage of Seeds in Pseudo-Random Number Generator
        mapToA02("CWE-336");  // Same Seed in Pseudo-Random Number Generator
        mapToA02("CWE-337");  // Predictable Seed in Pseudo-Random Number Generator
        mapToA02("CWE-338");  // Use of Cryptographically Weak Pseudo-Random Number Generator
        mapToA02("CWE-340");  // Generation of Predictable Numbers or Identifiers
        mapToA02("CWE-347");  // Improper Verification of Cryptographic Signature
        mapToA02("CWE-523");  // Unprotected Transport of Credentials
        mapToA02("CWE-757");  // Selection of Less-Secure Algorithm During Negotiation
        mapToA02("CWE-759");  // Use of a One-Way Hash without a Salt
        mapToA02("CWE-760");  // Use of a One-Way Hash with a Predictable Salt
        mapToA02("CWE-780");  // Use of RSA Algorithm without OAEP
        mapToA02("CWE-818");  // Insufficient Transport Layer Protection
        mapToA02("CWE-916");  // Use of Password Hash With Insufficient Computational Effort

        // A03:2021 - Injection
        mapToA03("CWE-20");   // Improper Input Validation
        mapToA03("CWE-74");   // Improper Neutralization of Special Elements
        mapToA03("CWE-75");   // Failure to Sanitize Special Elements into a Different Plane
        mapToA03("CWE-77");   // Command Injection
        mapToA03("CWE-78");   // OS Command Injection
        mapToA03("CWE-79");   // Cross-site Scripting (XSS)
        mapToA03("CWE-80");   // Improper Neutralization of Script-Related HTML Tags
        mapToA03("CWE-83");   // Improper Neutralization of Script in Attributes
        mapToA03("CWE-87");   // Improper Neutralization of Alternate XSS Syntax
        mapToA03("CWE-88");   // Argument Injection
        mapToA03("CWE-89");   // SQL Injection
        mapToA03("CWE-90");   // LDAP Injection
        mapToA03("CWE-91");   // XML Injection
        mapToA03("CWE-93");   // Improper Neutralization of CRLF Sequences
        mapToA03("CWE-94");   // Code Injection
        mapToA03("CWE-95");   // Eval Injection
        mapToA03("CWE-96");   // Improper Neutralization of Directives in Statically Saved Code
        mapToA03("CWE-97");   // Improper Neutralization of Server-Side Includes
        mapToA03("CWE-98");   // Remote File Inclusion
        mapToA03("CWE-99");   // Resource Injection
        mapToA03("CWE-100");  // Deprecated: Was catch-all for input validation issues
        mapToA03("CWE-113");  // HTTP Response Splitting
        mapToA03("CWE-116");  // Improper Encoding or Escaping of Output
        mapToA03("CWE-138");  // Improper Neutralization of Special Elements
        mapToA03("CWE-184");  // Incomplete List of Disallowed Inputs
        mapToA03("CWE-470");  // Use of Externally-Controlled Input to Select Classes or Code
        mapToA03("CWE-471");  // Modification of Assumed-Immutable Data
        mapToA03("CWE-564");  // SQL Injection: Hibernate
        mapToA03("CWE-610");  // Externally Controlled Reference to a Resource
        mapToA03("CWE-643");  // XPath Injection
        mapToA03("CWE-652");  // Improper Neutralization of Data within XQuery Expressions
        mapToA03("CWE-917");  // Expression Language Injection

        // A04:2021 - Insecure Design
        mapToA04("CWE-73");   // External Control of File Name or Path
        mapToA04("CWE-183");  // Permissive List of Allowed Inputs
        mapToA04("CWE-209");  // Generation of Error Message Containing Sensitive Information
        mapToA04("CWE-213");  // Exposure of Sensitive Information Due to Incompatible Policies
        mapToA04("CWE-235");  // Improper Handling of Extra Parameters
        mapToA04("CWE-256");  // Unprotected Storage of Credentials
        mapToA04("CWE-257");  // Storing Passwords in a Recoverable Format
        mapToA04("CWE-266");  // Incorrect Privilege Assignment
        mapToA04("CWE-269");  // Improper Privilege Management
        mapToA04("CWE-280");  // Improper Handling of Insufficient Permissions
        mapToA04("CWE-311");  // Missing Encryption of Sensitive Data
        mapToA04("CWE-312");  // Cleartext Storage of Sensitive Information
        mapToA04("CWE-313");  // Cleartext Storage in a File or on Disk
        mapToA04("CWE-316");  // Cleartext Storage of Sensitive Information in Memory
        mapToA04("CWE-419");  // Unprotected Primary Channel
        mapToA04("CWE-430");  // Deployment of Wrong Handler
        mapToA04("CWE-434");  // Unrestricted Upload of File with Dangerous Type
        mapToA04("CWE-444");  // HTTP Request Smuggling
        mapToA04("CWE-451");  // User Interface (UI) Misrepresentation of Critical Information
        mapToA04("CWE-472");  // External Control of Assumed-Immutable Web Parameter
        mapToA04("CWE-501");  // Trust Boundary Violation
        mapToA04("CWE-522");  // Insufficiently Protected Credentials
        mapToA04("CWE-525");  // Use of Web Browser Cache Containing Sensitive Information
        mapToA04("CWE-539");  // Use of Persistent Cookies Containing Sensitive Information
        mapToA04("CWE-579");  // J2EE Bad Practices: Non-serializable Object Stored in Session
        mapToA04("CWE-598");  // Use of GET Request Method With Sensitive Query Strings
        mapToA04("CWE-602");  // Client-Side Enforcement of Server-Side Security
        mapToA04("CWE-642");  // External Control of Critical State Data
        mapToA04("CWE-646");  // Reliance on File Name or Extension of Externally-Supplied File
        mapToA04("CWE-650");  // Trusting HTTP Permission Methods on the Server Side
        mapToA04("CWE-653");  // Insufficient Compartmentalization
        mapToA04("CWE-656");  // Reliance on Security Through Obscurity
        mapToA04("CWE-657");  // Violation of Secure Design Principles
        mapToA04("CWE-799");  // Improper Control of Interaction Frequency
        mapToA04("CWE-807");  // Reliance on Untrusted Inputs in a Security Decision
        mapToA04("CWE-840");  // Business Logic Errors
        mapToA04("CWE-841");  // Improper Enforcement of Behavioral Workflow
        mapToA04("CWE-927");  // Use of Implicit Intent for Sensitive Communication
        mapToA04("CWE-1021"); // Improper Restriction of Rendered UI Layers or Frames
        mapToA04("CWE-1173"); // Improper Use of Validation Framework

        // A05:2021 - Security Misconfiguration
        mapToA05("CWE-2");    // Environment
        mapToA05("CWE-11");   // ASP.NET Misconfiguration
        mapToA05("CWE-13");   // ASP.NET Misconfiguration: Password in Configuration File
        mapToA05("CWE-15");   // External Control of System or Configuration Setting
        mapToA05("CWE-16");   // Configuration
        mapToA05("CWE-260");  // Password in Configuration File
        mapToA05("CWE-315");  // Cleartext Storage of Sensitive Information in a Cookie
        mapToA05("CWE-520");  // .NET Misconfiguration: Use of Impersonation
        mapToA05("CWE-526");  // Exposure of Sensitive Information Through Environmental Variables
        mapToA05("CWE-537");  // Java Runtime Error Message Containing Sensitive Information
        mapToA05("CWE-541");  // Inclusion of Sensitive Information in an Include File
        mapToA05("CWE-547");  // Use of Hard-coded, Security-relevant Constants
        mapToA05("CWE-611");  // XML External Entity (XXE)
        mapToA05("CWE-614");  // Sensitive Cookie in HTTPS Session Without 'Secure' Attribute
        mapToA05("CWE-756");  // Missing Custom Error Page
        mapToA05("CWE-776");  // Improper Restriction of Recursive Entity References in DTDs
        mapToA05("CWE-942");  // Permissive Cross-domain Policy with Untrusted Domains
        mapToA05("CWE-1004"); // Sensitive Cookie Without 'HttpOnly' Flag
        mapToA05("CWE-1032"); // OWASP Top Ten 2017 Category A6 - Security Misconfiguration
        mapToA05("CWE-1174"); // ASP.NET Misconfiguration: Improper Model Validation

        // A06:2021 - Vulnerable and Outdated Components
        mapToA06("CWE-937");  // OWASP Top Ten 2013 Category A9 - Using Components with Known Vulnerabilities
        mapToA06("CWE-1035"); // OWASP Top Ten 2017 Category A9 - Using Components with Known Vulnerabilities
        mapToA06("CWE-1104"); // Use of Unmaintained Third Party Components

        // A07:2021 - Identification and Authentication Failures
        mapToA07("CWE-255");  // Credentials Management
        mapToA07("CWE-259");  // Use of Hard-coded Password
        mapToA07("CWE-287");  // Improper Authentication
        mapToA07("CWE-288");  // Authentication Bypass Using an Alternate Path or Channel
        mapToA07("CWE-290");  // Authentication Bypass by Spoofing
        mapToA07("CWE-294");  // Authentication Bypass by Capture-replay
        mapToA07("CWE-295");  // Improper Certificate Validation
        mapToA07("CWE-297");  // Improper Validation of Certificate with Host Mismatch
        mapToA07("CWE-300");  // Channel Accessible by Non-Endpoint
        mapToA07("CWE-302");  // Authentication Bypass by Assumed-Immutable Data
        mapToA07("CWE-304");  // Missing Critical Step in Authentication
        mapToA07("CWE-306");  // Missing Authentication for Critical Function
        mapToA07("CWE-307");  // Improper Restriction of Excessive Authentication Attempts
        mapToA07("CWE-346");  // Origin Validation Error
        mapToA07("CWE-384");  // Session Fixation
        mapToA07("CWE-521");  // Weak Password Requirements
        mapToA07("CWE-613");  // Insufficient Session Expiration
        mapToA07("CWE-620");  // Unverified Password Change
        mapToA07("CWE-640");  // Weak Password Recovery Mechanism
        mapToA07("CWE-798");  // Use of Hard-coded Credentials
        mapToA07("CWE-940");  // Improper Verification of Source of a Communication Channel
        mapToA07("CWE-1216"); // Lockout Mechanism Errors

        // A08:2021 - Software and Data Integrity Failures
        mapToA08("CWE-345");  // Insufficient Verification of Data Authenticity
        mapToA08("CWE-353");  // Missing Support for Integrity Check
        mapToA08("CWE-426");  // Untrusted Search Path
        mapToA08("CWE-494");  // Download of Code Without Integrity Check
        mapToA08("CWE-502");  // Deserialization of Untrusted Data
        mapToA08("CWE-565");  // Reliance on Cookies without Validation and Integrity Checking
        mapToA08("CWE-784");  // Reliance on Cookies without Validation and Integrity Checking in a Security Decision
        mapToA08("CWE-829");  // Inclusion of Functionality from Untrusted Control Sphere
        mapToA08("CWE-830");  // Inclusion of Web Functionality from an Untrusted Source
        mapToA08("CWE-915");  // Improperly Controlled Modification of Dynamically-Determined Object Attributes

        // A09:2021 - Security Logging and Monitoring Failures
        mapToA09("CWE-117");  // Improper Output Neutralization for Logs
        mapToA09("CWE-223");  // Omission of Security-relevant Information
        mapToA09("CWE-532");  // Insertion of Sensitive Information into Log File
        mapToA09("CWE-778");  // Insufficient Logging
        mapToA09("CWE-1188"); // Insecure Default Initialization of Resource

        // A10:2021 - Server-Side Request Forgery (SSRF)
        mapToA10("CWE-918");  // Server-Side Request Forgery (SSRF)
    }

    private static void mapToA01(String cweId) {
        CWE_TO_OWASP_2021.put(cweId, Owasp2021Category.A01_BROKEN_ACCESS_CONTROL);
    }

    private static void mapToA02(String cweId) {
        CWE_TO_OWASP_2021.put(cweId, Owasp2021Category.A02_CRYPTOGRAPHIC_FAILURES);
    }

    private static void mapToA03(String cweId) {
        CWE_TO_OWASP_2021.put(cweId, Owasp2021Category.A03_INJECTION);
    }

    private static void mapToA04(String cweId) {
        CWE_TO_OWASP_2021.put(cweId, Owasp2021Category.A04_INSECURE_DESIGN);
    }

    private static void mapToA05(String cweId) {
        CWE_TO_OWASP_2021.put(cweId, Owasp2021Category.A05_SECURITY_MISCONFIGURATION);
    }

    private static void mapToA06(String cweId) {
        CWE_TO_OWASP_2021.put(cweId, Owasp2021Category.A06_VULNERABLE_OUTDATED_COMPONENTS);
    }

    private static void mapToA07(String cweId) {
        CWE_TO_OWASP_2021.put(cweId, Owasp2021Category.A07_IDENTIFICATION_AUTHENTICATION_FAILURES);
    }

    private static void mapToA08(String cweId) {
        CWE_TO_OWASP_2021.put(cweId, Owasp2021Category.A08_SOFTWARE_DATA_INTEGRITY_FAILURES);
    }

    private static void mapToA09(String cweId) {
        CWE_TO_OWASP_2021.put(cweId, Owasp2021Category.A09_SECURITY_LOGGING_MONITORING_FAILURES);
    }

    private static void mapToA10(String cweId) {
        CWE_TO_OWASP_2021.put(cweId, Owasp2021Category.A10_SERVER_SIDE_REQUEST_FORGERY);
    }

    /**
     * 從 CWE ID 查找對應的 OWASP 2021 分類
     *
     * @param cweId CWE ID（如 "CWE-89" 或 "89"）
     * @return 對應的 OWASP 2021 分類，如果找不到則返回 null
     */
    public static Owasp2021Category getOwaspCategory(String cweId) {
        if (cweId == null) {
            return null;
        }

        // 標準化 CWE ID 格式
        String normalizedId = cweId.toUpperCase();
        if (!normalizedId.startsWith("CWE-")) {
            normalizedId = "CWE-" + normalizedId;
        }

        return CWE_TO_OWASP_2021.get(normalizedId);
    }

    /**
     * 獲取指定 OWASP 分類下的所有 CWE ID
     *
     * @param category OWASP 2021 分類
     * @return CWE ID 列表
     */
    public static List<String> getCwesByCategory(Owasp2021Category category) {
        if (category == null) {
            return Collections.emptyList();
        }

        return CWE_TO_OWASP_2021.entrySet().stream()
            .filter(entry -> entry.getValue() == category)
            .map(Map.Entry::getKey)
            .sorted()
            .toList();
    }

    /**
     * 獲取所有已映射的 CWE ID
     *
     * @return CWE ID 列表
     */
    public static List<String> getAllMappedCwes() {
        return new ArrayList<>(CWE_TO_OWASP_2021.keySet()).stream()
            .sorted()
            .toList();
    }

    /**
     * 獲取映射總數
     *
     * @return 映射數量
     */
    public static int getMappingCount() {
        return CWE_TO_OWASP_2021.size();
    }

    /**
     * 檢查 CWE ID 是否已映射到 OWASP 2021
     *
     * @param cweId CWE ID
     * @return 是否已映射
     */
    public static boolean isMapped(String cweId) {
        return getOwaspCategory(cweId) != null;
    }
}
