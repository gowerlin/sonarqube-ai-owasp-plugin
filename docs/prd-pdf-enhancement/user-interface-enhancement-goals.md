# User Interface Enhancement Goals

## Integration with Existing UI

PDF 報表生成功能將整合至 SonarQube 現有的報表匯出介面：

1. **報表匯出頁面擴展**：在現有的報表格式選擇器（目前僅 Markdown）中新增「PDF」選項，使用單選按鈕或下拉選單。
2. **配置頁面擴展**：在 SonarQube 的 **Administration → Configuration → OWASP AI Plugin** 頁面新增「PDF Report Settings」區塊，包含：
   - 公司 Logo 上傳（檔案選擇器，支援 PNG/JPG，最大 500KB）
   - 報表標題自訂（文字輸入框，預設「OWASP Security Analysis Report」）
   - 色彩主題選擇（下拉選單：Default / Dark / Light）
   - 頁首頁尾開關（切換按鈕）
3. **UI 一致性**：所有新增的 UI 元件必須遵循 SonarQube 的設計系統（使用 SonarQube React 組件庫），確保視覺和互動模式一致。

## Modified/New Screens and Views

### 新增視圖
- **PDF Report Configuration Panel**（新增）
  - 位置：Administration → Configuration → OWASP AI Plugin → PDF Report Settings
  - 功能：配置 PDF 報表的外觀和品牌元素

### 修改視圖
- **Report Export Screen**（修改）
  - 位置：Project → OWASP Security Report → Export
  - 修改內容：新增「PDF」格式選項至格式選擇器
  - 新增：PDF 預覽縮圖（可選，顯示首頁預覽）

## UI Consistency Requirements

**UID1**: 所有新增的 UI 元件必須使用 SonarQube 標準色彩（主色 #4B9FD5、成功綠 #00AA00、警告橙 #FFA500、錯誤紅 #D4333F）。

**UID2**: 所有表單輸入必須提供即時驗證（如 Logo 檔案大小檢查、標題長度限制），並顯示清晰的錯誤訊息。

**UID3**: PDF 報表配置的儲存必須觸發 SonarQube 標準的成功通知（綠色 Toast 訊息「PDF report settings saved successfully」）。

**UID4**: PDF 報表匯出必須顯示進度指示器（進度條或載入動畫），並在完成後提供下載連結。

---
