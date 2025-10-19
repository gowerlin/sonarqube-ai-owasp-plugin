# Testing Strategy

## Integration with Existing Tests

**Existing Test Framework:** JUnit 5 with Mockito
**Test Organization:** Tests mirror source structure (`src/test/java` mirrors `src/main/java`)
**Coverage Requirements:** ≥80% line coverage (enforced via JaCoCo)

## New Testing Requirements

### Unit Tests for New Components

- **Framework:** JUnit 5 with Mockito
- **Location:** `report-generator/src/test/java/com/github/sonarqube/report/pdf/`
- **Coverage Target:** ≥80% line coverage, ≥90% for `PdfReportGenerator` main class
- **Integration with Existing:** Reuse test data builders from `MarkdownReportGeneratorTest`

**Test Classes:**
1. `PdfReportGeneratorTest` - Main generator logic, configuration handling, error scenarios
2. `PdfChartGeneratorTest` - Chart generation, caching, color themes
3. `PdfLayoutManagerTest` - Document structure, page breaks, headers/footers
4. `PdfReportConfigTest` - Builder pattern, validation, defaults

### Integration Tests

- **Scope:** End-to-end PDF generation from `AnalysisReport` → PDF file
- **Existing System Verification:** Run existing Markdown tests to ensure no regression
- **New Feature Testing:** Generate PDF from real `AnalysisReport` data, validate with Apache PDFBox

**PdfReportIntegrationTest**:
```java
@Test
void testGeneratePdfReportWithRealData() {
    // Given: Real AnalysisReport with 50 security findings
    AnalysisReport report = createTestReport(50);

    // When: Generate PDF
    String pdfPath = pdfGenerator.generate(report);

    // Then: PDF file exists and is valid
    assertTrue(Files.exists(Paths.get(pdfPath)));

    // And: PDF structure is correct (using PDFBox)
    try (PDDocument doc = PDDocument.load(new File(pdfPath))) {
        assertEquals(expectedPageCount, doc.getNumberOfPages());
        assertTrue(hasBookmarks(doc));
        assertTrue(isPdfA compliant(doc));
    }
}
```

### Regression Testing

- **Existing Feature Verification:** Run full test suite including `MarkdownReportGeneratorTest` to ensure no impact
- **Automated Regression Suite:** Add PDF tests to Maven test phase (`mvn test`)
- **Manual Testing Requirements:**
  - Open generated PDF in Adobe Reader, Foxit, Chrome
  - Verify charts render correctly
  - Test print functionality
  - Verify with large reports (1000 issues)

---
