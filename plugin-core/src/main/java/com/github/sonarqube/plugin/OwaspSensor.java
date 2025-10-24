package com.github.sonarqube.plugin;

import com.github.sonarqube.ai.AiService;
import com.github.sonarqube.ai.AiServiceFactory;
import com.github.sonarqube.ai.model.AiConfig;
import com.github.sonarqube.ai.model.AiExecutionMode;
import com.github.sonarqube.ai.model.AiModel;
import com.github.sonarqube.ai.model.AiRequest;
import com.github.sonarqube.ai.model.AiResponse;
import com.github.sonarqube.ai.model.SecurityIssue;
import com.github.sonarqube.config.PluginConfiguration;
import com.github.sonarqube.plugin.util.SonarQubeVersionDetector;
import com.github.sonarqube.rules.RuleDefinition;
import com.github.sonarqube.rules.java.JavaSecurityRules;
import com.github.sonarqube.rules.javascript.JavaScriptSecurityRules;
import com.github.sonarqube.version.VersionManager;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * OWASP 安全掃描感測器
 *
 * 掃描專案中的程式碼檔案，使用 AI 進行安全分析，
 * 並根據 OWASP 規則報告安全問題。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class OwaspSensor implements Sensor {

    private static final Logger LOG = Loggers.get(OwaspSensor.class);

    private final PluginConfiguration config;
    private final AiService aiService;
    private final Map<String, RuleDefinition> ruleMap;

    public OwaspSensor() {
        this.config = PluginConfiguration.getInstance();
        this.aiService = AiServiceFactory.createService(convertToAiConfig(config));

        // 建立規則映射表以便快速查詢
        this.ruleMap = buildRuleMap();
    }

    /**
     * 將 PluginConfiguration 轉換為 AiConfig
     *
     * @param pluginConfig 插件配置
     * @return AI 配置物件
     */
    private AiConfig convertToAiConfig(PluginConfiguration pluginConfig) {
        // 從環境變數或系統屬性取得 API Key
        String apiKey = System.getenv("AI_API_KEY");
        if (apiKey == null) {
            apiKey = System.getProperty("ai.api.key", "");
        }

        // 從環境變數或系統屬性取得 API Endpoint
        String apiEndpoint = System.getenv("AI_API_ENDPOINT");
        if (apiEndpoint == null) {
            apiEndpoint = System.getProperty("ai.api.endpoint", "");
        }

        // 從模型 ID 字串轉換為 AiModel 枚舉，如果找不到則使用 GPT-4 作為預設值
        AiModel model = AiModel.fromModelId(pluginConfig.getAiModel());
        if (model == null) {
            LOG.warn("無法識別的 AI 模型 ID: {}，使用預設模型 GPT-4", pluginConfig.getAiModel());
            model = AiModel.GPT_4;
        }

        return AiConfig.builder()
            .model(model)
            .apiKey(apiKey)
            .apiEndpoint(apiEndpoint)
            .timeoutSeconds(pluginConfig.getAiTimeout() / 1000)
            .temperature(0.3)  // 預設值：較保守的創造性
            .maxTokens(4096)   // 預設值：標準 token 限制
            .maxRetries(3)     // 預設值：重試 3 次
            .retryDelayMs(1000L)  // 預設值：重試延遲 1 秒
            .executionMode(AiExecutionMode.API)  // 預設值：API 模式
            .build();
    }

    @Override
    public void describe(SensorDescriptor descriptor) {
        descriptor
                .name("OWASP AI Security Sensor")
                .onlyOnLanguages("java", "js")
                .global();
    }

    @Override
    public void execute(SensorContext context) {
        if (!config.isAiAnalysisEnabled()) {
            LOG.info("OWASP AI 分析已停用，略過掃描");
            return;
        }

        LOG.info("開始 OWASP AI 安全掃描 (OWASP 版本: {})", VersionManager.getCurrentVersion().getVersion());

        FileSystem fileSystem = context.fileSystem();

        // 掃描 Java 檔案
        scanFiles(context, fileSystem, "java", "owasp-java");

        // 掃描 JavaScript 檔案
        scanFiles(context, fileSystem, "js", "owasp-javascript");

        LOG.info("OWASP AI 安全掃描完成");
    }

    /**
     * 掃描指定語言的檔案
     */
    private void scanFiles(SensorContext context, FileSystem fileSystem, String language, String repositoryKey) {
        Iterable<InputFile> files = fileSystem.inputFiles(
                fileSystem.predicates().hasLanguage(language)
        );

        int fileCount = 0;
        int issueCount = 0;

        for (InputFile file : files) {
            fileCount++;
            try {
                List<SecurityIssue> issues = analyzeFile(file);
                issueCount += reportIssues(context, file, issues, repositoryKey);
            } catch (Exception e) {
                LOG.error("分析檔案時發生錯誤: {}", file.uri(), e);
            }
        }

        LOG.info("掃描 {} 語言: {} 個檔案, {} 個安全問題", language, fileCount, issueCount);
    }

    /**
     * 使用 AI 分析單一檔案
     */
    private List<SecurityIssue> analyzeFile(InputFile file) throws IOException {
        // 讀取檔案內容
        String content = new String(Files.readAllBytes(file.path()), StandardCharsets.UTF_8);

        // 建立 AI 請求 (builder 需要 code 參數)
        AiRequest request = AiRequest.builder(content)
                .language(file.language())
                .fileName(file.filename())
                .analysisType("security")
                .owaspVersion(VersionManager.getCurrentVersion().getVersion())
                .build();

        try {
            // 呼叫 AI 分析
            AiResponse response = aiService.analyzeCode(request);

            if (response == null || !response.isSuccess()) {
                LOG.warn("AI 分析失敗: {}", file.uri());
                return List.of();
            }

            // 解析安全問題 (正確的方法名是 getIssues)
            return response.getIssues();
        } catch (com.github.sonarqube.ai.AiException e) {
            LOG.error("AI 分析發生異常: {} - {}", file.uri(), e.getMessage());
            return List.of();
        }
    }

    /**
     * 報告安全問題到 SonarQube
     *
     * Story 10.2: 修改為保留完整 AI 增強資訊
     *
     * 註：由於 SonarQube API 限制，AI 資訊透過增強訊息格式傳遞
     */
    private int reportIssues(SensorContext context, InputFile file, List<SecurityIssue> issues, String repositoryKey) {
        int count = 0;

        for (SecurityIssue issue : issues) {
            try {
                // 根據 OWASP 分類和 CWE ID 找到對應的規則
                RuleDefinition rule = findMatchingRule(issue);

                if (rule == null) {
                    LOG.debug("找不到對應規則: OWASP={}, CWE={}", issue.getOwaspCategory(), issue.getCweId());
                    continue;
                }

                // 建立問題
                NewIssue newIssue = context.newIssue();
                newIssue.forRule(RuleKey.of(repositoryKey, rule.getRuleKey()));

                // 使用增強訊息格式（包含完整 AI 資訊）
                String message = buildEnhancedMessage(issue, rule);

                // 設定問題位置
                NewIssueLocation location = newIssue.newLocation()
                        .on(file)
                        .message(message);

                // 如果有行號，設定行號
                if (issue.getLineNumber() != null && issue.getLineNumber() > 0) {
                    location.at(file.selectLine(issue.getLineNumber()));
                }

                newIssue.at(location);
                newIssue.save();

                count++;
            } catch (Exception e) {
                LOG.error("報告問題時發生錯誤", e);
            }
        }

        return count;
    }

    /**
     * 建立增強訊息（包含完整 AI 資訊）
     *
     * Story 10.2: 確保所有 AI 提供的資訊都能顯示給使用者
     *
     * @param issue SecurityIssue
     * @param rule RuleDefinition
     * @return 包含完整資訊的訊息
     */
    private String buildEnhancedMessage(SecurityIssue issue, RuleDefinition rule) {
        // 檢查是否有 AI 增強資訊
        boolean hasAiEnhancement = (issue.getCodeExample() != null) ||
                                   (issue.getEffortEstimate() != null && !issue.getEffortEstimate().isEmpty());

        // 如果有 AI 資訊，使用完整格式；否則使用簡潔格式
        return hasAiEnhancement ? buildLegacyIssueMessage(issue, rule) : buildIssueMessage(issue, rule);
    }

    /**
     * 根據 SecurityIssue 找到對應的規則
     */
    private RuleDefinition findMatchingRule(SecurityIssue issue) {
        // 優先用 CWE ID 匹配
        if (issue.getCweId() != null && !issue.getCweId().isEmpty()) {
            String cweId = normalizeCweId(issue.getCweId());
            for (RuleDefinition rule : ruleMap.values()) {
                if (rule.getCweIds() != null && rule.getCweIds().contains(cweId)) {
                    return rule;
                }
            }
        }

        // 如果沒有 CWE，用 OWASP 分類匹配
        if (issue.getOwaspCategory() != null && !issue.getOwaspCategory().isEmpty()) {
            for (RuleDefinition rule : ruleMap.values()) {
                if (matchesOwaspCategory(rule, issue.getOwaspCategory())) {
                    return rule;
                }
            }
        }

        return null;
    }

    /**
     * 檢查規則是否匹配 OWASP 分類
     */
    private boolean matchesOwaspCategory(RuleDefinition rule, String owaspCategory) {
        if (rule.getOwaspCategory() == null) {
            return false;
        }

        String ruleCategoryId = rule.getOwaspCategory().getCategoryId();
        String issueCategory = owaspCategory.toUpperCase();

        // 移除年份部分進行比較 (A01:2021 → A01)
        String ruleCategoryPrefix = ruleCategoryId.split(":")[0];
        String issueCategoryPrefix = issueCategory.split(":")[0];

        return ruleCategoryPrefix.equals(issueCategoryPrefix);
    }

    /**
     * 標準化 CWE ID
     */
    private String normalizeCweId(String cweId) {
        if (cweId == null) {
            return "";
        }
        String normalized = cweId.toUpperCase().trim();
        if (!normalized.startsWith("CWE-")) {
            normalized = "CWE-" + normalized;
        }
        return normalized;
    }

    /**
     * 建立問題訊息（舊版本，保留用於降級模式）
     */
    private String buildIssueMessage(SecurityIssue issue, RuleDefinition rule) {
        StringBuilder message = new StringBuilder();

        message.append(rule.getName());

        if (issue.getDescription() != null && !issue.getDescription().isEmpty()) {
            message.append(": ").append(issue.getDescription());
        }

        if (issue.getFixSuggestion() != null && !issue.getFixSuggestion().isEmpty()) {
            message.append(" (建議: ").append(issue.getFixSuggestion()).append(")");
        }

        return message.toString();
    }

    /**
     * 建立降級模式的問題訊息（適用於不支援 Attributes 的舊版 SonarQube）
     *
     * @param issue SecurityIssue
     * @param rule RuleDefinition
     * @return 包含完整資訊的純文字訊息
     */
    private String buildLegacyIssueMessage(SecurityIssue issue, RuleDefinition rule) {
        StringBuilder message = new StringBuilder();

        // 基本訊息
        message.append(rule.getName());

        if (issue.getDescription() != null && !issue.getDescription().isEmpty()) {
            message.append(": ").append(issue.getDescription());
        }

        // 修復建議
        if (issue.getFixSuggestion() != null && !issue.getFixSuggestion().isEmpty()) {
            message.append("\n\n建議: ").append(issue.getFixSuggestion());
        }

        // 程式碼範例（如果有）
        if (issue.getCodeExample() != null) {
            if (issue.getCodeExample().getBefore() != null && !issue.getCodeExample().getBefore().isEmpty()) {
                message.append("\n\n程式碼範例（修復前）:\n").append(truncate(issue.getCodeExample().getBefore(), 1000));
            }
            if (issue.getCodeExample().getAfter() != null && !issue.getCodeExample().getAfter().isEmpty()) {
                message.append("\n\n程式碼範例（修復後）:\n").append(truncate(issue.getCodeExample().getAfter(), 1000));
            }
        }

        // 工作量評估
        if (issue.getEffortEstimate() != null && !issue.getEffortEstimate().isEmpty()) {
            message.append("\n\n工作量評估: ").append(issue.getEffortEstimate());
        }

        return message.toString();
    }

    /**
     * 截斷字串至指定長度
     *
     * @param text 原始文字
     * @param maxLength 最大長度
     * @return 截斷後的文字，如果超過長度則加上 "..."
     */
    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }

        LOG.warn("文字長度 {} 超過限制 {}，將被截斷", text.length(), maxLength);
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * 建立規則映射表
     */
    private Map<String, RuleDefinition> buildRuleMap() {
        List<RuleDefinition> allRules = List.of();

        // 合併 Java 和 JavaScript 規則
        return Stream.concat(
                JavaSecurityRules.getAllRules().stream(),
                JavaScriptSecurityRules.getAllRules().stream()
        ).collect(Collectors.toMap(
                RuleDefinition::getRuleKey,
                rule -> rule
        ));
    }
}
