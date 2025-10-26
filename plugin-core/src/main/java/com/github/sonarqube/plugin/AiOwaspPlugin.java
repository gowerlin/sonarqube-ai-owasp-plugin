package com.github.sonarqube.plugin;

import com.github.sonarqube.plugin.api.AiSuggestionController;
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
    public static final String PROPERTY_AI_ENABLED = "sonar.aiowasp.ai.enabled";
    public static final String PROPERTY_AI_PROVIDER = "sonar.aiowasp.ai.provider";

    // Provider 專用 API Keys (Epic 10)
    public static final String PROPERTY_OPENAI_API_KEY = "sonar.aiowasp.ai.openai.apikey";
    public static final String PROPERTY_ANTHROPIC_API_KEY = "sonar.aiowasp.ai.anthropic.apikey";
    public static final String PROPERTY_GOOGLE_API_KEY = "sonar.aiowasp.ai.google.apikey";

    public static final String PROPERTY_AI_MODEL = "sonar.aiowasp.ai.model";
    public static final String PROPERTY_AI_TEMPERATURE = "sonar.aiowasp.ai.temperature";
    public static final String PROPERTY_AI_MAX_TOKENS = "sonar.aiowasp.ai.maxTokens";
    public static final String PROPERTY_AI_TIMEOUT = "sonar.aiowasp.ai.timeout";
    public static final String PROPERTY_AI_RESPONSE_LANGUAGE = "sonar.aiowasp.ai.responseLanguage";

    // Rate Limiting 配置
    public static final String PROPERTY_AI_RATE_LIMIT_ENABLED = "sonar.aiowasp.ai.rateLimit.enabled";
    public static final String PROPERTY_AI_MAX_TOKENS_PER_MINUTE = "sonar.aiowasp.ai.rateLimit.maxTokensPerMinute";
    public static final String PROPERTY_AI_RATE_LIMIT_BUFFER_RATIO = "sonar.aiowasp.ai.rateLimit.bufferRatio";
    public static final String PROPERTY_AI_RATE_LIMIT_STRATEGY = "sonar.aiowasp.ai.rateLimit.strategy";

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

        // 註冊掃描器 (Sensor)
        context.addExtension(OwaspSensor.class);
        LOG.debug("已註冊 OwaspSensor");

        // 註冊其他擴充功能
        // TODO: 在後續 Epic 中實現
        // - 規則定義 (RulesDefinition)
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
            PropertyDefinition.builder(PROPERTY_AI_ENABLED)
                .name("Enable AI Analysis")
                .description("啟用或停用 AI 安全分析功能")
                .category(CATEGORY_AI)
                .subCategory("General")
                .defaultValue("true")
                .type(PropertyType.BOOLEAN)
                .index(1)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_AI_PROVIDER)
                .name("AI Provider")
                .description("AI 模型供應商")
                .category(CATEGORY_AI)
                .subCategory("Model Selection")
                .defaultValue("openai")
                .options("openai", "anthropic", "gemini-api", "gemini-cli", "copilot-cli", "claude-cli")
                .type(PropertyType.SINGLE_SELECT_LIST)
                .index(2)
                .build()
        );

        // Provider 專用 API Keys (Epic 10)
        context.addExtension(
            PropertyDefinition.builder(PROPERTY_OPENAI_API_KEY)
                .name("OpenAI API Key")
                .description("OpenAI 專用 API 金鑰")
                .category(CATEGORY_AI)
                .subCategory("Authentication")
                .type(PropertyType.PASSWORD)
                .index(3)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_ANTHROPIC_API_KEY)
                .name("Anthropic API Key")
                .description("Anthropic Claude 專用 API 金鑰")
                .category(CATEGORY_AI)
                .subCategory("Authentication")
                .type(PropertyType.PASSWORD)
                .index(4)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_GOOGLE_API_KEY)
                .name("Google API Key")
                .description("Google Gemini 專用 API 金鑰")
                .category(CATEGORY_AI)
                .subCategory("Authentication")
                .type(PropertyType.PASSWORD)
                .index(5)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_AI_MODEL)
                .name("AI Model")
                .description("選擇與 AI Provider 對應的模型 | OpenAI: gpt-4o* | Anthropic: claude-3.7*, claude-3.5* | Gemini: gemini-2.5*, gemini-2.0*")
                .category(CATEGORY_AI)
                .subCategory("Model Selection")
                .defaultValue("gpt-4o")
                .options(
                    // ============================================================
                    // OpenAI Models (推薦: 安全分析性價比最佳)
                    // ============================================================
                    "gpt-4o",                    // ⭐⭐ 旗艦 - $2.5/1M in, $10/1M out (128K) - 強大編碼
                    "gpt-4o-mini",               // ⭐⭐⭐ 最推薦 - $0.15/1M in, $0.6/1M out - 極高性價比
                    "gpt-4-turbo",               // 穩定版 - $10/1M in, $30/1M out (128K)
                    "gpt-4",                     // 經典版 (8K context)
                    "gpt-3.5-turbo",             // 經濟版 (16K context)

                    // ============================================================
                    // Anthropic Claude Models (推薦: 最佳編碼 + 推理能力)
                    // ============================================================
                    // Claude 4 系列 (2025 最新)
                    "claude-sonnet-4-5-20250929",  // ⭐⭐⭐ 最聰明模型 (2025-09) - $3/1M in, $15/1M out
                    "claude-haiku-4-5-20251001",   // ⭐⭐⭐ 超高性價比 (2025-10) - $1/1M in, $5/1M out
                    "claude-opus-4-1-20250805",    // 旗艦版本 (2025-08) - $15/1M in, $75/1M out
                    "claude-sonnet-4-20250514",    // 平衡版本 (2025-05) - $3/1M in, $15/1M out
                    "claude-opus-4-20250514",      // 推理專用 (2025-05) - $15/1M in, $75/1M out

                    // Claude 3 系列
                    "claude-3-7-sonnet-20250219",  // ⭐⭐ 穩定版 (2025-02) - $3/1M in, $15/1M out
                    "claude-3-5-sonnet-20241022",  // ⭐ 頂尖編碼 (2024-10) - $3/1M in, $15/1M out
                    "claude-3-5-haiku-20241022",   // 快速版 (2024-10) - $1/1M in, $5/1M out
                    "claude-3-opus-20240229",      // 最強推理 (200K) - $15/1M in, $75/1M out
                    "claude-3-sonnet-20240229",    // 平衡版 (200K)
                    "claude-3-haiku-20240307",     // 快速經濟 (200K)

                    // ============================================================
                    // Google Gemini Models (推薦: 超大 context + 最低價)
                    // ============================================================
                    "gemini-2.5-pro",            // ⭐⭐ 最新 thinking 模型 (2025)
                    "gemini-2.5-flash",          // ⭐⭐⭐ 最推薦 - thinking enabled
                    "gemini-2.5-flash-lite",     // ⭐⭐⭐ 超快超便宜 - $0.1/1M in, $0.4/1M out
                    "gemini-2.0-flash",          // $0.1/1M in, $0.4/1M out
                    "gemini-2.0-flash-lite",     // 預覽版 - 最低成本
                    "gemini-2.0-pro-exp",        // 實驗版 - 編碼專用
                    "gemini-1.5-pro",            // 旗艦版 (2M context)
                    "gemini-1.5-flash",          // 快速版 (1M context)
                    "gemini-pro"                 // 穩定版
                )
                .type(PropertyType.SINGLE_SELECT_LIST)
                .index(6)
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
                .index(7)
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
                .index(8)
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
                .index(9)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_AI_RESPONSE_LANGUAGE)
                .name("Response Language")
                .description("AI 回應的語言偏好 (English or Traditional Chinese)")
                .category(CATEGORY_AI)
                .subCategory("Model Parameters")
                .defaultValue("zh-TW")
                .options("en-US", "zh-TW")
                .type(PropertyType.SINGLE_SELECT_LIST)
                .index(10)
                .build()
        );

        // ============================================================
        // Rate Limiting 配置
        // ============================================================
        context.addExtension(
            PropertyDefinition.builder(PROPERTY_AI_RATE_LIMIT_ENABLED)
                .name("Enable Rate Limiting")
                .description("啟用 TPM (Tokens Per Minute) 速率限制，防止超過 API 限制")
                .category(CATEGORY_AI)
                .subCategory("Rate Limiting")
                .defaultValue("true")
                .type(PropertyType.BOOLEAN)
                .index(11)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_AI_MAX_TOKENS_PER_MINUTE)
                .name("Max Tokens Per Minute")
                .description("每分鐘最大 token 數量（TPM 限制）。OpenAI 免費層：30000，付費層：60000-90000")
                .category(CATEGORY_AI)
                .subCategory("Rate Limiting")
                .defaultValue("30000")
                .type(PropertyType.INTEGER)
                .index(12)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_AI_RATE_LIMIT_BUFFER_RATIO)
                .name("Rate Limit Buffer Ratio")
                .description("緩衝比例（0.0-1.0）。例如 0.9 表示使用 90% 的限制，保留 10% 緩衝")
                .category(CATEGORY_AI)
                .subCategory("Rate Limiting")
                .defaultValue("0.9")
                .type(PropertyType.FLOAT)
                .index(13)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_AI_RATE_LIMIT_STRATEGY)
                .name("Rate Limit Strategy")
                .description("速率限制策略。adaptive：自動調整等待時間；fixed：使用固定延遲")
                .category(CATEGORY_AI)
                .subCategory("Rate Limiting")
                .defaultValue("adaptive")
                .options("adaptive", "fixed")
                .type(PropertyType.SINGLE_SELECT_LIST)
                .index(14)
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
                .index(9)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_CLI_COPILOT_PATH)
                .name("GitHub Copilot CLI Path")
                .description("GitHub Copilot CLI 路徑（僅在使用 copilot-cli 時需要）")
                .category(CATEGORY_AI)
                .subCategory("CLI Configuration")
                .defaultValue("/usr/local/bin/gh")
                .index(10)
                .build()
        );

        context.addExtension(
            PropertyDefinition.builder(PROPERTY_CLI_CLAUDE_PATH)
                .name("Claude CLI Path")
                .description("Claude CLI 工具路徑（僅在使用 claude-cli 時需要）")
                .category(CATEGORY_AI)
                .subCategory("CLI Configuration")
                .defaultValue("/usr/local/bin/claude")
                .index(11)
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

        LOG.debug("已註冊 {} 個配置屬性", 21); // 新增 4 個 Rate Limiting 屬性
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

        // AI 建議 API - 按需生成修復建議（節省 Token）
        context.addExtension(AiSuggestionController.class);

        LOG.debug("已註冊 {} 個 Web Service (含 SonarQubeDataService)", 6);  // 暫時停用 ConfigurationApiController
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
