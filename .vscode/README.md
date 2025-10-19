# VSCode 開發環境配置說明

本目錄包含 **SonarQube AI OWASP Plugin** 專案的 VSCode 開發環境配置。

---

## 📁 檔案說明

| 檔案 | 用途 |
|------|------|
| `tasks.json` | VSCode 任務配置（建置、測試、部署） |
| `keybindings.json` | 快捷鍵配置 |
| `settings.json` | 編輯器設定 |
| `extensions.json` | 推薦的擴充功能 |
| `scripts/` | 自動化腳本目錄 |

---

## ⌨️ 快捷鍵

| 快捷鍵 | 功能 | 對應任務 |
|--------|------|----------|
| `Ctrl+Shift+B` | **建置並部署** | 🚀 Build & Deploy to SonarQube |
| `Ctrl+Shift+T` | **執行測試** | 🧪 Run Unit Tests |
| `Ctrl+Shift+P` → `Ctrl+Shift+D` | **僅部署插件** | 📦 Deploy Plugin Only |
| `Ctrl+Shift+P` → `Ctrl+Shift+S` | **啟動 SonarQube** | 🚀 Start SonarQube (Docker) |
| `Ctrl+Shift+P` → `Ctrl+Shift+R` | **重啟 SonarQube** | 🔄 Restart SonarQube (Docker) |

---

## 🛠️ 可用任務

### 建置任務

- **🚀 Build & Deploy to SonarQube** （預設建置任務）
  - 使用 Maven Docker 容器建置專案
  - 自動複製插件至 SonarQube 插件目錄
  - 顯示部署結果和下一步建議

- **🔨 Quick Build (No Tests)**
  - 跳過測試的快速建置
  - 適合快速驗證編譯

- **📦 Deploy Plugin Only**
  - 僅部署已建置的插件（不重新建置）
  - 適合已建置完成後的快速部署

### Maven 任務

- **Maven: Clean** - 清理建構產物
- **Maven: Compile** - 編譯專案
- **Maven: Test** - 執行測試
- **Maven: Package** - 打包插件

### SonarQube 管理

- **🚀 Start SonarQube (Docker)** - 啟動 Docker SonarQube
- **🛑 Stop SonarQube (Docker)** - 停止 Docker SonarQube
- **🔄 Restart SonarQube (Docker)** - 重啟 Docker SonarQube
- **📋 View SonarQube Logs** - 查看 SonarQube 日誌

### 測試與品質

- **🧪 Run Unit Tests** - 執行單元測試
- **📊 Generate Coverage Report** - 生成覆蓋率報告
- **🔍 Run Checkstyle** - 執行程式碼風格檢查

### 其他

- **🧹 Clean All** - 清理所有建構產物
- **🔍 Check Environment** - 檢查開發環境

---

## 📜 自動化腳本

### `scripts/build-and-deploy.ps1`

**用途**：快速建置並部署插件至 SonarQube

**參數**：
- `-SkipTests`: 跳過測試（預設：true）
- `-CleanBuild`: 執行 clean build（預設：true）
- `-Deploy`: 是否部署（預設：true）

**使用範例**：
```powershell
# 完整建置並部署
.\scripts\build-and-deploy.ps1

# 快速建置（跳過測試）
.\scripts\build-and-deploy.ps1 -SkipTests

# 僅建置不部署
.\scripts\build-and-deploy.ps1 -Deploy:$false
```

**執行流程**：
1. ✅ 檢查環境（Docker、SonarQube 目錄）
2. 📦 Maven 建置插件
3. 🔍 查找插件 JAR 檔案
4. 📥 複製插件至 SonarQube
5. ✅ 顯示部署結果

**部署路徑**：`E:\sonarqube-community-25.10.0.114319\extensions\plugins`

---

### `scripts/clean-plugins.ps1`

**用途**：清理 SonarQube 插件目錄中的舊版本

**參數**：
- `-RemoveAll`: 移除所有版本
- `-KeepLatest`: 保留最新版本（預設）

