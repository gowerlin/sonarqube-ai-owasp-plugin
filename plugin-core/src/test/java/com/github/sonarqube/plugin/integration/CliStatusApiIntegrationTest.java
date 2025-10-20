package com.github.sonarqube.plugin.integration;

import com.github.sonarqube.plugin.api.CliStatusApiController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CLI Status API 整合測試
 *
 * 測試範圍：
 * - Web Service 定義
 * - API 端點註冊
 * - 參數驗證
 *
 * @since 3.0.0 (Epic 8, Story 8.2)
 */
@DisplayName("CliStatusApi Integration Tests")
public class CliStatusApiIntegrationTest {

    private WebService.Context context;
    private CliStatusApiController controller;

    @BeforeEach
    void setUp() {
        context = new WebService.Context();
        MapSettings settings = new MapSettings();
        controller = new CliStatusApiController(settings.asConfig());
    }

    @Test
    @DisplayName("測試 Web Service 定義")
    void testWebServiceDefinition() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/cli");
        assertNotNull(apiController, "CLI API controller 應該存在");
        assertEquals("OWASP AI CLI Tools Status API", apiController.description());
        assertEquals("3.0.0", apiController.since());
    }

    @Test
    @DisplayName("測試 GET /api/owasp/cli/status 端點")
    void testGetAllStatusEndpoint() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/cli");
        WebService.Action action = apiController.action("status");

        assertNotNull(action, "status action 應該存在");
        assertEquals("Get all CLI tools status", action.description());
        assertEquals("3.0.0", action.since());
    }

    @Test
    @DisplayName("測試 GET /api/owasp/cli/check 端點")
    void testCheckCliEndpoint() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/cli");
        WebService.Action action = apiController.action("check");

        assertNotNull(action, "check action 應該存在");
        assertEquals("Check specific CLI tool availability", action.description());
    }

    @Test
    @DisplayName("測試 check 端點的 provider 參數")
    void testCheckEndpointProviderParameter() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/cli");
        WebService.Action action = apiController.action("check");
        WebService.Param providerParam = action.param("provider");

        assertNotNull(providerParam, "provider 參數應該存在");
        assertTrue(providerParam.isRequired(), "provider 應該是必要參數");

        // 驗證可能的值
        assertTrue(providerParam.possibleValues().contains("gemini-cli"));
        assertTrue(providerParam.possibleValues().contains("copilot-cli"));
        assertTrue(providerParam.possibleValues().contains("claude-cli"));
    }

    @Test
    @DisplayName("測試 GET /api/owasp/cli/version 端點")
    void testGetVersionEndpoint() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/cli");
        WebService.Action action = apiController.action("version");

        assertNotNull(action, "version action 應該存在");
        assertEquals("Get CLI tool version", action.description());
    }

    @Test
    @DisplayName("測試 version 端點的 provider 參數")
    void testVersionEndpointProviderParameter() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/cli");
        WebService.Action action = apiController.action("version");
        WebService.Param providerParam = action.param("provider");

        assertNotNull(providerParam, "provider 參數應該存在");
        assertTrue(providerParam.isRequired(), "provider 應該是必要參數");
    }

    @Test
    @DisplayName("測試 GET /api/owasp/cli/auth 端點")
    void testGetAuthStatusEndpoint() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/cli");
        WebService.Action action = apiController.action("auth");

        assertNotNull(action, "auth action 應該存在");
        assertEquals("Check CLI authentication status", action.description());
    }

    @Test
    @DisplayName("測試所有端點都有 Handler")
    void testAllEndpointsHaveHandlers() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/cli");

        apiController.actions().forEach(action -> {
            assertNotNull(action.handler(),
                    "Action " + action.key() + " 應該有 handler");
        });
    }

    @Test
    @DisplayName("測試端點數量正確")
    void testCorrectNumberOfEndpoints() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/cli");

        // 應該有 4 個端點: status, check, version, auth
        assertEquals(4, apiController.actions().size(),
                "應該有 4 個 CLI API 端點");
    }

    @Test
    @DisplayName("測試所有端點都設定 since 版本")
    void testAllEndpointsHaveSinceVersion() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/cli");

        apiController.actions().forEach(action -> {
            assertNotNull(action.since(),
                    "Action " + action.key() + " 應該設定 since 版本");
            assertEquals("3.0.0", action.since(),
                    "CLI API 端點應該標註為 3.0.0 版本");
        });
    }

    @Test
    @DisplayName("測試 API 路徑格式")
    void testApiPathFormat() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/cli");

        assertEquals("api/owasp/cli", apiController.path(),
                "API 路徑應該是 api/owasp/cli");
    }

    @Test
    @DisplayName("測試 Controller 可以多次定義")
    void testControllerCanBeDefinedMultipleTimes() {
        controller.define(context);

        WebService.Context newContext = new WebService.Context();
        assertDoesNotThrow(() -> controller.define(newContext));
    }

    @Test
    @DisplayName("測試所有 CLI provider 值一致性")
    void testCliProviderValuesConsistency() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/cli");

        // 所有需要 provider 參數的端點應該有相同的可能值
        String[] expectedValues = {"gemini-cli", "copilot-cli", "claude-cli"};

        apiController.actions().forEach(action -> {
            WebService.Param providerParam = action.param("provider");
            if (providerParam != null) {
                for (String expectedValue : expectedValues) {
                    assertTrue(providerParam.possibleValues().contains(expectedValue),
                            "Action " + action.key() + " 應該支援 " + expectedValue);
                }
            }
        });
    }
}
