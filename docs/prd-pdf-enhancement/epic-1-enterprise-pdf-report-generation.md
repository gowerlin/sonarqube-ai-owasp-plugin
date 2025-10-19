# Epic 1: Enterprise PDF Report Generation

**Epic Goal**: 在 `report-generator` 模組中實作企業級 PDF 報表生成功能，提供專業的視覺化呈現（圖表、目錄、品牌元素），並與現有的 Markdown 報表生成器並存，使用相同的資料模型確保內容一致性。此功能將滿足企業客戶的合規存檔、高階簡報和客戶交付需求。

**Integration Requirements**:
- 必須實作 `ReportGenerator` 介面，確保與現有報表架構相容
- 必須重用 `AnalysisReport`、`SecurityFinding`、`ReportSummary` 資料模型
- 不得修改現有的 Markdown 報表生成器
- 必須與 SonarQube UI 整合，提供 PDF 格式選項

---

## Story 1.1: iText 7 Integration and Basic PDF Infrastructure

**User Story**:
As a **plugin developer**,
I want **to integrate iText 7 library and establish basic PDF generation infrastructure**,
so that **I have a solid foundation for building enterprise-grade PDF reports**.

### Acceptance Criteria

1. **AC1**: `report-generator/pom.xml` 包含 iText 7 最新穩定版本（7.2.5+）的 Maven 相依性，並成功編譯無衝突。
2. **AC2**: 建立 `PdfReportGenerator` 類別，實作 `ReportGenerator` 介面，並通過基本的單元測試（驗證介面方法簽名正確）。
3. **AC3**: 實作 `PdfReportConfig` 類別，支援基本配置選項：Logo 路徑、報表標題、色彩主題（使用 Builder Pattern）。
4. **AC4**: 實作 `generate(AnalysisReport report)` 方法的基本骨架，能夠建立空白 PDF 檔案並寫入磁碟（暫無內容，僅驗證 iText API 可正常運作）。
5. **AC5**: 建立 `PdfReportGeneratorTest` 單元測試類別，驗證空白 PDF 的生成成功，並使用 Apache PDFBox 驗證 PDF 結構有效。

### Integration Verification

**IV1**: 執行 `mvn clean install` 確認編譯成功，且插件 JAR 包含 iText 7 相依性（JAR 大小增加約 10-15 MB）。

**IV2**: 驗證現有的 Markdown 報表生成器（`MarkdownReportGenerator`）功能未受影響，單元測試全數通過。

**IV3**: 檢查插件啟動時間增加 < 1 秒，符合 NFR11 要求。

---

## Story 1.2: PDF Document Structure and Layout Foundation

**User Story**:
As a **security report consumer**,
I want **PDF reports to have professional document structure including cover page, table of contents, and page headers/footers**,
so that **I can easily navigate the report and present it in formal settings**.

### Acceptance Criteria

1. **AC1**: 實作 `PdfLayoutManager` 類別，負責管理 PDF 文件結構（封面頁、目錄、內容頁、備註頁）。
2. **AC2**: 實作封面頁生成，包含：專案名稱（標題，24pt 粗體）、OWASP 版本（副標題）、分析時間（ISO 8601 格式）、公司 Logo（若有配置，置於頂部居中，最大寬度 200px）。
3. **AC3**: 實作目錄（Table of Contents）生成，包含可點擊的書籤連結：執行摘要、嚴重性分布、OWASP 分類分布、詳細發現（依嚴重性分組，如「BLOCKER Issues」）。
4. **AC4**: 實作頁首（Header）模板，每頁顯示：公司 Logo（左上，縮小版 50px）、報表標題（居中，12pt）、專案名稱（右上，10pt）。
5. **AC5**: 實作頁尾（Footer）模板，每頁顯示：生成時間（左下，8pt 灰色）、頁碼「Page X of Y」（居中，9pt）、OWASP 版本（右下，8pt）。
6. **AC6**: 使用 iText 的 `PdfPageEventHelper` 確保頁首頁尾在每頁正確渲染，並在首頁（封面）和目錄頁略過頁首頁尾。

### Integration Verification

**IV1**: 生成包含 10 個安全問題的測試 PDF，驗證封面頁、目錄、頁首頁尾正確顯示，並在 Adobe Acrobat Reader 中測試書籤導航功能。

**IV2**: 驗證 PDF 符合 PDF/A-1b 標準（使用 iText 的 `PdfADocument`），確保長期存檔相容性。

