# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### 🚧 Work in Progress
- Epic 2: AI 整合與基礎安全分析
- Epic 3: OWASP 2021 規則引擎實現
- Epic 4: OWASP 2017 規則與版本管理
- Epic 5: Story 5.4 多版本對照報告（規劃中）
- Epic 5: Story 5.6 報告查看 UI（規劃中）

### ✨ Added - Epic 9: 多 AI Provider 整合 (部分完成)

#### Epic 9: Multi-AI Provider Integration ✨**NEW (v2.1.0)** 🚧 (進行中)
- **Story 9.1: 統一架構設計** ✅
  - `AiExecutionMode` 列舉：雙模式執行架構（API/CLI）
  - `CliExecutor` 介面：CLI 工具執行抽象
  - `CliExecutionException` 例外類別：詳細錯誤上下文
  - `AiProvider` 列舉擴充：支援 6 種 Provider（OpenAI, Claude, Gemini API/CLI, Copilot CLI, Claude CLI）
  - 13 個單元測試（AiExecutionModeTest）

- **Story 9.2: Google Gemini API 整合** ✅
  - `GeminiApiService`: 完整 Gemini API v1beta 整合
  - `GeminiApiRequest/Response`: API 請求/回應模型（Builder 模式）
  - `AiModel` 列舉擴充：GEMINI_1_5_PRO（1M token context）、GEMINI_1_5_FLASH
  - `AiServiceFactory` 更新：支援 Gemini 路由與便利方法
  - Safety Settings 配置：BLOCK_ONLY_HIGH 閥值（允許安全分析內容）
  - 13 個單元測試（GeminiApiServiceTest）

- **Story 9.3: CLI 整合框架** ✅
  - `AbstractCliExecutor`: 通用 CLI 執行器基類（60s timeout, graceful/forceful 終止, 串流讀取）
  - `ProcessCliExecutor`: 具體 CLI 執行器（Builder 模式, 預設參數, 版本檢查）
  - `AbstractCliService`: CLI 模式 AI 服務基類（命令建構, 輸出解析範本方法）
  - 19 個單元測試（ProcessCliExecutorTest，包含 Windows/Unix 平台測試）

- **Story 9.7: 配置管理更新** ✅
  - `AiOwaspPlugin` 更新：17 個配置屬性（從 14 個增加）
  - AI Provider 下拉選單：6 個選項（openai, anthropic, gemini-api, gemini-cli, copilot-cli, claude-cli）
  - AI Model 下拉選單：8 個模型選項（包含 Gemini models）
  - CLI 路徑配置：3 個新屬性（Gemini CLI, Copilot CLI, Claude CLI 工具路徑）
  - README.md 更新：完整 6 種 Provider 配置說明、API/CLI 模式範例、配置優勢說明

---

## [2.0.0] - 2025-10-20

### ✨ Added - 多格式報告生成系統 (Epic 0 & Epic 5)

#### Epic 0: 企業級 PDF 報表生成 ✅ (已完成)
- **PdfReportGenerator**: 企業級 PDF 報表生成引擎
  - iText 7.2.5+ 整合，PDF/A-1b 長期存檔標準合規
  - 專業文件結構：封面頁、目錄、頁首頁尾、書籤導航
  - 資料視覺化：JFreeChart 1.5.4 圓餅圖與長條圖
  - Caffeine Cache 3.1.8 圖表快取機制
  - 完整元資料：專案名稱、OWASP 版本、AI 模型、分析時間
  - 詳細發現呈現：代碼片段、修復建議、CWE 映射
  - 可客製化品牌：Logo、標題、色彩主題、頁首頁尾

- **PdfReportSettings**: SonarQube Settings API 整合
  - 4 個配置屬性：Logo 路徑、報告標題、色彩主題、頁首頁尾啟用
  - 預設值與驗證機制