**使用範例**：
```powershell
# 保留最新版本，移除舊版本
.\scripts\clean-plugins.ps1

# 移除所有版本
.\scripts\clean-plugins.ps1 -RemoveAll
```

**功能**：
- 自動備份至 `backup/` 目錄
- 按修改時間識別最新版本
- 互動式確認

---

## 🚀 快速開始

### 1. 初次設定

```bash
# 1. 安裝推薦的擴充功能
按 F1 → 輸入 "Extensions: Show Recommended Extensions"

# 2. 檢查環境
按 Ctrl+Shift+P → 選擇 "Tasks: Run Task" → "Check Environment"

# 3. 啟動 SonarQube（如果使用 Docker）
按 Ctrl+Shift+P, Ctrl+Shift+S
```

### 2. 開發流程

```bash
# 1. 修改代碼
編輯 Java 檔案...

# 2. 建置並部署（快捷鍵）
按 Ctrl+Shift+B

# 3. 重啟 SonarQube（快捷鍵）
按 Ctrl+Shift+P, Ctrl+Shift+R

# 4. 驗證
訪問 http://localhost:9000
前往 Administration → Marketplace → Installed
查找 "AI OWASP Security"
```

### 3. 僅部署（已建置）

```bash
# 如果已經建置過，只想重新部署
按 Ctrl+Shift+P, Ctrl+Shift+D
```

---

## 🔧 設定說明

### `settings.json` 重點配置

| 設定項 | 說明 |
|--------|------|
| `java.project.sourcePaths` | Java 原始碼路徑（7 個模組） |
| `editor.formatOnSave` | 儲存時自動格式化 |
| `editor.rulers` | 120 字元標記線 |
| `files.exclude` | 隱藏 target、.idea 等目錄 |
| `terminal.integrated.defaultProfile.windows` | 預設使用 PowerShell |

### `tasks.json` 重點配置

- **所有 Maven 任務使用 Docker**：無需本機安裝 Maven
- **使用 `pwsh`**：PowerShell 7+ 執行腳本
- **專案根目錄變數**：`${workspaceFolder}`

---

## 💡 提示與技巧

### 1. 第一次建置很慢？
- Docker Maven 需要下載依賴（首次約 5-10 分鐘）
- 依賴會快取在 Docker 容器中，後續建置會快很多

### 2. 插件未載入？
- 確保 SonarQube 已重啟
- 檢查 `extensions/plugins` 目錄有插件 JAR
- 查看 SonarQube 日誌：`docker-compose logs -f sonarqube`

### 3. 修改代碼後如何測試？
```bash
1. Ctrl+Shift+B（建置並部署）
2. Ctrl+Shift+P, Ctrl+Shift+R（重啟 SonarQube）
3. 等待 15-30 秒
4. 訪問 http://localhost:9000 驗證
```

### 4. 如何清理舊插件？
```powershell
# 在終端機執行
.\.vscode\scripts\clean-plugins.ps1
```

### 5. 查看建置日誌
- 建置過程會在 VSCode 終端機顯示
- SonarQube 日誌：Task "View SonarQube Logs"

---

## 🐛 常見問題

### Q: Maven 建置失敗 "mvn: command not found"
**A**: 正常，此專案使用 Docker Maven，不需要本機安裝

### Q: 部署後插件未出現
**A**:
1. 檢查路徑是否正確：`E:\sonarqube-community-25.10.0.114319\extensions\plugins`
2. 重啟 SonarQube
3. 查看 SonarQube 日誌是否有錯誤

### Q: Docker 建置很慢
**A**:
- 首次建置需下載 Maven 映像檔和依賴
- 建議使用 SSD 和良好的網路連線
- 後續建置會快很多（Docker 層快取）

### Q: PowerShell 腳本無法執行
**A**:
```powershell
# 設定執行策略（以管理員身份執行）
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
```

---

## 📞 需要協助？

- **專案 Issues**: https://github.com/your-org/sonarqube-ai-owasp-plugin/issues
- **開發指南**: [CONTRIBUTING.md](../CONTRIBUTING.md)
- **專案文件**: [docs/](../docs/)

---

**Last Updated**: 2025-10-20
