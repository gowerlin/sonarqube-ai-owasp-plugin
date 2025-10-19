# Technical Constraints and Integration Requirements

## Existing Technology Stack

**Languages**: Java 11+
**Frameworks**: SonarQube Plugin API 9.9+, Maven 3.8+
**Database**: SonarQube 內建資料庫（H2/PostgreSQL/MySQL，由 SonarQube 管理）
**Infrastructure**: SonarQube Server Environment
**External Dependencies**:
- Jackson 2.x (JSON processing)
- OkHttp 4.x (AI API calls)
- Caffeine Cache 3.x (AI response caching)
- JUnit 5 + Mockito (testing)
- **新增**: iText 7.x (PDF generation) - AGPL/Commercial dual-license

## Integration Approach

**Database Integration Strategy**: 無需新增資料表。PDF 配置（Logo 路徑、標題、色彩主題）將存儲於 SonarQube 的 Settings API（使用 `Configuration` API），與現有的 AI API 金鑰存儲方式一致。

**API Integration Strategy**: PDF 報表生成器將實作 `ReportGenerator` 介面，並在 `report-generator` 模組中註冊。SonarQube Sensor 掃描完成後，根據使用者選擇的格式（Markdown 或 PDF）呼叫對應的生成器。

**Frontend Integration Strategy**: 使用 SonarQube 的 Web API 擴展點（`extension` API）在現有的報表頁面新增 PDF 格式選項。UI 將使用 SonarQube React 組件庫建構，確保與原生 UI 一致。

**Testing Integration Strategy**:
- 單元測試：使用 JUnit 5 測試 PDF 生成邏輯（圖表生成、排版、分頁）
- 整合測試：使用真實的 `AnalysisReport` 資料生成 PDF，並使用 Apache PDFBox 驗證 PDF 結構和內容
- 視覺測試：手動驗證 PDF 在不同閱讀器的顯示效果

## Code Organization and Standards

**File Structure Approach**:
```
report-generator/
├── src/main/java/com/github/sonarqube/report/
│   ├── pdf/
│   │   ├── PdfReportGenerator.java          (主要生成器)
│   │   ├── PdfReportConfig.java             (配置類別)
│   │   ├── PdfChartGenerator.java           (圖表生成)
│   │   ├── PdfLayoutManager.java            (排版管理)
│   │   └── PdfStyleConstants.java           (樣式常數)
│   └── ReportGenerator.java                 (既有介面)
├── src/main/resources/
│   └── pdf-templates/
│       ├── default-logo.png                 (預設 Logo)
│       └── fonts/                           (嵌入字型)
└── src/test/java/com/github/sonarqube/report/pdf/
    └── PdfReportGeneratorTest.java
```

**Naming Conventions**: 遵循現有的 Java 命名慣例（PascalCase for classes, camelCase for methods/variables）。PDF 相關類別統一使用 `Pdf` 前綴（如 `PdfReportGenerator`）。

**Coding Standards**:
- 遵循 SonarQube 插件開發指南
- 使用 Builder Pattern 建構複雜的 PDF 元素
- 所有公開方法必須有 Javadoc 註釋
- 使用 `@NotNull` 和 `@Nullable` 註解標示參數

**Documentation Standards**:
- Javadoc 必須包含：方法用途、參數說明、回傳值、可能的異常
- README.md 必須新增 PDF 報表配置章節
- 提供 PDF 報表範例（screenshot 和實際 PDF 檔案）

## Deployment and Operations

**Build Process Integration**:
- 在 `report-generator/pom.xml` 中新增 iText 7 相依性
- Maven 編譯時自動下載並包含 iText 7 JAR
- 使用 Maven Assembly Plugin 將所有相依性打包進最終的插件 JAR

**Deployment Strategy**:
- PDF 報表生成器隨插件一起部署，無需額外安裝步驟
- 首次啟用時，SonarQube 管理員需在配置頁面上傳公司 Logo（可選）
- Logo 檔案存儲於 SonarQube 的 `data/` 目錄下

**Monitoring and Logging**:
- 使用 SonarQube 標準的 `Loggers.get(PdfReportGenerator.class)` 記錄日誌
- 記錄 PDF 生成開始/完成時間、檔案大小、錯誤訊息
- 不記錄 PDF 內容或敏感資訊

**Configuration Management**:
- PDF 配置透過 SonarQube Settings API 管理
- 配置變更即時生效，無需重啟 SonarQube
- 提供預設配置，確保無配置時也能生成基本 PDF

## Risk Assessment and Mitigation

**Technical Risks**:
- **風險 1**: iText 7 的 AGPL 授權可能與企業客戶的授權政策衝突
  - **緩解策略**: 在文件中明確說明 AGPL 授權，並提供商業授權購買指引。考慮在未來版本提供 OpenPDF（Apache 2.0）作為替代選項。

- **風險 2**: PDF 生成可能消耗大量記憶體，導致 SonarQube 伺服器 OOM
  - **緩解策略**: 使用 iText 的串流寫入模式（`PdfWriter`），避免在記憶體中建構完整 PDF。針對大型報表（>500 個問題）實作分頁和記憶體限制。

**Integration Risks**:
- **風險 3**: iText 7 相依性可能與 SonarQube 或其他插件的相依性衝突
  - **緩解策略**: 使用 Maven Shade Plugin 將 iText 7 重新打包（relocate）至獨立的命名空間，避免類別載入衝突。

**Deployment Risks**:
- **風險 4**: PDF 生成失敗時可能導致整個報表匯出流程失敗
  - **緩解策略**: 實作降級機制，PDF 生成失敗時自動退回至 Markdown 格式，並記錄錯誤日誌供管理員診斷。

**Mitigation Strategies**:
- 完整的單元測試和整合測試覆蓋率 ≥ 80%
- 提供詳細的錯誤訊息和故障排除文件
- 在 Beta 階段進行負載測試，驗證大型報表的效能
- 與 iText 官方保持聯繫，獲取技術支援和授權諮詢

---