- **測試覆蓋**: 53 個測試案例（6 個測試類別）
  - PdfReportGeneratorTest (18 測試)
  - PdfCoverPageGeneratorTest (6 測試)
  - PdfTableOfContentsGeneratorTest (8 測試)
  - PdfHeaderFooterEventTest (5 測試)
  - PdfChartGeneratorTest (8 測試)
  - PdfFindingsGeneratorTest (8 測試)

#### Epic 5: 多格式報告生成 (Story 5.1-5.3, 5.5, 5.7) ✅
- **HtmlReportGenerator** (Story 5.2): 響應式 HTML 報表生成
  - Chart.js 4.4.0 CDN 整合（圓餅圖、長條圖）
  - 嵌入式 CSS，自包含 HTML 檔案
  - 響應式設計（768px 行動裝置斷點）
  - HTML 特殊字元轉義（&lt;, &gt;, &quot;, &amp;, &#39;）
  - WCAG 2.1 AA 無障礙標準合規
  - 梯度色卡嚴重性摘要
  - 代碼片段與修復建議格式化
  - 測試覆蓋：10 個測試案例（HtmlReportGeneratorTest）

- **JsonReportGenerator** (Story 5.3): 結構化 JSON 報表生成
  - 手動 JSON 生成（零外部相依）
  - RFC 8259 標準合規，完整特殊字元轉義
  - 三層架構：metadata, summary, findings
  - Map 鍵值按字母排序，確保輸出一致性
  - Null 安全處理，可選欄位智能隱藏
  - 控制字元 Unicode 轉義（\uXXXX）
  - 測試覆蓋：12 個測試案例（JsonReportGeneratorTest）

