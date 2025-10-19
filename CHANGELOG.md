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

### Epic 5: 報告生成與多版本對照（Week 15-18）
- [ ] HTML 報告生成（Thymeleaf）
- [ ] JSON 報告生成
- [ ] 多版本對照報告（2-3 版本並排）
- [ ] 圖表視覺化（Chart.js）

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

[Unreleased]: https://github.com/your-org/sonarqube-ai-owasp-plugin/compare/v1.0.0...HEAD
[1.0.0-SNAPSHOT]: https://github.com/your-org/sonarqube-ai-owasp-plugin/releases/tag/v1.0.0-SNAPSHOT
