# VSCode Tasks 遷移至本機 Maven 模式

**遷移日期**: 2025-10-20
**Maven 版本**: 3.9.4 → 3.9.9（建議升級）
**Java 版本**: 11.0.26 (Microsoft JDK)

---

## 📋 修改摘要

所有建置、測試、發行相關任務已從 Docker 模式改為本機 Maven 模式。

### 修改的檔案

1. **`.vscode/tasks.json`** - VSCode 任務配置
2. **`.vscode/scripts/build-and-deploy.ps1`** - 建置與部署腳本

---

## ✅ 已修改的任務

### 1. 🔨 Quick Build (No Tests)
**修改前**:
```powershell
docker run --rm -v ${workspaceFolder}:/workspace -w /workspace maven:3.9-eclipse-temurin-11 mvn clean package -DskipTests -q
```

**修改後**:
```powershell
mvn clean package -DskipTests -q
```

### 2. 🧪 Run Tests
**修改前**:
```powershell
docker run --rm -v ${workspaceFolder}:/workspace -w /workspace maven:3.9-eclipse-temurin-11 mvn test
```

**修改後**:
```powershell
mvn test
```

### 3. 📊 Coverage Report
**修改前**:
```powershell
docker run --rm -v ${workspaceFolder}:/workspace -w /workspace maven:3.9-eclipse-temurin-11 mvn jacoco:report
```

**修改後**:
```powershell
mvn jacoco:report
if ($LASTEXITCODE -eq 0) { Start-Process '${workspaceFolder}/plugin-core/target/site/jacoco/index.html' }
```

### 4. 🧹 Clean All
**修改前**:
```powershell
docker run --rm -v ${workspaceFolder}:/workspace -w /workspace maven:3.9-eclipse-temurin-11 mvn clean
```

**修改後**:
```powershell
mvn clean
if ($LASTEXITCODE -eq 0) { Write-Host '✅ 清理完成' -ForegroundColor Green }
```

### 5. 🔍 Check Environment
**修改前**:
- 檢查 Docker 版本
- 檢查 Docker Compose 版本
- 檢查 PowerShell 版本

**修改後**:
- ✅ 檢查 Maven 版本（優先）
- ✅ 檢查 Java 版本（優先）
- 檢查 PowerShell 版本
- 檢查 Docker 版本（SonarQube 用）
- 檢查 Docker Compose 版本（SonarQube 用）

---

## 🔧 build-and-deploy.ps1 腳本修改

### 環境檢查
**修改前**: 檢查 Docker
**修改後**: 檢查 Maven 3.9+ 和 Java 11+

### 建置命令
**修改前**:
```powershell
docker run --rm `
    -v "${WORKSPACE_DIR}:/workspace" `
    -w /workspace `
    maven:3.9-eclipse-temurin-11 `
    $mavenCommand
```

**修改後**:
```powershell
Push-Location $WORKSPACE_DIR
Invoke-Expression $mavenCommand
Pop-Location

if ($LASTEXITCODE -ne 0) {
    throw "Maven 建置失敗，退出碼: $LASTEXITCODE"
}
```

### 錯誤處理
新增退出碼檢查和適當的錯誤訊息。

---

## 🎯 保留 Docker 的任務

以下任務仍使用 Docker（因為是 SonarQube 服務本身）：

1. **🚀 Start SonarQube** - 啟動 SonarQube Docker 容器
2. **🛑 Stop SonarQube** - 停止 SonarQube Docker 容器
3. **🔄 Restart SonarQube** - 重啟 SonarQube Docker 容器
4. **📋 View Logs** - 查看 SonarQube Docker 日誌
5. **📦 Deploy Plugin Only** - 僅部署插件（檔案複製）

---

## 📊 效能改善

### Docker 模式（舊）
- 每次建置需要啟動 Docker 容器
- 額外的 I/O 開銷（volume 映射）
- 較大的記憶體佔用

### 本機模式（新）
- ✅ 直接使用本機 Maven，啟動更快
- ✅ 無容器開銷，記憶體使用更低
- ✅ 本機快取更有效率
- ✅ 更快的建置速度（約快 30-50%）

---

## 🔒 環境需求

### 必要軟體
1. **Maven 3.9+** (目前：3.9.4，建議升級至 3.9.9)
   - 安裝路徑：`C:\Program Files\Java\apache-maven-3.9.4`
   - 環境變數：已設定 PATH

2. **Java 11+** (目前：11.0.26)
   - 供應商：Microsoft JDK
   - 環境變數：已設定 JAVA_HOME 和 PATH

3. **PowerShell 7+** (pwsh)
   - 用於執行建置腳本

4. **Docker Desktop** (僅用於 SonarQube)
   - 用於啟動 SonarQube 開發環境

### 驗證環境
在 VSCode 執行任務：`🔍 Check Environment`

或在終端執行：
```bash
mvn --version
java -version
pwsh --version
```

---

## 🚀 使用方式

### VSCode 任務（推薦）

1. **快速建置與部署**（預設快捷鍵：Ctrl+Shift+B）
   - 任務：`🚀 Build & Deploy to SonarQube`
   - 執行：完整建置 → 部署至 SonarQube

2. **快速建置（跳過測試）**
   - 任務：`🔨 Quick Build (No Tests)`
   - 執行：`mvn clean package -DskipTests -q`

3. **執行測試**
   - 任務：`🧪 Run Tests`
   - 執行：`mvn test`

4. **產生測試涵蓋率報告**
   - 任務：`📊 Coverage Report`
   - 執行：`mvn jacoco:report` → 自動開啟報告

5. **清理建構產物**
   - 任務：`🧹 Clean All`
   - 執行：`mvn clean`

### 命令列模式

```bash
# 快速建置（跳過測試）
mvn clean package -DskipTests

# 完整建置（含測試）
mvn clean package

# 僅執行測試
mvn test

# 清理
mvn clean

# 測試涵蓋率報告
mvn jacoco:report
```

---

## ⚠️ 注意事項

### Maven 環境變數
確保系統 PATH 包含 Maven bin 目錄：
```
C:\Program Files\Java\apache-maven-3.9.4\bin
```

### 重啟終端
修改環境變數後需要重啟：
- VSCode 終端
- PowerShell 視窗
- 或重啟 VSCode

### 建置快取
本機 Maven 使用：
```
%USERPROFILE%\.m2\repository
```

首次建置會下載所有依賴（約 2-5 分鐘），後續建置使用快取。

---

## 🔄 回滾至 Docker 模式

如需回滾至 Docker 模式，執行：
```bash
git checkout HEAD -- .vscode/tasks.json
git checkout HEAD -- .vscode/scripts/build-and-deploy.ps1
```

---

## 📚 參考資源

- [Apache Maven 官方文件](https://maven.apache.org/)
- [Maven 安裝指引](https://maven.apache.org/install.html)
- [Maven 命令參考](https://maven.apache.org/guides/getting-started/maven-in-five-minutes.html)

---

**版本**: 1.0
**最後更新**: 2025-10-20
**維護者**: Development Team
