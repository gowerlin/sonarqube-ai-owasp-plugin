# SonarQube AI OWASP Security Plugin - 使用者手冊

**版本**: 1.0.0
**最後更新**: 2025-10-20
**適用對象**: 開發團隊、安全工程師、DevOps 工程師

---

## 目錄

1. [簡介](#簡介)
2. [快速開始](#快速開始)
3. [安裝指南](#安裝指南)
4. [配置設定](#配置設定)
5. [掃描專案](#掃描專案)
6. [查看報告](#查看報告)
7. [進階功能](#進階功能)
8. [故障排除](#故障排除)
9. [最佳實踐](#最佳實踐)
10. [常見問題](#常見問題)

---

## 簡介

### 什麼是 SonarQube AI OWASP Security Plugin?

SonarQube AI OWASP Security Plugin 是一款結合 AI 技術與 OWASP 安全規則的 SonarQube 外掛程式，提供：

- **AI 驅動分析**: 使用 GPT-4、Claude 3、Gemini 等先進 AI 模型進行程式碼安全分析
- **多版本 OWASP 支援**: 支援 OWASP Top 10 2017、2021、2025 三個版本
- **智能修復建議**: AI 生成具體的修復程式碼與工作量評估
- **高效能掃描**: 並行分析、智能快取、增量掃描
- **豐富報告**: HTML/JSON 多版本對照報告

### 核心特性

| 特性 | 說明 |
|------|------|
| **AI 模型支援** | GPT-4, GPT-3.5, Claude 3 (Opus/Sonnet/Haiku), Gemini Pro |
| **CLI 模式** | Gemini CLI, GitHub Copilot CLI, Claude CLI |
| **OWASP 版本** | 2017 (10項), 2021 (10項), 2025 (預覽) |
| **效能優化** | 並行分析 (3-5檔案), 快取 (70-90%命中率), 增量掃描 (90-95%減少) |
| **成本追蹤** | 8 種 AI 提供商定價、預算警告、成本預估 |

---

## 快速開始

### 5 分鐘快速入門

1. **安裝外掛程式**
   ```bash
   # 下載 JAR 檔案到 SonarQube 外掛程式目錄
   cp sonarqube-ai-owasp-plugin-1.0.0.jar $SONARQUBE_HOME/extensions/plugins/

   # 重新啟動 SonarQube
   $SONARQUBE_HOME/bin/sonar.sh restart
   ```

2. **配置 AI 模型**
   - 登入 SonarQube Web UI
   - 進入 `Administration` → `AI OWASP Configuration`
   - 設定 AI Provider: `openai`
   - 輸入 API Key
   - 選擇模型: `gpt-4`

3. **執行掃描**
   ```bash
   # 在專案目錄執行
   sonar-scanner \
     -Dsonar.projectKey=my-project \
     -Dsonar.sources=. \
     -Dsonar.host.url=http://localhost:9000
   ```

4. **查看報告**
   - 在 SonarQube Web UI 進入專案
   - 點擊 `More` → `OWASP Security Report`
   - 檢視 AI 分析結果與修復建議

---

## 安裝指南

### 系統需求

| 項目 | 最低需求 | 建議配置 |
|------|----------|----------|
| **SonarQube** | 9.9 LTS | 10.x 最新版 |
| **Java** | JDK 17 | JDK 21 |
| **記憶體** | 4GB | 8GB+ |
| **CPU** | 2 核心 | 4 核心+ |
| **儲存空間** | 5GB | 20GB+ |

### 安裝步驟

#### 方法 1: 手動安裝（推薦）

1. **下載外掛程式**
   ```bash
   wget https://github.com/your-org/sonarqube-ai-owasp-plugin/releases/download/v1.0.0/sonarqube-ai-owasp-plugin-1.0.0.jar
   ```

2. **部署 JAR 檔案**
   ```bash
   cp sonarqube-ai-owasp-plugin-1.0.0.jar $SONARQUBE_HOME/extensions/plugins/
   ```

3. **設定權限**
   ```bash
   chown sonarqube:sonarqube $SONARQUBE_HOME/extensions/plugins/sonarqube-ai-owasp-plugin-1.0.0.jar
   chmod 644 $SONARQUBE_HOME/extensions/plugins/sonarqube-ai-owasp-plugin-1.0.0.jar
   ```

4. **重新啟動 SonarQube**
   ```bash
   $SONARQUBE_HOME/bin/sonar.sh restart
   ```

5. **驗證安裝**
   - 登入 SonarQube Web UI
   - 進入 `Administration` → `Marketplace` → `Installed`
   - 確認看到 `AI OWASP Security Plugin v1.0.0`

#### 方法 2: Docker 部署

```dockerfile
FROM sonarqube:10-community

# 安裝外掛程式
COPY sonarqube-ai-owasp-plugin-1.0.0.jar /opt/sonarqube/extensions/plugins/

# 設定環境變數
ENV SONAR_AI_OWASP_PROVIDER=openai
ENV SONAR_AI_OWASP_API_KEY=your-api-key-here

EXPOSE 9000
```

```bash
# 建立映像
docker build -t sonarqube-ai-owasp:1.0.0 .

# 執行容器
docker run -d -p 9000:9000 sonarqube-ai-owasp:1.0.0
```

---

## 配置設定

### AI 模型配置

#### 在 SonarQube Web UI 配置

1. **登入 SonarQube**
   - 開啟瀏覽器訪問 `http://your-sonarqube-server:9000`
   - 使用管理員帳號登入

2. **進入配置頁面**
   - `Administration` → `Configuration` → `AI Configuration`

3. **設定 AI Provider**

   **OpenAI (GPT-4)**
   ```
   Provider: openai
   API Key: sk-...
   Model: gpt-4
   Temperature: 0.3
   Max Tokens: 2000
   Timeout: 60 seconds
   ```

   **Anthropic (Claude 3)**
   ```
   Provider: anthropic
   API Key: sk-ant-...
   Model: claude-3-opus-20240229
   Temperature: 0.3
   Max Tokens: 4000
   Timeout: 90 seconds
   ```

   **Google Gemini**
   ```
   Provider: gemini-api
   API Key: AIza...
   Model: gemini-1.5-pro
   Temperature: 0.3
   Max Tokens: 2000
   Timeout: 60 seconds
   ```

#### 使用配置檔案（sonar-project.properties）

```properties
# AI 配置
sonar.aiowasp.ai.provider=openai
sonar.aiowasp.ai.model=gpt-4
sonar.aiowasp.ai.temperature=0.3
sonar.aiowasp.ai.maxTokens=2000
sonar.aiowasp.ai.timeout=60

# OWASP 版本
sonar.aiowasp.version.2017.enabled=true
sonar.aiowasp.version.2021.enabled=true
sonar.aiowasp.version.2025.enabled=false

# 效能優化
sonar.aiowasp.parallel.files=3
sonar.aiowasp.cache.enabled=true
sonar.aiowasp.incremental.enabled=true

# 報告格式
sonar.aiowasp.report.format=both
sonar.aiowasp.report.multiVersion=true
```

### CLI 模式配置

#### Gemini CLI

```bash
# 安裝 Gemini CLI
npm install -g @google/generative-ai-cli

# 認證
gcloud auth login

# 在 SonarQube 配置
Provider: gemini-cli
CLI Path: /usr/local/bin/gemini
Model: gemini-1.5-pro
```

#### GitHub Copilot CLI

```bash
# 安裝 GitHub CLI
brew install gh  # macOS
sudo apt install gh  # Ubuntu

# 安裝 Copilot 擴充
gh extension install github/gh-copilot

# 認證
gh auth login

# 在 SonarQube 配置
Provider: copilot-cli
CLI Path: /usr/local/bin/gh
```

#### Claude CLI

```bash
# 安裝 Claude CLI
npm install -g @anthropic-ai/claude-cli

# 設定 API Key
claude config set api-key <YOUR_API_KEY>

# 在 SonarQube 配置
Provider: claude-cli
CLI Path: /usr/local/bin/claude
Model: claude-3-sonnet-20240229
```

---

## 掃描專案

### 基本掃描流程

#### 1. 準備專案

```bash
cd /path/to/your/project

# 建立 sonar-project.properties
cat > sonar-project.properties <<EOF
sonar.projectKey=my-app
sonar.projectName=My Application
sonar.projectVersion=1.0
sonar.sources=src
sonar.sourceEncoding=UTF-8
EOF
```

#### 2. 執行掃描

```bash
sonar-scanner \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=your-token-here
```

#### 3. 監控進度

```bash
# 使用 API 查詢進度
curl http://localhost:9000/api/owasp/scan/progress
```

### 增量掃描

```bash
# 僅掃描變更的檔案（相比上一次提交）
sonar-scanner \
  -Dsonar.aiowasp.incremental.enabled=true \
  -Dsonar.aiowasp.incremental.baseline=HEAD~1
```

### 並行掃描

```properties
# sonar-project.properties
sonar.aiowasp.parallel.files=5  # 同時分析 5 個檔案
```

### 快取管理

```properties
# 啟用智能快取
sonar.aiowasp.cache.enabled=true

# 清除快取（在配置變更後）
curl -X POST http://localhost:9000/api/owasp/cache/clear
```

---

## 查看報告

### Web UI 報告

#### 訪問報告

1. 登入 SonarQube
2. 進入專案
3. 點擊 `More` → `OWASP Security Report`

#### 報告內容

- **總覽儀表板**: 漏洞統計、嚴重程度分佈
- **詳細清單**: 所有發現的安全問題
- **修復建議**: AI 生成的修復程式碼
- **OWASP 分類**: 對照 OWASP Top 10 分類

#### 篩選功能

```
- 依嚴重程度篩選: Critical, High, Medium, Low
- 依 OWASP 類別篩選: A01-2021, A02-2021, ...
- 依檔案路徑篩選: src/main/java/...
- 搜尋: 關鍵字搜尋
```

### 匯出報告

#### HTML 報告

```bash
curl http://localhost:9000/api/owasp/report/export?format=html \
  -o owasp-report.html
```

#### JSON 報告

```bash
curl http://localhost:9000/api/owasp/report/export?format=json \
  -o owasp-report.json
```

#### PDF 報告

```bash
curl http://localhost:9000/api/owasp/report/pdf \
  -H "Accept: application/pdf" \
  -o owasp-report.pdf
```

---

## 進階功能

### 成本追蹤

#### 查看成本統計

```bash
curl http://localhost:9000/api/owasp/cost/stats
```

**回應範例**:
```json
{
  "totalCost": 2.45,
  "totalApiCalls": 150,
  "totalInputTokens": 45000,
  "totalOutputTokens": 30000,
  "budgetUsagePercent": 24.5,
  "estimatedMonthlyCost": 73.50
}
```

#### 設定預算警告

```properties
# sonar-project.properties
sonar.aiowasp.cost.budget=100  # 美元
sonar.aiowasp.cost.warning.threshold=75  # 75% 警告
```

### 多版本對照

#### 啟用多版本分析

```properties
sonar.aiowasp.version.2017.enabled=true
sonar.aiowasp.version.2021.enabled=true
sonar.aiowasp.version.2025.enabled=true
sonar.aiowasp.report.multiVersion=true
```

#### 版本對照報告

報告將顯示：
- 三個版本的漏洞分類對比
- 版本間的規則差異
- 遷移建議

### API 端點

#### 配置管理

```bash
# 取得配置
GET /api/owasp/config

# 更新配置
POST /api/owasp/config/update

# 驗證配置
GET /api/owasp/config/validate
```

#### 掃描控制

```bash
# 啟動掃描
POST /api/owasp/scan/start?projectKey=my-app

# 查詢進度
GET /api/owasp/scan/progress

# 取消掃描
POST /api/owasp/scan/cancel?scanId=...

# 掃描歷史
GET /api/owasp/scan/history?projectKey=my-app
```

#### CLI 工具狀態

```bash
# 查詢所有 CLI 工具狀態
GET /api/owasp/cli/status

# 檢查特定 CLI 工具
GET /api/owasp/cli/check?provider=gemini-cli

# 取得 CLI 版本
GET /api/owasp/cli/version?provider=claude-cli

# 檢查認證狀態
GET /api/owasp/cli/auth?provider=copilot-cli
```

---

## 故障排除

### 常見錯誤

#### 1. API Key 無效

**錯誤訊息**: `Invalid API key`

**解決方法**:
```bash
# 驗證 API Key
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer YOUR_API_KEY"

# 重新設定
sonar.aiowasp.ai.apikey=sk-correct-key-here
```

#### 2. CLI 工具不可用

**錯誤訊息**: `CLI tool not available`

**解決方法**:
```bash
# 檢查 CLI 工具安裝
which gemini  # 應該返回路徑

# 檢查權限
ls -l /usr/local/bin/gemini

# 測試 CLI 工具
gemini --version

# 更新配置路徑
sonar.aiowasp.cli.gemini.path=/usr/local/bin/gemini
```

#### 3. 快取失效

**錯誤訊息**: `Cache invalidation error`

**解決方法**:
```bash
# 清除快取
curl -X POST http://localhost:9000/api/owasp/cache/clear

# 停用快取（暫時）
sonar.aiowasp.cache.enabled=false

# 重新啟用快取
sonar.aiowasp.cache.enabled=true
```

#### 4. 超時錯誤

**錯誤訊息**: `AI API timeout`

**解決方法**:
```properties
# 增加超時時間
sonar.aiowasp.ai.timeout=120  # 秒

# 減少並行數
sonar.aiowasp.parallel.files=2

# 減少 token 數量
sonar.aiowasp.ai.maxTokens=1500
```

### 日誌分析

#### 查看 SonarQube 日誌

```bash
tail -f $SONARQUBE_HOME/logs/sonar.log | grep AI_OWASP
```

#### 啟用 DEBUG 模式

```properties
# sonar.properties
sonar.log.level.web=DEBUG
sonar.log.level=com.github.sonarqube.plugin=DEBUG
```

---

## 最佳實踐

### 1. AI 模型選擇

| 場景 | 推薦模型 | 理由 |
|------|----------|------|
| **高精度分析** | GPT-4, Claude 3 Opus | 最佳品質，適合關鍵專案 |
| **平衡性價比** | Claude 3 Sonnet, Gemini Pro | 品質與成本平衡 |
| **大量掃描** | GPT-3.5 Turbo, Gemini Flash | 低成本，高吞吐量 |
| **離線環境** | Gemini CLI, Copilot CLI | 本地化 CLI 工具 |

### 2. 效能優化

```properties
# 大型專案優化配置
sonar.aiowasp.parallel.files=5          # 並行數量
sonar.aiowasp.cache.enabled=true        # 啟用快取
sonar.aiowasp.incremental.enabled=true  # 增量掃描
sonar.aiowasp.ai.maxTokens=1500         # 適度減少 token
```

### 3. 成本控制

```properties
# 設定預算
sonar.aiowasp.cost.budget=50            # 每月 50 美元
sonar.aiowasp.cost.warning.threshold=80 # 80% 警告

# 使用增量掃描
sonar.aiowasp.incremental.enabled=true

# 選擇性啟用版本
sonar.aiowasp.version.2017.enabled=false  # 停用舊版本
sonar.aiowasp.version.2021.enabled=true   # 主要版本
sonar.aiowasp.version.2025.enabled=false  # 預覽版本
```

### 4. 安全最佳實踐

```properties
# 使用環境變數儲存 API Key（不要寫入原始碼）
export SONAR_AI_OWASP_API_KEY=sk-...

# 在 CI/CD 中使用 secrets
sonar-scanner -Dsonar.aiowasp.ai.apikey=${OPENAI_API_KEY}
```

### 5. CI/CD 整合

#### Jenkins Pipeline

```groovy
pipeline {
    agent any
    environment {
        SONAR_TOKEN = credentials('sonarqube-token')
        OPENAI_API_KEY = credentials('openai-api-key')
    }
    stages {
        stage('SonarQube Analysis') {
            steps {
                sh '''
                    sonar-scanner \
                      -Dsonar.host.url=http://sonarqube:9000 \
                      -Dsonar.login=${SONAR_TOKEN} \
                      -Dsonar.aiowasp.ai.apikey=${OPENAI_API_KEY}
                '''
            }
        }
    }
}
```

#### GitHub Actions

```yaml
name: SonarQube Scan

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  sonarqube:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2

      - name: SonarQube Scan
        uses: sonarsource/sonarqube-scan-action@master
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
        with:
          args: >
            -Dsonar.aiowasp.ai.apikey=${{ secrets.OPENAI_API_KEY }}
```

---

## 常見問題

### Q1: 支援哪些程式語言？

A: 目前支援：
- Java
- JavaScript / TypeScript
- Python
- C# / .NET
- Go
- PHP
- Ruby

### Q2: API Key 是否安全？

A: 是的。API Key 使用 SonarQube 內建的加密機制儲存，不會以明文形式儲存或傳輸。

### Q3: 掃描需要多久時間？

A: 取決於專案大小與配置：
- 小型專案 (< 100 檔案): 5-10 分鐘
- 中型專案 (100-500 檔案): 20-30 分鐘
- 大型專案 (> 500 檔案): 1-2 小時

使用並行分析與快取可顯著減少時間（約 60-70%）。

### Q4: 成本如何計算？

A: 基於 AI 提供商定價：
- GPT-4: $0.03/1K input + $0.06/1K output
- Claude 3 Opus: $0.015/1K input + $0.075/1K output
- Gemini Pro: $0.00025/1K input + $0.0005/1K output

平均單檔案分析成本約 $0.01-0.05。

### Q5: 可以離線使用嗎？

A: 部分支援。使用 CLI 模式（Gemini CLI, Copilot CLI, Claude CLI）可以減少對外部 API 的依賴，但仍需網路連線進行認證。

### Q6: 如何處理誤報？

A: 可以在 SonarQube UI 中：
1. 標記為「誤報」(False Positive)
2. 加入排除規則
3. 調整 AI 溫度參數以獲得更保守的分析

---

## 支援與聯絡

### 文件資源

- **使用者手冊**: 本文件
- **開發者文件**: `/docs/DEVELOPER_GUIDE.md`
- **API 文件**: `/docs/API_DOCUMENTATION.md`
- **變更日誌**: `/CHANGELOG.md`

### 回報問題

- **GitHub Issues**: https://github.com/your-org/sonarqube-ai-owasp-plugin/issues
- **Email**: support@your-org.com

### 社群

- **Slack**: #ai-owasp-plugin
- **論壇**: https://community.sonarsource.com/

---

**© 2025 SonarQube AI OWASP Plugin Team**
**授權**: MIT License
