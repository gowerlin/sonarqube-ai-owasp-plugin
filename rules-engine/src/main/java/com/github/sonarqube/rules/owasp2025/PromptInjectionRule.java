package com.github.sonarqube.rules.owasp2025;

import com.github.sonarqube.rules.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * OWASP 2025 A03 (Preview): Prompt Injection 檢測規則
 *
 * 檢測 AI/LLM 提示詞注入攻擊漏洞，這是 OWASP 2025 新增的關鍵安全風險。
 *
 * 檢測類型：
 * - Direct Prompt Injection (直接提示詞注入)
 * - Indirect Prompt Injection (間接提示詞注入，透過訓練資料)
 * - System Prompt Bypass (系統提示詞繞過)
 * - Excessive Agency (LLM 被賦予過多權限)
 * - Training Data Poisoning (訓練資料投毒)
 *
 * CWE 映射：
 * - CWE-1236 (Improper Neutralization of Formula Elements in a CSV File)
 * - CWE-20 (Improper Input Validation)
 * - CWE-74 (Improper Neutralization of Special Elements)
 * - CWE-77 (Improper Neutralization of Command Elements)
 * - CWE-94 (Improper Control of Generation of Code)
 *
 * 參考：OWASP Top 10 for LLM Applications 2025
 * - LLM01: Prompt Injection
 * - LLM03: Training Data Poisoning
 * - LLM08: Excessive Agency
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.6.0 (Epic 6, Story 6.2 - OWASP 2025 Preview)
 */
public class PromptInjectionRule extends AbstractOwaspRule {

    private static final String RULE_ID = "owasp-2025-a03-prompt-injection";
    private static final String RULE_NAME = "Prompt Injection Vulnerabilities Detection (PREVIEW)";
    private static final String DESCRIPTION = "[PREVIEW] Detects AI/LLM prompt injection vulnerabilities " +
        "where user input is directly concatenated into system prompts without proper isolation or validation.";

    // Direct Prompt Injection 模式：使用者輸入直接串接至提示詞
    private static final Pattern DIRECT_PROMPT_INJECTION_PATTERN = Pattern.compile(
        "(?:prompt|systemPrompt|userMessage|llmInput)\\s*[+]\\s*(?:request\\.|params\\.|input\\.|user\\.)",
        Pattern.CASE_INSENSITIVE
    );

    // System Prompt Bypass 模式：缺少提示詞隔離
    private static final Pattern SYSTEM_PROMPT_BYPASS_PATTERN = Pattern.compile(
        "(?:\"You are a|\"Act as|\"System:|\"Assistant:).*\\+.*(?:request\\.|params\\.|input\\.|user\\.)",
        Pattern.CASE_INSENSITIVE
    );

    // LLM API 調用模式（OpenAI, Claude, Gemini 等）
    private static final Pattern LLM_API_CALL_PATTERN = Pattern.compile(
        "(?:openai|claude|gemini|llm|gpt|chatgpt|anthropic)\\.(?:chat|complete|generate|analyze|create).*\\(",
        Pattern.CASE_INSENSITIVE
    );

    // Excessive Agency 模式：LLM 可執行系統命令
    private static final Pattern EXCESSIVE_AGENCY_PATTERN = Pattern.compile(
        "@Tool|@Function.*(?:exec|execute|run|shell|cmd|bash|powershell)",
        Pattern.CASE_INSENSITIVE
    );

    // Training Data 模式：使用者輸入直接用於訓練
    private static final Pattern TRAINING_DATA_POISONING_PATTERN = Pattern.compile(
        "(?:train|fit|fine_?tune).*(?:request\\.|params\\.|input\\.|user\\.)",
        Pattern.CASE_INSENSITIVE
    );

    public PromptInjectionRule() {
        super(RuleDefinition.builder(RULE_ID)
            .name(RULE_NAME)
            .description(DESCRIPTION)
            .severity(RuleDefinition.RuleSeverity.CRITICAL)
            .type(RuleDefinition.RuleType.VULNERABILITY)
            .language("java")
            .tag("owasp-2025")
            .tag("security")
            .tag("ai")
            .tag("llm")
            .tag("prompt-injection")
            .cweId("CWE-1236")
            .cweId("CWE-20")
            .cweId("CWE-74")
            .cweId("CWE-77")
            .cweId("CWE-94")
            .build());
    }

