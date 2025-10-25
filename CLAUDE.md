# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

## 專案概述

**SonarQube AI OWASP Security Plugin** - AI 驅動的 OWASP 安全分析插件，支援多版本 OWASP Top 10（2017、2021、2025），提供智能修復建議與並行分析。

**技術棧**:
- Java 17
- SonarQube Plugin API 9.17.0
- Maven 多模組專案
- AI Integration: OpenAI GPT-4, Anthropic Claude, Google Gemini (API + CLI 雙模式)

---

## 模組架構

專案採用 **Maven 多模組架構**，按照依賴順序排列（從底層到頂層）：

### 1. `shared-utils` - 共用工具模組
**職責**: 基礎工具類，被所有其他模組依賴
**關鍵類別**:
- `FileUtils`: 檔案操作工具（讀取、hash 計算）
- `SecurityUtils`: 安全工具（API 金鑰加密/解密）
- `StringUtils`: 字串處理工具
- `JsonUtils`: JSON 序列化/反序列化

**依賴**: 無（底層模組）

### 2. `version-manager` - OWASP 版本管理模組
**職責**: 管理 OWASP Top 10 多版本（2017、2021、2025）及版本間映射
**關鍵類別**:
- `OwaspVersion`: OWASP 版本枚舉（V2017, V2021, V2025）
- `OwaspVersionManager`: 版本管理服務，處理版本切換與映射

**依賴**: `shared-utils`

### 3. `config-manager` - 配置管理模組
**職責**: 插件配置管理，包含 AI 設定、掃描參數、報告選項
**關鍵類別**:
- `AiOwaspPluginSettings`: 全域配置管理（讀取/儲存配置）

**依賴**: `shared-utils`

### 4. `ai-connector` - AI 整合模組
**職責**: AI Provider 整合，支援 6 種 AI Provider（3 API + 3 CLI）

**雙模式架構**:
```
AiServiceFactory
├── API 模式（HTTP API 呼叫）
│   ├── OpenAiService (GPT-4)
│   ├── ClaudeService (Claude 3)
│   └── GeminiApiService (Gemini Pro)
└── CLI 模式（本地 CLI 工具）
    ├── GeminiCliService (gemini cli)
    ├── CopilotCliService (github-copilot-cli)
    └── ClaudeCliService (claude cli)
```

**關鍵類別**:
- `AiServiceFactory`: AI Provider 工廠，根據配置建立對應服務
- `AiService`: 統一抽象介面（analyze() 方法）
- `AiCacheManager`: Caffeine Cache 快取管理（TTL 1 小時，LRU eviction）
- `CliExecutor`: CLI 執行器（ProcessCliExecutor 實作 Runtime.exec()）
- `AiResponseParser`: AI 回應解析器（JSON + Regex fallback）

**依賴**: `shared-utils`, `config-manager`

### 5. `rules-engine` - 規則引擎模組
**職責**: OWASP 規則定義、CWE 映射、漏洞檢測邏輯
**關鍵類別**:
- `OwaspRulesRepository`: 規則儲存庫（所有 OWASP 規則定義）
- `RuleMapper`: CWE ↔ OWASP 類別映射
- `SecurityIssueDetector`: 漏洞檢測器

**依賴**: `shared-utils`, `version-manager`

### 6. `report-generator` - 報告生成模組
**職責**: 生成 4 種格式報告（PDF、HTML、JSON、Markdown）

**報告生成器**:
```
ReportGenerator (介面)
├── PdfReportGenerator (iText 7)
│   ├── PdfLayoutManager (封面、目錄、頁首頁尾)
│   └── PdfChartGenerator (圖表生成 + Caffeine Cache)
├── HtmlReportGenerator (Thymeleaf 模板引擎)
├── JsonReportGenerator (手動 JSON 生成)
└── MarkdownReportGenerator (CommonMark 格式)
```

**關鍵類別**:
- `PdfReportGenerator`: PDF 報告生成器（使用 iText 7.2.5）
  - ⚠️ **重要**: 快取 `byte[]` PNG 資料，不快取 `Image` 物件（避免跨文檔物件重用錯誤）
