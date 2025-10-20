package com.github.sonarqube.plugin.api;

import com.github.sonarqube.config.AiConfiguration;
import com.github.sonarqube.config.ConfigurationManager;
import com.github.sonarqube.config.ScanScopeConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 配置 API 控制器
 *
 * 提供 RESTful API 端點，用於讀取和更新 AI 配置、掃描範圍配置。
 * 支援專案級和全域級配置管理。
 *
 * API 端點：
 * - GET  /api/owasp/config/ai?project=<key>
 * - POST /api/owasp/config/ai?project=<key>
 * - GET  /api/owasp/config/scan?project=<key>
 * - POST /api/owasp/config/scan?project=<key>
 * - GET  /api/owasp/config/global/ai
 * - POST /api/owasp/config/global/ai
 * - GET  /api/owasp/config/global/scan
 * - POST /api/owasp/config/global/scan
 * - GET  /api/owasp/config/validate?project=<key>
 * - POST /api/owasp/config/reset?project=<key>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.7.0 (Epic 7, Story 7.1)
 */
public class ConfigurationApiController implements WebService {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationApiController.class);
    private final ConfigurationManager configManager = ConfigurationManager.getInstance();

    @Override
    public void define(Context context) {
        NewController controller = context.createController("api/owasp/config")
            .setDescription("OWASP Security Plugin Configuration API")
            .setSince("2.7.0");

        // AI 配置端點
        defineAiConfigurationEndpoints(controller);

        // 掃描範圍配置端點
        defineScanScopeConfigurationEndpoints(controller);

        // 全域配置端點
        defineGlobalConfigurationEndpoints(controller);

        // 配置驗證與重置端點
        defineUtilityEndpoints(controller);

        controller.done();
    }

    /**
     * 定義 AI 配置端點
     */
    private void defineAiConfigurationEndpoints(NewController controller) {
        // GET /api/owasp/config/ai?project=<key>
        controller.createAction("ai")
            .setDescription("Get AI configuration for a project")
            .setSince("2.7.0")
            .setHandler(new GetAiConfigurationHandler())
            .setResponseExample(getClass().getResource("/examples/ai-config.json"))
            .createParam("project")
                .setDescription("Project key (optional, defaults to global if not provided)")
                .setRequired(false);

        // POST /api/owasp/config/ai?project=<key>
        controller.createAction("update_ai")
            .setDescription("Update AI configuration for a project")
            .setSince("2.7.0")
            .setPost(true)
            .setHandler(new UpdateAiConfigurationHandler())
            .createParam("project")
                .setDescription("Project key (required for project-level config)")
                .setRequired(false)
            .and()
            .createParam("provider")
                .setDescription("AI provider (OPENAI, ANTHROPIC)")
                .setRequired(true)
            .and()
            .createParam("apiKey")
                .setDescription("AI API key (will be encrypted)")
                .setRequired(true)
            .and()
            .createParam("model")
                .setDescription("AI model name (e.g., gpt-4, claude-3-sonnet)")
                .setRequired(true)
            .and()
            .createParam("temperature")
                .setDescription("Temperature (0.0-2.0, default 0.3)")
                .setRequired(false)
                .setDefaultValue("0.3")
            .and()
            .createParam("maxTokens")
                .setDescription("Maximum tokens (default 4096)")
                .setRequired(false)
                .setDefaultValue("4096")
            .and()
            .createParam("timeoutSeconds")
                .setDescription("Timeout in seconds (default 60)")
                .setRequired(false)
                .setDefaultValue("60");
    }

    /**
     * 定義掃描範圍配置端點
     */
    private void defineScanScopeConfigurationEndpoints(NewController controller) {
        // GET /api/owasp/config/scan?project=<key>
        controller.createAction("scan")
            .setDescription("Get scan scope configuration for a project")
            .setSince("2.7.0")
            .setHandler(new GetScanScopeConfigurationHandler())
            .setResponseExample(getClass().getResource("/examples/scan-config.json"))
            .createParam("project")
                .setDescription("Project key (optional, defaults to global if not provided)")
                .setRequired(false);

        // POST /api/owasp/config/scan?project=<key>
        controller.createAction("update_scan")
            .setDescription("Update scan scope configuration for a project")
            .setSince("2.7.0")
            .setPost(true)
            .setHandler(new UpdateScanScopeConfigurationHandler())
            .createParam("project")
                .setDescription("Project key (required for project-level config)")
                .setRequired(false)
            .and()
            .createParam("scanMode")
                .setDescription("Scan mode (FULL_PROJECT, INCREMENTAL, MANUAL_SELECTION)")
                .setRequired(true)
            .and()
            .createParam("includePatterns")
                .setDescription("Include file patterns (comma-separated)")
                .setRequired(false)
            .and()
            .createParam("excludePatterns")
                .setDescription("Exclude file patterns (comma-separated)")
                .setRequired(false)
            .and()
            .createParam("enableParallelAnalysis")
                .setDescription("Enable parallel analysis (true/false)")
                .setRequired(false)
                .setDefaultValue("true")
            .and()
            .createParam("parallelDegree")
                .setDescription("Parallel analysis degree (1-10, default 3)")
                .setRequired(false)
                .setDefaultValue("3");
    }

    /**
     * 定義全域配置端點
     */
    private void defineGlobalConfigurationEndpoints(NewController controller) {
        // GET /api/owasp/config/global/ai
        controller.createAction("global_ai")
            .setDescription("Get global AI configuration")
            .setSince("2.7.0")
            .setHandler(new GetGlobalAiConfigurationHandler())
            .setResponseExample(getClass().getResource("/examples/ai-config.json"));

        // POST /api/owasp/config/global/ai
        controller.createAction("update_global_ai")
            .setDescription("Update global AI configuration")
            .setSince("2.7.0")
            .setPost(true)
            .setHandler(new UpdateGlobalAiConfigurationHandler())
            .createParam("provider")
                .setDescription("AI provider (OPENAI, ANTHROPIC)")
                .setRequired(true)
            .and()
            .createParam("apiKey")
                .setDescription("AI API key (will be encrypted)")
                .setRequired(true)
            .and()
            .createParam("model")
                .setDescription("AI model name")
                .setRequired(true)
            .and()
            .createParam("temperature")
                .setDescription("Temperature (0.0-2.0)")
                .setRequired(false)
                .setDefaultValue("0.3")
            .and()
            .createParam("maxTokens")
                .setDescription("Maximum tokens")
                .setRequired(false)
                .setDefaultValue("4096")
            .and()
            .createParam("timeoutSeconds")
                .setDescription("Timeout in seconds")
                .setRequired(false)
                .setDefaultValue("60");

        // GET /api/owasp/config/global/scan
        controller.createAction("global_scan")
            .setDescription("Get global scan scope configuration")
            .setSince("2.7.0")
            .setHandler(new GetGlobalScanScopeConfigurationHandler())
            .setResponseExample(getClass().getResource("/examples/scan-config.json"));

        // POST /api/owasp/config/global/scan
        controller.createAction("update_global_scan")
            .setDescription("Update global scan scope configuration")
            .setSince("2.7.0")
            .setPost(true)
            .setHandler(new UpdateGlobalScanScopeConfigurationHandler())
            .createParam("scanMode")
                .setDescription("Scan mode (FULL_PROJECT, INCREMENTAL, MANUAL_SELECTION)")
                .setRequired(true)
            .and()
            .createParam("includePatterns")
                .setDescription("Include file patterns (comma-separated)")
                .setRequired(false)
            .and()
            .createParam("excludePatterns")
                .setDescription("Exclude file patterns (comma-separated)")
                .setRequired(false)
            .and()
            .createParam("enableParallelAnalysis")
                .setDescription("Enable parallel analysis (true/false)")
                .setRequired(false)
                .setDefaultValue("true")
            .and()
            .createParam("parallelDegree")
                .setDescription("Parallel analysis degree (1-10)")
                .setRequired(false)
                .setDefaultValue("3");
    }

    /**
     * 定義工具端點（驗證、重置）
     */
    private void defineUtilityEndpoints(NewController controller) {
        // GET /api/owasp/config/validate?project=<key>
        controller.createAction("validate")
            .setDescription("Validate project configuration")
            .setSince("2.7.0")
            .setHandler(new ValidateConfigurationHandler())
            .setResponseExample(getClass().getResource("/examples/validate-result.json"))
            .createParam("project")
                .setDescription("Project key")
                .setRequired(true);

        // POST /api/owasp/config/reset?project=<key>
        controller.createAction("reset")
            .setDescription("Reset project configuration to global defaults")
            .setSince("2.7.0")
            .setPost(true)
            .setHandler(new ResetConfigurationHandler())
            .createParam("project")
                .setDescription("Project key")
                .setRequired(true);

        // GET /api/owasp/config/statistics
        controller.createAction("statistics")
            .setDescription("Get configuration statistics")
            .setSince("2.7.0")
            .setHandler(new GetStatisticsHandler())
            .setResponseExample(getClass().getResource("/examples/config-stats.json"));
    }

    // ==================== Request Handlers ====================

    /**
     * GET /api/owasp/config/ai?project=<key>
     */
    private class GetAiConfigurationHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            String projectKey = request.param("project");

            AiConfiguration config;
            if (projectKey != null && !projectKey.isEmpty()) {
                config = configManager.getProjectAiConfiguration(projectKey);
                logger.info("Retrieved AI configuration for project: {}", projectKey);
            } else {
                config = configManager.getGlobalAiConfiguration();
                logger.info("Retrieved global AI configuration");
            }

            writeJsonResponse(response, buildAiConfigJson(config));
        }
    }

    /**
     * POST /api/owasp/config/ai?project=<key>
     */
    private class UpdateAiConfigurationHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            String projectKey = request.param("project");

            AiConfiguration config = AiConfiguration.builder()
                .aiProvider(request.mandatoryParam("provider"))
                .apiKey(request.mandatoryParam("apiKey"))
                .model(request.mandatoryParam("model"))
                .temperature(Double.parseDouble(request.param("temperature")))
                .maxTokens(Integer.parseInt(request.param("maxTokens")))
                .timeoutSeconds(Integer.parseInt(request.param("timeoutSeconds")))
                .build();

            if (projectKey != null && !projectKey.isEmpty()) {
                configManager.setProjectAiConfiguration(projectKey, config);
                logger.info("Updated AI configuration for project: {}", projectKey);
            } else {
                configManager.setGlobalAiConfiguration(config);
                logger.info("Updated global AI configuration");
            }

            writeJsonResponse(response, "{\"status\": \"success\", \"message\": \"AI configuration updated\"}");
        }
    }

    /**
     * GET /api/owasp/config/scan?project=<key>
     */
    private class GetScanScopeConfigurationHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            String projectKey = request.param("project");

            ScanScopeConfiguration config;
            if (projectKey != null && !projectKey.isEmpty()) {
                config = configManager.getProjectScanScopeConfiguration(projectKey);
                logger.info("Retrieved scan scope configuration for project: {}", projectKey);
            } else {
                config = configManager.getGlobalScanScopeConfiguration();
                logger.info("Retrieved global scan scope configuration");
            }

            writeJsonResponse(response, buildScanScopeConfigJson(config));
        }
    }

    /**
     * POST /api/owasp/config/scan?project=<key>
     */
    private class UpdateScanScopeConfigurationHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            String projectKey = request.param("project");

            String includePatterns = request.param("includePatterns");
            String excludePatterns = request.param("excludePatterns");

            ScanScopeConfiguration.Builder builder = ScanScopeConfiguration.builder()
                .scanMode(ScanScopeConfiguration.ScanMode.valueOf(request.mandatoryParam("scanMode")))
                .enableParallelAnalysis(Boolean.parseBoolean(request.param("enableParallelAnalysis")))
                .parallelDegree(Integer.parseInt(request.param("parallelDegree")));

            if (includePatterns != null && !includePatterns.isEmpty()) {
                builder.includePatterns(java.util.Arrays.asList(includePatterns.split(",")));
            }

            if (excludePatterns != null && !excludePatterns.isEmpty()) {
                builder.excludePatterns(java.util.Arrays.asList(excludePatterns.split(",")));
            }

            ScanScopeConfiguration config = builder.build();

            if (projectKey != null && !projectKey.isEmpty()) {
                configManager.setProjectScanScopeConfiguration(projectKey, config);
                logger.info("Updated scan scope configuration for project: {}", projectKey);
            } else {
                configManager.setGlobalScanScopeConfiguration(config);
                logger.info("Updated global scan scope configuration");
            }

            writeJsonResponse(response, "{\"status\": \"success\", \"message\": \"Scan scope configuration updated\"}");
        }
    }

    /**
     * GET /api/owasp/config/global/ai
     */
    private class GetGlobalAiConfigurationHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            AiConfiguration config = configManager.getGlobalAiConfiguration();
            writeJsonResponse(response, buildAiConfigJson(config));
        }
    }

    /**
     * POST /api/owasp/config/global/ai
     */
    private class UpdateGlobalAiConfigurationHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            AiConfiguration config = AiConfiguration.builder()
                .aiProvider(request.mandatoryParam("provider"))
                .apiKey(request.mandatoryParam("apiKey"))
                .model(request.mandatoryParam("model"))
                .temperature(Double.parseDouble(request.param("temperature")))
                .maxTokens(Integer.parseInt(request.param("maxTokens")))
                .timeoutSeconds(Integer.parseInt(request.param("timeoutSeconds")))
                .build();

            configManager.setGlobalAiConfiguration(config);
            logger.info("Updated global AI configuration");

            writeJsonResponse(response, "{\"status\": \"success\", \"message\": \"Global AI configuration updated\"}");
        }
    }

    /**
     * GET /api/owasp/config/global/scan
     */
    private class GetGlobalScanScopeConfigurationHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            ScanScopeConfiguration config = configManager.getGlobalScanScopeConfiguration();
            writeJsonResponse(response, buildScanScopeConfigJson(config));
        }
    }

    /**
     * POST /api/owasp/config/global/scan
     */
    private class UpdateGlobalScanScopeConfigurationHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            String includePatterns = request.param("includePatterns");
            String excludePatterns = request.param("excludePatterns");

            ScanScopeConfiguration.Builder builder = ScanScopeConfiguration.builder()
                .scanMode(ScanScopeConfiguration.ScanMode.valueOf(request.mandatoryParam("scanMode")))
                .enableParallelAnalysis(Boolean.parseBoolean(request.param("enableParallelAnalysis")))
                .parallelDegree(Integer.parseInt(request.param("parallelDegree")));

            if (includePatterns != null && !includePatterns.isEmpty()) {
                builder.includePatterns(java.util.Arrays.asList(includePatterns.split(",")));
            }

            if (excludePatterns != null && !excludePatterns.isEmpty()) {
                builder.excludePatterns(java.util.Arrays.asList(excludePatterns.split(",")));
            }

            ScanScopeConfiguration config = builder.build();
            configManager.setGlobalScanScopeConfiguration(config);
            logger.info("Updated global scan scope configuration");

            writeJsonResponse(response, "{\"status\": \"success\", \"message\": \"Global scan scope configuration updated\"}");
        }
    }

    /**
     * GET /api/owasp/config/validate?project=<key>
     */
    private class ValidateConfigurationHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            String projectKey = request.mandatoryParam("project");

            ConfigurationManager.ConfigurationValidationResult result =
                configManager.validateProjectConfiguration(projectKey);

            String json = String.format(
                "{\"valid\": %s, \"message\": \"%s\"}",
                result.isValid(),
                escapeJson(result.getMessage())
            );

            writeJsonResponse(response, json);
        }
    }

    /**
     * POST /api/owasp/config/reset?project=<key>
     */
    private class ResetConfigurationHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            String projectKey = request.mandatoryParam("project");

            configManager.resetProjectConfiguration(projectKey);
            logger.info("Reset configuration for project: {}", projectKey);

            writeJsonResponse(response, "{\"status\": \"success\", \"message\": \"Project configuration reset to global defaults\"}");
        }
    }

    /**
     * GET /api/owasp/config/statistics
     */
    private class GetStatisticsHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            ConfigurationManager.ConfigurationStatistics stats = configManager.getStatistics();

            String json = String.format(
                "{\"projectAiConfigCount\": %d, \"projectScanConfigCount\": %d, " +
                "\"globalAi\": %s, \"globalScan\": %s}",
                stats.getProjectAiConfigCount(),
                stats.getProjectScanConfigCount(),
                buildAiConfigJson(stats.getGlobalAiConfig()),
                buildScanScopeConfigJson(stats.getGlobalScanConfig())
            );

            writeJsonResponse(response, json);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * 建立 AI 配置 JSON 字串
     */
    private String buildAiConfigJson(AiConfiguration config) {
        return String.format(
            "{\"provider\": \"%s\", \"model\": \"%s\", \"temperature\": %.2f, " +
            "\"maxTokens\": %d, \"timeoutSeconds\": %d, \"valid\": %s, \"summary\": \"%s\"}",
            config.getAiProvider(),
            config.getModel(),
            config.getTemperature(),
            config.getMaxTokens(),
            config.getTimeoutSeconds(),
            config.isValid(),
            escapeJson(config.getSummary())
        );
    }

    /**
     * 建立掃描範圍配置 JSON 字串
     */
    private String buildScanScopeConfigJson(ScanScopeConfiguration config) {
        return String.format(
            "{\"scanMode\": \"%s\", \"includePatterns\": [%s], \"excludePatterns\": [%s], " +
            "\"enableParallelAnalysis\": %s, \"parallelDegree\": %d, \"summary\": \"%s\"}",
            config.getScanMode(),
            joinAsJsonArray(config.getIncludePatterns()),
            joinAsJsonArray(config.getExcludePatterns()),
            config.isEnableParallelAnalysis(),
            config.getParallelDegree(),
            escapeJson(config.getSummary())
        );
    }

    /**
     * 寫入 JSON 響應
     */
    private void writeJsonResponse(Response response, String json) throws IOException {
        response.stream().setStatus(200);
        response.stream().setMediaType("application/json");
        response.stream().output().write(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * JSON 特殊字元轉義
     */
    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    /**
     * 將列表轉為 JSON 陣列字串
     */
    private String joinAsJsonArray(java.util.List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.stream()
            .map(s -> "\"" + escapeJson(s) + "\"")
            .reduce((a, b) -> a + ", " + b)
            .orElse("");
    }
}
