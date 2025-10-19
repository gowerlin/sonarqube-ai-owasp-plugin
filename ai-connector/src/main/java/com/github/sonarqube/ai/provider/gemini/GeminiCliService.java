package com.github.sonarqube.ai.provider.gemini;

import com.github.sonarqube.ai.AiException;
import com.github.sonarqube.ai.cli.CliExecutor;
import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiRequest;
import com.github.sonarqube.ai.model.AiResponse;
import com.github.sonarqube.ai.model.Finding;
import com.github.sonarqube.ai.provider.cli.AbstractCliService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Google Gemini CLI 服務實作
 *
 * 使用 Gemini CLI 工具進行代碼安全分析。
 *
 * CLI 安裝方式:
 * <pre>
 * npm install -g @google/generative-ai-cli
 * </pre>
 *
 * CLI 使用範例:
 * <pre>
 * gemini chat "分析這段代碼的安全問題"
 * </pre>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9, Story 9.4)
 */
public class GeminiCliService extends AbstractCliService {

    private static final Logger LOG = LoggerFactory.getLogger(GeminiCliService.class);

    // Gemini CLI 命令結構
    private static final String CLI_SUBCOMMAND = "chat";

    // 輸出解析的正則表達式模式
    private static final Pattern SEVERITY_PATTERN = Pattern.compile(
        "(?i)severity[:\\s]+(BLOCKER|CRITICAL|MAJOR|MINOR|INFO)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern OWASP_PATTERN = Pattern.compile(
        "(A\\d{2}:\\d{4}-[A-Za-z\\s-]+)",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CWE_PATTERN = Pattern.compile(
        "CWE[-\\s]*(\\d+)",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 建構 Gemini CLI 服務
     *
     * @param config   AI 配置（需包含 CLI 路徑）
     * @param executor CLI 執行器
     */
    public GeminiCliService(AiConfig config, CliExecutor executor) {
        super(config, executor);

        // 驗證 Gemini CLI 配置
        if (config.getCliPath() == null || config.getCliPath().isEmpty()) {
            throw new IllegalArgumentException("Gemini CLI path is required");
        }

        LOG.info("Initialized Gemini CLI service with path: {}", config.getCliPath());
    }

    @Override
    public String getProviderName() {
        return "Google Gemini CLI";
    }

    @Override
    public String getModelName() {
        // Gemini CLI 通常使用預設模型，可從配置中取得
        return config.getModel() != null ? config.getModel().getModelId() : "gemini-1.5-pro";
    }

    @Override
    protected String[] buildCliCommand(AiRequest request) {
        // 建構 Gemini CLI 命令
        // 格式: gemini chat "prompt"
        String prompt = buildAnalysisPrompt(request);

        List<String> command = new ArrayList<>();
        command.add(executor.getCliPath()); // gemini 執行檔路徑
        command.add(CLI_SUBCOMMAND);        // chat 子命令
        command.add(prompt);                 // 提示詞

        LOG.debug("Built Gemini CLI command with {} arguments", command.size());

        return command.toArray(new String[0]);
    }

    @Override
    protected String buildAnalysisPrompt(AiRequest request) {
        // 使用專門為 Gemini 最佳化的提示詞格式
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a security expert. Analyze the following code for security vulnerabilities.\n\n");

        prompt.append("File: ").append(request.getFileName()).append("\n");
        prompt.append("Language: ").append(request.getLanguage() != null ? request.getLanguage() : "Unknown").append("\n\n");

        prompt.append("Code:\n");
        prompt.append("```\n");
        prompt.append(request.getCode());
        prompt.append("\n```\n\n");

        prompt.append("Instructions:\n");
        prompt.append("1. Identify ALL security vulnerabilities based on OWASP Top 10 (2017, 2021, 2025)\n");
        prompt.append("2. For EACH vulnerability found, provide:\n");
        prompt.append("   - Severity: BLOCKER, CRITICAL, MAJOR, MINOR, or INFO\n");
        prompt.append("   - OWASP Category: e.g., A03:2021-Injection\n");
        prompt.append("   - CWE IDs: e.g., CWE-89, CWE-79\n");
        prompt.append("   - Description: Clear explanation of the issue\n");
        prompt.append("   - Suggested Fix: Concrete code example or steps to fix\n");
        prompt.append("3. If no vulnerabilities found, explicitly state 'No security vulnerabilities detected.'\n\n");

        prompt.append("Format each finding clearly with markers like '## Finding 1', '## Finding 2', etc.\n");

        return prompt.toString();
    }

    @Override
    protected AiResponse parseCliOutput(String output, AiRequest request) throws AiException {
        LOG.debug("Parsing Gemini CLI output ({} chars)", output.length());

        if (output == null || output.trim().isEmpty()) {
            throw new AiException("Gemini CLI returned empty output");
        }

        List<Finding> findings = new ArrayList<>();

        // 檢查是否明確表示無漏洞
        if (output.contains("No security vulnerabilities detected") ||
            output.contains("No vulnerabilities found")) {
            LOG.info("Gemini CLI analysis: No vulnerabilities detected");
            return AiResponse.builder()
                .findings(findings) // 空列表
                .rawResponse(output)
                .build();
        }

        // 按 "## Finding" 分割輸出
        String[] sections = output.split("##\\s*Finding\\s*\\d+");

        for (int i = 1; i < sections.length; i++) { // 跳過第 0 個（標題前的內容）
            String section = sections[i].trim();

            if (section.isEmpty()) {
                continue;
            }

            try {
                Finding finding = parseFindingSection(section, request.getFileName());
                findings.add(finding);
                LOG.debug("Parsed finding {}: {} - {}", i, finding.getSeverity(), finding.getOwaspCategory());
            } catch (Exception e) {
                LOG.warn("Failed to parse finding section {}: {}", i, e.getMessage());
                // 繼續解析其他發現，不中斷整個流程
            }
        }

        // 如果沒有找到格式化的發現，嘗試整體解析
        if (findings.isEmpty()) {
            LOG.warn("No structured findings found, attempting fallback parsing");
            Finding fallbackFinding = parseFallbackFinding(output, request.getFileName());
            if (fallbackFinding != null) {
                findings.add(fallbackFinding);
            }
        }

        LOG.info("Gemini CLI analysis completed: {} findings", findings.size());

        return AiResponse.builder()
            .findings(findings)
            .rawResponse(output)
            .build();
    }

    /**
     * 解析單一發現區塊
     */
    private Finding parseFindingSection(String section, String fileName) {
        Finding.Builder builder = Finding.builder()
            .fileName(fileName);

        // 解析嚴重性
        Matcher severityMatcher = SEVERITY_PATTERN.matcher(section);
        if (severityMatcher.find()) {
            builder.severity(severityMatcher.group(1).toUpperCase());
        } else {
            builder.severity("MAJOR"); // 預設嚴重性
        }

        // 解析 OWASP 類別
        Matcher owaspMatcher = OWASP_PATTERN.matcher(section);
        if (owaspMatcher.find()) {
            builder.owaspCategory(owaspMatcher.group(1).trim());
        } else {
            builder.owaspCategory("Unknown");
        }

        // 解析 CWE IDs
        List<Integer> cweIds = new ArrayList<>();
        Matcher cweMatcher = CWE_PATTERN.matcher(section);
        while (cweMatcher.find()) {
            try {
                cweIds.add(Integer.parseInt(cweMatcher.group(1)));
            } catch (NumberFormatException e) {
                LOG.warn("Invalid CWE ID: {}", cweMatcher.group(1));
            }
        }
        builder.cweIds(cweIds);

        // 描述和修復建議從區塊內容中提取
        String description = extractDescription(section);
        String suggestedFix = extractSuggestedFix(section);

        builder.message(description);
        builder.suggestedFix(suggestedFix);

        // 預設行號（Gemini CLI 輸出通常不包含具體行號）
        builder.startLine(1);
        builder.endLine(1);

        return builder.build();
    }

    /**
     * 提取描述（查找 "Description:" 後的內容）
     */
    private String extractDescription(String section) {
        Pattern descPattern = Pattern.compile(
            "(?i)description[:\\s]*(.+?)(?=(?:suggested fix|fix|cwe|$))",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = descPattern.matcher(section);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 如果沒有找到 "Description:"，返回區塊的前 200 字元作為描述
        return section.length() > 200 ? section.substring(0, 200) + "..." : section;
    }

    /**
     * 提取修復建議（查找 "Suggested Fix:" 或 "Fix:" 後的內容）
     */
    private String extractSuggestedFix(String section) {
        Pattern fixPattern = Pattern.compile(
            "(?i)(?:suggested\\s+)?fix[:\\s]*(.+?)(?=$)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = fixPattern.matcher(section);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "Please review the vulnerability and apply appropriate security measures.";
    }

    /**
     * 備用解析方法：當找不到結構化輸出時使用
     */
    private Finding parseFallbackFinding(String output, String fileName) {
        // 簡單地將整個輸出視為一個發現
        if (output.length() < 50) {
            return null; // 輸出太短，可能不是有效的分析結果
        }

        return Finding.builder()
            .fileName(fileName)
            .severity("MAJOR")
            .owaspCategory("Unknown")
            .message(output.length() > 500 ? output.substring(0, 500) + "..." : output)
            .suggestedFix("Please review the AI analysis output for detailed recommendations.")
            .startLine(1)
            .endLine(1)
            .build();
    }
}