    @Override
    public boolean matches(RuleContext context) {
        String code = context.getCode();

        // 快速過濾：必須包含 AI/LLM 相關關鍵字或提示詞模式
        return code.toLowerCase().contains("prompt")
            || code.toLowerCase().contains("llm")
            || code.toLowerCase().contains("openai")
            || code.toLowerCase().contains("claude")
            || code.toLowerCase().contains("gemini")
            || code.toLowerCase().contains("gpt")
            || code.toLowerCase().contains("chatgpt")
            || code.toLowerCase().contains("anthropic");
    }

    @Override
    protected RuleResult doExecute(RuleContext context) {
        RuleResult.Builder resultBuilder = RuleResult.builder(getRuleId())
            .success(true);

        String code = context.getCode();
        List<RuleResult.RuleViolation> violations = new ArrayList<>();

        // 檢測 1: Direct Prompt Injection (使用者輸入直接串接至提示詞)
        violations.addAll(detectDirectPromptInjection(code));

        // 檢測 2: System Prompt Bypass (缺少提示詞隔離)
        violations.addAll(detectSystemPromptBypass(code));

        // 檢測 3: Excessive Agency (LLM 被賦予過多權限)
        violations.addAll(detectExcessiveAgency(code));

        // 檢測 4: Training Data Poisoning (訓練資料投毒)
        violations.addAll(detectTrainingDataPoisoning(code));

        if (!violations.isEmpty()) {
            resultBuilder.violations(violations);
        }

        return resultBuilder.build();
    }

    /**
     * 檢測直接提示詞注入：使用者輸入直接串接至提示詞
     */
    private List<RuleResult.RuleViolation> detectDirectPromptInjection(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();

        if (containsPattern(code, DIRECT_PROMPT_INJECTION_PATTERN)) {
            List<Integer> lines = findMatchingLines(code, DIRECT_PROMPT_INJECTION_PATTERN);
            for (Integer lineNumber : lines) {
                violations.add(createViolation(
                    lineNumber,
                    "Direct Prompt Injection: 使用者輸入直接串接至提示詞，可能導致提示詞注入攻擊",
                    code,
                    "建議修復：\n" +
                        "1. 使用結構化提示詞（JSON 格式，分離 system 與 user 訊息）\n" +
                        "2. 實現輸入驗證與消毒（過濾特殊字元如 '\\n', '\\r', '<|im_end|>' 等）\n" +
                        "3. 使用提示詞模板引擎（如 LangChain PromptTemplate）\n" +
                        "4. 實現輸出驗證（檢查 LLM 回應是否符合預期格式）\n\n" +
                        "範例修復：\n" +
                        "// ❌ 不安全\n" +
                        "String prompt = systemPrompt + \"\\nUser: \" + userInput;\n\n" +
                        "// ✅ 安全\n" +
                        "PromptTemplate template = new PromptTemplate(\n" +
                        "    \"system\": \"You are a helpful assistant.\",\n" +
                        "    \"user\": \"{user_input}\"\n" +
                        ");\n" +
                        "String prompt = template.format(Map.of(\"user_input\", sanitizeInput(userInput)));"
                ));
            }
        }

        return violations;
    }

    /**
     * 檢測系統提示詞繞過：缺少提示詞隔離機制
     */
    private List<RuleResult.RuleViolation> detectSystemPromptBypass(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();

        if (containsPattern(code, SYSTEM_PROMPT_BYPASS_PATTERN)) {
            List<Integer> lines = findMatchingLines(code, SYSTEM_PROMPT_BYPASS_PATTERN);
            for (Integer lineNumber : lines) {
                violations.add(createViolation(
                    lineNumber,
                    "System Prompt Bypass: 系統提示詞與使用者輸入未正確隔離，可能被繞過",
                    code,
                    "建議修復：\n" +
                        "1. 使用 ChatML 格式明確區分 system 和 user 角色\n" +
                        "2. 實現提示詞注入攻擊偵測（檢查 '忽略以上指示' 等模式）\n" +
                        "3. 使用 LLM Guard 或類似工具進行提示詞防護\n" +
                        "4. 限制 LLM 輸出長度與格式\n\n" +
                        "範例修復：\n" +
                        "// ❌ 不安全\n" +
                        "String prompt = \"You are a helpful assistant.\\n\\nUser: \" + userInput;\n\n" +
                        "// ✅ 安全（ChatML 格式）\n" +
                        "List<Message> messages = List.of(\n" +
                        "    new Message(\"system\", \"You are a helpful assistant.\"),\n" +
                        "    new Message(\"user\", sanitizeAndValidate(userInput))\n" +
                        ");"
                ));
            }
        }

        return violations;
    }

