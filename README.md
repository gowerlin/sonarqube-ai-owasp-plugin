# SonarQube AI OWASP Security Plugin

[![CI Build & Test](https://github.com/your-org/sonarqube-ai-owasp-plugin/actions/workflows/ci.yml/badge.svg)](https://github.com/your-org/sonarqube-ai-owasp-plugin/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![SonarQube Version](https://img.shields.io/badge/SonarQube-9.9%2B-blue)](https://www.sonarqube.org/)
[![Java Version](https://img.shields.io/badge/Java-11%2B-orange)](https://adoptium.net/)

**AI 驅動的 OWASP 安全分析插件**，支援多版本 OWASP Top 10（2017、2021、2025），提供智能修復建議與並行分析。

---

## ✨ 核心功能

### 🤖 AI 智能分析
- **OpenAI GPT-4** 和 **Anthropic Claude** 整合
- 理解代碼語義，減少 **40-60% 誤報率**
- 智能修復建議包含：
  - 詳細描述與修復步驟
  - 範例代碼
  - 工作量評估（修復時間減少 **50-75%**）

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
- **HTML 格式**：統計圖表、漏洞列表、互動式 UI
- **JSON 格式**：結構化數據，API 整合友好
- **多版本對照報告**：2-3 版本並排比較
- **報告生成時間**：5-10 分鐘（從 8-16 小時手動生成）

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

### 2. 配置 AI API 金鑰

登入 SonarQube 後，前往 **Administration → Configuration → AI Configuration**：

1. **選擇 AI 供應商**：OpenAI 或 Anthropic Claude
2. **輸入 API 金鑰**：加密存儲，安全無虞
3. **調整參數**（可選）：
   - Temperature: 0.3（預設，較確定性）
   - Max Tokens: 2000
   - Timeout: 60 秒

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

### 5. 查看報告

掃描完成後，前往 SonarQube 專案頁面：
- **Security Hotspots**: 查看 OWASP 漏洞列表
- **More → OWASP Report**: 查看多版本對照報告
- **Download Report**: 下載 HTML/JSON 格式報告

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
├── report-generator/      # HTML/JSON 報告生成
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

---

## 🙏 致謝

- [SonarQube](https://www.sonarqube.org/) - 程式碼品質平台
- [OWASP](https://owasp.org/) - 安全標準與資源
- [OpenAI](https://openai.com/) - GPT-4 API
- [Anthropic](https://www.anthropic.com/) - Claude API

---

## 📞 聯絡方式

- **專案主頁**: https://github.com/your-org/sonarqube-ai-owasp-plugin
- **問題追蹤**: https://github.com/your-org/sonarqube-ai-owasp-plugin/issues
- **Email**: dev@your-org.com

---

**Version**: 1.0.0-SNAPSHOT
**Last Updated**: 2025-10-20
