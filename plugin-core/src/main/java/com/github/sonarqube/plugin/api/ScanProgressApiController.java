package com.github.sonarqube.plugin.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 掃描進度 API 控制器
 *
 * 提供即時掃描進度追蹤，包含：
 * - 當前掃描狀態（進行中/完成/失敗）
 * - 已處理/總檔案數
 * - 當前處理檔案路徑
 * - 預估剩餘時間
 * - 掃描開始/結束時間
 * - 錯誤資訊
 *
 * API 端點：
 * - GET  /api/owasp/scan/progress?project=<key>
 * - POST /api/owasp/scan/start?project=<key>
 * - POST /api/owasp/scan/update?project=<key>&file=<path>&processed=<n>&total=<n>
 * - POST /api/owasp/scan/complete?project=<key>
 * - POST /api/owasp/scan/fail?project=<key>&error=<message>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.7.0 (Epic 7, Story 7.5)
 */
public class ScanProgressApiController implements WebService {

    private static final Logger logger = LoggerFactory.getLogger(ScanProgressApiController.class);

    // 專案掃描進度儲存（記憶體快取）
    private static final Map<String, ScanProgress> progressMap = new ConcurrentHashMap<>();

    @Override
    public void define(Context context) {
        NewController controller = context.createController("api/owasp/scan")
            .setDescription("OWASP Security Scan Progress Tracking API")
            .setSince("2.7.0");

        // GET /api/owasp/scan/progress?project=<key>
        controller.createAction("progress")
            .setDescription("Get current scan progress for a project")
            .setSince("2.7.0")
            .setHandler(new GetProgressHandler())
            .setResponseExample(getClass().getResource("/examples/scan-progress.json"))
            .createParam("project")
                .setDescription("Project key")
                .setRequired(true);

        // POST /api/owasp/scan/start?project=<key>&totalFiles=<n>
        NewAction startAction = controller.createAction("start")
            .setDescription("Start a new scan and initialize progress tracking")
            .setSince("2.7.0")
            .setPost(true)
            .setHandler(new StartScanHandler());

        startAction.createParam("project")
                .setDescription("Project key")
                .setRequired(true);
        startAction.createParam("totalFiles")
                .setDescription("Total number of files to scan")
                .setRequired(true);

        // POST /api/owasp/scan/update?project=<key>&file=<path>&processed=<n>&total=<n>
        NewAction updateAction = controller.createAction("update")
            .setDescription("Update scan progress")
            .setSince("2.7.0")
            .setPost(true)
            .setHandler(new UpdateProgressHandler());

        updateAction.createParam("project")
                .setDescription("Project key")
                .setRequired(true);
        updateAction.createParam("file")
                .setDescription("Current file being processed")
                .setRequired(true);
        updateAction.createParam("processed")
                .setDescription("Number of files processed")
                .setRequired(true);
        updateAction.createParam("total")
                .setDescription("Total number of files")
                .setRequired(true);

        // POST /api/owasp/scan/complete?project=<key>
        controller.createAction("complete")
            .setDescription("Mark scan as completed")
            .setSince("2.7.0")
            .setPost(true)
            .setHandler(new CompleteScanHandler())
            .createParam("project")
                .setDescription("Project key")
                .setRequired(true);

        // POST /api/owasp/scan/fail?project=<key>&error=<message>
        NewAction failAction = controller.createAction("fail")
            .setDescription("Mark scan as failed")
            .setSince("2.7.0")
            .setPost(true)
            .setHandler(new FailScanHandler());

        failAction.createParam("project")
                .setDescription("Project key")
                .setRequired(true);
        failAction.createParam("error")
                .setDescription("Error message")
                .setRequired(true);

        controller.done();
    }

    // ==================== Request Handlers ====================

    /**
     * GET /api/owasp/scan/progress?project=<key>
     */
    private class GetProgressHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            String projectKey = request.mandatoryParam("project");

            ScanProgress progress = progressMap.get(projectKey);
            if (progress == null) {
                // 無進度資料，返回未開始狀態
                progress = new ScanProgress(projectKey);
            }

