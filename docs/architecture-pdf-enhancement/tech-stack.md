# Tech Stack

## Existing Technology Stack

| Category | Current Technology | Version | Usage in Enhancement | Notes |
|----------|-------------------|---------|---------------------|-------|
| Language | Java | 11+ | All PDF generator code | No change required |
| Build Tool | Maven | 3.8+ | Build and package PDF module | Add iText dependency |
| Framework | SonarQube Plugin API | 9.9+ | Plugin integration | No change required |
| Data Processing | Jackson | 2.x | Not used in PDF gen | Existing for JSON reports |
| Caching | Caffeine Cache | 3.x | Chart image caching | Reuse for PDF charts |
| Testing | JUnit 5 + Mockito | Latest | Unit & integration tests | No change required |

## New Technology Additions

| Technology | Version | Purpose | Rationale | Integration Method |
|-----------|---------|---------|-----------|-------------------|
| iText 7 | 7.2.5+ | PDF generation engine | Industry-standard PDF library with comprehensive features (charts, bookmarks, PDF/A compliance) | Maven dependency in report-generator pom.xml |
| JFreeChart or XChart | Latest stable | Chart generation (pie/bar) | Generate PNG images for embedding in PDF | Maven dependency, images converted to iText Image objects |
| Apache PDFBox | Latest stable | PDF validation in tests | Verify PDF structure in integration tests | Maven test dependency |

**iText 7 License Note:**
- Dual license: AGPL 3.0 (open source) or Commercial License
- AGPL requires source code disclosure if distributing modified versions
- **Recommendation**: Document license clearly in README and provide commercial license purchase guidance for enterprise customers

---
