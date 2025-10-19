package com.github.sonarqube.ai.provider.claude;

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
 * Claude Code CLI 服務實作
 *
 * 使用 Claude Code CLI 工具進行代碼安全分析。
 *
 * CLI 安裝方式:
 * <pre>
 * npm install -g @anthropic-ai/claude-code
 * </pre>
 *
 * CLI 使用範例:
 * <pre>
 * claude analyze "分析這段代碼的安全問題"
 * </pre>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9, Story 9.6)
 */
public class ClaudeCliService extends AbstractCliService {

    private static final Logger LOG = LoggerFactory.getLogger(ClaudeCliService.class);

    // Claude CLI 命令結構
    private static final String CLI_COMMAND = "claude";
    private static final String CLI_ACTION = "analyze";

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
    private static final Pattern VULNERABILITY_SECTION = Pattern.compile(
        "(?i)(?:vulnerability|issue|finding)[\\s]*[:#]?[\\s]*\\d+",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * 建構 Claude CLI 服務
     *
     * @param config   AI 配置（需包含 CLI 路徑）
     * @param executor CLI 執行器
     */
    public ClaudeCliService(AiConfig config, CliExecutor executor) {
        super(config, executor);

        // 驗證 Claude CLI 配置
        if (config.getCliPath() == null || config.getCliPath().isEmpty()) {
            throw new IllegalArgumentException("Claude CLI path is required");
        }

        LOG.info("Initialized Claude CLI service with path: {}", config.getCliPath());
    }

    @Override
    public String getProviderName() {
        return "Anthropic Claude CLI";
    }

    @Override
    public String getModelName() {
        // Claude CLI 使用內建模型，返回配置的模型或預設值
        return config.getModel() != null ? config.getModel().getModelId() : "claude-3-opus";
    }

    @Override
    protected String[] buildCliCommand(AiRequest request) {
        // 建構 Claude CLI 命令
        // 格式: claude analyze "prompt"
        String prompt = buildAnalysisPrompt(request);

        List<String> command = new ArrayList<>();
        command.add(executor.getCliPath()); // claude 執行檔路徑
        command.add(CLI_ACTION);            // analyze
        command.add(prompt);                 // 提示詞

        LOG.debug("Built Claude CLI command with {} arguments", command.size());

        return command.toArray(new String[0]);
    }

    @Override
    protected String buildAnalysisPrompt(AiRequest request) {
        // 使用專門為 Claude 最佳化的提示詞格式
        StringBuilder prompt = new StringBuilder();

        prompt.append("Security Analysis Request:\\n\\n");

        prompt.append("File: ").append(request.getFileName()).append("\\n");
        if (request.getLanguage() != null) {
            prompt.append("Language: ").append(request.getLanguage()).append("\\n");
        }
        prompt.append("\\n");

        prompt.append("Code to analyze:\\n");
        prompt.append("```\\n");
        prompt.append(request.getCode());
        prompt.append("\\n```\\n\\n");

        prompt.append("Please perform a comprehensive security analysis:\\n");
        prompt.append("1. Identify ALL security vulnerabilities based on OWASP Top 10\\n");
        prompt.append("2. For each vulnerability, provide:\\n");
        prompt.append("   - Severity Level: BLOCKER, CRITICAL, MAJOR, MINOR, or INFO\\n");
        prompt.append("   - OWASP Category: e.g., A03:2021-Injection, A01:2021-Broken Access Control\\n");
        prompt.append("   - CWE IDs: e.g., CWE-89, CWE-79\\n");
        prompt.append("   - Clear Description: Explain the vulnerability and its impact\\n");
        prompt.append("   - Fix Recommendation: Provide concrete code examples or steps\\n");
        prompt.append("3. If no vulnerabilities found, state: 'No security vulnerabilities detected.'\\n\\n");

        prompt.append("Format each finding as:\\n");
        prompt.append("Vulnerability 1:\\n");
        prompt.append("Severity: [LEVEL]\\n");
        prompt.append("OWASP: [CATEGORY]\\n");
        prompt.append("CWE: [IDs]\\n");
        prompt.append("Description: [DETAILS]\\n");
        prompt.append("Fix: [RECOMMENDATION]\\n");

        return prompt.toString();
    }

    @Override
    protected AiResponse parseCliOutput(String output, AiRequest request) throws AiException {
        LOG.debug("Parsing Claude CLI output ({} chars)", output.length());

        if (output == null || output.trim().isEmpty()) {
            throw new AiException("Claude CLI returned empty output");
        }

        List<Finding> findings = new ArrayList<>();

        // 檢查是否明確表示無漏洞
        if (output.contains("No security vulnerabilities detected") ||
            output.contains("No vulnerabilities found") ||
            output.contains("no issues found")) {
            LOG.info("Claude CLI analysis: No vulnerabilities detected");
            return AiResponse.builder()
                .findings(findings) // 空列表
                .rawResponse(output)
                .build();
        }

        // 嘗試按 "Vulnerability N:" 分割輸出
        String[] sections = output.split("(?i)Vulnerability\\s+\\d+:");

        if (sections.length > 1) {
            // 找到結構化格式
            for (int i = 1; i < sections.length; i++) {
                String section = sections[i].trim();
                if (section.isEmpty()) {
                    continue;
                }

                try {
                    Finding finding = parseFindingSection(section, request.getFileName());
                    findings.add(finding);
                    LOG.debug("Parsed vulnerability {}: {} - {}", i, finding.getSeverity(), finding.getOwaspCategory());
                } catch (Exception e) {
                    LOG.warn("Failed to parse vulnerability section {}: {}", i, e.getMessage());
                }
            }
        }

        // 如果沒有找到 "Vulnerability N:" 格式，嘗試其他模式
        if (findings.isEmpty()) {
            String[] altSections = output.split("(?i)(?:Issue|Finding)\\s+\\d+:");
            if (altSections.length > 1) {
                for (int i = 1; i < altSections.length; i++) {
                    String section = altSections[i].trim();
                    if (!section.isEmpty()) {
                        try {
                            Finding finding = parseFindingSection(section, request.getFileName());
                            findings.add(finding);
                        } catch (Exception e) {
                            LOG.warn("Failed to parse issue section {}: {}", i, e.getMessage());
                        }
                    }
                }
            }
        }

        // 備用解析：如果還是找不到結構化內容
        if (findings.isEmpty()) {
            LOG.warn("No structured findings found, attempting fallback parsing");
            Finding fallbackFinding = parseFallbackFinding(output, request.getFileName());
            if (fallbackFinding != null) {
                findings.add(fallbackFinding);
            }
        }

        LOG.info("Claude CLI analysis completed: {} findings", findings.size());

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

        // 提取描述和修復建議
        String description = extractDescription(section);
        String suggestedFix = extractFix(section);

        builder.message(description);
        builder.suggestedFix(suggestedFix);

        // 預設行號（Claude CLI 輸出通常不包含具體行號）
        builder.startLine(1);
        builder.endLine(1);

        return builder.build();
    }

    /**
     * 提取描述（查找 "Description:" 後的內容）
     */
    private String extractDescription(String section) {
        Pattern descPattern = Pattern.compile(
            "(?i)description[:\\s]*(.+?)(?=(?:fix|recommendation|$))",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = descPattern.matcher(section);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 如果沒有找到 "Description:"，返回區塊的前 250 字元
        return section.length() > 250 ? section.substring(0, 250) + "..." : section;
    }

    /**
     * 提取修復建議（查找 "Fix:" 或 "Recommendation:" 後的內容）
     */
    private String extractFix(String section) {
        Pattern fixPattern = Pattern.compile(
            "(?i)(?:fix|recommendation)[:\\s]*(.+?)(?=$)",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = fixPattern.matcher(section);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return "Review the security issue and apply appropriate security controls based on OWASP guidelines.";
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
            .suggestedFix("Please review the Claude CLI analysis output for detailed recommendations.")
            .startLine(1)
            .endLine(1)
            .build();
    }
}
