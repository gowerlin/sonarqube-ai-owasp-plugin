package com.github.sonarqube.plugin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OWASP 分類映射工具
 *
 * <p>將 SonarQube rule tags、CWE ID、rule key 映射到 OWASP Top 10 2021 分類。</p>
 *
 * <p><strong>映射策略：</strong></p>
 * <ol>
 *   <li>優先從 tags 中提取 OWASP 分類 (例如: owasp-a01, owasp-2021-a01)</li>
 *   <li>次要從 CWE ID 映射到 OWASP 分類 (例如: CWE-89 → A03:2021-Injection)</li>
 *   <li>最後從 rule key 模式推斷 (例如: squid:S2077 → Injection)</li>
 * </ol>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0
 */
public class OwaspCategoryMapper {

    private static final Logger LOG = LoggerFactory.getLogger(OwaspCategoryMapper.class);

    // OWASP Top 10 2021 分類定義
    private static final Map<String, String> OWASP_2021_CATEGORIES = Map.ofEntries(
        Map.entry("A01", "A01:2021-Broken Access Control"),
        Map.entry("A02", "A02:2021-Cryptographic Failures"),
        Map.entry("A03", "A03:2021-Injection"),
        Map.entry("A04", "A04:2021-Insecure Design"),
        Map.entry("A05", "A05:2021-Security Misconfiguration"),
        Map.entry("A06", "A06:2021-Vulnerable and Outdated Components"),
        Map.entry("A07", "A07:2021-Identification and Authentication Failures"),
        Map.entry("A08", "A08:2021-Software and Data Integrity Failures"),
        Map.entry("A09", "A09:2021-Security Logging and Monitoring Failures"),
        Map.entry("A10", "A10:2021-Server-Side Request Forgery (SSRF)")
    );


    // OWASP Top 10 2017 分類定義
    private static final Map<String, String> OWASP_2017_CATEGORIES = Map.ofEntries(
        Map.entry("A01", "A1:2017-Injection"),
        Map.entry("A02", "A2:2017-Broken Authentication"),
        Map.entry("A03", "A3:2017-Sensitive Data Exposure"),
        Map.entry("A04", "A4:2017-XML External Entities (XXE)"),
        Map.entry("A05", "A5:2017-Broken Access Control"),
        Map.entry("A06", "A6:2017-Security Misconfiguration"),
        Map.entry("A07", "A7:2017-Cross-Site Scripting (XSS)"),
        Map.entry("A08", "A8:2017-Insecure Deserialization"),
        Map.entry("A09", "A9:2017-Using Components with Known Vulnerabilities"),
        Map.entry("A10", "A10:2017-Insufficient Logging and Monitoring")
    );

    // OWASP Top 10 2025 Preview 分類定義
    private static final Map<String, String> OWASP_2025_CATEGORIES = Map.ofEntries(
        Map.entry("A01", "A01:2025-Broken Access Control"),
        Map.entry("A02", "A02:2025-Cryptographic Failures"),
        Map.entry("A03", "A03:2025-Injection"),
        Map.entry("A04", "A04:2025-Insecure Design"),
        Map.entry("A05", "A05:2025-Security Misconfiguration"),
        Map.entry("A06", "A06:2025-Vulnerable and Outdated Components"),
        Map.entry("A07", "A07:2025-Identification and Authentication Failures"),
        Map.entry("A08", "A08:2025-Software and Data Integrity Failures"),
        Map.entry("A09", "A09:2025-Security Logging and Monitoring Failures"),
        Map.entry("A10", "A10:2025-Insecure Use of AI")
    );

