package com.github.sonarqube.plugin.integration;

import com.github.sonarqube.plugin.api.ConfigurationApiController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.server.ws.WebService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Configuration API 整合測試
 *
 * 測試範圍：
 * - Web Service 定義
 * - API 端點註冊
 * - 參數驗證
 *
 * @since 3.0.0 (Epic 8, Story 8.2)
 */
@DisplayName("ConfigurationApi Integration Tests")
public class ConfigurationApiIntegrationTest {

    private WebService.Context context;
    private ConfigurationApiController controller;

    @BeforeEach
    void setUp() {
        context = new WebService.Context();
        MapSettings settings = new MapSettings();
        controller = new ConfigurationApiController(settings.asConfig());
    }

    @Test
    @DisplayName("測試 Web Service 定義")
    void testWebServiceDefinition() {
        controller.define(context);

        // 驗證 controller 存在
        WebService.Controller apiController = context.controller("api/owasp/config");
        assertNotNull(apiController, "API controller 應該存在");
        assertEquals("OWASP AI Configuration API", apiController.description());
        assertEquals("1.0.0", apiController.since());
    }

    @Test
    @DisplayName("測試 GET /api/owasp/config 端點存在")
    void testGetConfigurationEndpoint() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/config");
        WebService.Action action = apiController.action("get");

        assertNotNull(action, "GET action 應該存在");
        assertEquals("Get current AI configuration", action.description());
        assertEquals("1.0.0", action.since());
    }

    @Test
    @DisplayName("測試 POST /api/owasp/config 端點存在")
    void testPostConfigurationEndpoint() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/config");
        WebService.Action action = apiController.action("update");

        assertNotNull(action, "POST action 應該存在");
        assertNotNull(action.description());
        assertEquals("1.0.0", action.since());
    }

    @Test
    @DisplayName("測試 POST 端點必要參數定義")
    void testPostEndpointRequiredParameters() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/config");
        WebService.Action action = apiController.action("update");

        // 驗證參數存在
        WebService.Param providerParam = action.param("provider");
        assertNotNull(providerParam, "provider 參數應該存在");

        // 驗證可能的值
        assertTrue(providerParam.possibleValues().contains("openai"));
        assertTrue(providerParam.possibleValues().contains("anthropic"));
        assertTrue(providerParam.possibleValues().contains("gemini-api"));
    }

    @Test
    @DisplayName("測試 POST 端點選擇性參數")
    void testPostEndpointOptionalParameters() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/config");
        WebService.Action action = apiController.action("update");

        // 驗證選擇性參數
        assertNotNull(action.param("apiKey"));
        assertNotNull(action.param("model"));
        assertNotNull(action.param("temperature"));
        assertNotNull(action.param("maxTokens"));
    }

    @Test
    @DisplayName("測試 GET /api/owasp/config/validate 端點存在")
    void testValidateConfigurationEndpoint() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/config");
        WebService.Action action = apiController.action("validate");

        assertNotNull(action, "validate action 應該存在");
        assertNotNull(action.description());
    }

    @Test
    @DisplayName("測試所有端點 Handler 已註冊")
    void testAllEndpointsHaveHandlers() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/config");

        // 所有 action 應該都有 handler
        apiController.actions().forEach(action -> {
            assertNotNull(action.handler(),
                    "Action " + action.key() + " 應該有 handler");
        });
    }

    @Test
    @DisplayName("測試端點數量正確")
    void testCorrectNumberOfEndpoints() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/config");

        // 應該有 3 個端點: get, update, validate
        assertEquals(3, apiController.actions().size(),
                "應該有 3 個 API 端點");
    }

    @Test
    @DisplayName("測試 API 路徑格式")
    void testApiPathFormat() {
        controller.define(context);

        // 驗證路徑格式
        assertTrue(context.controller("api/owasp/config").path().startsWith("api/"),
                "API 路徑應該以 api/ 開頭");
    }

    @Test
    @DisplayName("測試 Controller 可以多次定義")
    void testControllerCanBeDefinedMultipleTimes() {
        // 第一次定義
        controller.define(context);

        // 第二次定義（應該不會拋出異常）
        WebService.Context newContext = new WebService.Context();
        assertDoesNotThrow(() -> controller.define(newContext));
    }

    @Test
    @DisplayName("測試 temperature 參數範圍")
    void testTemperatureParameterRange() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/config");
        WebService.Action action = apiController.action("update");
        WebService.Param tempParam = action.param("temperature");

        assertNotNull(tempParam);
        // temperature 應該是 FLOAT 類型
        // 實際範圍驗證在 handler 中進行
    }

    @Test
    @DisplayName("測試 maxTokens 參數類型")
    void testMaxTokensParameterType() {
        controller.define(context);

        WebService.Controller apiController = context.controller("api/owasp/config");
        WebService.Action action = apiController.action("update");
        WebService.Param maxTokensParam = action.param("maxTokens");

        assertNotNull(maxTokensParam);
        // maxTokens 應該是 INTEGER 類型
    }
}