- `PdfChartGenerator`: XChart 圖表生成器（嚴重性圓餅圖、OWASP 長條圖）
- `HtmlReportGenerator`: HTML 報告生成器（響應式設計 + Chart.js）

**依賴**: `shared-utils`, `version-manager`, `rules-engine`

### 7. `plugin-core` - SonarQube 插件核心
**職責**: SonarQube 插件入口，整合所有模組，提供 Web UI 與 API

**核心元件**:
```
AiOwaspPlugin (插件入口)
├── OwaspSensor (掃描器)
│   └── 並行分析邏輯（ExecutorService）
├── PdfReportApiController (報告匯出 API)
│   └── /api/owasp/report/export
├── OwaspReportPageDefinition (Web UI 頁面)
└── SonarQubeDataService (WsClient API)
    └── 查詢 SonarQube 問題數據
```

**關鍵類別**:
- `AiOwaspPlugin`: 插件入口點，註冊所有元件
- `OwaspSensor`: 掃描器，執行代碼分析
- `PdfReportApiController`: 報告匯出 API 控制器
- `SonarQubeDataService`: SonarQube 數據查詢服務（使用 WsClient）

**依賴**: 所有其他模組

---

## 編譯與測試

### 完整編譯
```bash
cd "D:/ForgejoGit/Security_Plugin_for_SonarQube"
mvn clean package -Dmaven.test.skip=true
```

**輸出**: `plugin-core/target/sonar-aiowasp-plugin-1.0.0-SNAPSHOT.jar` (~33MB)

### 執行測試
```bash
# 執行所有測試
mvn test

# 執行單一測試類別
mvn test -Dtest=PdfReportGeneratorTest

# 執行單一測試方法
mvn test -Dtest=PdfReportGeneratorTest#testGeneratePdfReport
```

### 程式碼品質檢查
```bash
# 執行 JaCoCo 測試覆蓋率報告
mvn jacoco:report

# 查看覆蓋率報告
open target/site/jacoco/index.html
```

---

## SonarQube 測試環境

### 環境資訊
- **SonarQube 目錄**: `E:\sonarqube-community-25.10.0.114319`
- **版本**: SonarQube Community 25.10.0
- **測試帳密**: `admin` / `P@ssw0rd`
- **測試專案**: `NCCS2.CallCenterWeb.backend` (67 個安全問題)

### 完整部署流程（一鍵執行）

```bash
# Step 1: 編譯
cd "D:/ForgejoGit/Security_Plugin_for_SonarQube"
mvn clean package -Dmaven.test.skip=true

# Step 2: 停止 SonarQube
taskkill //F //IM java.exe 2>/dev/null
sleep 3

# Step 3: 部署插件
cp "plugin-core/target/sonar-aiowasp-plugin-1.0.0-SNAPSHOT.jar" \
   "E:/sonarqube-community-25.10.0.114319/extensions/plugins/"

# Step 4: 清理緩存與日誌
rm -rf "E:/sonarqube-community-25.10.0.114319/data/web/deploy/"
rm "E:/sonarqube-community-25.10.0.114319/logs/"*.log

# Step 5: 啟動 SonarQube
cd "E:/sonarqube-community-25.10.0.114319/bin/windows-x86-64"
./StartSonar.bat &

# Step 6: 等待啟動（60-90 秒）
sleep 70
tail -10 "E:/sonarqube-community-25.10.0.114319/logs/sonar.log"
```

### 驗證部署成功

```bash
# 1. 檢查插件載入
grep "AI OWASP Security Plugin 載入完成" \
     "E:/sonarqube-community-25.10.0.114319/logs/web.log" | tail -1

# 2. 測試報告 API
curl -u admin:P@ssw0rd \
     "http://localhost:9000/api/owasp/report/export?format=json&project=NCCS2.CallCenterWeb.backend" \
     -s | jq '.summary.totalFindings'

# 3. 檢查錯誤
tail -50 "E:/sonarqube-community-25.10.0.114319/logs/web.log" | grep -i error
```

---

## 關鍵架構決策

