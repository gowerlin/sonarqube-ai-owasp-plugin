# Infrastructure and Deployment Integration

## Existing Infrastructure

**Current Deployment:** SonarQube plugin JAR deployed to `$SONARQUBE_HOME/extensions/plugins/`
**Infrastructure Tools:** Maven 3.8+ for build, no CI/CD currently configured
**Environments:** Development (local SonarQube), Production (customer SonarQube installations)

## Enhancement Deployment Strategy

**Deployment Approach:**
- PDF generator bundled in existing plugin JAR (no separate deployment needed)
- Maven Shade Plugin (if needed) to relocate iText classes and avoid conflicts
- Logo files stored in `<sonarqube-data>/owasp-plugin/` directory (auto-created on first use)

**Infrastructure Changes:**
- **Maven POM**: Add iText 7, JFreeChart/XChart, Apache PDFBox (test) dependencies
- **Plugin JAR Size**: Increase from ~50 MB to ~60 MB (acceptable per NFR7)
- **File System**: PDF reports generated in SonarQube temp directory, then served for download

**Pipeline Integration:**
- No CI/CD changes required (same build process: `mvn clean package`)
- Consider adding PDF generation performance test in future CI pipeline

## Rollback Strategy

**Rollback Method:**
- PDF feature is additive and isolated - can be disabled via configuration without plugin rollback
- If critical issues arise, users can switch back to Markdown format immediately
- Plugin rollback: replace JAR with previous version and restart SonarQube

**Risk Mitigation:**
- Comprehensive unit and integration tests (≥80% coverage)
- Manual testing on multiple PDF readers before release
- Beta testing phase with select customers
- Clear documentation for troubleshooting common issues

**Monitoring:**
- Log PDF generation time, file size, errors via SonarQube Loggers
- Track PDF generation success/failure rate
- Monitor memory usage during PDF generation (should be <500 MB per NFR9)

---
