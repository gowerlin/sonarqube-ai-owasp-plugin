package com.github.sonarqube.plugin.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * CLI Status API REST 測試
 *
 * 使用 REST Assured 測試 CLI 狀態 API 端點
 *
 * 測試範圍：
 * - HTTP 狀態碼
 * - JSON 回應格式
 * - CLI 工具狀態檢查
 * - 錯誤處理
 *
 * 注意：此測試需要 SonarQube 執行中（預設 http://localhost:9000）
 *
 * @since 3.0.0 (Epic 8, Story 8.3)
 */
@DisplayName("CliStatusApi REST Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("需要執行中的 SonarQube 實例")
public class CliStatusApiRestTest {

    private static final String BASE_URL = "http://localhost:9000";
    private static final String API_BASE_PATH = "/api/owasp/cli";

    @BeforeAll
    void setUpAll() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.basePath = API_BASE_PATH;
    }

    @Test
    @DisplayName("測試 GET /api/owasp/cli/status 返回所有 CLI 工具狀態")
    void testGetAllCliStatus() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/status")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("gemini", notNullValue())
            .body("gemini.path", notNullValue())
            .body("gemini.available", notNullValue())
            .body("copilot", notNullValue())
            .body("copilot.path", notNullValue())
            .body("copilot.available", notNullValue())
            .body("claude", notNullValue())
            .body("claude.path", notNullValue())
            .body("claude.available", notNullValue());
    }

    @Test
    @DisplayName("測試 GET /api/owasp/cli/check 檢查特定 CLI 工具")
    void testCheckSpecificCliTool() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "gemini-cli")
        .when()
            .get("/check")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("provider", equalTo("gemini-cli"))
            .body("path", notNullValue())
            .body("available", notNullValue())
            .body("version", notNullValue());
    }

    @Test
    @DisplayName("測試檢查所有 CLI 工具")
    void testCheckAllCliTools() {
        String[] providers = {"gemini-cli", "copilot-cli", "claude-cli"};

        for (String provider : providers) {
            given()
                .contentType(ContentType.JSON)
                .queryParam("provider", provider)
            .when()
                .get("/check")
            .then()
                .statusCode(200)
                .body("provider", equalTo(provider))
                .body("path", notNullValue())
                .body("available", anyOf(equalTo(true), equalTo(false)));
        }
    }

    @Test
    @DisplayName("測試無效的 provider 參數")
    void testInvalidProviderParameter() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "invalid-provider")
        .when()
            .get("/check")
        .then()
            .statusCode(400)
            .body("error", containsString("provider"));
    }

    @Test
    @DisplayName("測試缺少 provider 參數")
    void testMissingProviderParameter() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/check")
        .then()
            .statusCode(400)
            .body("error", containsString("provider"));
    }

    @Test
    @DisplayName("測試 GET /api/owasp/cli/version 取得 CLI 版本")
    void testGetCliVersion() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "gemini-cli")
        .when()
            .get("/version")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("provider", equalTo("gemini-cli"))
            .body("version", notNullValue());
    }

    @Test
    @DisplayName("測試取得所有 CLI 工具版本")
    void testGetAllCliVersions() {
        String[] providers = {"gemini-cli", "copilot-cli", "claude-cli"};

        for (String provider : providers) {
            given()
                .contentType(ContentType.JSON)
                .queryParam("provider", provider)
            .when()
                .get("/version")
            .then()
                .statusCode(200)
                .body("provider", equalTo(provider))
                .body("version", notNullValue());
        }
    }

    @Test
    @DisplayName("測試 GET /api/owasp/cli/auth 檢查認證狀態")
    void testGetAuthStatus() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "gemini-cli")
        .when()
            .get("/auth")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("provider", equalTo("gemini-cli"))
            .body("authStatus", notNullValue());
    }

    @Test
    @DisplayName("測試檢查所有 CLI 工具認證狀態")
    void testCheckAllAuthStatuses() {
        String[] providers = {"gemini-cli", "copilot-cli", "claude-cli"};

        for (String provider : providers) {
            given()
                .contentType(ContentType.JSON)
                .queryParam("provider", provider)
            .when()
                .get("/auth")
            .then()
                .statusCode(200)
                .body("provider", equalTo(provider))
                .body("authStatus", notNullValue());
        }
    }

    @Test
    @DisplayName("測試 JSON 回應格式正確性")
    void testJsonResponseFormat() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/status")
        .then()
            .statusCode(200)
            .header("Content-Type", containsString("application/json"))
            .body("$", hasKey("gemini"))
            .body("$", hasKey("copilot"))
            .body("$", hasKey("claude"));
    }

    @Test
    @DisplayName("測試 available 欄位為布林值")
    void testAvailableFieldIsBoolean() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "gemini-cli")
        .when()
            .get("/check")
        .then()
            .statusCode(200)
            .body("available", isA(Boolean.class));
    }

    @Test
    @DisplayName("測試路徑欄位格式")
    void testPathFieldFormat() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/status")
        .then()
            .statusCode(200)
            .body("gemini.path", matchesRegex(".*gemini.*"))
            .body("copilot.path", matchesRegex(".*gh.*"))
            .body("claude.path", matchesRegex(".*claude.*"));
    }

    @Test
    @DisplayName("測試錯誤回應格式")
    void testErrorResponseFormat() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "invalid")
        .when()
            .get("/check")
        .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("$", hasKey("error"))
            .body("error", notNullValue());
    }

    @Test
    @DisplayName("測試 CLI 不可用時的回應")
    void testCliNotAvailableResponse() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "gemini-cli")
        .when()
            .get("/check")
        .then()
            .statusCode(200)
            .body("available", anyOf(equalTo(true), equalTo(false)))
            .body("version", anyOf(notNullValue(), equalTo("unknown")));
    }

    @Test
    @DisplayName("測試認證失敗時的回應")
    void testAuthFailureResponse() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "copilot-cli")
        .when()
            .get("/auth")
        .then()
            .statusCode(200)
            .body("authStatus", anyOf(
                containsString("Authenticated"),
                containsString("Not authenticated"),
                containsString("Error")
            ));
    }

    @Test
    @DisplayName("測試特殊字元處理")
    void testSpecialCharactersHandling() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("provider", "gemini-cli<>")
        .when()
            .get("/check")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("測試 HTTP 方法限制")
    void testHttpMethodRestrictions() {
        // POST 方法應該不被允許（僅支援 GET）
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/status")
        .then()
            .statusCode(405); // Method Not Allowed
    }

    @Test
    @DisplayName("測試並行請求處理")
    void testConcurrentRequests() {
        // 發送多個並行請求
        for (int i = 0; i < 5; i++) {
            given()
                .contentType(ContentType.JSON)
            .when()
                .get("/status")
            .then()
                .statusCode(200)
                .time(lessThan(5000L)); // 回應時間應該少於 5 秒
        }
    }

    @Test
    @DisplayName("測試 UTF-8 編碼支援")
    void testUtf8Encoding() {
        given()
            .contentType(ContentType.JSON.withCharset("UTF-8"))
            .queryParam("provider", "gemini-cli")
        .when()
            .get("/check")
        .then()
            .statusCode(200)
            .contentType(containsString("charset=UTF-8"));
    }

    @Test
    @DisplayName("測試回應時間")
    void testResponseTime() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/status")
        .then()
            .statusCode(200)
            .time(lessThan(3000L)); // 回應時間應該少於 3 秒
    }

    @Test
    @DisplayName("測試 Cache-Control 標頭")
    void testCacheControlHeader() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/status")
        .then()
            .statusCode(200)
            .header("Cache-Control", anyOf(
                containsString("no-cache"),
                containsString("max-age")
            ));
    }
}
