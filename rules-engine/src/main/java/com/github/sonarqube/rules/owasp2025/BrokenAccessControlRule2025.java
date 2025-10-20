package com.github.sonarqube.rules.owasp2025;

import com.github.sonarqube.rules.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2025 A01 (Preview): Broken Access Control 檢測規則
 *
 * 這是 OWASP 2021 A01 的擴展版本，新增了：
 * - API 授權漏洞檢測（GraphQL, REST API）
 * - 雲端 IAM 錯誤配置檢測（AWS, Azure, GCP）
 * - 微服務授權檢測（服務間通信）
 *
 * 繼承 OWASP 2021 的核心檢測：
 * - Path Traversal (路徑遍歷)
 * - Unsafe File Operations (不安全檔案操作)
 * - Insecure Direct Object Reference (不安全直接物件參考)
 * - Missing Authorization (缺少授權檢查)
 * - Unsafe Redirect (開放重導向)
 *
 * CWE 映射：CWE-22, CWE-23, CWE-35, CWE-36, CWE-73, CWE-99, CWE-180,
 * CWE-200, CWE-250, CWE-256, CWE-266, CWE-269, CWE-276, CWE-284, CWE-285,
 * CWE-352, CWE-359, CWE-377, CWE-425, CWE-441, CWE-497, CWE-538, CWE-552,
 * CWE-566, CWE-601, CWE-639, CWE-651, CWE-668, CWE-706, CWE-862, CWE-863,
 * CWE-913 (共 33 個 CWE)
 *
 * OWASP 2025 新增 CWE：
 * - CWE-1270 (Generation of Incorrect Security Identifiers)
 * - CWE-1390 (Weak Authentication)
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.6.0 (Epic 6, Story 6.2 - OWASP 2025 Preview)
 */
public class BrokenAccessControlRule2025 extends AbstractOwaspRule {

    private static final String RULE_ID = "owasp-2025-a01-001";
    private static final String RULE_NAME = "Broken Access Control Detection (PREVIEW)";
    private static final String DESCRIPTION = "[PREVIEW] Detects broken access control vulnerabilities " +
        "including path traversal, missing authorization, insecure direct object references, API authorization " +
        "bypass, and cloud IAM misconfigurations.";

    // API 授權漏洞模式（GraphQL, REST）
    private static final Pattern API_AUTH_BYPASS_PATTERN = Pattern.compile(
        "(?:@GetMapping|@PostMapping|@PutMapping|@DeleteMapping|@RequestMapping|@Query|@Mutation)(?!.*@PreAuthorize|.*@Secured|.*@RolesAllowed)",
        Pattern.CASE_INSENSITIVE
    );

    // GraphQL 授權缺失模式
    private static final Pattern GRAPHQL_AUTH_MISSING_PATTERN = Pattern.compile(
        "@(?:Query|Mutation|Subscription)\\s*\\([^)]*\\)\\s*(?!.*@PreAuthorize|.*@Authorize)",
        Pattern.CASE_INSENSITIVE
    );

    // 雲端 IAM 錯誤配置模式（AWS, Azure, GCP）
    private static final Pattern CLOUD_IAM_MISCONFIGURATION_PATTERN = Pattern.compile(
        "(?:\"Principal\"\\s*:\\s*\"\\*\"|publicRead|public-read|AllUsers|allAuthenticatedUsers|Action.*\\*)",
        Pattern.CASE_INSENSITIVE
    );

    // 微服務授權缺失模式（Feign, RestTemplate）
    private static final Pattern MICROSERVICE_AUTH_MISSING_PATTERN = Pattern.compile(
        "(?:@FeignClient|RestTemplate|WebClient).*(?!.*Authorization|.*Bearer|.*OAuth)",
        Pattern.CASE_INSENSITIVE
    );

