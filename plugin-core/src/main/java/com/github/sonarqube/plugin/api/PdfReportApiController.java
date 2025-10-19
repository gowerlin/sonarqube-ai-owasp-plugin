package com.github.sonarqube.plugin.api;

import com.github.sonarqube.plugin.settings.PdfReportSettings;
import com.github.sonarqube.report.model.AnalysisReport;
import com.github.sonarqube.report.pdf.PdfReportGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * PDF Report Export API Controller
 *
 * <p>提供 PDF 報表匯出 API 端點，支援報表格式選擇（Markdown 或 PDF）。</p>
 *
 * <p>API 端點: {@code /api/owasp/report/export?format=pdf&project=<key>}</p>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.6)
 */
public class PdfReportApiController implements WebService {

    private static final Logger LOG = LoggerFactory.getLogger(PdfReportApiController.class);

    private static final String API_ENDPOINT = "api/owasp";
    private static final String ACTION_EXPORT = "export";
    private static final String PARAM_FORMAT = "format";
    private static final String PARAM_PROJECT = "project";

    private final Configuration configuration;
    private final PdfReportGenerator pdfReportGenerator;

    /**
     * Constructor
     *
     * @param configuration SonarQube configuration
     */
    public PdfReportApiController(Configuration configuration) {
        this.configuration = configuration;
        this.pdfReportGenerator = new PdfReportGenerator();
    }

    @Override
    public void define(Context context) {
        NewController controller = context.createController(API_ENDPOINT)
                .setDescription("OWASP AI Plugin Report API")
                .setSince("2.0.0");

        // Define export action
        NewAction exportAction = controller.createAction(ACTION_EXPORT)
                .setDescription("Export OWASP security analysis report")
                .setSince("2.0.0")
                .setHandler(this::handleExportRequest)
                .setResponseExample(getClass().getResource("/api-examples/export-report.json"));

        // Define parameters
        exportAction.createParam(PARAM_FORMAT)
                .setDescription("Report format (markdown or pdf)")
                .setRequired(true)
                .setPossibleValues("markdown", "pdf")
                .setExampleValue("pdf");

        exportAction.createParam(PARAM_PROJECT)
                .setDescription("Project key")
                .setRequired(true)
                .setExampleValue("com.example:my-project");

        controller.done();

        LOG.info("PDF Report API Controller registered successfully");
    }

    /**
     * Handle export request
     *
     * @param request  Web service request
     * @param response Web service response
     */
    private void handleExportRequest(Request request, Response response) {
        String format = request.mandatoryParam(PARAM_FORMAT);
        String projectKey = request.mandatoryParam(PARAM_PROJECT);

        LOG.info("Export request received: format={}, project={}", format, projectKey);

        try {
            if ("pdf".equalsIgnoreCase(format)) {
                exportPdfReport(request, response, projectKey);
            } else if ("markdown".equalsIgnoreCase(format)) {
                exportMarkdownReport(request, response, projectKey);
            } else {
                throw new IllegalArgumentException("Unsupported format: " + format);
            }
        } catch (Exception e) {
            LOG.error("Failed to export report: format={}, project={}", format, projectKey, e);
            response.stream().setStatus(500);
            response.stream().output().write(
                    String.format("{\"error\": \"Failed to export report: %s\"}", e.getMessage()).getBytes()
            );
        }
    }

