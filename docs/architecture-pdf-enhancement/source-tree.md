# Source Tree

## Existing Project Structure (Relevant Parts)

```plaintext
Security_Plugin_for_SonarQube/
├── report-generator/                   # Existing module
│   ├── src/main/java/com/github/sonarqube/report/
│   │   ├── markdown/
│   │   │   └── MarkdownReportGenerator.java  # Existing, no changes
│   │   ├── model/
│   │   │   ├── AnalysisReport.java           # Existing, reused
│   │   │   ├── SecurityFinding.java          # Existing, reused
│   │   │   └── ReportSummary.java            # Existing, reused
│   │   └── ReportGenerator.java              # Existing interface, no changes
│   ├── src/test/java/com/github/sonarqube/report/
│   │   └── markdown/
│   │       └── MarkdownReportGeneratorTest.java  # Existing, no changes
│   └── pom.xml                                # Modified: add iText dependency
├── plugin-core/                        # Existing module
│   └── src/main/java/com/github/sonarqube/plugin/
│       ├── OwaspAiPlugin.java          # Existing, no changes (PDF auto-registered)
│       └── OwaspSensor.java            # Existing, may need minor update for format selection
```

## New File Organization

```plaintext
Security_Plugin_for_SonarQube/
├── report-generator/
│   ├── src/main/java/com/github/sonarqube/report/
│   │   ├── pdf/                        # NEW: PDF report package
│   │   │   ├── PdfReportGenerator.java         # Main generator (implements ReportGenerator)
│   │   │   ├── PdfReportConfig.java            # Configuration POJO with Builder
│   │   │   ├── PdfLayoutManager.java           # Document structure management
│   │   │   ├── PdfChartGenerator.java          # Chart generation (pie/bar)
│   │   │   └── PdfStyleConstants.java          # Centralized style definitions
│   │   ├── markdown/                   # Existing, no changes
│   │   ├── model/                      # Existing, no changes
│   │   └── ReportGenerator.java        # Existing interface, no changes
│   ├── src/main/resources/
│   │   └── pdf-templates/              # NEW: PDF resources
│   │       ├── default-logo.png        # Default logo if user doesn't upload
│   │       └── fonts/                  # Embedded fonts for PDF/A compliance
│   │           ├── OpenSans-Regular.ttf
│   │           └── OpenSans-Bold.ttf
│   ├── src/test/java/com/github/sonarqube/report/
│   │   ├── pdf/                        # NEW: PDF tests
│   │   │   ├── PdfReportGeneratorTest.java     # Main generator tests
│   │   │   ├── PdfChartGeneratorTest.java      # Chart generation tests
│   │   │   ├── PdfLayoutManagerTest.java       # Layout tests
│   │   │   └── PdfReportIntegrationTest.java   # End-to-end PDF generation test
│   │   └── markdown/                   # Existing, no changes
│   └── pom.xml                         # MODIFIED: Add iText 7, JFreeChart, PDFBox dependencies
```

## Integration Guidelines

- **File Naming:** Follow existing convention - `Pdf` prefix for all PDF-related classes (e.g., `PdfReportGenerator`, `PdfLayoutManager`)
- **Folder Organization:** All PDF code isolated in `report/pdf/` package, mirrors existing `report/markdown/` structure
- **Import/Export Patterns:** Use existing `ReportGenerator` interface, no new imports in existing files

---
