package com.github.sonarqube.plugin;

import com.github.sonarqube.plugin.api.CliStatusApiController;
// import com.github.sonarqube.plugin.api.ConfigurationApiController; // TODO: 需要實作 AiConfiguration, ConfigurationManager, ScanScopeConfiguration 類別後才能啟用
import com.github.sonarqube.plugin.api.OwaspVersionApiController;
import com.github.sonarqube.plugin.api.PdfReportApiController;
import com.github.sonarqube.plugin.api.ScanProgressApiController;
import com.github.sonarqube.plugin.web.OwaspReportPageDefinition;
import com.github.sonarqube.rules.RuleRegistry;
import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SonarQube AI OWASP Security Plugin 主入口
 *
 * 功能：
 * - AI 驅動的 OWASP 安全分析
 * - 支援 OWASP Top 10 2017、2021、2025 三版本
 * - 智能修復建議與工作量評估
 * - 並行分析與智能快取
 * - HTML/JSON 多版本對照報告
 *
 * @since 1.0.0
 */
public class AiOwaspPlugin implements Plugin {

    private static final Logger LOG = LoggerFactory.getLogger(AiOwaspPlugin.class);

    // 插件配置屬性分類
    public static final String CATEGORY_AI = "AI Configuration";
    public static final String CATEGORY_OWASP = "OWASP Versions";
    public static final String CATEGORY_PERFORMANCE = "Performance";
    public static final String CATEGORY_REPORT = "Reporting";

    // AI 模型配置
    public static final String PROPERTY_AI_PROVIDER = "sonar.aiowasp.ai.provider";
    public static final String PROPERTY_AI_API_KEY = "sonar.aiowasp.ai.apikey";
    public static final String PROPERTY_AI_MODEL = "sonar.aiowasp.ai.model";
    public static final String PROPERTY_AI_TEMPERATURE = "sonar.aiowasp.ai.temperature";
    public static final String PROPERTY_AI_MAX_TOKENS = "sonar.aiowasp.ai.maxTokens";
    public static final String PROPERTY_AI_TIMEOUT = "sonar.aiowasp.ai.timeout";

    // CLI 模式配置 (Epic 9)
    public static final String PROPERTY_CLI_GEMINI_PATH = "sonar.aiowasp.cli.gemini.path";
    public static final String PROPERTY_CLI_COPILOT_PATH = "sonar.aiowasp.cli.copilot.path";
    public static final String PROPERTY_CLI_CLAUDE_PATH = "sonar.aiowasp.cli.claude.path";

    // OWASP 版本配置
    public static final String PROPERTY_OWASP_2017_ENABLED = "sonar.aiowasp.version.2017.enabled";
    public static final String PROPERTY_OWASP_2021_ENABLED = "sonar.aiowasp.version.2021.enabled";
    public static final String PROPERTY_OWASP_2025_ENABLED = "sonar.aiowasp.version.2025.enabled";

    // 效能配置
    public static final String PROPERTY_PARALLEL_FILES = "sonar.aiowasp.parallel.files";
    public static final String PROPERTY_CACHE_ENABLED = "sonar.aiowasp.cache.enabled";
    public static final String PROPERTY_INCREMENTAL_SCAN = "sonar.aiowasp.incremental.enabled";

    // 報告配置
    public static final String PROPERTY_REPORT_FORMAT = "sonar.aiowasp.report.format";
    public static final String PROPERTY_REPORT_MULTI_VERSION = "sonar.aiowasp.report.multiVersion";

    @Override
    public void define(Context context) {
        LOG.info("正在載入 AI OWASP Security Plugin v1.0.0");

        // 註冊配置屬性
        defineProperties(context);

        // 註冊 Web Services (API 端點)
        defineWebServices(context);

        // 註冊 Web 頁面 (Epic 5.6 + 7.4)
        defineWebPages(context);

        // 註冊擴充功能
        // TODO: 在後續 Epic 中實現
        // - 規則定義 (RulesDefinition)
        // - 掃描器 (Scanner)
        // - 品質檔案 (QualityProfile)
        // - 度量指標 (Metrics)

        LOG.info("AI OWASP Security Plugin 載入完成");
    }