### 1. sonar-ws 依賴打包策略
**問題**: SonarQube 25.x 不再為插件提供 `sonar-ws` 庫
**解決方案**: 在 `plugin-core/pom.xml` 中將 `sonar-ws` 的 scope 設為 `compile`（不設 `provided`）
```xml
<dependency>
    <groupId>org.sonarsource.sonarqube</groupId>
    <artifactId>sonar-ws</artifactId>
    <version>9.9.0.65466</version>
    <!-- 不設置 scope，預設為 compile，打包進插件 -->
</dependency>
```

### 2. PDF 圖表快取策略
**問題**: 快取 iText `Image` 物件導致「Pdf indirect object belongs to other PDF document」錯誤
**解決方案**: `PdfChartGenerator` 快取 `byte[]` PNG 資料而非 `Image` 物件
```java
// ✅ 正確：快取 PNG 位元組資料
private final Cache<String, byte[]> chartCache;

public Image generateSeverityPieChart(ReportSummary summary) {
    byte[] cachedPngBytes = chartCache.getIfPresent(cacheKey);
    if (cachedPngBytes != null) {
        return new Image(ImageDataFactory.create(cachedPngBytes));
    }
    // ... 生成圖表並快取 pngBytes
}
```

### 3. PDF/A 合規性處理
**問題**: PDF/A-1b 需要 ICC 色彩描述檔（`default_rgb.icc`），但資源檔案缺失
**解決方案**: 使用標準 PDF（`PdfDocument`）而非 PDF/A（`PdfADocument`）
```java
// 修改前：PdfADocument pdfADoc = createPdfADocument(writer);
// 修改後：
try (PdfWriter writer = new PdfWriter(outputPath);
     PdfDocument pdfDoc = new PdfDocument(writer);
     Document document = new Document(pdfDoc)) {
    // ...
}
```

### 4. 部署緩存清理
**問題**: 部署新版本後 SonarQube 仍使用舊版本
**解決方案**: 每次部署前刪除 `data/web/deploy/` 目錄

### 5. 背景進程停止策略
**問題**: `StopSonar.bat` 無法完全停止 SonarQube，背景進程會自動重啟
**解決方案**: 使用 `taskkill //F //IM java.exe` 強制停止所有 Java 進程

---

## 報告 API 使用

### 支援的格式

```bash
# JSON 格式（結構化數據）
curl -u admin:P@ssw0rd \
     "http://localhost:9000/api/owasp/report/export?format=json&project=<PROJECT_KEY>"

# PDF 格式（企業級報告）
curl -u admin:P@ssw0rd \
     "http://localhost:9000/api/owasp/report/export?format=pdf&project=<PROJECT_KEY>" \
     -o report.pdf

# HTML 格式（響應式設計）
curl -u admin:P@ssw0rd \
     "http://localhost:9000/api/owasp/report/export?format=html&project=<PROJECT_KEY>" \
     -o report.html

# Markdown 格式（Git 整合）
curl -u admin:P@ssw0rd \
     "http://localhost:9000/api/owasp/report/export?format=markdown&project=<PROJECT_KEY>" \
     -o report.md
```

### 回應格式

**JSON 結構**:
```json
{
  "metadata": {
    "projectKey": "NCCS2.CallCenterWeb.backend",
    "projectName": "CallCenter Backend",
    "owaspVersion": "2021",
    "generatedAt": "2025-10-24T23:16:37Z"
  },
  "summary": {
    "totalFindings": 67,
    "blockerCount": 0,
    "criticalCount": 0,
    "majorCount": 33,
    "minorCount": 6,
    "infoCount": 0
  },
  "findings": [
    {
      "key": "issue-key-1",
      "severity": "MAJOR",
      "owaspCategory": "A01:2021",
      "message": "SQL Injection vulnerability detected",
      "file": "src/main/java/UserService.java",
      "line": 45,
      "aiSuggestion": "Use PreparedStatement with parameterized queries"
    }
  ]
}
```

---

## 前端開發原則

### SonarQube CSP (Content Security Policy) 合規

**重要**: SonarQube 強制執行嚴格的 CSP 政策，所有前端代碼必須遵守以下規則：