    // (Based on official OWASP mapping)
    private static final Map<String, String> CWE_TO_OWASP = Map.ofEntries(
        // A01: Broken Access Control
        Map.entry("CWE-22", "A01"), // Path Traversal
        Map.entry("CWE-23", "A01"), // Relative Path Traversal
        Map.entry("CWE-35", "A01"), // Path Traversal: '.../...//'
        Map.entry("CWE-59", "A01"), // Improper Link Resolution Before File Access
        Map.entry("CWE-200", "A01"), // Exposure of Sensitive Information
        Map.entry("CWE-201", "A01"), // Exposure of Sensitive Information Through Sent Data
        Map.entry("CWE-219", "A01"), // Storage of File with Sensitive Data Under Web Root
        Map.entry("CWE-264", "A01"), // Permissions, Privileges, and Access Controls
        Map.entry("CWE-275", "A01"), // Permission Issues
        Map.entry("CWE-284", "A01"), // Improper Access Control
        Map.entry("CWE-285", "A01"), // Improper Authorization
        Map.entry("CWE-352", "A01"), // Cross-Site Request Forgery (CSRF)
        Map.entry("CWE-359", "A01"), // Exposure of Private Personal Information to an Unauthorized Actor
        Map.entry("CWE-377", "A01"), // Insecure Temporary File
        Map.entry("CWE-402", "A01"), // Transmission of Private Resources into a New Sphere
        Map.entry("CWE-425", "A01"), // Direct Request ('Forced Browsing')
        Map.entry("CWE-441", "A01"), // Unintended Proxy or Intermediary
        Map.entry("CWE-497", "A01"), // Exposure of Sensitive System Information
        Map.entry("CWE-538", "A01"), // Insertion of Sensitive Information into Externally-Accessible File or Directory
        Map.entry("CWE-540", "A01"), // Inclusion of Sensitive Information in Source Code
        Map.entry("CWE-548", "A01"), // Exposure of Information Through Directory Listing
        Map.entry("CWE-552", "A01"), // Files or Directories Accessible to External Parties
        Map.entry("CWE-566", "A01"), // Authorization Bypass Through User-Controlled SQL Primary Key
        Map.entry("CWE-601", "A01"), // URL Redirection to Untrusted Site ('Open Redirect')
        Map.entry("CWE-639", "A01"), // Authorization Bypass Through User-Controlled Key
        Map.entry("CWE-651", "A01"), // Exposure of WSDL File Containing Sensitive Information
        Map.entry("CWE-668", "A01"), // Exposure of Resource to Wrong Sphere
        Map.entry("CWE-706", "A01"), // Use of Incorrectly-Resolved Name or Reference
        Map.entry("CWE-862", "A01"), // Missing Authorization
        Map.entry("CWE-863", "A01"), // Incorrect Authorization
        Map.entry("CWE-913", "A01"), // Improper Control of Dynamically-Managed Code Resources
        Map.entry("CWE-922", "A01"), // Insecure Storage of Sensitive Information
        Map.entry("CWE-1275", "A01"), // Sensitive Cookie with Improper SameSite Attribute

        // A02: Cryptographic Failures
        Map.entry("CWE-261", "A02"), // Weak Encoding for Password
        Map.entry("CWE-296", "A02"), // Improper Following of a Certificate's Chain of Trust
        Map.entry("CWE-310", "A02"), // Cryptographic Issues
        Map.entry("CWE-319", "A02"), // Cleartext Transmission of Sensitive Information
        Map.entry("CWE-321", "A02"), // Use of Hard-coded Cryptographic Key
        Map.entry("CWE-322", "A02"), // Key Exchange without Entity Authentication
        Map.entry("CWE-323", "A02"), // Reusing a Nonce, Key Pair in Encryption
        Map.entry("CWE-324", "A02"), // Use of a Key Past its Expiration Date
        Map.entry("CWE-325", "A02"), // Missing Required Cryptographic Step
        Map.entry("CWE-326", "A02"), // Inadequate Encryption Strength
        Map.entry("CWE-327", "A02"), // Use of a Broken or Risky Cryptographic Algorithm
        Map.entry("CWE-328", "A02"), // Use of Weak Hash
        Map.entry("CWE-329", "A02"), // Generation of Predictable IV with CBC Mode
        Map.entry("CWE-330", "A02"), // Use of Insufficiently Random Values
        Map.entry("CWE-331", "A02"), // Insufficient Entropy
        Map.entry("CWE-335", "A02"), // Incorrect Usage of Seeds in Pseudo-Random Number Generator
        Map.entry("CWE-336", "A02"), // Same Seed in Pseudo-Random Number Generator
        Map.entry("CWE-337", "A02"), // Predictable Seed in Pseudo-Random Number Generator
        Map.entry("CWE-338", "A02"), // Use of Cryptographically Weak Pseudo-Random Number Generator
        Map.entry("CWE-340", "A02"), // Generation of Predictable Numbers or Identifiers
        Map.entry("CWE-347", "A02"), // Improper Verification of Cryptographic Signature
        Map.entry("CWE-523", "A02"), // Unprotected Transport of Credentials
        Map.entry("CWE-720", "A02"), // OWASP Top Ten 2007 Category A9 - Insecure Communications
        Map.entry("CWE-757", "A02"), // Selection of Less-Secure Algorithm During Negotiation
        Map.entry("CWE-759", "A02"), // Use of a One-Way Hash without a Salt
        Map.entry("CWE-760", "A02"), // Use of a One-Way Hash with a Predictable Salt
        Map.entry("CWE-780", "A02"), // Use of RSA Algorithm without OAEP
        Map.entry("CWE-818", "A02"), // Insufficient Transport Layer Protection
        Map.entry("CWE-916", "A02"), // Use of Password Hash With Insufficient Computational Effort

        // A03: Injection
        Map.entry("CWE-20", "A03"), // Improper Input Validation
        Map.entry("CWE-74", "A03"), // Improper Neutralization of Special Elements in Output
        Map.entry("CWE-75", "A03"), // Failure to Sanitize Special Elements into a Different Plane
        Map.entry("CWE-77", "A03"), // Improper Neutralization of Special Elements used in a Command
        Map.entry("CWE-78", "A03"), // OS Command Injection
        Map.entry("CWE-79", "A03"), // Cross-site Scripting (XSS)
        Map.entry("CWE-80", "A03"), // Improper Neutralization of Script-Related HTML Tags
        Map.entry("CWE-83", "A03"), // Improper Neutralization of Script in Attributes
        Map.entry("CWE-87", "A03"), // Improper Neutralization of Alternate XSS Syntax
        Map.entry("CWE-88", "A03"), // Improper Neutralization of Argument Delimiters in a Command
        Map.entry("CWE-89", "A03"), // SQL Injection
        Map.entry("CWE-90", "A03"), // LDAP Injection
        Map.entry("CWE-91", "A03"), // XML Injection
        Map.entry("CWE-93", "A03"), // Improper Neutralization of CRLF Sequences
        Map.entry("CWE-94", "A03"), // Improper Control of Generation of Code
        Map.entry("CWE-95", "A03"), // Improper Neutralization of Directives in Dynamically Evaluated Code
        Map.entry("CWE-96", "A03"), // Improper Neutralization of Directives in Statically Saved Code
        Map.entry("CWE-97", "A03"), // Improper Neutralization of Server-Side Includes (SSI)
        Map.entry("CWE-98", "A03"), // Improper Control of Filename for Include/Require Statement in PHP Program
        Map.entry("CWE-99", "A03"), // Improper Control of Resource Identifiers
        Map.entry("CWE-100", "A03"), // Deprecated: Was catch-all for input validation issues
        Map.entry("CWE-113", "A03"), // HTTP Response Splitting
        Map.entry("CWE-116", "A03"), // Improper Encoding or Escaping of Output
        Map.entry("CWE-138", "A03"), // Improper Neutralization of Special Elements
        Map.entry("CWE-184", "A03"), // Incomplete List of Disallowed Inputs
        Map.entry("CWE-470", "A03"), // Use of Externally-Controlled Input to Select Classes or Code
        Map.entry("CWE-471", "A03"), // Modification of Assumed-Immutable Data
        Map.entry("CWE-564", "A03"), // SQL Injection: Hibernate
        Map.entry("CWE-610", "A03"), // Externally Controlled Reference to a Resource in Another Sphere
        Map.entry("CWE-643", "A03"), // Improper Neutralization of Data within XPath Expressions
        Map.entry("CWE-644", "A03"), // Improper Neutralization of HTTP Headers for Scripting Syntax
        Map.entry("CWE-652", "A03"), // Improper Neutralization of Data within XQuery Expressions
        Map.entry("CWE-917", "A03"), // Improper Neutralization of Special Elements used in an Expression Language Statement

        // A04: Insecure Design (沒有直接的 CWE 映射，主要透過設計審查)
        Map.entry("CWE-209", "A04"), // Generation of Error Message Containing Sensitive Information
        Map.entry("CWE-256", "A04"), // Unprotected Storage of Credentials
        Map.entry("CWE-257", "A04"), // Storing Passwords in a Recoverable Format
        Map.entry("CWE-258", "A04"), // Empty Password in Configuration File
        // CWE-259 移至 A07 (Identification and Authentication Failures)
        // CWE-260 移至 A05 (Security Misconfiguration)
        Map.entry("CWE-266", "A04"), // Incorrect Privilege Assignment
        Map.entry("CWE-269", "A04"), // Improper Privilege Management
        Map.entry("CWE-280", "A04"), // Improper Handling of Insufficient Permissions or Privileges
        Map.entry("CWE-311", "A04"), // Missing Encryption of Sensitive Data
        Map.entry("CWE-312", "A04"), // Cleartext Storage of Sensitive Information
        Map.entry("CWE-313", "A04"), // Cleartext Storage in a File or on Disk
        Map.entry("CWE-316", "A04"), // Cleartext Storage of Sensitive Information in Memory
        Map.entry("CWE-419", "A04"), // Unprotected Primary Channel
        Map.entry("CWE-430", "A04"), // Deployment of Wrong Handler
        Map.entry("CWE-434", "A04"), // Unrestricted Upload of File with Dangerous Type
        Map.entry("CWE-444", "A04"), // Inconsistent Interpretation of HTTP Requests
        Map.entry("CWE-451", "A04"), // User Interface (UI) Misrepresentation of Critical Information
        Map.entry("CWE-472", "A04"), // External Control of Assumed-Immutable Web Parameter
        Map.entry("CWE-501", "A04"), // Trust Boundary Violation
        Map.entry("CWE-522", "A04"), // Insufficiently Protected Credentials
        Map.entry("CWE-525", "A04"), // Use of Web Browser Cache Containing Sensitive Information
        Map.entry("CWE-539", "A04"), // Use of Persistent Cookies Containing Sensitive Information
        Map.entry("CWE-579", "A04"), // J2EE Bad Practices: Non-serializable Object Stored in Session
        Map.entry("CWE-598", "A04"), // Use of GET Request Method With Sensitive Query Strings
        Map.entry("CWE-602", "A04"), // Client-Side Enforcement of Server-Side Security
        Map.entry("CWE-642", "A04"), // External Control of Critical State Data
        Map.entry("CWE-646", "A04"), // Reliance on File Name or Extension of Externally-Supplied File
        Map.entry("CWE-650", "A04"), // Trusting HTTP Permission Methods on the Server Side
        Map.entry("CWE-653", "A04"), // Insufficient Compartmentalization
        Map.entry("CWE-656", "A04"), // Reliance on Security Through Obscurity
        Map.entry("CWE-657", "A04"), // Violation of Secure Design Principles
        Map.entry("CWE-799", "A04"), // Improper Control of Interaction Frequency
        Map.entry("CWE-807", "A04"), // Reliance on Untrusted Inputs in a Security Decision
        Map.entry("CWE-840", "A04"), // Business Logic Errors
        Map.entry("CWE-841", "A04"), // Improper Enforcement of Behavioral Workflow
        Map.entry("CWE-927", "A04"), // Use of Implicit Intent for Sensitive Communication
        Map.entry("CWE-1021", "A04"), // Improper Restriction of Rendered UI Layers or Frames
        Map.entry("CWE-1173", "A04"), // Improper Use of Validation Framework

        // A05: Security Misconfiguration
        Map.entry("CWE-2", "A05"), // 7PK - Environment
        Map.entry("CWE-11", "A05"), // ASP.NET Misconfiguration: Creating Debug Binary
        Map.entry("CWE-13", "A05"), // ASP.NET Misconfiguration: Password in Configuration File
        Map.entry("CWE-15", "A05"), // External Control of System or Configuration Setting
        Map.entry("CWE-16", "A05"), // Configuration
        Map.entry("CWE-260", "A05"), // Password in Configuration File
        Map.entry("CWE-315", "A05"), // Cleartext Storage of Sensitive Information in a Cookie
        Map.entry("CWE-520", "A05"), // .NET Misconfiguration: Use of Impersonation
        Map.entry("CWE-526", "A05"), // Exposure of Sensitive Information Through Environmental Variables
        Map.entry("CWE-537", "A05"), // Java Runtime Error Message Containing Sensitive Information
        Map.entry("CWE-541", "A05"), // Inclusion of Sensitive Information in an Include File
        Map.entry("CWE-547", "A05"), // Use of Hard-coded, Security-relevant Constants
        Map.entry("CWE-611", "A05"), // Improper Restriction of XML External Entity Reference
        Map.entry("CWE-614", "A05"), // Sensitive Cookie in HTTPS Session Without 'Secure' Attribute
        Map.entry("CWE-756", "A05"), // Missing Custom Error Page
        Map.entry("CWE-776", "A05"), // Improper Restriction of Recursive Entity References in DTDs
        Map.entry("CWE-942", "A05"), // Permissive Cross-domain Policy with Untrusted Domains
        Map.entry("CWE-1004", "A05"), // Sensitive Cookie Without 'HttpOnly' Flag
        Map.entry("CWE-1032", "A05"), // OWASP Top Ten 2017 Category A6 - Security Misconfiguration
        Map.entry("CWE-1174", "A05"), // ASP.NET Misconfiguration: Improper Model Validation

        // A06: Vulnerable and Outdated Components
        Map.entry("CWE-937", "A06"), // OWASP Top Ten 2013 Category A9 - Using Components with Known Vulnerabilities
        Map.entry("CWE-1035", "A06"), // OWASP Top Ten 2017 Category A9 - Using Components with Known Vulnerabilities
        Map.entry("CWE-1104", "A06"), // Use of Unmaintained Third Party Components

        // A07: Identification and Authentication Failures
        Map.entry("CWE-255", "A07"), // Credentials Management
        Map.entry("CWE-259", "A07"), // Use of Hard-coded Password
        Map.entry("CWE-287", "A07"), // Improper Authentication
        Map.entry("CWE-288", "A07"), // Authentication Bypass Using an Alternate Path or Channel
        Map.entry("CWE-290", "A07"), // Authentication Bypass by Spoofing
        Map.entry("CWE-294", "A07"), // Authentication Bypass by Capture-replay
        Map.entry("CWE-295", "A07"), // Improper Certificate Validation
        Map.entry("CWE-297", "A07"), // Improper Validation of Certificate with Host Mismatch
        Map.entry("CWE-300", "A07"), // Channel Accessible by Non-Endpoint
        Map.entry("CWE-302", "A07"), // Authentication Bypass by Assumed-Immutable Data
        Map.entry("CWE-304", "A07"), // Missing Critical Step in Authentication
        Map.entry("CWE-306", "A07"), // Missing Authentication for Critical Function
        Map.entry("CWE-307", "A07"), // Improper Restriction of Excessive Authentication Attempts
        Map.entry("CWE-346", "A07"), // Origin Validation Error
        Map.entry("CWE-384", "A07"), // Session Fixation
        Map.entry("CWE-521", "A07"), // Weak Password Requirements
        Map.entry("CWE-613", "A07"), // Insufficient Session Expiration
        Map.entry("CWE-620", "A07"), // Unverified Password Change
        Map.entry("CWE-640", "A07"), // Weak Password Recovery Mechanism for Forgotten Password
        Map.entry("CWE-798", "A07"), // Use of Hard-coded Credentials
        Map.entry("CWE-940", "A07"), // Improper Verification of Source of a Communication Channel
        Map.entry("CWE-1216", "A07"), // Lockout Mechanism Errors

        // A08: Software and Data Integrity Failures
        Map.entry("CWE-345", "A08"), // Insufficient Verification of Data Authenticity
        Map.entry("CWE-353", "A08"), // Missing Support for Integrity Check
        Map.entry("CWE-426", "A08"), // Untrusted Search Path
        Map.entry("CWE-494", "A08"), // Download of Code Without Integrity Check
        Map.entry("CWE-502", "A08"), // Deserialization of Untrusted Data
        Map.entry("CWE-565", "A08"), // Reliance on Cookies without Validation and Integrity Checking
        Map.entry("CWE-784", "A08"), // Reliance on Cookies without Validation and Integrity Checking in a Security Decision
        Map.entry("CWE-829", "A08"), // Inclusion of Functionality from Untrusted Control Sphere
        Map.entry("CWE-830", "A08"), // Inclusion of Web Functionality from an Untrusted Source
        Map.entry("CWE-915", "A08"), // Improperly Controlled Modification of Dynamically-Determined Object Attributes

        // A09: Security Logging and Monitoring Failures
        Map.entry("CWE-117", "A09"), // Improper Output Neutralization for Logs
        Map.entry("CWE-223", "A09"), // Omission of Security-relevant Information
        Map.entry("CWE-532", "A09"), // Insertion of Sensitive Information into Log File
        Map.entry("CWE-778", "A09"), // Insufficient Logging

        // A10: Server-Side Request Forgery (SSRF)
        Map.entry("CWE-918", "A10") // Server-Side Request Forgery (SSRF)
    );

