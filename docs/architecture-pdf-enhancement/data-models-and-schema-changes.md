# Data Models and Schema Changes

## Data Models Integration

**No new data models required.** PDF report generator fully reuses existing models:

1. **AnalysisReport** (`report-generator/src/main/java/com/github/sonarqube/report/model/AnalysisReport.java`)
   - **Usage**: Primary input for PDF generation
   - **Integration**: Passed to `PdfReportGenerator.generate(AnalysisReport report)`
   - **No modifications**: All fields used as-is (projectName, owaspVersion, analysisTime, findings, summary, aiModel)

2. **SecurityFinding** (`report-generator/src/main/java/com/github/sonarqube/report/model/SecurityFinding.java`)
   - **Usage**: Individual security issue details
   - **Integration**: Rendered in "Detailed Findings" section of PDF
   - **No modifications**: All fields rendered (ruleKey, ruleName, filePath, lineNumber, severity, owaspCategory, cweIds, description, codeSnippet, fixSuggestion)

3. **ReportSummary** (`report-generator/src/main/java/com/github/sonarqube/report/model/ReportSummary.java`)
   - **Usage**: Executive summary statistics and charts
   - **Integration**: Rendered in executive summary page and used for chart data
   - **No modifications**: All fields used (totalFindings, severityCounts, categoryCounts, blockerCount, criticalCount, majorCount, filesAnalyzed)

## New Internal Models (Non-persistent)

**PdfReportConfig** (Configuration POJO, not persisted to DB):
```java
public class PdfReportConfig {
    private String logoPath;           // Path to company logo (stored in SonarQube data/)
    private String reportTitle;        // Custom report title
    private ColorTheme colorTheme;     // DEFAULT, DARK, LIGHT
    private boolean headerFooterEnabled; // Enable/disable headers/footers

    // Builder pattern for construction
    public static Builder builder() { ... }
}
```

**Storage**: Configuration values stored via SonarQube Settings API as key-value pairs:
- `owasp.pdf.logo.path` → String
- `owasp.pdf.report.title` → String
- `owasp.pdf.color.theme` → Enum (DEFAULT/DARK/LIGHT)
- `owasp.pdf.header.footer.enabled` → Boolean

## Schema Integration Strategy

**Database Changes Required:** None

**Configuration Storage:**
- Use SonarQube `Configuration` API (same as AI API keys)
- Key-value pairs stored in SonarQube's internal properties table
- Logo file stored in `<sonarqube-data>/owasp-plugin/logo.png`

**Backward Compatibility:**
- All configuration optional with sensible defaults
- PDF generator works without any configuration (uses default styling)
- Existing Markdown generator completely unaffected

---