            writeJsonResponse(response, progress.toJson());
        }
    }

    /**
     * POST /api/owasp/scan/start?project=<key>&totalFiles=<n>
     */
    private class StartScanHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            String projectKey = request.mandatoryParam("project");
            int totalFiles = Integer.parseInt(request.mandatoryParam("totalFiles"));

            ScanProgress progress = new ScanProgress(projectKey);
            progress.start(totalFiles);
            progressMap.put(projectKey, progress);

            logger.info("Scan started for project {}: {} files", projectKey, totalFiles);

            writeJsonResponse(response, "{\"status\": \"success\", \"message\": \"Scan started\"}");
        }
    }

    /**
     * POST /api/owasp/scan/update?project=<key>&file=<path>&processed=<n>&total=<n>
     */
    private class UpdateProgressHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            String projectKey = request.mandatoryParam("project");
            String currentFile = request.mandatoryParam("file");
            int processed = Integer.parseInt(request.mandatoryParam("processed"));
            int total = Integer.parseInt(request.mandatoryParam("total"));

            ScanProgress progress = progressMap.computeIfAbsent(projectKey, ScanProgress::new);
            progress.update(currentFile, processed, total);

            logger.debug("Scan progress for {}: {}/{} - {}", projectKey, processed, total, currentFile);

            writeJsonResponse(response, "{\"status\": \"success\", \"message\": \"Progress updated\"}");
        }
    }

    /**
     * POST /api/owasp/scan/complete?project=<key>
     */
    private class CompleteScanHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            String projectKey = request.mandatoryParam("project");

            ScanProgress progress = progressMap.get(projectKey);
            if (progress != null) {
                progress.complete();
                logger.info("Scan completed for project {}: {} files processed in {}ms",
                    projectKey, progress.totalFiles, progress.getDurationMillis());
            }

            writeJsonResponse(response, "{\"status\": \"success\", \"message\": \"Scan completed\"}");
        }
    }

    /**
     * POST /api/owasp/scan/fail?project=<key>&error=<message>
     */
    private class FailScanHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) throws Exception {
            String projectKey = request.mandatoryParam("project");
            String error = request.mandatoryParam("error");

            ScanProgress progress = progressMap.get(projectKey);
            if (progress != null) {
                progress.fail(error);
                logger.error("Scan failed for project {}: {}", projectKey, error);
            }

            writeJsonResponse(response, "{\"status\": \"success\", \"message\": \"Scan marked as failed\"}");
        }
    }

    // ==================== Helper Methods ====================

    /**
     * 寫入 JSON 響應
     */
    private void writeJsonResponse(Response response, String json) throws IOException {
        response.stream().setStatus(200);
        response.stream().setMediaType("application/json");
        response.stream().output().write(json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 掃描進度資料模型
     */
    public static class ScanProgress {
        private final String projectKey;
        private ScanStatus status;
        private int processedFiles;
        private int totalFiles;
        private String currentFile;
        private long startTimeMillis;
        private long endTimeMillis;
        private String errorMessage;

        public ScanProgress(String projectKey) {
            this.projectKey = projectKey;
            this.status = ScanStatus.NOT_STARTED;
            this.processedFiles = 0;
            this.totalFiles = 0;
            this.currentFile = "";
            this.startTimeMillis = 0;
            this.endTimeMillis = 0;
            this.errorMessage = "";
        }

        public void start(int totalFiles) {
            this.status = ScanStatus.IN_PROGRESS;
            this.totalFiles = totalFiles;
            this.processedFiles = 0;
            this.currentFile = "";
            this.startTimeMillis = System.currentTimeMillis();
            this.endTimeMillis = 0;
            this.errorMessage = "";
        }

        public void update(String currentFile, int processed, int total) {
            this.currentFile = currentFile;
            this.processedFiles = processed;
            this.totalFiles = total;
        }

        public void complete() {
            this.status = ScanStatus.COMPLETED;
            this.endTimeMillis = System.currentTimeMillis();
            this.currentFile = "";
        }

        public void fail(String error) {
            this.status = ScanStatus.FAILED;
            this.endTimeMillis = System.currentTimeMillis();
            this.errorMessage = error;
        }

        public long getDurationMillis() {
            if (startTimeMillis == 0) {
                return 0;
            }
            long endTime = (endTimeMillis > 0) ? endTimeMillis : System.currentTimeMillis();
            return endTime - startTimeMillis;
        }

        public double getProgressPercentage() {
            if (totalFiles == 0) {
                return 0.0;
            }
            return (double) processedFiles / totalFiles * 100.0;
        }

        public long getEstimatedRemainingMillis() {
            if (processedFiles == 0 || totalFiles == 0 || status != ScanStatus.IN_PROGRESS) {
                return 0;
            }

            long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
            double avgTimePerFile = (double) elapsedMillis / processedFiles;
            int remainingFiles = totalFiles - processedFiles;

            return (long) (avgTimePerFile * remainingFiles);
        }

        public String toJson() {
            return String.format(
                "{" +
                    "\"projectKey\": \"%s\", " +
                    "\"status\": \"%s\", " +
                    "\"processedFiles\": %d, " +
                    "\"totalFiles\": %d, " +
                    "\"currentFile\": \"%s\", " +
                    "\"progressPercentage\": %.2f, " +
                    "\"startTime\": \"%s\", " +
                    "\"endTime\": \"%s\", " +
                    "\"durationMillis\": %d, " +
                    "\"estimatedRemainingMillis\": %d, " +
                    "\"errorMessage\": \"%s\"" +
                "}",
                escapeJson(projectKey),
                status,
                processedFiles,
                totalFiles,
                escapeJson(currentFile),
                getProgressPercentage(),
                (startTimeMillis > 0) ? Instant.ofEpochMilli(startTimeMillis).toString() : "",
                (endTimeMillis > 0) ? Instant.ofEpochMilli(endTimeMillis).toString() : "",
                getDurationMillis(),
                getEstimatedRemainingMillis(),
                escapeJson(errorMessage)
            );
        }

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
    }

    /**
     * 掃描狀態枚舉
     */
    public enum ScanStatus {
        NOT_STARTED,    // 未開始
        IN_PROGRESS,    // 進行中
        COMPLETED,      // 已完成
        FAILED          // 失敗
    }
}