**IV3**: 測試 Logo 檔案不存在時的降級處理，確認 PDF 仍能生成（使用預設樣式，無 Logo），並記錄警告日誌。

---

## Story 1.3: Executive Summary and Statistical Tables

**User Story**:
As a **security team lead**,
I want **PDF reports to include a clear executive summary with key statistics in table format**,
so that **I can quickly understand the overall security posture without reading detailed findings**.

### Acceptance Criteria

1. **AC1**: 實作執行摘要頁面（「Executive Summary」章節），包含：章節標題（16pt 粗體）、統計表格、簡短說明文字。
2. **AC2**: 統計表格包含以下行（使用 iText `Table` API）：
   - 總發現數（Total Findings）
   - BLOCKER 數量（加紅色標籤）
   - CRITICAL 數量（加橙色標籤）
   - MAJOR 數量（加黃色標籤）
   - 分析檔案數（若有，Analyzed Files）
3. **AC3**: 表格樣式：標題行使用深灰色背景 (#333333)、白色文字，資料行使用交替的白色和淺灰色背景 (#F5F5F5) 提升可讀性。
4. **AC4**: 在統計表格下方新增簡短文字摘要（3-5 句），自動生成基於資料的描述，例如：「This project has 15 security findings, including 3 critical issues that require immediate attention.」
5. **AC5**: 確保執行摘要頁面在目錄中正確連結，點擊目錄的「Executive Summary」可跳轉至此頁。

### Integration Verification

**IV1**: 使用包含不同嚴重性組合的測試資料（如 5 BLOCKER、10 CRITICAL、20 MAJOR）生成 PDF，驗證統計數字正確無誤。

**IV2**: 驗證統計表格在不同 PDF 閱讀器（Adobe Reader、Foxit、Chrome 內建）顯示一致，無排版錯位。

**IV3**: 驗證與 Markdown 報表的執行摘要內容一致（數字相同），確保資料模型的正確性。

---

## Story 1.4: Visual Charts for Severity and Category Distribution

**User Story**:
As a **compliance officer**,
I want **PDF reports to include visual charts (pie chart for severity, bar chart for OWASP categories)**,
so that **I can quickly visualize security issue distribution for audit presentations**.

### Acceptance Criteria

1. **AC1**: 實作 `PdfChartGenerator` 類別，負責生成嵌入式圖表（使用 iText 的 `Image` API）。
2. **AC2**: 實作嚴重性分布圓餅圖，包含：
   - 各嚴重性等級（BLOCKER、CRITICAL、MAJOR、MINOR、INFO）的扇形區塊
   - 每個區塊顯示百分比（例如「BLOCKER 20%」）
   - 使用對應顏色：BLOCKER 紅色 (#D4333F)、CRITICAL 橙色 (#FFA500)、MAJOR 黃色 (#FFD700)、MINOR 藍色 (#4B9FD5)、INFO 綠色 (#00AA00)
   - 圖表尺寸 400x300px，置於「Severity Distribution」章節
3. **AC3**: 實作 OWASP 分類分布長條圖，包含：
   - X 軸顯示 OWASP 類別（A01-A10），Y 軸顯示問題數量
   - 長條依數量降序排列（最高的在左邊）
   - 每個長條頂部顯示數字（例如「15」）
   - 長條顏色使用漸層藍色（深 #003F7F 到淺 #4B9FD5）
   - 圖表尺寸 600x400px，置於「OWASP Category Distribution」章節
4. **AC4**: 使用 JFreeChart 或 XChart 函式庫生成圖表圖片（PNG 格式），然後使用 iText 嵌入 PDF。
5. **AC5**: 實作圖表快取機制，相同資料的圖表僅生成一次，避免重複計算（使用 Caffeine Cache）。

### Integration Verification

**IV1**: 生成包含 50 個安全問題（涵蓋所有嚴重性和多個 OWASP 類別）的測試 PDF，驗證圖表顯示正確、顏色鮮明、標籤清晰。

**IV2**: 測試極端情況：
   - 僅有一個嚴重性等級時，圓餅圖顯示 100% 單一扇形
   - OWASP 類別超過 10 個時（預測 2025 版本），長條圖正確顯示所有類別

**IV3**: 驗證圖表生成時間 < 3 秒（NFR5），使用包含 1000 個資料點的測試資料。

---

## Story 1.5: Detailed Findings Section with Code Snippets

**User Story**:
As a **developer**,
I want **PDF reports to include detailed security findings with code snippets, file paths, and fix suggestions**,
so that **I can understand the issues and apply fixes directly without switching tools**.

### Acceptance Criteria

1. **AC1**: 實作詳細發現章節（「Detailed Findings」），依嚴重性分組（BLOCKER → CRITICAL → MAJOR → MINOR → INFO），每組使用獨立的子章節標題（例如「BLOCKER Issues (3)」）。
2. **AC2**: 每個安全問題顯示為獨立的區塊，包含：
   - 編號（例如「1.」、「2.」）
   - 規則名稱（14pt 粗體）
   - 檔案路徑與行號（例如「`src/main/java/Auth.java:45`」，使用等寬字體）
   - OWASP 分類（例如「A03:2021 - Injection」）
   - CWE ID（例如「CWE-89」）
   - 問題描述（12pt 正常字體，支援多行文字）
3. **AC3**: 代碼片段（Code Snippet）顯示，若有：
   - 使用等寬字體（Courier New，10pt）
   - 淺灰色背景區塊 (#F5F5F5)
   - 保持原始縮排和換行
   - 左右各留 10px 間距
4. **AC4**: 修復建議（Fix Suggestion）顯示，若有：
   - 使用「💡 Fix Suggestion」標題（加燈泡 emoji）
   - 12pt 正常字體
   - 淺黃色背景區塊 (#FFFBCC)
5. **AC5**: 實作分頁邏輯，單一安全問題內容過長時自動分頁，確保代碼片段不被切斷（使用 iText 的 `KeepTogether` 屬性）。

### Integration Verification

**IV1**: 生成包含長代碼片段（>50 行）和長修復建議（>200 字）的測試 PDF，驗證分頁正確、代碼片段不被切斷。

**IV2**: 驗證代碼片段的縮排格式與原始檔案一致（使用空白和 Tab 的混合縮排）。

**IV3**: 對比 Markdown 報表和 PDF 報表的詳細發現內容，確認資訊完整一致（無遺漏欄位）。

---

## Story 1.6: Configuration UI and SonarQube Integration

**User Story**:
As a **SonarQube administrator**,
I want **a configuration panel in SonarQube to customize PDF report appearance (logo, title, colors)**,
so that **I can brand the reports with our company identity**.

### Acceptance Criteria

1. **AC1**: 在 SonarQube 的 **Administration → Configuration → OWASP AI Plugin** 頁面新增「PDF Report Settings」區塊（使用 SonarQube Extension API）。
2. **AC2**: 配置選項包含：
   - **Company Logo Upload**：檔案上傳器，支援 PNG/JPG，最大 500KB，上傳後顯示預覽縮圖
   - **Report Title**：文字輸入框，預設「OWASP Security Analysis Report」，最大 100 字元
   - **Color Theme**：下拉選單，選項：Default（藍色系）、Dark（深色系）、Light（淺色系）
   - **Enable Header/Footer**：切換按鈕，預設啟用
3. **AC3**: 實作配置的儲存與載入邏輯，使用 SonarQube Settings API（`Configuration.get()` / `Configuration.set()`），Logo 檔案儲存於 `<sonarqube-data>/owasp-plugin/logo.png`。
4. **AC4**: 在報表匯出頁面（Project → OWASP Security Report → Export）新增格式選擇器，選項：「Markdown」、「PDF」（使用單選按鈕）。
5. **AC5**: 實作 PDF 匯出 API 端點（`/api/owasp/report/export?format=pdf&project=<key>`），呼叫時觸發 PDF 生成並回傳檔案下載。
6. **AC6**: 新增 UI 驗證：Logo 檔案大小超過 500KB 時顯示錯誤訊息「Logo file size must be less than 500KB」，不允許上傳。

### Integration Verification

**IV1**: 在 SonarQube 中上傳自訂 Logo、修改報表標題為「ABC Corporation Security Report」、選擇 Dark 色彩主題，然後匯出 PDF 驗證配置生效。

**IV2**: 測試無 Logo 配置時，PDF 使用預設樣式（無 Logo，標準標題），確認不會崩潰。

**IV3**: 驗證配置儲存後重啟 SonarQube，配置仍保留（持久化正確）。

---

## Story 1.7: Error Handling, Logging, and Performance Optimization

**User Story**:
As a **DevOps engineer**,
I want **PDF generation to have robust error handling, detailed logging, and optimized performance**,
so that **I can troubleshoot issues quickly and ensure reports are generated within acceptable time**.

### Acceptance Criteria

1. **AC1**: 實作全面的異常處理，包含：
   - iText API 異常（`PdfException`）→ 記錄錯誤日誌並回傳友善錯誤訊息給使用者
   - Logo 檔案不存在 → 記錄警告日誌，使用降級樣式（無 Logo）
   - 資料為空（`AnalysisReport` 無安全問題）→ 生成包含「No security issues found」訊息的 PDF
   - 記憶體不足 → 記錄錯誤並建議減少報表大小或增加 JVM 記憶體
2. **AC2**: 實作詳細的日誌記錄（使用 SonarQube `Loggers`），包含：
   - INFO 級別：PDF 生成開始/完成、檔案大小、生成時間
   - WARN 級別：降級處理（如 Logo 不存在）、異常但可恢復的情況
   - ERROR 級別：PDF 生成失敗、嚴重異常
   - 不記錄敏感資訊（如 PDF 內容、檔案路徑的完整絕對路徑）
3. **AC3**: 實作效能優化：
   - 使用 iText 的串流寫入模式（`PdfWriter`），避免完整 PDF 在記憶體中建構
   - 對大型報表（>500 個安全問題）實作分頁和記憶體限制
   - 圖表生成使用快取（Caffeine Cache），相同資料的圖表僅生成一次
4. **AC4**: 實作超時控制，單一 PDF 生成超過 60 秒時中斷並記錄錯誤（使用 `Future` 和 `ExecutorService`）。
5. **AC5**: 新增效能指標記錄：PDF 檔案大小、生成時間、記憶體使用（記錄於 INFO 日誌），供後續優化參考。

### Integration Verification

**IV1**: 模擬異常情況（刪除 Logo 檔案、提供空的 `AnalysisReport`），驗證 PDF 仍能生成且錯誤訊息清晰。

**IV2**: 生成包含 1000 個安全問題的大型 PDF，驗證：
   - 生成時間 < 15 秒（NFR1）
   - 記憶體使用 < 500 MB（NFR9）
   - PDF 檔案大小 < 50 MB（合理範圍內）

**IV3**: 檢查日誌輸出，確認包含關鍵資訊（生成時間、檔案大小）且無敏感資訊洩漏。

---

## Story 1.8: Comprehensive Testing and Documentation

**User Story**:
As a **QA engineer and future maintainer**,
I want **comprehensive test coverage (unit + integration tests) and clear documentation**,
so that **I can verify PDF functionality works correctly and understand how to maintain it**.

### Acceptance Criteria

1. **AC1**: 單元測試覆蓋率 ≥ 80%（使用 JaCoCo 測試覆蓋率工具驗證），涵蓋：
   - `PdfReportGenerator` 核心邏輯
   - `PdfChartGenerator` 圖表生成
   - `PdfLayoutManager` 排版邏輯
   - `PdfReportConfig` 配置管理
2. **AC2**: 整合測試包含：
   - 使用真實的 `AnalysisReport` 資料（從 Markdown 報表測試中重用）生成 PDF
   - 使用 Apache PDFBox 驗證 PDF 結構：頁數正確、書籤存在、文字可搜尋
   - 測試極端情況：0 個問題、1000 個問題、多種嚴重性組合
3. **AC3**: 視覺測試（手動）：
   - 在 Adobe Acrobat Reader、Foxit Reader、Chrome 內建閱讀器開啟測試 PDF
   - 驗證排版正確、圖表清晰、顏色鮮明、書籤導航功能正常
   - 驗證列印效果（列印至 PDF 或實體印表機）
4. **AC4**: 更新 README.md，新增「PDF Report Configuration」章節，包含：
   - 如何在 SonarQube 配置 PDF 報表（截圖說明）
   - Logo 上傳指引（支援格式、大小限制）
   - 色彩主題說明（預覽圖）
   - 故障排除常見問題（FAQ）
5. **AC5**: 新增 Javadoc 註釋至所有公開類別和方法，確保 IDE 自動完成時顯示清晰說明。
6. **AC6**: 提供 PDF 報表範例檔案（`docs/examples/sample-report.pdf`），供使用者參考最終輸出效果。

### Integration Verification

**IV1**: 執行 `mvn test` 確認所有單元測試和整合測試通過，無失敗案例。

**IV2**: 執行 `mvn jacoco:report` 驗證測試覆蓋率 ≥ 80%，關鍵類別（如 `PdfReportGenerator`）覆蓋率 ≥ 90%。

**IV3**: 在乾淨的 SonarQube 安裝環境中部署插件，按照 README 指引配置 PDF 報表，驗證文件的正確性和完整性。

---