    /**
     * 定義插件配置屬性
     */
    private void defineProperties(Context context) {
        // ============================================================
        // AI 配置
        // ============================================================
        context.addExtension(
            PropertyDefinition.builder(PROPERTY_AI_PROVIDER)
                .name("AI Provider")
                .description("AI 模型供應商")
                .category(CATEGORY_AI)
                .subCategory("Model Selection")
                .defaultValue("openai")
                .options("openai", "anthropic", "gemini-api", "gemini-cli", "copilot-cli", "claude-cli")
                .type(PropertyType.SINGLE_SELECT_LIST)
                .index(1)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_AI_API_KEY)
                .name("AI API Key")
                .description("AI 模型的 API 金鑰（加密存儲）")
                .category(CATEGORY_AI)
                .subCategory("Authentication")
                .type(PropertyType.PASSWORD)
                .index(2)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_AI_MODEL)
                .name("AI Model")
                .description("使用的 AI 模型名稱 (gpt-4, claude-3-opus, gemini-1.5-pro)")
                .category(CATEGORY_AI)
                .subCategory("Model Selection")
                .defaultValue("gpt-4")
                .options("gpt-4", "gpt-4-turbo", "gpt-3.5-turbo",
                         "claude-3-opus-20240229", "claude-3-sonnet-20240229", "claude-3-haiku-20240307",
                         "gemini-1.5-pro", "gemini-1.5-flash")
                .type(PropertyType.SINGLE_SELECT_LIST)
                .index(3)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_AI_TEMPERATURE)
                .name("Temperature")
                .description("AI 模型的溫度參數 (0.0-1.0)，較低值產生更確定性的輸出")
                .category(CATEGORY_AI)
                .subCategory("Model Parameters")
                .defaultValue("0.3")
                .type(PropertyType.FLOAT)
                .index(4)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_AI_MAX_TOKENS)
                .name("Max Tokens")
                .description("AI 回應的最大 token 數量")
                .category(CATEGORY_AI)
                .subCategory("Model Parameters")
                .defaultValue("2000")
                .type(PropertyType.INTEGER)
                .index(5)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_AI_TIMEOUT)
                .name("Timeout (seconds)")
                .description("AI API 呼叫的超時時間（秒）")
                .category(CATEGORY_AI)
                .subCategory("Model Parameters")
                .defaultValue("60")
                .type(PropertyType.INTEGER)
                .index(6)
                .build()
        );

        // ============================================================
        // CLI 模式配置 (Epic 9)
        // ============================================================
        context.addExtension(
            PropertyDefinition.builder(PROPERTY_CLI_GEMINI_PATH)
                .name("Gemini CLI Path")
                .description("Gemini CLI 工具路徑（僅在使用 gemini-cli 時需要）")
                .category(CATEGORY_AI)
                .subCategory("CLI Configuration")
                .defaultValue("/usr/local/bin/gemini")
                .index(7)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_CLI_COPILOT_PATH)
                .name("GitHub Copilot CLI Path")
                .description("GitHub Copilot CLI 路徑（僅在使用 copilot-cli 時需要）")
                .category(CATEGORY_AI)
                .subCategory("CLI Configuration")
                .defaultValue("/usr/local/bin/gh")
                .index(8)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_CLI_CLAUDE_PATH)
                .name("Claude CLI Path")
                .description("Claude CLI 工具路徑（僅在使用 claude-cli 時需要）")
                .category(CATEGORY_AI)
                .subCategory("CLI Configuration")
                .defaultValue("/usr/local/bin/claude")
                .index(9)
                .build()
        );

        // ============================================================
        // OWASP 版本配置
        // ============================================================
        context.addExtension(
            PropertyDefinition.builder(PROPERTY_OWASP_2017_ENABLED)
                .name("Enable OWASP 2017")
                .description("啟用 OWASP Top 10 2017 規則（10 個類別）")
                .category(CATEGORY_OWASP)
                .defaultValue("true")
                .type(PropertyType.BOOLEAN)
                .index(1)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_OWASP_2021_ENABLED)
                .name("Enable OWASP 2021")
                .description("啟用 OWASP Top 10 2021 規則（10 個類別，預設版本）")
                .category(CATEGORY_OWASP)
                .defaultValue("true")
                .type(PropertyType.BOOLEAN)
                .index(2)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_OWASP_2025_ENABLED)
                .name("Enable OWASP 2025 (Preview)")
                .description("啟用 OWASP Top 10 2025 預覽版規則")
                .category(CATEGORY_OWASP)
                .defaultValue("false")
                .type(PropertyType.BOOLEAN)
                .index(3)
                .build()
        );

        // ============================================================
        // 效能配置
        // ============================================================
        context.addExtension(
            PropertyDefinition.builder(PROPERTY_PARALLEL_FILES)
                .name("Parallel Files Count")
                .description("並行分析的檔案數量（建議值：3-5）")
                .category(CATEGORY_PERFORMANCE)
                .subCategory("Parallelism")
                .defaultValue("3")
                .type(PropertyType.INTEGER)
                .index(1)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_CACHE_ENABLED)
                .name("Enable Intelligent Cache")
                .description("啟用智能快取（基於檔案 hash）")
                .category(CATEGORY_PERFORMANCE)
                .subCategory("Caching")
                .defaultValue("true")
                .type(PropertyType.BOOLEAN)
                .index(2)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_INCREMENTAL_SCAN)
                .name("Enable Incremental Scan")
                .description("啟用增量掃描（僅分析變更的檔案）")
                .category(CATEGORY_PERFORMANCE)
                .subCategory("Scanning")
                .defaultValue("true")
                .type(PropertyType.BOOLEAN)
                .index(3)
                .build()
        );

        // ============================================================
        // 報告配置
        // ============================================================
        context.addExtension(
            PropertyDefinition.builder(PROPERTY_REPORT_FORMAT)
                .name("Report Format")
                .description("報告輸出格式")
                .category(CATEGORY_REPORT)
                .defaultValue("html")
                .options("html", "json", "both")
                .type(PropertyType.SINGLE_SELECT_LIST)
                .index(1)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_REPORT_MULTI_VERSION)
                .name("Enable Multi-Version Comparison")
                .description("啟用多版本對照報告（最多 3 個版本）")
                .category(CATEGORY_REPORT)
                .defaultValue("true")
                .type(PropertyType.BOOLEAN)
                .index(2)
                .build()
        );

        LOG.debug("已註冊 {} 個配置屬性", 17);
    }

    /**
     * 註冊 Web Services (API 端點)
     */
    private void defineWebServices(Context context) {
        // 創建共享的 RuleRegistry 實例
        RuleRegistry ruleRegistry = new RuleRegistry();
        context.addExtension(ruleRegistry);

        // 註冊 SonarQube 數據查詢服務
        context.addExtension(com.github.sonarqube.plugin.service.SonarQubeDataService.class);

        // 配置管理 API (Epic 7.1)
        // TODO: 暫時停用，需要實作 AiConfiguration, ConfigurationManager, ScanScopeConfiguration 類別後才能啟用
        // context.addExtension(ConfigurationApiController.class);

        // 掃描進度追蹤 API (Epic 7.5)
        context.addExtension(ScanProgressApiController.class);

        // 版本管理 API - 手動實例化並注入依賴
        context.addExtension(new OwaspVersionApiController(ruleRegistry));

        // PDF 報告匯出 API (包含 HTML/JSON/Markdown)
        // SonarQube 會自動注入 Configuration 和 SonarQubeDataService
        context.addExtension(PdfReportApiController.class);

        // CLI 狀態檢查 API (Epic 9)
        context.addExtension(CliStatusApiController.class);

        LOG.debug("已註冊 {} 個 Web Service (含 SonarQubeDataService)", 5);  // 暫時停用 ConfigurationApiController
    }

    /**
     * 註冊 Web 頁面 (Epic 5.6 + 7.4)
     */
    private void defineWebPages(Context context) {
        // OWASP 報告查看頁面 (Epic 5.6 + 7.4)
        context.addExtension(OwaspReportPageDefinition.class);

        LOG.debug("已註冊 {} 個 Web Page", 1);
    }
}
