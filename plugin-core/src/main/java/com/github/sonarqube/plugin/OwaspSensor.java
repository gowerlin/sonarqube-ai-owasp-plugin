package com.github.sonarqube.plugin;

import com.github.sonarqube.ai.AiConnector;
import com.github.sonarqube.ai.model.AiRequest;
import com.github.sonarqube.ai.model.AiResponse;
import com.github.sonarqube.ai.model.SecurityIssue;
import com.github.sonarqube.config.PluginConfiguration;
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
    private final AiConnector aiConnector;
    private final Map<String, RuleDefinition> ruleMap;

    public OwaspSensor() {
        this.config = PluginConfiguration.getInstance();
        this.aiConnector = new AiConnector();

        // 建立規則映射表以便快速查詢
        this.ruleMap = buildRuleMap();
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

        // 建立 AI 請求
        AiRequest request = AiRequest.builder()
                .code(content)
                .language(file.language())
                .fileName(file.filename())
                .analysisType("security")
                .owaspVersion(VersionManager.getCurrentVersion().getVersion())
                .build();

        // 呼叫 AI 分析
        AiResponse response = aiConnector.analyze(request);

        if (response == null || !response.isSuccess()) {
            LOG.warn("AI 分析失敗: {}", file.uri());
            return List.of();
        }

        // 解析安全問題
        return response.getSecurityIssues();
    }

    /**
     * 報告安全問題到 SonarQube
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

                // 設定問題位置
                NewIssueLocation location = newIssue.newLocation()
                        .on(file)
                        .message(buildIssueMessage(issue, rule));

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
     * 建立問題訊息
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
