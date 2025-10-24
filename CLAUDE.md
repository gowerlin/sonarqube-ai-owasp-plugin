# SonarQube AI OWASP Plugin - Development Guide

**專案名稱**: SonarQube AI-Powered OWASP Security Analysis Plugin
**版本**: 1.0.0-SNAPSHOT
**最後更新**: 2025-10-24

---

## 環境配置

### SonarQube 測試環境

**程式根目錄**: `E:\sonarqube-community-25.10.0.114319`

**測試帳密**:
- 帳號: `admin`
- 密碼: `P@ssw0rd`

**目錄結構**:
```
E:\sonarqube-community-25.10.0.114319\
├── bin\windows-x86-64\          # 啟動腳本目錄
│   ├── StartSonar.bat           # 啟動腳本
│   └── StopSonar.bat            # 停止腳本（不可靠）
├── extensions\plugins\          # 插件部署目錄
├── logs\                        # 日誌目錄
│   ├── sonar.log               # 主日誌
│   └── web.log                 # Web Server 日誌
└── data\web\deploy\            # 部署緩存（需清理）
```

---

## 插件部署流程（正確方法）

### ⚠️ 重要經驗教訓

**問題**: 使用 `StopSonar.bat` 無法完全停止 SonarQube，背景進程會持續運行並自動重啟。

**解決方案**: 使用 `taskkill` 強制停止所有 Java 進程。

### 標準部署流程

#### 1. 編譯插件

```bash
cd "D:/ForgejoGit/Security_Plugin_for_SonarQube"
mvn clean package -Dmaven.test.skip=true
```

**輸出檔案**: `plugin-core/target/sonar-aiowasp-plugin-1.0.0-SNAPSHOT.jar`
**檔案大小**: 約 33MB（包含 sonar-ws 依賴）

#### 2. 停止 SonarQube（關鍵步驟）

```bash
# 方法 1: 強制停止所有 Java 進程（最可靠）
taskkill //F //IM java.exe

# 方法 2: 如果有背景 bash 進程，先停止它們
# 使用 Claude Code 的 KillShell 工具停止所有背景 shell
```

**驗證停止成功**:
```bash
tasklist | grep -i java
# 應該沒有任何輸出
```

#### 3. 部署新版本插件

```bash
# 複製新版本 JAR
cp "D:/ForgejoGit/Security_Plugin_for_SonarQube/plugin-core/target/sonar-aiowasp-plugin-1.0.0-SNAPSHOT.jar" "E:/sonarqube-community-25.10.0.114319/extensions/plugins/"

# 清理部署緩存（重要！）
rm -rf "E:/sonarqube-community-25.10.0.114319/data/web/deploy/"
```

**驗證部署成功**:
```bash
ls -lh "E:/sonarqube-community-25.10.0.114319/extensions/plugins/sonar-aiowasp-plugin-1.0.0-SNAPSHOT.jar"
# 應該顯示 33M 大小，時間戳為最新
```

#### 4. 清空日誌（保持測試關注點）

```bash
# 清空所有日誌檔案，避免舊的錯誤訊息干擾分析
rm "E:/sonarqube-community-25.10.0.114319/logs/"*.log
```

**重要性**: 每次測試前清空日誌，確保只看到當前運行的訊息

#### 5. 啟動 SonarQube

```bash
cd "E:/sonarqube-community-25.10.0.114319/bin/windows-x86-64"
./StartSonar.bat &
```

**等待時間**: 約 60-90 秒完全啟動

**監控啟動狀態**:
```bash
# 監控主日誌
tail -f "E:/sonarqube-community-25.10.0.114319/logs/sonar.log"

# 等待以下訊息出現：
# "SonarQube is operational"
```

#### 6. 驗證插件載入

```bash
# 檢查插件載入訊息
grep -i "aiowasp\|ai owasp" "E:/sonarqube-community-25.10.0.114319/logs/web.log" | tail -5

# 應該看到：
# INFO  web[][c.g.s.p.AiOwaspPlugin] 正在載入 AI OWASP Security Plugin v1.0.0
# INFO  web[][c.g.s.p.AiOwaspPlugin] AI OWASP Security Plugin 載入完成
```

#### 7. 測試 API 端點

```bash
# 測試 JSON 格式報告 API（需要認證）
curl -u admin:P@ssw0rd -s "http://localhost:9000/api/owasp/report/export?format=json&project=NCCS2.CallCenterWeb.backend" | jq .

# 測試 PDF 格式報告 API
curl -u admin:P@ssw0rd "http://localhost:9000/api/owasp/report/export?format=pdf&project=NCCS2.CallCenterWeb.backend" -o test-report.pdf
```

---

## 常見問題排查

### 問題 1: ClassNotFoundException: org.sonarqube.ws.client.WsConnector

**原因**: SonarQube 25.x 不再為插件提供 `sonar-ws` 庫

**解決方案**: 在 `plugin-core/pom.xml` 中將 `sonar-ws` 的 scope 設為 `compile`（預設值），而不是 `provided`

```xml
<!-- 正確配置 -->
<dependency>
    <groupId>org.sonarsource.sonarqube</groupId>
    <artifactId>sonar-ws</artifactId>
    <version>9.9.0.65466</version>
    <!-- 不要設置 scope，讓它打包進插件 -->
</dependency>
```

### 問題 2: HTTP 401 Unauthorized

**原因**: SonarQube API 需要認證

**解決方案**: 使用 `-u admin:P@ssw0rd` 參數

