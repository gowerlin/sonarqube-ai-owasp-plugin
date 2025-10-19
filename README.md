# SonarQube AI OWASP Security Plugin

[![CI Build & Test](https://github.com/your-org/sonarqube-ai-owasp-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/your-org/sonarqube-ai-owasp-plugin/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![SonarQube Version](https://img.shields.io/badge/SonarQube-9.9%2B-blue)](https://www.sonarqube.org/)
[![Java Version](https://img.shields.io/badge/Java-11%2B-orange)](https://adoptium.net/)

**AI 驅動的 OWASP 安全分析插件**，支援多版本 OWASP Top 10（2017、2021、2025），提供智能修復建議與並行分析。

---

## ✨ 核心功能

### 🤖 AI 智能分析
- **多 AI Provider 支援** ✨**NEW (v2.1.0 - Epic 9 完成)**：
  - **API 模式**（3 個）: OpenAI GPT-4, Anthropic Claude, Google Gemini
  - **CLI 模式**（3 個）: Gemini CLI, GitHub Copilot CLI, Claude CLI
  - **雙模式架構**: API 與 CLI 模式無縫切換，成本與效能彈性最大化
  - **智慧路徑偵測**: 自動識別 CLI 工具類型，降低配置複雜度
  - **6 種 Provider 自由選擇**: 降低供應商依賴風險，成本優化彈性
- **完整 AI 整合架構** ✅ **(Epic 2 完成)**：
  - **統一抽象介面**: AiService 支援所有 AI Provider（OpenAI, Claude, Gemini）
  - **HTTP API 整合**: OkHttp 3.14.9 + Jackson JSON 序列化
  - **智能快取機制**: Caffeine Cache 3.1.8，LRU eviction，預設 TTL 1 小時
  - **自動重試**: 指數退避（1s → 2s → 4s），最多 3 次重試
  - **雙格式解析**: JSON 結構化 + Regex 非結構化 fallback
  - **錯誤分類**: INVALID_API_KEY, RATE_LIMIT_EXCEEDED, TIMEOUT, NETWORK_ERROR
- 理解代碼語義，減少 **40-60% 誤報率**
- 智能修復建議包含：
  - 詳細描述與修復步驟
  - Before/After 範例代碼
  - 工作量評估（Simple: 0.5-1h, Medium: 2-4h, Complex: 4-8h）
  - 修復時間減少 **50-75%**

### 🛡️ 多版本 OWASP 支援
- **OWASP Top 10 2017**（10 個類別）
- **OWASP Top 10 2021**（10 個類別，預設）
- **OWASP Top 10 2025 預覽版**（架構支援快速更新）
- 完整的 **CWE 映射**
- **版本映射表**（2017 ↔ 2021 ↔ 2025）

### ⚡ 高效能設計
- **並行分析**：可配置檔案數量（預設 3），效能提升 ≥ 40%
- **智能快取**：基於檔案 hash，避免重複分析
- **增量掃描**：Git diff 整合，僅分析變更檔案
- **響應時間**：< 30 秒/1000 行代碼

### 📊 豐富報告

**四種格式，滿足不同需求**：

- **HTML 格式** ✨**NEW (v2.0.0)**：
  - 響應式設計，支援桌面與行動裝置
  - Chart.js 互動式圖表（嚴重性圓餅圖、OWASP 分類長條圖）
  - 詳細漏洞列表（代碼片段、修復建議、CWE 映射）
  - 符合 WCAG 2.1 AA 無障礙標準

- **JSON 格式** ✨**NEW (v2.0.0)**：
  - RFC 8259 標準結構化數據
  - 三層架構：metadata、summary、findings
  - API 整合友好，支援自動化處理
  - 零外部相依（手動 JSON 生成）

- **Markdown 格式** ✨**NEW (v2.0.0)**：
  - CommonMark 規範易讀格式
  - Git 整合友好，版本控制追蹤
  - 完整表格、代碼區塊、Emoji 標籤
  - 適合技術文件和 README 嵌入

- **PDF 格式** (v2.0.0)：
  - 企業級 PDF 報表（封面頁、目錄、圖表、詳細發現）
  - 可客製化品牌（Logo、標題、色彩主題）
  - PDF/A-1b 合規（長期存檔標準）
  - 專業頁首頁尾（Logo、專案名稱、頁碼、時間戳記）
  - 可點擊書籤導航（Adobe Reader 支援）
  - 使用 iText 7 生成（AGPL 3.0 license）

- **多版本對照報告** (規劃中)：2-3 版本並排比較，差異高亮分析
- **報告生成時間**：5-10 分鐘（從 8-16 小時手動生成）
- **匯出 API**：`/api/owasp/report/export?format=pdf|html|json|markdown&project=<key>`

### 🔧 完整配置
- AI 模型選擇與參數調整
- OWASP 版本啟用/停用
- 掃描範圍與並行參數
- API 金鑰加密存儲

---

## 📋 系統需求

| 項目 | 最低需求 | 建議配置 |
|------|----------|----------|
| **SonarQube** | 9.9 LTS | 9.9 LTS 或更新 |
| **Java** | 11+ | 17+ |
| **Maven** | 3.8+ | 3.9+ |
| **記憶體** | 4 GB | 8 GB+ |
| **CPU** | 2 核心 | 4 核心+ |

---

## 🚀 快速開始

### 1. 安裝插件

#### 方法一：從 GitHub Releases 下載
```bash
# 1. 下載最新版本
wget https://github.com/your-org/sonarqube-ai-owasp-plugin/releases/latest/download/sonar-aiowasp-plugin-1.0.0.jar

# 2. 複製至 SonarQube 插件目錄
cp sonar-aiowasp-plugin-1.0.0.jar $SONARQUBE_HOME/extensions/plugins/

# 3. 重啟 SonarQube
$SONARQUBE_HOME/bin/linux-x86-64/sonar.sh restart
```

#### 方法二：從原始碼建構
```bash
# 1. Clone 專案
git clone https://github.com/your-org/sonarqube-ai-owasp-plugin.git
cd sonarqube-ai-owasp-plugin

# 2. 編譯與打包
mvn clean package -DskipTests

# 3. 複製插件
cp plugin-core/target/sonar-aiowasp-plugin-*.jar $SONARQUBE_HOME/extensions/plugins/

# 4. 重啟 SonarQube
$SONARQUBE_HOME/bin/linux-x86-64/sonar.sh restart
```

### 2. 配置 AI Provider ✨**NEW (v2.1.0 - Epic 9 完成)**

登入 SonarQube 後，前往 **Administration → Configuration → AI Configuration**：

#### 支援的 AI Provider（6 個）

**API 模式**（需要 API Key）:
1. **OpenAI GPT-4**: `openai`
   - 模型：gpt-4, gpt-4-turbo, gpt-3.5-turbo
   - 優勢：成熟穩定，中文支援佳
   - API 端點：https://api.openai.com/v1/chat/completions

2. **Anthropic Claude**: `anthropic`
   - 模型：claude-3-opus, claude-3-sonnet, claude-3-haiku
   - 優勢：長文本處理能力強（200K tokens）
   - API 端點：https://api.anthropic.com/v1/messages

3. **Google Gemini API**: `gemini-api`
   - 模型：gemini-1.5-pro (1M token context!), gemini-1.5-flash
   - 優勢：超大上下文窗口，成本低廉
   - API 端點：自動配置

**CLI 模式**（本地工具，無需 API Key）:
4. **Gemini CLI**: `gemini-cli`
   - 工具路徑：`/usr/local/bin/gemini`（預設）
   - 安裝：參照 [Gemini CLI](https://github.com/google-gemini/gemini-cli)
   - 優勢：本地執行，無 API 費用，適合內網環境

5. **GitHub Copilot CLI**: `copilot-cli`
   - 工具路徑：`/usr/local/bin/gh`（預設）
   - 安裝：`gh extension install github/gh-copilot`
   - 優勢：GitHub 企業用戶免費使用

6. **Claude CLI**: `claude-cli`
   - 工具路徑：`/usr/local/bin/claude`（預設）
   - 安裝：參照 [Claude CLI](https://claude.ai/cli)
   - 優勢：Anthropic 最新模型，本地執行

#### 配置步驟

**API 模式範例**（Google Gemini API）:
1. **選擇執行模式**: 下拉選單選擇 `API`
2. **選擇 AI Provider**: 下拉選單選擇 `gemini-api`
3. **選擇 AI Model**: 下拉選單選擇 `gemini-1.5-pro` 或 `gemini-1.5-flash`
4. **輸入 API Key**: 從 [Google AI Studio](https://ai.google.dev/gemini-api/docs?hl=zh-tw) 取得 API 金鑰（加密存儲）
5. **調整參數**（可選）:
   - Temperature: 0.3（預設，較確定性輸出，範圍 0.0-2.0）
   - Max Tokens: 4096（預設，Gemini 支援最高 8192）
   - Timeout: 60 秒（預設）
   - Max Retries: 3（預設）

**CLI 模式範例**（Gemini CLI）:
1. **安裝 Gemini CLI 工具**:
   ```bash
   # 參照官方文件安裝
   # https://github.com/google-gemini/gemini-cli
   ```
2. **驗證安裝**:
   ```bash
   gemini --version
   which gemini  # 確認安裝路徑
   ```
3. **選擇執行模式**: 下拉選單選擇 `CLI`
4. **選擇 AI Provider**: 下拉選單選擇 `gemini-cli`
5. **設定工具路徑**: `/usr/local/bin/gemini` 或您的實際安裝路徑
6. **調整參數**: 同 API 模式（Temperature, Max Tokens, Timeout）

**模式切換範例**（API ↔ CLI）:
```java
// 從 API 模式切換到 CLI 模式（僅需修改配置）
// API 模式配置
AiConfig apiConfig = AiConfig.builder()
    .model(AiModel.GEMINI_1_5_PRO)
    .apiKey("your-api-key")
    .build();

// CLI 模式配置
AiConfig cliConfig = AiConfig.builder()
    .model(AiModel.GEMINI_1_5_PRO)
    .cliPath("/usr/local/bin/gemini")
    .executionMode(AiExecutionMode.CLI)
    .build();
```

#### 配置優勢

- **降低供應商依賴**: 6 種 Provider 自由切換，避免單一供應商風險
- **成本優化**:
  - Gemini Flash 模型成本更低，適合大量掃描
  - CLI 模式本地執行，無 API 呼叫成本
- **效能彈性**: API 模式速度較快，CLI 模式適合批量處理
- **離線場景**: CLI 模式可在內網環境使用（無需外部 API 呼叫）
- **智慧偵測**: 根據 CLI 路徑自動識別工具類型（gemini/gh/claude）

### 3. 啟用 OWASP 版本

前往 **Administration → Configuration → OWASP Versions**：

- ✅ OWASP Top 10 2017（10 個類別）
- ✅ OWASP Top 10 2021（10 個類別，預設）
- ☑ OWASP Top 10 2025（預覽版）

### 4. 執行掃描

```bash
# 使用 Maven 插件
mvn sonar:sonar \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<your-token>

# 或使用 SonarScanner
sonar-scanner \
  -Dsonar.projectKey=my-project \
  -Dsonar.sources=./src \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=<your-token>
```

### 5. 查看與匯出報告

掃描完成後，前往 SonarQube 專案頁面：
- **Security Hotspots**: 查看 OWASP 漏洞列表
- **More → OWASP Report**: 查看多版本對照報告（規劃中）
- **Download Report**: 匯出多格式報告

#### 匯出報告 API

```bash
# 匯出 PDF 報告
curl "http://localhost:9000/api/owasp/report/export?format=pdf&project=my-project" \
  -H "Authorization: Bearer <token>" \
  -o report.pdf

# 匯出 HTML 報告
curl "http://localhost:9000/api/owasp/report/export?format=html&project=my-project" \
  -H "Authorization: Bearer <token>" \
  -o report.html

# 匯出 JSON 報告（API 整合）
curl "http://localhost:9000/api/owasp/report/export?format=json&project=my-project" \
  -H "Authorization: Bearer <token>" \
  -o report.json

# 匯出 Markdown 報告（Git 整合）
curl "http://localhost:9000/api/owasp/report/export?format=markdown&project=my-project" \
  -H "Authorization: Bearer <token>" \
  -o report.md
```

---

## 🛠️ 開發指南

### 環境設置

#### 使用 Docker（推薦）
```bash
# 1. 啟動 SonarQube 開發環境
make start

# 2. 編譯專案
make build

# 3. 打包並安裝插件
make install

# 4. 重啟 SonarQube
make restart

# 5. 查看日誌
make logs
```

#### 手動設置
```bash
# 1. 確保 Java 11+ 和 Maven 3.8+ 已安裝
java -version
mvn -version

# 2. 編譯專案
mvn clean compile

# 3. 執行測試
mvn test

# 4. 打包插件
mvn package -DskipTests
```

### 專案結構

```
sonarqube-ai-owasp-plugin/
├── plugin-core/           # SonarQube 插件核心（入口點、規則、掃描器、UI）
├── ai-connector/          # AI 模型整合（OpenAI、Claude、並行分析、快取）
├── rules-engine/          # OWASP 規則引擎
│   ├── owasp2017/        # 2017 版本規則（10 個類別）
│   ├── owasp2021/        # 2021 版本規則（10 個類別）
│   └── owasp2025/        # 2025 預備版規則（10 個類別）
├── report-generator/      # 多格式報告生成（PDF/HTML/JSON/Markdown）
├── version-manager/       # 版本管理與映射
├── config-manager/        # 配置管理
├── shared-utils/          # 共用工具程式庫
├── docs/                  # 文件目錄
│   ├── architecture.md    # 架構文件
│   ├── ux-specification.md # UX 規格
│   ├── prd.md            # 產品需求文件
│   └── brief.md          # 專案簡介
├── docker-compose.yml     # Docker 開發環境
├── Makefile              # 開發指令快捷方式
└── pom.xml               # Maven 父 POM
```

### 開發工作流程

```bash
# 1. 建立 feature 分支
git checkout -b feature/your-feature-name

# 2. 開發功能（建議使用 TDD）
# - 編寫測試
# - 實現功能
# - 執行測試

# 3. 提交變更
git add .
git commit -m "feat(module): add your feature description"

# 4. 執行完整測試
make test

# 5. 推送至 remote
git push origin feature/your-feature-name

# 6. 建立 Pull Request
```

### 測試策略

| 測試類型 | 目標覆蓋率 | 工具 |
|---------|-----------|------|
| 單元測試 | ≥ 80% | JUnit 5 + Mockito |
| 整合測試 | ≥ 70% | Testcontainers + SonarQube API |
| E2E 測試 | ≥ 50% | Selenium/Playwright |

```bash
# 執行單元測試
mvn test

# 執行整合測試
mvn verify -Pintegration-tests

# 生成覆蓋率報告
mvn jacoco:report
```

---

## 📚 文件

- **[架構文件](docs/architecture.md)** - 技術架構與設計決策
- **[UX 規格](docs/ux-specification.md)** - 使用者介面設計規格
- **[PRD](docs/prd.md)** - 產品需求文件（完整功能需求）
- **[專案摘要](docs/PROJECT_SUMMARY.md)** - 專案概覽與時間線

---

## 🤝 貢獻指南

我們歡迎任何形式的貢獻！請參閱 [CONTRIBUTING.md](CONTRIBUTING.md)。

### 報告問題
- 前往 [GitHub Issues](https://github.com/your-org/sonarqube-ai-owasp-plugin/issues)
- 提供詳細的重現步驟
- 附上錯誤訊息和日誌

### Pull Request
1. Fork 專案
2. 建立 feature 分支
3. 提交變更（遵循 [Conventional Commits](https://www.conventionalcommits.org/)）
4. 推送至您的 fork
5. 建立 Pull Request

---

## 📄 授權

本專案採用 [Apache License 2.0](LICENSE) 授權。

### 第三方授權聲明

**iText 7 PDF Library** (用於 PDF 報表生成功能):
- **授權**: AGPL 3.0 (開源) 或商業授權 (雙授權模式)
- **版本**: 7.2.5+
- **影響**:
  - 如果您分發修改版本的插件，AGPL 3.0 要求您公開原始碼
  - 企業客戶若不希望公開原始碼，可向 [iText Software](https://itextpdf.com/) 購買商業授權
- **更多資訊**: [iText Licensing](https://itextpdf.com/en/how-buy/legal/agpl-gnu-affero-general-public-license)

**注意**: Markdown 和 JSON 報表生成功能不使用 iText，不受 AGPL 授權影響。

---

## 📝 PDF 報表配置

### Logo 檔案需求

PDF 報表支援在封面頁和頁首顯示公司 Logo。若要使用此功能：

**檔案格式**：
- 支援格式：PNG、JPG/JPEG
- 建議格式：PNG（透明背景支援）

**檔案大小**：
- 最大檔案大小：500 KB
- 建議大小：< 200 KB（加快載入速度）

**圖片尺寸**：
- 封面頁 Logo：最大 200x100 像素（寬x高）
- 頁首 Logo：固定 50x25 像素
- 建議原始尺寸：400x200 像素（高解析度，自動縮放）

**檔案路徑**：
```java
PdfReportConfig config = PdfReportConfig.builder()
    .logoPath("/path/to/company-logo.png")  // 絕對路徑或相對路徑
    .build();
```

**錯誤處理**：
- 若 Logo 檔案不存在或無效，PDF 報表仍會正常生成（無 Logo）
- 系統會記錄 `WARN` 級別日誌，便於除錯

**疑難排解**：
| 問題 | 原因 | 解決方法 |
|------|------|----------|
| Logo 不顯示 | 檔案路徑錯誤 | 檢查 `logoPath` 是否為有效路徑 |
| Logo 模糊 | 原始圖片解析度過低 | 使用至少 400x200 像素的高解析度圖片 |
| PDF 生成失敗 | Logo 檔案過大或格式不支援 | 確認檔案 < 500 KB 且為 PNG/JPG 格式 |

### PDF/A-1b 合規說明

本插件生成的 PDF 報表符合 **PDF/A-1b 標準**，確保：
- **長期存檔**：未來數十年後仍可正常開啟
- **視覺一致性**：所有字型和圖片完整嵌入
- **不依賴外部資源**：無需額外軟體或字型庫

適用於需要長期保存安全分析報告的合規場景（如 ISO 27001、SOC 2 稽核）。

---

## 🙏 致謝

- [SonarQube](https://www.sonarqube.org/) - 程式碼品質平台
- [OWASP](https://owasp.org/) - 安全標準與資源
- [OpenAI](https://openai.com/) - GPT-4 API
- [Anthropic](https://www.anthropic.com/) - Claude API
- [Google](https://ai.google.dev/) - Gemini API
- [GitHub](https://github.com/) - GitHub Copilot
- [iText Software](https://itextpdf.com/) - PDF 生成函式庫

---

## 📞 聯絡方式

- **專案主頁**: https://github.com/your-org/sonarqube-ai-owasp-plugin
- **問題追蹤**: https://github.com/your-org/sonarqube-ai-owasp-plugin/issues
- **Email**: dev@your-org.com

---

**Version**: 1.0.0-SNAPSHOT
**Last Updated**: 2025-10-20