- **MarkdownReportGenerator** (Story 5.7): Git 整合友好 Markdown 報表
  - CommonMark 規範格式
  - Emoji 嚴重性標籤（🚨 BLOCKER, 🔴 CRITICAL, 🟠 MAJOR, 🟡 MINOR, ℹ️ INFO）
  - 完整表格（專案資訊、執行摘要）
  - 代碼區塊格式化（```java）
  - 嚴重性與分類分布清單
  - 依嚴重性分組的詳細發現
  - 測試覆蓋：16 個測試案例（MarkdownReportGeneratorTest）

- **PdfReportApiController** (Story 5.5): 統一報告匯出 API
  - RESTful API 端點：`/api/owasp/report/export`
  - 4 種格式支援：pdf, html, json, markdown
  - 查詢參數：`?format=<format>&project=<key>`
  - 正確的 Content-Type 與 Content-Disposition 標頭
  - 檔案命名格式：`owasp-security-report-<project-key>.<ext>`
  - Switch-case 路由機制
  - 完整錯誤處理與日誌記錄

### 📊 Statistics
- **程式碼總量**: ~5,000 行（Epic 0: ~3,500 行, Epic 5: ~1,500 行）
- **測試案例**: 91 個測試（PDF: 53, HTML: 10, JSON: 12, Markdown: 16）
- **Git 提交**: 11 次提交
- **Stories 完成**: 10/12 Stories
  - Epic 0: 7/8 Stories (Story 0.1-0.7 完成)
  - Epic 5: 5/7 Stories (Story 5.1-5.3, 5.5, 5.7 完成)

### 📚 Documentation
- **PRD 更新**: Epic 0 與 Epic 5 詳細需求文件化
  - Epic 0 完成狀態記錄與實作統計
  - Epic 5 Stories 5.4 & 5.6 詳細需求定義
  - Story 5.7 Markdown 報表正式納入
  - 技術設計建議與驗收標準

- **README.md 更新**:
  - 四種報表格式完整說明
  - 報告匯出 API 範例（curl 命令）
  - 專案結構更新

- **CHANGELOG.md**: 本變更紀錄更新

### 🔧 Changed
- `report-generator` 模組從 HTML/JSON 擴展為 PDF/HTML/JSON/Markdown 四種格式
- Epic 5 標題從「報告生成與多版本對照」改為「多格式報告生成與多版本對照」

### 🚀 Performance
- **報告生成時間**: < 10 秒（1,000 行代碼專案）
- **PDF 生成**: 60 秒超時控制，包含完整錯誤處理
- **Chart 快取**: Caffeine Cache 減少重複圖表生成

### 🔒 Security
- **HTML 轉義**: 防止 XSS 攻擊（&lt;, &gt;, &quot;, &amp;, &#39;）
- **JSON 轉義**: RFC 8259 合規，完整特殊字元處理
- **Markdown 轉義**: 特殊字元安全處理

### 📦 Dependencies
- **iText 7.2.5+** (AGPL 3.0): PDF 生成
- **JFreeChart 1.5.4**: 圖表生成
- **Caffeine Cache 3.1.8**: 圖表快取
- **Apache PDFBox 2.0.30**: PDF 驗證（測試用）
- **Chart.js 4.4.0** (CDN): HTML 互動式圖表

### ⚠️ Known Limitations
- Epic 5 Story 5.4 (多版本對照報告) 待實作
- Epic 5 Story 5.6 (報告查看 UI) 待實作
- Markdown 報表 TOC 自動生成尚未實現
- PDF 報表目前使用 placeholder 資料（資料庫整合待 Epic 2-4 完成）

---

## [1.0.0-SNAPSHOT] - 2025-10-20

### ✨ Added

#### 專案架構（Epic 1）
- **Maven Monorepo 結構**：7 個子模組設計
  - `plugin-core`: SonarQube 插件核心
  - `ai-connector`: AI 模型整合模組
  - `rules-engine`: OWASP 規則引擎（含 3 個子模組：2017、2021、2025）
  - `report-generator`: HTML/JSON 報告生成
  - `version-manager`: 版本管理與映射
  - `config-manager`: 配置管理
  - `shared-utils`: 共用工具程式庫

#### 插件核心功能
- **AiOwaspPlugin** 主入口類別
  - 14 個配置屬性定義
  - AI 模型配置支援（OpenAI、Anthropic Claude）
  - OWASP 版本啟用配置（2017、2021、2025）
  - 效能參數配置（並行分析、快取、增量掃描）
  - 報告格式配置（HTML、JSON、Both）

#### 共用工具
- **PluginConstants** 常數定義
  - OWASP 版本列舉（2017、2021、2025）
  - AI 供應商列舉（OpenAI、Anthropic）
  - 嚴重性等級列舉（Critical、High、Medium、Low）
  - 報告格式列舉（HTML、JSON、Both）
  - 快取與效能配置常數

- **例外處理類別**
  - `AiOwaspException`: 基礎例外
  - `AiAnalysisException`: AI 分析例外
  - `ConfigurationException`: 配置例外

#### 測試框架
- **AiOwaspPluginTest** 單元測試
  - 插件定義驗證
  - 擴充功能註冊驗證

#### 開發環境
- **Docker Compose** 配置
  - SonarQube 9.9 LTS 容器
  - PostgreSQL 15 資料庫
  - 自動化健康檢查
  - 插件熱載入支援

- **Dockerfile.build** 建構環境
  - Maven 3.9 + Eclipse Temurin 11
  - Git 和 Curl 工具

- **Makefile** 開發指令
  - `make build`: 編譯專案
  - `make test`: 執行測試
  - `make package`: 打包插件
  - `make start`: 啟動 SonarQube
  - `make dev`: 完整開發循環（build + install + restart）

#### CI/CD 流程
- **GitHub Actions CI**（`.github/workflows/ci.yml`）
  - 建構與測試工作流程
  - 程式碼品質分析（Checkstyle、SpotBugs）
  - 整合測試（PostgreSQL）
  - 安全掃描（OWASP Dependency Check）
  - 程式碼覆蓋率報告（Codecov）

- **GitHub Actions Release**（`.github/workflows/release.yml`）
  - 自動化版本發布
  - 生成 Release Notes
  - 產生 SHA256 checksums
  - 上傳插件 artifact

#### 文件
- **README.md**: 專案首頁與快速開始指南
- **CONTRIBUTING.md**: 貢獻者指南
  - 代碼規範
  - Commit 訊息格式
  - 測試要求
  - PR Checklist
- **CHANGELOG.md**: 本變更紀錄

### 📚 Documentation
- [架構文件](docs/architecture.md) - 50 頁技術架構詳細說明
- [UX 規格](docs/ux-specification.md) - 40 頁使用者介面設計規格
- [產品需求文件](docs/prd.md) - 30 頁完整功能需求
- [專案摘要](docs/PROJECT_SUMMARY.md) - 10 頁專案概覽

### 🔧 Changed
- 專案從概念階段進入基礎架構實現階段

### 🐛 Fixed
- N/A（首次發布）

### 🔒 Security
- API 金鑰使用 SonarQube 的 `PASSWORD` 類型存儲，確保加密
- CI/CD 流程中整合 OWASP Dependency Check

### ⚡ Performance
- 並行分析架構設計（預設 3 檔案同時分析）
- 智能快取機制（基於檔案 hash）
- 增量掃描支援（Git diff 整合）

---

## 📋 待實現功能（Roadmap）

### Epic 2: AI 整合與基礎安全分析（Week 4-7）
- [ ] OpenAI GPT-4 API 客戶端實現
- [ ] Anthropic Claude API 客戶端實現
- [ ] 並行分析引擎
- [ ] 智能快取服務
- [ ] Circuit Breaker 錯誤處理

### Epic 3: OWASP 2021 規則引擎（Week 8-11）
- [ ] 10 個 OWASP 2021 規則實現
- [ ] CWE 映射表
- [ ] 規則描述與修復建議

### Epic 4: OWASP 2017 規則與版本管理（Week 12-14）
- [ ] 10 個 OWASP 2017 規則實現
- [ ] 版本管理與映射服務
- [ ] 熱載入機制

### Epic 5: 多格式報告生成與多版本對照（Week 15-18）
- [x] Story 5.1: 報告生成架構（ReportGenerator 介面）
- [x] Story 5.2: HTML 報告生成（Chart.js 互動式圖表）
- [x] Story 5.3: JSON 報告生成（RFC 8259 合規）
- [ ] Story 5.4: 多版本對照報告（2-3 版本並排）
- [x] Story 5.5: 報告匯出功能（API 端點）
- [ ] Story 5.6: 報告查看 UI（SonarQube Web Extension）
- [x] Story 5.7: Markdown 報告生成（CommonMark 規範）

### Epic 6: OWASP 2025 與進階功能（Week 18-21）
- [ ] OWASP 2025 預覽版規則
- [ ] 增量掃描實現
- [ ] 工作量評估演算法

### Epic 7: 配置管理與 UI 完善（Week 20-22）
- [ ] 完整配置介面
- [ ] UI 頁面（7 個核心畫面）
- [ ] 無障礙功能（WCAG 2.1 AA）
- [ ] 響應式設計

### Epic 8: 測試、文件與發布準備（Week 22-24）
- [ ] E2E 測試
- [ ] 使用者手冊
- [ ] API 文件
- [ ] Beta 測試
- [ ] v1.0.0 正式發布

---

## [版本編號說明]

- **Major**: 重大功能變更或不相容的 API 變更
- **Minor**: 向後相容的新功能
- **Patch**: 向後相容的錯誤修復

**範例**：
- `1.0.0` - 首次正式發布
- `1.1.0` - 新增 OWASP 2025 支援
- `1.1.1` - 修復 AI API 逾時問題

---

[Unreleased]: https://github.com/your-org/sonarqube-ai-owasp-plugin/compare/v2.0.0...HEAD
[2.0.0]: https://github.com/your-org/sonarqube-ai-owasp-plugin/releases/tag/v2.0.0
[1.0.0-SNAPSHOT]: https://github.com/your-org/sonarqube-ai-owasp-plugin/releases/tag/v1.0.0-SNAPSHOT