#### 禁止使用 Inline Event Handlers
```javascript
// ❌ 錯誤：Inline event handlers 違反 CSP
<button onclick="handleClick()">Click</button>
<a href="javascript:void(0)" onclick="doSomething()">Link</a>

// ✅ 正確：使用 addEventListener
<button class="btn-action" data-index="0">Click</button>

// JavaScript
const buttons = document.querySelectorAll('.btn-action');
buttons.forEach(button => {
    button.addEventListener('click', function(e) {
        const index = parseInt(this.getAttribute('data-index'));
        handleClick(index);
    });
});
```

#### 靜態資源路徑規則
**檔案位置**: `plugin-core/src/main/resources/static/`

所有靜態資源（CSS、JS、圖片）必須放在此目錄，SonarQube 會自動掛載為：
- `report.html` → `/static/report.html`
- `report.js` → `/static/report.js`
- `report.css` → `/static/report.css`

#### HTML 報告 UI 架構
**主檔案**: `plugin-core/src/main/resources/static/report.html`

**關鍵 CSS 類別**:
- `.original-code-section`: 代碼區塊容器（深色背景 `#1e1e1e`）
- `.original-code-header`: 代碼區塊標題（深灰背景 `#252525`）
- `.original-code-snippet`: 代碼內容區（包含語法高亮）

**VS Code 深色主題配色**:
```css
/* 背景與文字 */
background: #1e1e1e;
color: #d4d4d4;

/* 語法高亮 */
.hljs-keyword { color: #569cd6; }  /* 藍色 - 關鍵字 */
.hljs-string { color: #ce9178; }   /* 橘色 - 字串 */
.hljs-number { color: #b5cea8; }   /* 淺綠 - 數字 */
.hljs-comment { color: #6a9955; }  /* 深綠 - 註解 */
.hljs-function { color: #dcdcaa; } /* 淺黃 - 函式 */
.hljs-variable { color: #4ec9b0; } /* 青色 - 變數 */
```

#### Chart.js 整合
**前端圖表**: 使用 Chart.js 生成互動式圖表

**範例** (`report.js` lines 225-309):
```javascript
function createSeverityChart(summary) {
    const ctx = document.getElementById('severityChart');
    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['Blocker', 'Critical', 'Major', 'Minor', 'Info'],
            datasets: [{
                data: [
                    summary.blockerCount,
                    summary.criticalCount,
                    summary.majorCount,
                    summary.minorCount,
                    summary.infoCount
                ],
                backgroundColor: ['#d32f2f', '#f57c00', '#fbc02d', '#689f38', '#1976d2']
            }]
        }
    });
}
```

#### API 整合規則
**重要**: 前端呼叫 SonarQube API 的注意事項

1. **GET 請求優先**: 避免 POST CSRF Token 複雜性
2. **Credentials 設定**: 必須加上 `credentials: 'same-origin'`
3. **錯誤處理**: 提供使用者友善的錯誤訊息

**範例**:
```javascript
fetch(`/api/aiowasp/suggest?${params}`, {
    method: 'GET',
    headers: {
        'Content-Type': 'application/json'
    },
    credentials: 'same-origin'
})
.then(response => {
    if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
    }
    return response.json();
})
.catch(error => {
    console.error('API Error:', error);
    alert('無法取得 AI 建議，請稍後再試');
});
```

---

## 常見問題排查

### 問題 1: ClassNotFoundException: org.sonarqube.ws.client.WsConnector
**症狀**: 插件載入時拋出 ClassNotFoundException
**原因**: SonarQube 25.x 不提供 `sonar-ws` 庫
**解決**: 確認 `plugin-core/pom.xml` 中 `sonar-ws` 依賴未設置 `scope` 為 `provided`

### 問題 2: PDF 第二次下載失敗
**症狀**: 第一次 PDF 下載成功，第二次失敗並拋出 `PdfException: Pdf indirect object belongs to other PDF document`
**原因**: 圖表快取錯誤地快取了 iText `Image` 物件
**解決**: 已修復於 commit `8bc9aa2`，確保使用最新版本

### 問題 3: HTTP 401 Unauthorized
**症狀**: API 請求返回 401 錯誤
**原因**: SonarQube API 需要認證
**解決**: 使用 `-u admin:P@ssw0rd` 參數進行 Basic Authentication

