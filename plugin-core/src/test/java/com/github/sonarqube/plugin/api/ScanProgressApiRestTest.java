package com.github.sonarqube.plugin.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Scan Progress API REST 測試
 *
 * 使用 REST Assured 測試掃描進度 API 端點
 *
 * 測試範圍：
 * - HTTP 狀態碼
 * - JSON 回應格式
 * - 進度追蹤
 * - WebSocket 連線（若支援）
 *
 * 注意：此測試需要 SonarQube 執行中（預設 http://localhost:9000）
 *
 * @since 3.0.0 (Epic 8, Story 8.3)
 */
@DisplayName("ScanProgressApi REST Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled("需要執行中的 SonarQube 實例")
public class ScanProgressApiRestTest {

    private static final String BASE_URL = "http://localhost:9000";
    private static final String API_BASE_PATH = "/api/owasp/scan";

    @BeforeAll
    void setUpAll() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.basePath = API_BASE_PATH;
    }

    @Test
    @DisplayName("測試 GET /api/owasp/scan/progress 返回掃描進度")
    void testGetScanProgress() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/progress")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("status", notNullValue())
            .body("totalFiles", notNullValue())
            .body("completedFiles", notNullValue())
            .body("progressPercent", notNullValue());
    }

    @Test
    @DisplayName("測試進度百分比範圍")
    void testProgressPercentRange() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/progress")
        .then()
            .statusCode(200)
            .body("progressPercent", allOf(
                greaterThanOrEqualTo(0.0f),
                lessThanOrEqualTo(100.0f)
            ));
    }

    @Test
    @DisplayName("測試掃描狀態值")
    void testScanStatusValues() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/progress")
        .then()
            .statusCode(200)
            .body("status", anyOf(
                equalTo("IDLE"),
                equalTo("RUNNING"),
                equalTo("COMPLETED"),
                equalTo("FAILED"),
                equalTo("CANCELLED")
            ));
    }

    @Test
    @DisplayName("測試 GET /api/owasp/scan/stats 返回統計資訊")
    void testGetScanStatistics() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/stats")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("totalIssues", notNullValue())
            .body("criticalIssues", notNullValue())
            .body("highIssues", notNullValue())
            .body("mediumIssues", notNullValue())
            .body("lowIssues", notNullValue());
    }

    @Test
    @DisplayName("測試統計數字非負")
    void testStatisticsNonNegative() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/stats")
        .then()
            .statusCode(200)
            .body("totalIssues", greaterThanOrEqualTo(0))
            .body("criticalIssues", greaterThanOrEqualTo(0))
            .body("highIssues", greaterThanOrEqualTo(0))
            .body("mediumIssues", greaterThanOrEqualTo(0))
            .body("lowIssues", greaterThanOrEqualTo(0));
    }

    @Test
    @DisplayName("測試 POST /api/owasp/scan/start 啟動掃描")
    void testStartScan() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("projectKey", "test-project")
        .when()
            .post("/start")
        .then()
            .statusCode(anyOf(equalTo(200), equalTo(202)))
            .body("scanId", notNullValue())
            .body("status", equalTo("RUNNING"));
    }

    @Test
    @DisplayName("測試啟動掃描缺少參數")
    void testStartScanMissingParameter() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .post("/start")
        .then()
            .statusCode(400)
            .body("error", containsString("projectKey"));
    }

    @Test
    @DisplayName("測試 POST /api/owasp/scan/cancel 取消掃描")
    void testCancelScan() {
        // 先啟動掃描
        String scanId = given()
            .contentType(ContentType.JSON)
            .queryParam("projectKey", "test-project")
        .when()
            .post("/start")
        .then()
            .extract()
            .path("scanId");

        // 取消掃描
        given()
            .contentType(ContentType.JSON)
            .queryParam("scanId", scanId)
        .when()
            .post("/cancel")
        .then()
            .statusCode(200)
            .body("status", anyOf(
                equalTo("CANCELLED"),
                equalTo("CANCELLING")
            ));
    }

    @Test
    @DisplayName("測試 GET /api/owasp/scan/history 返回掃描歷史")
    void testGetScanHistory() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("projectKey", "test-project")
        .when()
            .get("/history")
        .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("scans", notNullValue())
            .body("scans", isA(java.util.List.class));
    }

    @Test
    @DisplayName("測試歷史記錄分頁")
    void testScanHistoryPagination() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("projectKey", "test-project")
            .queryParam("page", 1)
            .queryParam("pageSize", 10)
        .when()
            .get("/history")
        .then()
            .statusCode(200)
            .body("scans.size()", lessThanOrEqualTo(10))
            .body("totalCount", notNullValue())
            .body("currentPage", equalTo(1));
    }

    @Test
    @DisplayName("測試 JSON 回應格式")
    void testJsonResponseFormat() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/progress")
        .then()
            .statusCode(200)
            .header("Content-Type", containsString("application/json"))
            .body("$", hasKey("status"))
            .body("$", hasKey("totalFiles"))
            .body("$", hasKey("completedFiles"));
    }

    @Test
    @DisplayName("測試錯誤回應格式")
    void testErrorResponseFormat() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("scanId", "invalid-scan-id")
        .when()
            .post("/cancel")
        .then()
            .statusCode(anyOf(equalTo(400), equalTo(404)))
            .contentType(ContentType.JSON)
            .body("$", hasKey("error"))
            .body("error", notNullValue());
    }

    @Test
    @DisplayName("測試回應時間")
    void testResponseTime() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/progress")
        .then()
            .statusCode(200)
            .time(lessThan(2000L)); // 回應時間應該少於 2 秒
    }

    @Test
    @DisplayName("測試並行進度查詢")
    void testConcurrentProgressQueries() {
        for (int i = 0; i < 10; i++) {
            given()
                .contentType(ContentType.JSON)
            .when()
                .get("/progress")
            .then()
                .statusCode(200)
                .time(lessThan(3000L));
        }
    }

    @Test
    @DisplayName("測試 UTF-8 編碼支援")
    void testUtf8Encoding() {
        given()
            .contentType(ContentType.JSON.withCharset("UTF-8"))
        .when()
            .get("/progress")
        .then()
            .statusCode(200)
            .contentType(containsString("charset=UTF-8"));
    }

    @Test
    @DisplayName("測試 HTTP 方法限制")
    void testHttpMethodRestrictions() {
        // DELETE 方法應該不被允許
        given()
            .contentType(ContentType.JSON)
        .when()
            .delete("/progress")
        .then()
            .statusCode(405); // Method Not Allowed
    }

    @Test
    @DisplayName("測試時間戳格式")
    void testTimestampFormat() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("projectKey", "test-project")
        .when()
            .get("/history")
        .then()
            .statusCode(200)
            .body("scans[0].startTime", matchesRegex("\\d{4}-\\d{2}-\\d{2}T.*"))
            .body("scans[0].endTime", anyOf(
                nullValue(),
                matchesRegex("\\d{4}-\\d{2}-\\d{2}T.*")
            ));
    }

    @Test
    @DisplayName("測試掃描持續時間欄位")
    void testScanDurationField() {
        given()
            .contentType(ContentType.JSON)
            .queryParam("projectKey", "test-project")
        .when()
            .get("/history")
        .then()
            .statusCode(200)
            .body("scans[0].durationMs", anyOf(
                nullValue(),
                greaterThanOrEqualTo(0)
            ));
    }

    @Test
    @DisplayName("測試 Cache-Control 標頭")
    void testCacheControlHeader() {
        given()
            .contentType(ContentType.JSON)
        .when()
            .get("/progress")
        .then()
            .statusCode(200)
            .header("Cache-Control", containsString("no-cache"));
    }
}
