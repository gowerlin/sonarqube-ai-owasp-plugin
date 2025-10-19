# Requirements

## Functional

**FR1**: PDF 報表生成器必須實作 `ReportGenerator` 介面，接受 `AnalysisReport` 資料模型作為輸入，並輸出符合 PDF/A 標準的 PDF 檔案。

**FR2**: PDF 報表必須包含專業封面頁，顯示：專案名稱、OWASP 版本、分析時間、公司 Logo（可配置）、報表標題「Security Analysis Report」。

**FR3**: PDF 報表必須包含可導航的目錄（Table of Contents），支援點擊跳轉至：執行摘要、嚴重性分布、OWASP 分類分布、詳細發現（依嚴重性分組）。

**FR4**: PDF 報表必須包含執行摘要頁面，以表格形式顯示：總發現數、BLOCKER 數量、CRITICAL 數量、MAJOR 數量、分析檔案數（若有）。

**FR5**: PDF 報表必須包含嚴重性分布視覺化圖表（圓餅圖或長條圖），顯示各嚴重性等級（BLOCKER、CRITICAL、MAJOR、MINOR、INFO）的問題數量與百分比。

**FR6**: PDF 報表必須包含 OWASP 分類分布視覺化圖表（長條圖），顯示各 OWASP 類別（A01-A10）的問題數量，依數量降序排列。

**FR7**: PDF 報表必須包含詳細發現章節，依嚴重性分組（BLOCKER → CRITICAL → MAJOR → MINOR → INFO），每個安全問題顯示：規則名稱、檔案路徑、行號（若有）、OWASP 分類、CWE ID、問題描述、代碼片段（若有）、修復建議。

**FR8**: PDF 報表必須在每頁頁首顯示：公司 Logo（左上）、報表標題「OWASP Security Analysis Report」（居中）、專案名稱（右上）。

**FR9**: PDF 報表必須在每頁頁尾顯示：生成時間（左下）、頁碼「Page X of Y」（居中）、OWASP 版本（右下）。

**FR10**: PDF 報表的代碼片段必須使用等寬字體（Courier New 或類似）並保持縮排格式，背景色使用淺灰色區塊以提升可讀性。

**FR11**: PDF 報表必須支援顏色標示，嚴重性圖示使用對應顏色：BLOCKER 紅色、CRITICAL 橙色、MAJOR 黃色、MINOR 藍色、INFO 綠色。

**FR12**: PDF 報表生成器必須提供配置選項，允許用戶設定：公司 Logo 路徑、報表標題、頁首頁尾樣式、色彩主題。

**FR13**: PDF 報表必須包含備註頁面，顯示：報表生成時間、OWASP 版本、AI 模型（若有）、插件版本、免責聲明。

**FR14**: PDF 報表生成器必須處理大型報表（>1000 個安全問題）時使用分頁機制，避免單頁內容過長導致渲染問題。

**FR15**: PDF 報表必須提供檔案命名規則：`{project-name}_OWASP_{version}_Security_Report_{timestamp}.pdf`，例如 `MyApp_OWASP_2021_Security_Report_20251020-143022.pdf`。

## Non Functional

**NFR1**: PDF 報表生成時間必須 < 15 秒（針對包含 100 個安全問題的報表），確保不顯著延緩報表匯出流程。

**NFR2**: PDF 報表檔案大小必須 < 5 MB（針對 100 個安全問題），透過圖片壓縮和字型嵌入優化控制檔案大小。

**NFR3**: PDF 報表必須符合 PDF/A-1b 或 PDF/A-2b 標準，確保長期存檔相容性。

**NFR4**: PDF 報表必須可在主流 PDF 閱讀器（Adobe Acrobat Reader、Foxit Reader、瀏覽器內建閱讀器）正確顯示，無排版錯誤。

**NFR5**: 圖表生成必須支援至少 1000 個資料點，且渲染時間 < 3 秒。

**NFR6**: PDF 報表生成器必須處理異常情況（如 Logo 檔案不存在、資料為空）時提供有意義的錯誤訊息，並生成降級版本的 PDF（無 Logo、預設樣式）。

**NFR7**: iText 7 相依性的新增必須不導致 Maven 編譯失敗，且插件總大小不超過 60 MB（原 50 MB + iText 依賴）。

**NFR8**: PDF 報表生成器必須是執行緒安全的，支援並行生成多個報表而不產生競爭條件。

**NFR9**: PDF 報表的記憶體使用必須 < 500 MB（針對包含 1000 個安全問題的報表），透過串流寫入優化記憶體占用。

**NFR10**: PDF 報表生成器的單元測試覆蓋率必須 ≥ 80%，確保核心邏輯的正確性。

## Compatibility Requirements

**CR1**: PDF 報表生成器必須與現有的 `AnalysisReport`、`SecurityFinding`、`ReportSummary` 資料模型完全相容，不得修改現有欄位或新增強制欄位。

**CR2**: PDF 報表生成器必須實作 `ReportGenerator` 介面的 `generate(AnalysisReport report)` 方法和 `getFormat()` 方法（回傳 "pdf"），確保與現有報表生成架構相容。

**CR3**: PDF 報表生成器的新增不得影響現有 Markdown 報表生成器的功能，兩者必須能獨立運作並產生內容一致的報表。

**CR4**: PDF 報表生成器必須與 SonarQube Plugin API 9.9+ 相容，不使用任何已廢棄的 API，確保未來 SonarQube 版本升級時的相容性。

**CR5**: iText 7 的授權模式（AGPL）必須在插件文件中明確說明，並提供商業授權選項的指引，避免企業客戶的授權合規風險。

---
