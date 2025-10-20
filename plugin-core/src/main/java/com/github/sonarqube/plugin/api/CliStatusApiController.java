package com.github.sonarqube.plugin.api;

import com.github.sonarqube.plugin.ai.AiProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.github.sonarqube.plugin.AiOwaspPlugin.*;

/**
 * CLI 狀態檢查 API 控制器
 *
 * 提供 AI CLI 工具的狀態檢查、版本查詢、認證驗證功能。
 *
 * API 端點：
 * - GET /api/owasp/cli/status - 取得所有 CLI 工具狀態
 * - GET /api/owasp/cli/check?provider=<type> - 檢查特定 CLI 工具
 * - GET /api/owasp/cli/version?provider=<type> - 取得 CLI 版本
 * - GET /api/owasp/cli/auth?provider=<type> - 檢查認證狀態
 *
 * @since 3.0.0 (Epic 9)
 * @author SonarQube AI OWASP Plugin Team
 */
public class CliStatusApiController implements WebService {

    private static final Logger LOG = LoggerFactory.getLogger(CliStatusApiController.class);

    private final Configuration configuration;

    public CliStatusApiController(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void define(Context context) {
        NewController controller = context.createController("api/owasp/cli")
                .setDescription("OWASP AI CLI Tools Status API")
                .setSince("3.0.0");

        // GET /api/owasp/cli/status
        controller.createAction("status")
                .setDescription("Get all CLI tools status")
                .setSince("3.0.0")
                .setHandler(new GetAllStatusHandler());

        // GET /api/owasp/cli/check?provider=<type>
        controller.createAction("check")
                .setDescription("Check specific CLI tool availability")
                .setSince("3.0.0")
                .setHandler(new CheckCliHandler())
                .createParam("provider")
                .setDescription("Provider type (gemini-cli, copilot-cli, claude-cli)")
                .setRequired(true)
                .setPossibleValues("gemini-cli", "copilot-cli", "claude-cli");

        // GET /api/owasp/cli/version?provider=<type>
        controller.createAction("version")
                .setDescription("Get CLI tool version")
                .setSince("3.0.0")
                .setHandler(new GetVersionHandler())
                .createParam("provider")
                .setDescription("Provider type")
                .setRequired(true)
                .setPossibleValues("gemini-cli", "copilot-cli", "claude-cli");

        // GET /api/owasp/cli/auth?provider=<type>
        controller.createAction("auth")
                .setDescription("Check CLI authentication status")
                .setSince("3.0.0")
                .setHandler(new GetAuthStatusHandler())
                .createParam("provider")
                .setDescription("Provider type")
                .setRequired(true)
                .setPossibleValues("gemini-cli", "copilot-cli", "claude-cli");

        controller.done();
    }

    /**
     * GET /api/owasp/cli/status
     */
    private class GetAllStatusHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            LOG.debug("取得所有 CLI 工具狀態");

            // 取得配置的 CLI 路徑
            String geminiPath = configuration.get(PROPERTY_CLI_GEMINI_PATH)
                    .orElse(AiProviderFactory.getDefaultCliPath(AiProviderFactory.ProviderType.GEMINI_CLI));
            String copilotPath = configuration.get(PROPERTY_CLI_COPILOT_PATH)
                    .orElse(AiProviderFactory.getDefaultCliPath(AiProviderFactory.ProviderType.COPILOT_CLI));
            String claudePath = configuration.get(PROPERTY_CLI_CLAUDE_PATH)
                    .orElse(AiProviderFactory.getDefaultCliPath(AiProviderFactory.ProviderType.CLAUDE_CLI));

            // 檢查各 CLI 工具狀態
            boolean geminiAvailable = AiProviderFactory.isCliAvailable(
                    AiProviderFactory.ProviderType.GEMINI_CLI, geminiPath);
            boolean copilotAvailable = AiProviderFactory.isCliAvailable(
                    AiProviderFactory.ProviderType.COPILOT_CLI, copilotPath);
            boolean claudeAvailable = AiProviderFactory.isCliAvailable(
                    AiProviderFactory.ProviderType.CLAUDE_CLI, claudePath);

            // 建構 JSON 回應
            String json = String.format(
                    "{" +
                            "\"gemini\": {\"path\": \"%s\", \"available\": %b}, " +
                            "\"copilot\": {\"path\": \"%s\", \"available\": %b}, " +
                            "\"claude\": {\"path\": \"%s\", \"available\": %b}" +
                            "}",
                    escapeJson(geminiPath), geminiAvailable,
                    escapeJson(copilotPath), copilotAvailable,
                    escapeJson(claudePath), claudeAvailable
            );

            writeJsonResponse(response, json);
        }
    }

    /**
     * GET /api/owasp/cli/check?provider=<type>
     */
    private class CheckCliHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            String provider = request.mandatoryParam("provider");

            LOG.debug("檢查 CLI 工具: {}", provider);

            // 取得 CLI 路徑
            String cliPath = getCliPath(provider);

            // 檢查可用性
            AiProviderFactory.ProviderType type = AiProviderFactory.ProviderType.fromConfigValue(provider);
            boolean available = AiProviderFactory.isCliAvailable(type, cliPath);

            // 取得版本資訊
            String version = available ? AiProviderFactory.getCliVersion(type, cliPath) : "unknown";

            // 建構 JSON 回應
            String json = String.format(
                    "{\"provider\": \"%s\", \"path\": \"%s\", \"available\": %b, \"version\": \"%s\"}",
                    provider, escapeJson(cliPath), available, escapeJson(version)
            );

            writeJsonResponse(response, json);
        }
    }

    /**
     * GET /api/owasp/cli/version?provider=<type>
     */
    private class GetVersionHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            String provider = request.mandatoryParam("provider");

            LOG.debug("取得 CLI 版本: {}", provider);

            // 取得 CLI 路徑
            String cliPath = getCliPath(provider);

            // 取得版本
            AiProviderFactory.ProviderType type = AiProviderFactory.ProviderType.fromConfigValue(provider);
            String version = AiProviderFactory.getCliVersion(type, cliPath);

            // 建構 JSON 回應
            String json = String.format(
                    "{\"provider\": \"%s\", \"version\": \"%s\"}",
                    provider, escapeJson(version)
            );

            writeJsonResponse(response, json);
        }
    }

    /**
     * GET /api/owasp/cli/auth?provider=<type>
     */
    private class GetAuthStatusHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            String provider = request.mandatoryParam("provider");

            LOG.debug("檢查 CLI 認證狀態: {}", provider);

            // 取得 CLI 路徑
            String cliPath = getCliPath(provider);

            // 檢查認證狀態
            AiProviderFactory.ProviderType type = AiProviderFactory.ProviderType.fromConfigValue(provider);
            String authStatus = AiProviderFactory.getAuthenticationStatus(type, cliPath);

            // 建構 JSON 回應
            String json = String.format(
                    "{\"provider\": \"%s\", \"authStatus\": \"%s\"}",
                    provider, escapeJson(authStatus)
            );

            writeJsonResponse(response, json);
        }
    }

    /**
     * 取得 CLI 路徑
     */
    private String getCliPath(String provider) {
        AiProviderFactory.ProviderType type = AiProviderFactory.ProviderType.fromConfigValue(provider);

        switch (type) {
            case GEMINI_CLI:
                return configuration.get(PROPERTY_CLI_GEMINI_PATH)
                        .orElse(AiProviderFactory.getDefaultCliPath(type));
            case COPILOT_CLI:
                return configuration.get(PROPERTY_CLI_COPILOT_PATH)
                        .orElse(AiProviderFactory.getDefaultCliPath(type));
            case CLAUDE_CLI:
                return configuration.get(PROPERTY_CLI_CLAUDE_PATH)
                        .orElse(AiProviderFactory.getDefaultCliPath(type));
            default:
                throw new IllegalArgumentException("Unsupported CLI provider: " + provider);
        }
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
     * 跳脫 JSON 字串
     */
    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
