# Coding Standards

## Existing Standards Compliance

**Code Style:** Google Java Style Guide (inferred from existing code)
**Linting Rules:** SonarQube internal rules (plugin follows its own standards)
**Testing Patterns:** JUnit 5 with Mockito, Builder Pattern for test data
**Documentation Style:** Javadoc for all public classes/methods, inline comments for complex logic

## Enhancement-Specific Standards

- **PDF Class Naming:** All PDF-related classes use `Pdf` prefix (e.g., `PdfReportGenerator`, `PdfChartGenerator`)
- **iText API Usage:** Use iText 7 modern API (`Document`, `PdfWriter`), avoid deprecated `PdfDocument` legacy API
- **Error Handling:** All iText exceptions (`PdfException`, `IOException`) must be caught and wrapped in `ReportGenerationException` with meaningful messages
- **Resource Management:** Use try-with-resources for all iText `Document` and `PdfWriter` instances
- **Logging:** Use SonarQube `Loggers.get(PdfReportGenerator.class)`, log generation start/end/errors
- **Performance:** Use Caffeine Cache for chart images, avoid in-memory PDF construction (use streaming)

## Critical Integration Rules

- **Existing API Compatibility:** MUST implement `ReportGenerator` interface without modifications
- **Database Integration:** MUST use SonarQube Settings API for configuration, NO direct database access
- **Error Handling:** PDF generation errors MUST NOT break Markdown report generation (isolated failure)
- **Logging Consistency:** Use same logging levels and patterns as `MarkdownReportGenerator`

---