    // Tag 模式識別
    private static final Pattern OWASP_TAG_PATTERN = Pattern.compile("owasp(?:-2021)?-a(\\d{2})", Pattern.CASE_INSENSITIVE);
    private static final Pattern CWE_PATTERN = Pattern.compile("CWE-(\\d+)", Pattern.CASE_INSENSITIVE);

    /**
     * 從 SonarQube issue tags 和 rule key 映射到 OWASP 分類
     *
     * @param tags SonarQube issue tags (例如: ["owasp-a03", "cwe", "injection"])
     * @param ruleKey SonarQube rule key (例如: "java:S2077")
     * @return OWASP 分類 (例如: "A03:2021-Injection")，若無法映射則返回 null
     */
    public static String mapToOwaspCategory(List<String> tags, String ruleKey, String owaspVersion) {
        if (tags == null || tags.isEmpty()) {
            LOG.debug("No tags provided for rule: {}", ruleKey);
            return null;
        }

        // Select category map based on OWASP version
        Map<String, String> categoryMap;
        switch (owaspVersion) {
            case "2017":
                categoryMap = OWASP_2017_CATEGORIES;
                break;
            case "2025":
                categoryMap = OWASP_2025_CATEGORIES;
                break;
            default:
                categoryMap = OWASP_2021_CATEGORIES;
        }

        // 策略 1: 從 tags 中提取 OWASP 分類
        for (String tag : tags) {
            Matcher matcher = OWASP_TAG_PATTERN.matcher(tag);
            if (matcher.find()) {
                String categoryCode = "A" + matcher.group(1);
                String category = categoryMap.get(categoryCode);
                if (category != null) {
                    LOG.debug("Mapped rule {} to OWASP category {} via tag: {}", ruleKey, category, tag);
                    return category;
                }
            }
        }

        // 策略 2: 從 tags 中提取 CWE ID 並映射
        for (String tag : tags) {
            Matcher matcher = CWE_PATTERN.matcher(tag);
            if (matcher.find()) {
                String cweId = "CWE-" + matcher.group(1);
                String categoryCode = CWE_TO_OWASP.get(cweId);
                if (categoryCode != null) {
                    String category = categoryMap.get(categoryCode);
                    LOG.debug("Mapped rule {} to OWASP category {} via CWE: {}", ruleKey, category, cweId);
                    return category;
                }
            }
        }

        // 策略 3: 基於常見 tag 關鍵字推斷
        String inferredCategory = inferCategoryFromTags(tags, ruleKey, categoryMap);
        if (inferredCategory != null) {
            return inferredCategory;
        }

        LOG.debug("Could not map rule {} with tags {} to any OWASP category", ruleKey, tags);
        return null;
    }