    /**
     * 檢測過度授權：LLM 被賦予執行系統命令等危險權限
     */
    private List<RuleResult.RuleViolation> detectExcessiveAgency(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();

        if (containsPattern(code, EXCESSIVE_AGENCY_PATTERN)) {
            List<Integer> lines = findMatchingLines(code, EXCESSIVE_AGENCY_PATTERN);
            for (Integer lineNumber : lines) {
                violations.add(createViolation(
                    lineNumber,
                    "Excessive Agency: LLM 被賦予過多權限（如執行系統命令），存在高風險",
                    code,
                    "建議修復：\n" +
                        "1. 實現最小權限原則（只賦予 LLM 必要的 API 權限）\n" +
                        "2. 使用白名單限制 LLM 可調用的函式\n" +
                        "3. 實現人工確認機制（高風險操作需人工批准）\n" +
                        "4. 記錄所有 LLM 執行的操作（審計追蹤）\n\n" +
                        "範例修復：\n" +
                        "// ❌ 不安全\n" +
                        "@Tool(\"execute_shell_command\")\n" +
                        "public String executeCommand(String cmd) {\n" +
                        "    return Runtime.getRuntime().exec(cmd);\n" +
                        "}\n\n" +
                        "// ✅ 安全\n" +
                        "@Tool(\"list_files\")\n" +
                        "@RequireHumanApproval  // 高風險操作需人工確認\n" +
                        "public List<String> listFiles(String directory) {\n" +
                        "    if (!isWhitelisted(directory)) throw new SecurityException();\n" +
                        "    // 只允許讀取操作，不允許執行\n" +
                        "    return Files.list(Path.of(directory)).toList();\n" +
                        "}"
                ));
            }
        }

        return violations;
    }

    /**
     * 檢測訓練資料投毒：使用者輸入直接用於 AI 訓練
     */
    private List<RuleResult.RuleViolation> detectTrainingDataPoisoning(String code) {
        List<RuleResult.RuleViolation> violations = new ArrayList<>();

        if (containsPattern(code, TRAINING_DATA_POISONING_PATTERN)) {
            List<Integer> lines = findMatchingLines(code, TRAINING_DATA_POISONING_PATTERN);
            for (Integer lineNumber : lines) {
                violations.add(createViolation(
                    lineNumber,
                    "Training Data Poisoning: 使用者輸入直接用於 AI 訓練，可能導致模型投毒攻擊",
                    code,
                    "建議修復：\n" +
                        "1. 實現訓練資料驗證與過濾機制\n" +
                        "2. 使用資料來源白名單（只接受可信來源）\n" +
                        "3. 實現異常偵測（識別惡意訓練資料）\n" +
                        "4. 定期重新訓練模型以減少投毒影響\n" +
                        "5. 實現聯邦學習或差分隱私保護\n\n" +
                        "範例修復：\n" +
                        "// ❌ 不安全\n" +
                        "model.train(userInput);\n\n" +
                        "// ✅ 安全\n" +
                        "if (isFromTrustedSource(userInput) && passesAnomalyDetection(userInput)) {\n" +
                        "    TrainingData validatedData = sanitizeAndValidate(userInput);\n" +
                        "    model.train(validatedData);\n" +
                        "    logTrainingDataSource(validatedData);  // 審計追蹤\n" +
                        "}"
                ));
            }
        }

        return violations;
    }

    @Override
    public boolean requiresAi() {
        // Prompt Injection 規則強烈建議使用 AI 進行語義分析
        // 因為注入攻擊可能非常隱蔽（如使用自然語言繞過）
        return true;
    }
}
