package com.github.sonarqube.plugin.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Configuration API REST 測試
 *
 * 使用 REST Assured 測試 API 端點的 HTTP 行為
 *
 * 測試範圍：
 * - HTTP 狀態碼
 * - JSON 回應格式
 * - 請求參數驗證
 * - 錯誤處理
 *
 * 注意：此測試需要 SonarQube 執行中（預設 http://localhost:9000）
 *
 * @since 3.0.0 (Epic 8, Story 8.3)
 */
@DisplayName("ConfigurationApi REST Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("需要執行中的 SonarQube 實例")
public class ConfigurationApiRestTest {

    private static final String BASE_URL = "http://localhost:9000";
    private static final String API_BASE_PATH = "/api/owasp/config";

    @BeforeAll
    void setUpAll() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.basePath = API_BASE_PATH;
    }

    @Test
    @DisplayName("測試 GET /api/owasp/config 返回配置資訊")
    void testGetConfiguration() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get()
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("provider", notNullValue())
            .body("model", notNullValue())
            .body("temperature", notNullValue())
            .body("maxTokens", notNullValue());
    }

    @Test
    @DisplayName("測試 POST /api/owasp/config 更新配置")
    void testUpdateConfiguration() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "openai")
            .queryParam("model", "gpt-4")
            .queryParam("temperature", "0.3")
            .queryParam("maxTokens", "2000")
        .when()
            .post("/update")
        .then()
            .statusCode(200)
            .body("success", equalTo(true))
            .body("message", containsString("Configuration updated"));
    }

    @Test
    @DisplayName("測試 POST 無效 provider 參數")
    void testUpdateConfigurationInvalidProvider() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "invalid-provider")
        .when()
            .post("/update")
        .then()
            .statusCode(400)
            .body("error", containsString("Invalid provider"));
    }

    @Test
    @DisplayName("測試 POST 缺少必要參數")
    void testUpdateConfigurationMissingParameter() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/update")
        .then()
            .statusCode(400)
            .body("error", containsString("provider"));
    }

    @Test
    @DisplayName("測試 POST temperature 參數範圍驗證")
    void testUpdateConfigurationInvalidTemperature() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "openai")
            .queryParam("temperature", "2.0") // 超出範圍 (0.0-1.0)
        .when()
            .post("/update")
        .then()
            .statusCode(400)
            .body("error", containsString("temperature"));
    }

    @Test
    @DisplayName("測試 POST maxTokens 參數驗證")
    void testUpdateConfigurationInvalidMaxTokens() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "openai")
            .queryParam("maxTokens", "-100") // 負值
        .when()
            .post("/update")
        .then()
            .statusCode(400)
            .body("error", containsString("maxTokens"));
    }

    @Test
    @DisplayName("測試 GET /api/owasp/config/validate 驗證配置")
    void testValidateConfiguration() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/validate")
        .then()
            .statusCode(200)
            .body("valid", notNullValue())
            .body("issues", notNullValue());
    }

    @Test
    @DisplayName("測試不同 AI 提供商配置")
    void testDifferentProviders() {
        String[] providers = {"openai", "anthropic", "gemini-api"};

        for (String provider : providers) {
            given()
                .contentType(ContentType.JSON)
                .queryParam("provider", provider)
                .queryParam("model", "test-model")
            .when()
                .post("/update")
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .contentType(ContentType.JSON);
        }
    }

    @Test
    @DisplayName("測試 CLI 模式提供商配置")
    void testCliProviders() {
        String[] cliProviders = {"gemini-cli", "copilot-cli", "claude-cli"};

        for (String provider : cliProviders) {
            given()
                .contentType(ContentType.JSON)
                .queryParam("provider", provider)
                .queryParam("cliPath", "/usr/local/bin/" + provider)
            .when()
                .post("/update")
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(400)))
                .contentType(ContentType.JSON);
        }
    }

    @Test
    @DisplayName("測試 API Key 參數（應該被加密）")
    void testApiKeyParameter() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "openai")
            .queryParam("apiKey", "sk-test-key-12345")
        .when()
            .post("/update")
        .then()
            .statusCode(200);

        // 再次取得配置，API Key 不應該被返回
        given()
            .contentType(ContentType.JSON)
        .when()
            .get()
        .then()
            .statusCode(200)
            .body("apiKey", either(nullValue()).or(equalTo("***")));
    }

    @Test
    @DisplayName("測試 JSON 回應格式")
    void testJsonResponseFormat() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get()
        .then()
            .statusCode(200)
            .header("Content-Type", containsString("application/json"))
            .body("$", hasKey("provider"))
            .body("$", hasKey("model"))
            .body("$", hasKey("temperature"))
            .body("$", hasKey("maxTokens"));
    }

    @Test
    @DisplayName("測試 CORS 標頭")
    void testCorsHeaders() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .options()
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(204)))
            .header("Access-Control-Allow-Origin", notNullValue());
    }

    @Test
    @DisplayName("測試同時更新多個參數")
    void testUpdateMultipleParameters() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "anthropic")
            .queryParam("model", "claude-3-opus-20240229")
            .queryParam("temperature", "0.5")
            .queryParam("maxTokens", "4000")
            .queryParam("timeout", "90")
        .when()
            .post("/update")
        .then()
            .statusCode(200)
            .body("success", equalTo(true));
    }

    @Test
    @DisplayName("測試錯誤回應格式")
    void testErrorResponseFormat() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "invalid")
        .when()
            .post("/update")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("$", hasKey("error"))
            .body("error", notNullValue());
    }

    @Test
    @DisplayName("測試特殊字元處理")
    void testSpecialCharactersHandling() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "openai")
            .queryParam("model", "gpt-4-turbo-<test>")
        .when()
            .post("/update")
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(400)));
    }

    @Test
    @DisplayName("測試 UTF-8 編碼支援")
    void testUtf8Encoding() {
        given()
            .contentType(ContentType.JSON.withCharset("UTF-8"))
            .queryParam("provider", "openai")
            .queryParam("model", "測試模型")
        .when()
            .post("/update")
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(400)))
            .contentType(containsString("charset=UTF-8"));
    }
}
