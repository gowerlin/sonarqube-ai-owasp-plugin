# API Design and Integration

## API Integration Strategy

**No new public APIs required.** PDF report generation integrates through existing internal mechanisms:

**Integration Method:** Strategy Pattern via `ReportGenerator` interface
**Authentication:** Uses existing SonarQube authentication (no separate auth needed)
**Versioning:** Not applicable (internal component, follows plugin versioning)

## Internal Interface Implementation

**ReportGenerator Interface** (Existing, no modifications):
```java
public interface ReportGenerator {
    String generate(AnalysisReport report) throws ReportGenerationException;
    String getFormat();
    String getFileExtension();
}
```

**PdfReportGenerator Implementation**:
```java
public class PdfReportGenerator implements ReportGenerator {
    @Override
    public String generate(AnalysisReport report) throws ReportGenerationException {
        // 1. Load configuration
        PdfReportConfig config = loadConfiguration();

        // 2. Initialize PDF document
        String filename = buildFilename(report);
        Document document = initializeDocument(filename, config);

        // 3. Create document structure
        layoutManager.createCoverPage(document, report, config);
        layoutManager.createTableOfContents(document, buildSections());

        // 4. Add executive summary
        addExecutiveSummary(document, report.getSummary());

        // 5. Generate and embed charts
        Image severityChart = chartGenerator.generateSeverityPieChart(report.getSummary());
        Image categoryChart = chartGenerator.generateOwaspCategoryBarChart(report.getSummary());
        document.add(severityChart);
        document.add(categoryChart);

        // 6. Add detailed findings
        addDetailedFindings(document, report.getFindings());

        // 7. Add footer page
        addFooterPage(document, report, config);

        // 8. Close and return path
        document.close();
        return filename;
    }

    @Override
    public String getFormat() {
        return "pdf";
    }

    @Override
    public String getFileExtension() {
        return ".pdf";
    }
}
```

## Configuration API (SonarQube Settings Integration)

**Configuration Loading**:
```java
private PdfReportConfig loadConfiguration() {
    Configuration config = sonarQubeConfiguration.get();

    return PdfReportConfig.builder()
        .logoPath(config.getString("owasp.pdf.logo.path").orElse(null))
        .reportTitle(config.getString("owasp.pdf.report.title")
            .orElse("OWASP Security Analysis Report"))
        .colorTheme(config.getString("owasp.pdf.color.theme")
            .map(ColorTheme::valueOf)
            .orElse(ColorTheme.DEFAULT))
        .headerFooterEnabled(config.getBoolean("owasp.pdf.header.footer.enabled")
            .orElse(true))
        .build();
}
```

**Configuration Saving** (from UI):
```java
public void saveConfiguration(PdfReportConfig config) {
    Configuration sonarConfig = sonarQubeConfiguration.get();

    sonarConfig.setProperty("owasp.pdf.logo.path", config.getLogoPath());
    sonarConfig.setProperty("owasp.pdf.report.title", config.getReportTitle());
    sonarConfig.setProperty("owasp.pdf.color.theme", config.getColorTheme().name());
    sonarConfig.setProperty("owasp.pdf.header.footer.enabled",
        String.valueOf(config.isHeaderFooterEnabled()));
}
```

---
