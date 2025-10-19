# VSCode Tasks 快速參考

## 📋 可用任務清單（僅顯示 Workspace 任務）

### 🚀 建置與部署（3 個任務）

| 任務 | 快捷鍵 | 說明 |
|------|--------|------|
| **🚀 Build & Deploy to SonarQube** | `Ctrl+Shift+B` | **完整建置並部署**（預設） |
| 🔨 Quick Build (No Tests) | - | 快速建置（跳過測試） |
| 📦 Deploy Plugin Only | `Ctrl+Shift+P, D` | 僅部署插件（不重新建置） |

### 🐳 SonarQube 管理（4 個任務）

| 任務 | 快捷鍵 | 說明 |
|------|--------|------|
| 🚀 Start SonarQube | `Ctrl+Shift+P, S` | 啟動 Docker SonarQube |
| 🛑 Stop SonarQube | - | 停止 Docker SonarQube |
| 🔄 Restart SonarQube | `Ctrl+Shift+P, R` | 重啟 SonarQube |
| 📋 View Logs | - | 查看 SonarQube 日誌 |

### 🧪 測試與品質（2 個任務）

| 任務 | 快捷鍵 | 說明 |
|------|--------|------|
| 🧪 Run Tests | `Ctrl+Shift+T` | 執行單元測試 |
| 📊 Coverage Report | - | 生成覆蓋率報告並開啟 |

### 🛠️ 工具任務（3 個任務）

| 任務 | 快捷鍵 | 說明 |
|------|--------|------|
| 🧹 Clean All | - | 清理所有建構產物 |
| 🗑️ Clean Old Plugins | - | 清理舊版本插件 |
| 🔍 Check Environment | - | 檢查開發環境 |

---

## ⌨️ 快捷鍵總覽

| 快捷鍵 | 功能 |
|--------|------|
| `Ctrl+Shift+B` | 🚀 **建置並部署**（最常用） |
| `Ctrl+Shift+T` | 🧪 執行測試 |
| `Ctrl+Shift+P` → `Ctrl+Shift+D` | 📦 僅部署 |
| `Ctrl+Shift+P` → `Ctrl+Shift+S` | 🚀 啟動 SonarQube |
| `Ctrl+Shift+P` → `Ctrl+Shift+R` | 🔄 重啟 SonarQube |

---

## 🚀 快速工作流程

### 開發流程
```
1. 修改代碼
2. Ctrl+Shift+B（建置並部署）
3. Ctrl+Shift+P, R（重啟 SonarQube）
4. 訪問 http://localhost:9000 驗證
```

### 僅測試流程
```
1. 修改代碼
2. Ctrl+Shift+T（執行測試）
3. 查看測試結果
```

### 清理流程
```
1. 執行 "Clean All" 任務
2. 執行 "Clean Old Plugins" 任務（可選）
```

---

## 📂 部署路徑

**插件部署位置**: `E:\sonarqube-community-25.10.0.114319\extensions\plugins`

**插件檔案名稱**: `sonar-aiowasp-plugin-*.jar`

---

## 💡 提示

1. **首次建置較慢**：Docker 需下載 Maven 映像和依賴（約 5-10 分鐘）
2. **後續建置快速**：依賴會快取，約 1-2 分鐘
3. **插件未載入**：確保已重啟 SonarQube
4. **查看詳細日誌**：執行 "View Logs" 任務

---

**總任務數**: 12 個（精簡版，只顯示 Workspace 任務）
**最常用**: 🚀 Build & Deploy to SonarQube（Ctrl+Shift+B）