### 問題 4: 插件持續重啟
**症狀**: 停止 SonarQube 後插件自動重啟
**原因**: 背景 bash 進程持續執行 `StartSonar.bat`
**解決**:
1. 使用 Claude Code 的 `KillShell` 工具停止所有背景 shell
2. 執行 `taskkill //F //IM java.exe`

### 問題 5: 部署後仍使用舊版本
**症狀**: 複製新 JAR 後插件功能未更新
**原因**: 部署緩存未清理
**解決**: 刪除 `E:/sonarqube-community-25.10.0.114319/data/web/deploy/` 目錄

### 問題 6: Content Security Policy (CSP) 違規
**症狀**: 瀏覽器 Console 錯誤 `Refused to execute inline event handler because it violates the following Content Security Policy directive`
**原因**: 使用 inline event handlers（如 `onclick="..."`）違反 SonarQube CSP 政策
**解決**:
- ❌ **錯誤**：`<button onclick="handleClick()">Click</button>`
- ✅ **正確**：使用 `addEventListener` 綁定事件
  ```javascript
  // HTML
  <button class="btn-action" data-index="0">Click</button>

  // JavaScript
  const buttons = document.querySelectorAll('.btn-action');
  buttons.forEach(button => {
      button.addEventListener('click', function(e) {
          const index = parseInt(this.getAttribute('data-index'));
          handleClick(index);
      });
  });
  ```
**參考**: 見「前端開發原則」章節

### 問題 7: HTTP 401 Unauthorized（AI 建議 API）
**症狀**: 前端呼叫 `/api/aiowasp/suggest` 時返回 401 錯誤
**原因**: SonarQube POST API 需要 CSRF Token，且未標記為內部 API
**解決**:
1. **方法一（推薦）**: 改用 GET 請求
   - Frontend: `fetch('/api/aiowasp/suggest?${params}', {method: 'GET'})`
   - Backend: 移除 `.setPost(true)`，保留 `.setInternal(true)`
2. **方法二**: 使用 POST + CSRF Token（較複雜）
   - 需要從頁面取得 CSRF token 並傳送

**修改檔案**:
- `report.js` line 394-397: 改為 GET 請求，加上 `credentials: 'same-origin'`
- `AiSuggestionController.java` line 90: 加上 `.setInternal(true)`

### 問題 8: HTTP 400 Bad Request（空 CWE ID 參數）
**症狀**: API 返回 `The 'cweId' parameter is missing`，即使前端傳送了 `cweId=`
**原因**: 雙層驗證問題
1. **API 定義層**: `setRequired(true)` 要求參數必須存在且非空
2. **Handler 讀取層**: `request.mandatoryParam()` 強制要求參數

**根本原因**: 並非所有 SonarQube 安全問題都有對應的 CWE ID，某些問題的 `cweId` 為空字串

**解決**:
```java
// AiSuggestionController.java

// 1. API 定義（line 103-106）
suggestAction.createParam(PARAM_CWE_ID)
    .setDescription("CWE ID (e.g., CWE-89)")
    .setRequired(false) // 非必填，某些問題可能沒有 CWE ID
    .setExampleValue("CWE-89");

// 2. Handler 讀取（line 133）
String cweId = request.param(PARAM_CWE_ID); // 改為 param()，不使用 mandatoryParam()
```

**重要提醒**: SonarQube WebService API 有兩層驗證：
- `.setRequired()`: 定義層驗證
- `request.mandatoryParam()` vs `request.param()`: 讀取層驗證
- **兩層都必須調整**才能讓可選參數正常工作

---

## 日誌位置

- **主日誌**: `E:/sonarqube-community-25.10.0.114319/logs/sonar.log`
- **Web 日誌**: `E:/sonarqube-community-25.10.0.114319/logs/web.log`（插件相關錯誤在此）
- **插件載入訊息**: 搜尋 `AI OWASP Security Plugin` 關鍵字

---

## 專案文件

- **README.md**: 功能說明、安裝指南、配置範例
- **docs/**: 詳細技術文件、架構圖、API 規格
- **spec/**: Epic 與 Story 規格文件

---

**最後更新**: 2025-10-25
**版本**: 1.0.0-SNAPSHOT