```bash
# 錯誤
curl "http://localhost:9000/api/owasp/report/export?format=json&project=XXX"

# 正確
curl -u admin:P@ssw0rd "http://localhost:9000/api/owasp/report/export?format=json&project=XXX"
```

### 問題 3: 插件無法停止/持續重啟

**原因**: 背景 bash 進程持續嘗試啟動 SonarQube

**解決方案**:
1. 使用 Claude Code 的 KillShell 工具停止所有背景 shell
2. 使用 `taskkill //F //IM java.exe` 強制停止所有 Java 進程
3. 確認沒有 Java 進程後再部署新版本

### 問題 4: 部署後仍使用舊版本

**原因**: 部署緩存未清理

**解決方案**: 刪除 `E:/sonarqube-community-25.10.0.114319/data/web/deploy/` 目錄

---

## 開發工作流程

### 完整部署命令序列（一鍵執行）

```bash
# Step 1: 編譯
cd "D:/ForgejoGit/Security_Plugin_for_SonarQube" && mvn clean package -Dmaven.test.skip=true

# Step 2: 停止服務
taskkill //F //IM java.exe 2>/dev/null
sleep 3

# Step 3: 部署
cp "D:/ForgejoGit/Security_Plugin_for_SonarQube/plugin-core/target/sonar-aiowasp-plugin-1.0.0-SNAPSHOT.jar" "E:/sonarqube-community-25.10.0.114319/extensions/plugins/"
rm -rf "E:/sonarqube-community-25.10.0.114319/data/web/deploy/"

# Step 4: 清空日誌（保持測試關注點）
rm "E:/sonarqube-community-25.10.0.114319/logs/"*.log

# Step 5: 啟動
cd "E:/sonarqube-community-25.10.0.114319/bin/windows-x86-64" && ./StartSonar.bat &

# Step 6: 等待啟動
sleep 60
tail -10 "E:/sonarqube-community-25.10.0.114319/logs/sonar.log"
```

### 快速驗證流程

```bash
# 1. 確認插件載入
grep "AI OWASP Security Plugin 載入完成" "E:/sonarqube-community-25.10.0.114319/logs/web.log" | tail -1

# 2. 測試 API
curl -u admin:P@ssw0rd -s "http://localhost:9000/api/owasp/report/export?format=json&project=NCCS2.CallCenterWeb.backend" | jq '.summary.totalFindings'

# 3. 檢查錯誤
tail -50 "E:/sonarqube-community-25.10.0.114319/logs/web.log" | grep -i error
```

---

## API 測試指令

### 支援的報告格式

```bash
# JSON 格式
curl -u admin:P@ssw0rd "http://localhost:9000/api/owasp/report/export?format=json&project=NCCS2.CallCenterWeb.backend"

# PDF 格式
curl -u admin:P@ssw0rd "http://localhost:9000/api/owasp/report/export?format=pdf&project=NCCS2.CallCenterWeb.backend" -o report.pdf

# HTML 格式
curl -u admin:P@ssw0rd "http://localhost:9000/api/owasp/report/export?format=html&project=NCCS2.CallCenterWeb.backend" -o report.html

# Markdown 格式
curl -u admin:P@ssw0rd "http://localhost:9000/api/owasp/report/export?format=markdown&project=NCCS2.CallCenterWeb.backend" -o report.md
```

### 測試專案

**專案 Key**: `NCCS2.CallCenterWeb.backend`

**預期結果**: 應該返回 67 個安全問題（與 bitegarden 插件一致）
- 28 Minor
- 21 Info
- 18 Security Hotspots

---

## 版本歷史

### 2025-10-24 - WsClient 實作（compile scope）

**變更內容**:
- 修改 `SonarQubeDataService.java` 使用 WsClient API 查詢 SonarQube 數據
- 修改 `plugin-core/pom.xml`，將 `sonar-ws` 依賴改為 `compile` scope（打包進插件）
- 移除 HTTP-based 實作，解決 ClassNotFoundException 問題

**關鍵配置**:
```java
// SonarQubeDataService.java - WsClient 初始化
this.wsClient = WsClientFactories.getDefault().newClient(
    HttpConnector.newBuilder()
        .url("http://localhost:9000")
        .build()
);
```

**檔案大小**: 33MB（包含 sonar-ws 及其依賴）

---

## 參考資訊

### 相關檔案路徑

- **插件專案**: `D:\ForgejoGit\Security_Plugin_for_SonarQube`
- **編譯輸出**: `plugin-core/target/sonar-aiowasp-plugin-1.0.0-SNAPSHOT.jar`
- **SonarQube 根目錄**: `E:\sonarqube-community-25.10.0.114319`
- **插件部署目錄**: `E:\sonarqube-community-25.10.0.114319\extensions\plugins`
- **日誌目錄**: `E:\sonarqube-community-25.10.0.114319\logs`

### 關鍵 Java 類別

- `com.github.sonarqube.plugin.AiOwaspPlugin` - 插件入口點
- `com.github.sonarqube.plugin.service.SonarQubeDataService` - SonarQube 數據查詢服務
- `com.github.sonarqube.plugin.api.PdfReportApiController` - 報告匯出 API 控制器
- `com.github.sonarqube.plugin.web.OwaspReportPageDefinition` - Web UI 頁面定義

---

**最後驗證日期**: 2025-10-24
**驗證狀態**: ✅ 插件成功載入，等待 API 測試確認