    // Path Traversal 模式（繼承自 2021）
    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "\\.\\.\\/|\\.\\.\\\\\"|%2e%2e%2f|%2e%2e%5c|\\.\\.%2F|\\.\\.%5C",
        Pattern.CASE_INSENSITIVE
    );

    // 缺少授權檢查模式（繼承自 2021）
    private static final Pattern MISSING_AUTHORIZATION_PATTERN = Pattern.compile(
        "@(?:GetMapping|PostMapping|PutMapping|DeleteMapping).*(?!/admin/|/api/admin/)(?!.*@PreAuthorize|.*@Secured)",
        Pattern.CASE_INSENSITIVE
    );

    public BrokenAccessControlRule2025() {
        super(createRuleDefinition());
    }

    private static RuleDefinition createRuleDefinition() {
        return RuleDefinition.builder(RULE_ID)
            .name(RULE_NAME)
            .description(DESCRIPTION)
            .severity(RuleDefinition.RuleSeverity.BLOCKER)
            .type(RuleDefinition.RuleType.VULNERABILITY)
            .owaspCategory("A01")  // OWASP 2025 A01: Broken Access Control
            .cweId("CWE-22").cweId("CWE-284").cweId("CWE-639").cweId("CWE-862").cweId("CWE-863")  // 核心 CWE
            .cweId("CWE-1270").cweId("CWE-1390")  // OWASP 2025 新增
            .language("java")
            .tag("owasp-2025").tag("security").tag("access-control")
            .build();
    }

    @Override
    public boolean matches(RuleContext context) {
        if (!super.matches(context)) {
            return false;
        }

        String code = context.getCode();

        // 快速過濾：必須包含 API、授權或檔案操作相關關鍵字
        return code.contains("@GetMapping")
            || code.contains("@PostMapping")
            || code.contains("@Query")
            || code.contains("@Mutation")
            || code.contains("@FeignClient")
            || code.contains("RestTemplate")
            || code.contains("File")
            || code.contains("Principal")
            || code.contains("AllUsers");
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        String code = context.getCode();
        List<RuleResult.RuleViolation> violations = new ArrayList<>();

        // OWASP 2025 新增檢測
        violations.addAll(detectApiAuthorizationBypass(code));
        violations.addAll(detectGraphQLAuthMissing(code));
        violations.addAll(detectCloudIamMisconfiguration(code));
        violations.addAll(detectMicroserviceAuthMissing(code));

        // OWASP 2021 繼承檢測
        violations.addAll(detectPathTraversal(code));
        violations.addAll(detectMissingAuthorization(code));

        return RuleResult.builder(getRuleId())
            .success(true)
            .violations(violations)
            .build();
    }

    /**
     * OWASP 2025 新增：檢測 API 授權繞過
     */
    private List<RuleResult.RuleViolation> detectApiAuthorizationBypass(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();

        if (containsPattern(code, API_AUTH_BYPASS_PATTERN)) {
            List<Integer> lines = findMatchingLines(code, API_AUTH_BYPASS_PATTERN);
            for (Integer lineNumber : lines) {
                violations.add(createViolation(
                    lineNumber,
                    "API Authorization Bypass: API 端點缺少授權檢查 (@PreAuthorize, @Secured) (CWE-862)",
                    code,
                    "建議修復：\n" +
                        "1. 為所有 API 端點新增 @PreAuthorize 或 @Secured 註解\n" +
                        "2. 實現基於角色的存取控制 (RBAC)\n" +
                        "3. 使用 Spring Security 全域方法安全配置\n\n" +
                        "範例修復：\n" +
                        "// ❌ 不安全\n" +
                        "@GetMapping(\"/api/users\")\n" +
                        "public List<User> getUsers() { ... }\n\n" +
                        "// ✅ 安全\n" +
                        "@GetMapping(\"/api/users\")\n" +
                        "@PreAuthorize(\"hasRole('ADMIN')\")\n" +
                        "public List<User> getUsers() { ... }"
                ));
            }
        }

        return violations;
    }

    /**
     * OWASP 2025 新增：檢測 GraphQL 授權缺失
     */
    private List<RuleResult.RuleViolation> detectGraphQLAuthMissing(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();

        if (containsPattern(code, GRAPHQL_AUTH_MISSING_PATTERN)) {
            List<Integer> lines = findMatchingLines(code, GRAPHQL_AUTH_MISSING_PATTERN);
            for (Integer lineNumber : lines) {
                violations.add(createViolation(
                    lineNumber,
                    "GraphQL Authorization Missing: GraphQL Resolver 缺少授權檢查 (CWE-862)",
                    code,
                    "建議修復：\n" +
                        "1. 為 GraphQL Resolver 新增 @PreAuthorize 註解\n" +
                        "2. 實現 GraphQL Directive 進行授權檢查\n" +
                        "3. 使用 DataFetcherExceptionHandler 統一處理授權失敗\n\n" +
                        "範例修復：\n" +
                        "// ❌ 不安全\n" +
                        "@Query\n" +
                        "public List<User> users() { ... }\n\n" +
                        "// ✅ 安全\n" +
                        "@Query\n" +
                        "@PreAuthorize(\"hasRole('ADMIN')\")\n" +
                        "public List<User> users() { ... }"
                ));
            }
        }

        return violations;
    }

    /**
     * OWASP 2025 新增：檢測雲端 IAM 錯誤配置
     */
    private List<RuleResult.RuleViolation> detectCloudIamMisconfiguration(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();

        if (containsPattern(code, CLOUD_IAM_MISCONFIGURATION_PATTERN)) {
            List<Integer> lines = findMatchingLines(code, CLOUD_IAM_MISCONFIGURATION_PATTERN);
            for (Integer lineNumber : lines) {
                violations.add(createViolation(
                    lineNumber,
                    "Cloud IAM Misconfiguration: 雲端資源配置允許公開存取或過寬權限 (CWE-284)",
                    code,
                    "建議修復：\n" +
                        "1. AWS S3: 移除 'public-read' ACL，使用 Bucket Policy 精確控制\n" +
                        "2. AWS IAM: 避免 'Action: *' 和 'Principal: *'，遵循最小權限原則\n" +
                        "3. Azure: 移除 'AllUsers' 和 'allAuthenticatedUsers' 權限\n" +
                        "4. GCP: 使用 IAM Conditions 進行細粒度存取控制\n\n" +
                        "範例修復：\n" +
                        "// ❌ 不安全 (AWS S3)\n" +
                        "{\n" +
                        "  \"Principal\": \"*\",\n" +
                        "  \"Action\": \"s3:*\"\n" +
                        "}\n\n" +
                        "// ✅ 安全\n" +
                        "{\n" +
                        "  \"Principal\": {\"AWS\": \"arn:aws:iam::123456789012:role/MyRole\"},\n" +
                        "  \"Action\": \"s3:GetObject\",\n" +
                        "  \"Resource\": \"arn:aws:s3:::my-bucket/public/*\"\n" +
                        "}"
                ));
            }
        }

        return violations;
    }

    /**
     * OWASP 2025 新增：檢測微服務授權缺失
     */
    private List<RuleResult.RuleViolation> detectMicroserviceAuthMissing(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();

        if (containsPattern(code, MICROSERVICE_AUTH_MISSING_PATTERN)) {
            List<Integer> lines = findMatchingLines(code, MICROSERVICE_AUTH_MISSING_PATTERN);
            for (Integer lineNumber : lines) {
                violations.add(createViolation(
                    lineNumber,
                    "Microservice Authorization Missing: 微服務間調用缺少授權憑證 (CWE-862)",
                    code,
                    "建議修復：\n" +
                        "1. 使用 OAuth 2.0 Client Credentials Flow 進行服務間認證\n" +
                        "2. 實現 JWT Token 傳遞與驗證\n" +
                        "3. 使用 Service Mesh (如 Istio) 進行 mTLS 認證\n" +
                        "4. 實現 API Gateway 集中授權檢查\n\n" +
                        "範例修復：\n" +
                        "// ❌ 不安全\n" +
                        "@FeignClient(\"user-service\")\n" +
                        "public interface UserClient { ... }\n\n" +
                        "// ✅ 安全\n" +
                        "@FeignClient(name = \"user-service\", configuration = OAuth2FeignConfig.class)\n" +
                        "public interface UserClient { ... }\n\n" +
                        "// OAuth2FeignConfig.java\n" +
                        "@Configuration\n" +
                        "public class OAuth2FeignConfig {\n" +
                        "    @Bean\n" +
                        "    public RequestInterceptor oauth2FeignRequestInterceptor() {\n" +
                        "        return requestTemplate -> {\n" +
                        "            String token = getServiceToken();\n" +
                        "            requestTemplate.header(\"Authorization\", \"Bearer \" + token);\n" +
                        "        };\n" +
                        "    }\n" +
                        "}"
                ));
            }
        }

        return violations;
    }

    /**
     * OWASP 2021 繼承：檢測路徑遍歷攻擊
     */
    private List<RuleResult.RuleViolation> detectPathTraversal(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();

        if (containsPattern(code, PATH_TRAVERSAL_PATTERN)) {
            List<Integer> lines = findMatchingLines(code, PATH_TRAVERSAL_PATTERN);
            for (Integer lineNumber : lines) {
                violations.add(createViolation(
                    lineNumber,
                    "Path Traversal: 路徑遍歷攻擊風險（../ 或編碼變體） (CWE-22)",
                    code,
                    "建議修復：使用白名單驗證檔案路徑，禁止 ../ 等特殊字元"
                ));
            }
        }

        return violations;
    }

    /**
     * OWASP 2021 繼承：檢測缺少授權檢查
     */
    private List<RuleResult.RuleViolation> detectMissingAuthorization(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();

        if (containsPattern(code, MISSING_AUTHORIZATION_PATTERN)) {
            List<Integer> lines = findMatchingLines(code, MISSING_AUTHORIZATION_PATTERN);
            for (Integer lineNumber : lines) {
                violations.add(createViolation(
                    lineNumber,
                    "Missing Authorization: API 端點缺少授權檢查註解 (CWE-862)",
                    code,
                    "建議修復：新增 @PreAuthorize 或 @Secured 註解進行授權檢查"
                ));
            }
        }

        return violations;
    }

    @Override
    public boolean requiresAi() {
        // 授權邏輯通常需要 AI 語義分析來判斷業務邏輯是否正確
        return true;
    }
}
