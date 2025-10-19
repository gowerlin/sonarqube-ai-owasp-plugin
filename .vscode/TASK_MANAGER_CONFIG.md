# Task Manager 延伸套件配置指南

## 問題說明

當使用 VSCode 的 **Task Manager** 延伸套件時，可能會看到來自多個專案的任務混在一起顯示（例如：CMake、go、java、Workspace 等）。

本指南提供幾種方法來篩選顯示，只顯示目前工作區的任務。

---

## 方案 1: 使用單一工作區模式（推薦）

### 步驟

1. **關閉所有 VSCode 視窗**

2. **只開啟本專案資料夾**
   - 檔案 → 開啟資料夾
   - 選擇：`E:\ForgejoGit\Security_Plugin_for_SonarQube`

3. **不要使用多資料夾工作區**
   - 避免使用「將資料夾新增至工作區」功能
   - 這會導致多個專案的任務同時顯示

### 優點
- Task Manager 會自動只顯示目前資料夾的任務
- 最簡單、最直接的方法
- 不需要額外配置

---

## 方案 2: 配置 Task Manager 延伸套件設定

### 步驟

1. **開啟 VSCode 設定**
   - 按 `Ctrl + ,`
   - 或：檔案 → 喜好設定 → 設定

2. **搜尋 "task manager"**

3. **檢查以下設定項目**（依您使用的 Task Manager 延伸套件而定）：
   - `taskManager.exclude`: 排除特定任務
   - `taskManager.includeGlobs`: 只包含特定檔案
   - `taskManager.excludeGlobs`: 排除特定檔案

### 範例設定（新增至 settings.json）

```json
{
    "taskManager.excludeGlobs": [
        "**/CMakeFiles/**",
        "**/go/**",
        "**/java/**"
    ],
    "taskManager.includeGlobs": [
        "**/Security_Plugin_for_SonarQube/.vscode/tasks.json"
    ]
}
```

---

## 方案 3: 使用工作區檔案 (.code-workspace)

如果您必須使用多資料夾工作區，可以建立工作區檔案來管理。

### 步驟

1. **建立工作區檔案**
   - 檔案 → 將工作區另存為...
   - 儲存為：`SonarQube-Plugin.code-workspace`

2. **編輯工作區檔案**，只包含本專案：

```json
{
    "folders": [
        {
            "name": "SonarQube AI OWASP Plugin",
            "path": "E:\\ForgejoGit\\Security_Plugin_for_SonarQube"
        }
    ],
    "settings": {
        "taskManager.exclude": [
            "CMake",
            "go",
            "java"
        ]
    }
}
```

3. **開啟工作區檔案**
   - 檔案 → 開啟工作區...
   - 選擇剛建立的 `.code-workspace` 檔案

---

## 方案 4: 手動篩選 Task Manager 顯示

某些 Task Manager 延伸套件提供 UI 篩選功能。

### 步驟

1. **開啟 Task Manager 面板**

2. **尋找篩選或設定按鈕**（通常在面板右上角）

3. **選擇篩選選項**：
   - 只顯示目前工作區
   - 隱藏特定專案
   - 按資料夾分組

---

## 驗證您使用的 Task Manager 延伸套件

若要提供更精確的設定指引，請確認您使用的是哪一個 Task Manager 延伸套件：

### 常見的 Task Manager 延伸套件

1. **Task Explorer by Scott Meesseman**
   - 延伸套件 ID: `spmeesseman.vscode-taskexplorer`

2. **Tasks by actboy168**
   - 延伸套件 ID: `actboy168.tasks`

3. **Task Manager by cnshenj**
   - 延伸套件 ID: `cnshenj.vscode-task-manager`

### 查看已安裝的延伸套件

```
按 Ctrl+Shift+X → 搜尋 "task manager" → 查看已安裝的延伸套件
```

---

## 推薦作法

✅ **建議使用方案 1**（單一工作區模式）
- 最簡單、最可靠
- 不需要額外配置
- Task Manager 會自動只顯示目前資料夾的任務

⚠️ **避免**
- 同時開啟多個專案資料夾
- 使用多資料夾工作區（除非必要）

---

## 疑難排解

### 問題：關閉其他專案後，Task Manager 仍顯示其他任務

**解決方法**：
1. 完全關閉 VSCode
2. 清除工作區快取：刪除 `%APPDATA%\Code\User\workspaceStorage\` 中的舊快取
3. 重新開啟專案資料夾

### 問題：無法篩選特定任務

**解決方法**：
1. 確認 Task Manager 延伸套件版本是否最新
2. 查看延伸套件的官方文件
3. 嘗試重新安裝延伸套件

---

**最後更新**: 2025-10-20