    /**
     * 從 tag 關鍵字推斷 OWASP 分類
     */
    private static String inferCategoryFromTags(List<String> tags, String ruleKey, Map<String, String> categoryMap) {
        Set<String> tagSet = new HashSet<>();
        for (String tag : tags) {
            tagSet.add(tag.toLowerCase());
        }

        // A03: Injection
        if (containsAny(tagSet, "injection", "sql", "xss", "ldap", "xml", "command-injection", "code-injection")) {
            LOG.debug("Inferred A03:Injection for rule {} via tags", ruleKey);
            return categoryMap.get("A03");
        }

        // A02: Cryptographic Failures
        if (containsAny(tagSet, "crypto", "cryptography", "encryption", "hash", "weak-cipher", "random", "ssl", "tls")) {
            LOG.debug("Inferred A02:Cryptographic Failures for rule {} via tags", ruleKey);
            return categoryMap.get("A02");
        }

        // A01: Broken Access Control
        if (containsAny(tagSet, "access-control", "authorization", "permission", "csrf", "path-traversal", "directory-traversal")) {
            LOG.debug("Inferred A01:Broken Access Control for rule {} via tags", ruleKey);
            return categoryMap.get("A01");
        }

        // A07: Authentication Failures
        if (containsAny(tagSet, "authentication", "credential", "password", "session", "cookie")) {
            LOG.debug("Inferred A07:Authentication Failures for rule {} via tags", ruleKey);
            return categoryMap.get("A07");
        }

        // A05: Security Misconfiguration
        if (containsAny(tagSet, "configuration", "misconfiguration", "default", "debug", "error-handling")) {
            LOG.debug("Inferred A05:Security Misconfiguration for rule {} via tags", ruleKey);
            return categoryMap.get("A05");
        }

        // A08: Software and Data Integrity Failures
        if (containsAny(tagSet, "deserialization", "integrity", "insecure-deserialization", "untrusted-data")) {
            LOG.debug("Inferred A08:Software and Data Integrity Failures for rule {} via tags", ruleKey);
            return categoryMap.get("A08");
        }

        // A09: Security Logging and Monitoring Failures
        if (containsAny(tagSet, "logging", "monitoring", "log-injection", "audit")) {
            LOG.debug("Inferred A09:Security Logging and Monitoring Failures for rule {} via tags", ruleKey);
            return categoryMap.get("A09");
        }

        // A10: SSRF
        if (containsAny(tagSet, "ssrf", "server-side-request-forgery", "url-redirect")) {
            LOG.debug("Inferred A10:SSRF for rule {} via tags", ruleKey);
            return categoryMap.get("A10");
        }

        // A06: Vulnerable and Outdated Components
        if (containsAny(tagSet, "dependency", "vulnerable-dependency", "outdated", "cve")) {
            LOG.debug("Inferred A06:Vulnerable and Outdated Components for rule {} via tags", ruleKey);
            return categoryMap.get("A06");
        }

        return null;
    }

    /**
     * 檢查集合是否包含任何指定的元素
     */
    private static boolean containsAny(Set<String> set, String... elements) {
        for (String element : elements) {
            if (set.contains(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 從 CWE ID 提取 OWASP 分類
     *
     * @param cweId CWE ID (例如: "CWE-89")
     * @return OWASP 分類 (例如: "A03:2021-Injection")，若無法映射則返回 null
     */
    public static String mapCweToOwasp(String cweId, String owaspVersion) {
        if (cweId == null || cweId.isEmpty()) {
            return null;
        }

        // Select category map based on OWASP version
        Map<String, String> categoryMap;
        switch (owaspVersion) {
            case "2017":
                categoryMap = OWASP_2017_CATEGORIES;
                break;
            case "2025":
                categoryMap = OWASP_2025_CATEGORIES;
                break;
            default:
                categoryMap = OWASP_2021_CATEGORIES;
        }

        String categoryCode = CWE_TO_OWASP.get(cweId);
        if (categoryCode != null) {
            return categoryMap.get(categoryCode);
        }

        return null;
    }
}