    /**
     * Export PDF report
     *
     * @param request    Web service request
     * @param response   Web service response
     * @param projectKey Project key
     * @throws IOException If file operations fail
     */
    private void exportPdfReport(Request request, Response response, String projectKey) throws IOException {
        LOG.info("Generating PDF report for project: {}", projectKey);

        // TODO: Retrieve actual analysis data from SonarQube database
        // For now, use a placeholder report
        AnalysisReport report = createPlaceholderReport(projectKey);

        // Apply configuration settings
        applyConfigurationToGenerator();

        // Generate PDF
        String pdfPath = pdfReportGenerator.generate(report);
        File pdfFile = new File(pdfPath);

        if (!pdfFile.exists()) {
            throw new IOException("PDF file not found: " + pdfPath);
        }

        LOG.info("PDF report generated successfully: {}", pdfPath);

        // Set response headers
        String filename = String.format("owasp-security-report-%s.pdf", projectKey.replace(":", "-"));
        response.stream().setMediaType("application/pdf");
        response.stream().setStatus(200);
        response.stream().output().write(("Content-Disposition: attachment; filename=\"" + filename + "\"").getBytes());

        // Write PDF file to response
        byte[] pdfBytes = Files.readAllBytes(pdfFile.toPath());
        response.stream().output().write(pdfBytes);

        LOG.info("PDF report sent to client: {} ({} bytes)", filename, pdfBytes.length);

        // Clean up temporary file
        if (pdfFile.delete()) {
            LOG.debug("Temporary PDF file deleted: {}", pdfPath);
        } else {
            LOG.warn("Failed to delete temporary PDF file: {}", pdfPath);
        }
    }

    /**
     * Export Markdown report
     *
     * @param request    Web service request
     * @param response   Web service response
     * @param projectKey Project key
     * @throws IOException If file operations fail
     */
    private void exportMarkdownReport(Request request, Response response, String projectKey) throws IOException {
        LOG.info("Generating Markdown report for project: {}", projectKey);

        // TODO: Implement Markdown report generation
        // Placeholder implementation
        String markdownContent = String.format("# OWASP Security Analysis Report\n\n**Project**: %s\n\n" +
                "Markdown report generation not yet implemented.\n", projectKey);

        // Set response headers
        String filename = String.format("owasp-security-report-%s.md", projectKey.replace(":", "-"));
        response.stream().setMediaType("text/markdown");
        response.stream().setStatus(200);
        response.stream().output().write(("Content-Disposition: attachment; filename=\"" + filename + "\"").getBytes());

        // Write Markdown content to response
        response.stream().output().write(markdownContent.getBytes());

        LOG.info("Markdown report sent to client: {} ({} bytes)", filename, markdownContent.length());
    }

    /**
     * Apply configuration settings to PDF generator
     */
    private void applyConfigurationToGenerator() {
        // Load configuration from SonarQube Settings API
        String logoPath = configuration.get(PdfReportSettings.LOGO_PATH).orElse(null);
        String reportTitle = configuration.get(PdfReportSettings.REPORT_TITLE)
                .orElse(PdfReportSettings.DEFAULT_REPORT_TITLE);
        String colorTheme = configuration.get(PdfReportSettings.COLOR_THEME)
                .orElse(PdfReportSettings.DEFAULT_COLOR_THEME);
        boolean headerFooterEnabled = configuration.getBoolean(PdfReportSettings.HEADER_FOOTER_ENABLED)
                .orElse(PdfReportSettings.DEFAULT_HEADER_FOOTER_ENABLED);

        LOG.debug("Applying PDF configuration: logo={}, title={}, theme={}, headerFooter={}",
                logoPath, reportTitle, colorTheme, headerFooterEnabled);

        // TODO: Apply these settings to PdfReportGenerator
        // This will be implemented in Task 6
    }

    /**
     * Create placeholder report for testing
     * TODO: Replace with actual data retrieval from SonarQube database
     *
     * @param projectKey Project key
     * @return Placeholder analysis report
     */
    private AnalysisReport createPlaceholderReport(String projectKey) {
        LOG.warn("Using placeholder report data - actual database integration not yet implemented");

        return AnalysisReport.builder()
                .projectName(projectKey)
                .owaspVersion("2021")
                .analysisTime(java.time.LocalDateTime.now())
                .findings(java.util.Collections.emptyList())
                .reportSummary(com.github.sonarqube.report.model.ReportSummary.builder()
                        .totalFindings(0)
                        .build())
                .build();
    }
}
