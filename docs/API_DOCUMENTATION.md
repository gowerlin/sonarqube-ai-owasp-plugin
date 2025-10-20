# SonarQube AI OWASP Security Plugin - API 文件

**版本**: 1.0.0
**API 版本**: v1
**最後更新**: 2025-10-20

---

## API 概述

### Base URL

```
http://your-sonarqube-server:9000/api/owasp
```

### 認證

所有 API 請求需要 SonarQube 認證 Token:

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:9000/api/owasp/config
```

### 回應格式

所有 API 回應格式為 JSON:

```json
{
  "success": true,
  "data": { ... },
  "error": null
}
```

---

## Configuration API

### GET /api/owasp/config

取得目前的 AI 配置

**請求**:
```bash
GET /api/owasp/config
```

**回應**:
```json
{
  "provider": "openai",
  "model": "gpt-4",
  "temperature": 0.3,
  "maxTokens": 2000,
  "timeout": 60,
  "owasp2017Enabled": true,
  "owasp2021Enabled": true,
  "owasp2025Enabled": false
}
```

### POST /api/owasp/config/update

更新 AI 配置

**請求**:
```bash
POST /api/owasp/config/update?provider=anthropic&model=claude-3-opus
```

**參數**:
| 參數 | 類型 | 必要 | 說明 |
|------|------|------|------|
| provider | string | 是 | AI 提供商 (openai, anthropic, gemini-api, etc.) |
| apiKey | string | 否 | API 金鑰 |
| model | string | 否 | 模型名稱 |
| temperature | float | 否 | 溫度參數 (0.0-1.0) |
| maxTokens | int | 否 | 最大 token 數 |
| timeout | int | 否 | 超時時間（秒） |

**回應**:
```json
{
  "success": true,
  "message": "Configuration updated successfully"
}
```

### GET /api/owasp/config/validate

驗證目前配置

**請求**:
```bash
GET /api/owasp/config/validate
```

**回應**:
```json
{
  "valid": true,
  "issues": []
}
```

---

## CLI Status API

### GET /api/owasp/cli/status

取得所有 CLI 工具狀態

**請求**:
```bash
GET /api/owasp/cli/status
```

**回應**:
```json
{
  "gemini": {
    "path": "/usr/local/bin/gemini",
    "available": true
  },
  "copilot": {
    "path": "/usr/local/bin/gh",
    "available": true
  },
  "claude": {
    "path": "/usr/local/bin/claude",
    "available": false
  }
}
```

### GET /api/owasp/cli/check

檢查特定 CLI 工具

**請求**:
```bash
GET /api/owasp/cli/check?provider=gemini-cli
```

**參數**:
| 參數 | 類型 | 必要 | 說明 |
|------|------|------|------|
| provider | string | 是 | CLI 提供商 (gemini-cli, copilot-cli, claude-cli) |

**回應**:
```json
{
  "provider": "gemini-cli",
  "path": "/usr/local/bin/gemini",
  "available": true,
  "version": "1.5.0"
}
```

### GET /api/owasp/cli/version

取得 CLI 工具版本

**請求**:
```bash
GET /api/owasp/cli/version?provider=claude-cli
```

**回應**:
```json
{
  "provider": "claude-cli",
  "version": "0.8.2"
}
```

### GET /api/owasp/cli/auth

檢查 CLI 認證狀態

**請求**:
```bash
GET /api/owasp/cli/auth?provider=copilot-cli
```

**回應**:
```json
{
  "provider": "copilot-cli",
  "authStatus": "Authenticated: user@example.com"
}
```

---

## Scan Progress API

### GET /api/owasp/scan/progress

取得目前掃描進度

**請求**:
```bash
GET /api/owasp/scan/progress
```

**回應**:
```json
{
  "scanId": "scan-12345",
  "status": "RUNNING",
  "totalFiles": 150,
  "completedFiles": 75,
  "progressPercent": 50.0,
  "estimatedTimeRemaining": "10 minutes",
  "startTime": "2025-10-20T10:00:00Z",
  "currentFile": "src/main/java/MyClass.java"
}
```

**Status 值**:
- `IDLE`: 閒置
- `RUNNING`: 執行中
- `COMPLETED`: 已完成
- `FAILED`: 失敗
- `CANCELLED`: 已取消

### GET /api/owasp/scan/stats

取得掃描統計資訊

**請求**:
```bash
GET /api/owasp/scan/stats
```

**回應**:
```json
{
  "totalIssues": 45,
  "criticalIssues": 5,
  "highIssues": 12,
  "mediumIssues": 18,
  "lowIssues": 10,
  "owaspCategories": {
    "A01-2021": 8,
    "A02-2021": 12,
    "A03-2021": 6
  }
}
```

### POST /api/owasp/scan/start

啟動新的掃描

**請求**:
```bash
POST /api/owasp/scan/start?projectKey=my-project
```

**參數**:
| 參數 | 類型 | 必要 | 說明 |
|------|------|------|------|
| projectKey | string | 是 | 專案識別碼 |
| incremental | boolean | 否 | 是否增量掃描（預設 false） |
| baseline | string | 否 | 增量掃描的 baseline（如 HEAD~1） |

**回應**:
```json
{
  "scanId": "scan-12345",
  "status": "RUNNING",
  "message": "Scan started successfully"
}
```

### POST /api/owasp/scan/cancel

取消執行中的掃描

**請求**:
```bash
POST /api/owasp/scan/cancel?scanId=scan-12345
```

**回應**:
```json
{
  "scanId": "scan-12345",
  "status": "CANCELLED",
  "message": "Scan cancelled successfully"
}
```

### GET /api/owasp/scan/history

取得掃描歷史記錄

**請求**:
```bash
GET /api/owasp/scan/history?projectKey=my-project&page=1&pageSize=10
```

**參數**:
| 參數 | 類型 | 必要 | 說明 |
|------|------|------|------|
| projectKey | string | 是 | 專案識別碼 |
| page | int | 否 | 頁數（預設 1） |
| pageSize | int | 否 | 每頁筆數（預設 10） |

**回應**:
```json
{
  "scans": [
    {
      "scanId": "scan-12345",
      "startTime": "2025-10-20T10:00:00Z",
      "endTime": "2025-10-20T10:30:00Z",
      "durationMs": 1800000,
      "status": "COMPLETED",
      "totalIssues": 45,
      "filesScanned": 150
    }
  ],
  "totalCount": 50,
  "currentPage": 1,
  "pageSize": 10
}
```

---

## OWASP Version API

### GET /api/owasp/versions

取得支援的 OWASP 版本

**請求**:
```bash
GET /api/owasp/versions
```

**回應**:
```json
{
  "versions": [
    {
      "version": "2017",
      "enabled": true,
      "categories": 10
    },
    {
      "version": "2021",
      "enabled": true,
      "categories": 10
    },
    {
      "version": "2025",
      "enabled": false,
      "categories": 10,
      "preview": true
    }
  ]
}
```

---

## PDF Report API

### GET /api/owasp/report/pdf

匯出 PDF 報告

**請求**:
```bash
GET /api/owasp/report/pdf?projectKey=my-project&scanId=scan-12345
```

**參數**:
| 參數 | 類型 | 必要 | 說明 |
|------|------|------|------|
| projectKey | string | 是 | 專案識別碼 |
| scanId | string | 否 | 掃描 ID（預設最新） |
| includeSourceCode | boolean | 否 | 是否包含原始碼（預設 false） |

**回應**: PDF 檔案（Content-Type: application/pdf）

### GET /api/owasp/report/export

匯出報告（多種格式）

**請求**:
```bash
GET /api/owasp/report/export?format=json&projectKey=my-project
```

**參數**:
| 參數 | 類型 | 必要 | 說明 |
|------|------|------|------|
| format | string | 是 | 格式 (html, json, markdown) |
| projectKey | string | 是 | 專案識別碼 |
| scanId | string | 否 | 掃描 ID（預設最新） |

**回應**: 根據 format 參數返回對應格式的報告

---

## Cost Tracking API

### GET /api/owasp/cost/stats

取得成本統計

**請求**:
```bash
GET /api/owasp/cost/stats
```

**回應**:
```json
{
  "totalCost": 2.45,
  "totalApiCalls": 150,
  "totalInputTokens": 45000,
  "totalOutputTokens": 30000,
  "budgetUsagePercent": 24.5,
  "estimatedMonthlyCost": 73.50,
  "costByProvider": {
    "openai": 1.80,
    "anthropic": 0.65
  }
}
```

### POST /api/owasp/cost/reset

重置成本統計

**請求**:
```bash
POST /api/owasp/cost/reset
```

**回應**:
```json
{
  "success": true,
  "message": "Cost statistics reset successfully"
}
```

---

## Cache Management API

### POST /api/owasp/cache/clear

清除快取

**請求**:
```bash
POST /api/owasp/cache/clear
```

**回應**:
```json
{
  "success": true,
  "itemsCleared": 1250,
  "message": "Cache cleared successfully"
}
```

### GET /api/owasp/cache/stats

取得快取統計

**請求**:
```bash
GET /api/owasp/cache/stats
```

**回應**:
```json
{
  "cacheSize": 1000,
  "cacheHits": 8500,
  "cacheMisses": 1500,
  "hitRate": 85.0,
  "timeSavedSeconds": 8500
}
```

---

## 錯誤處理

### 錯誤回應格式

```json
{
  "success": false,
  "error": {
    "code": "INVALID_PARAMETER",
    "message": "Invalid provider value",
    "details": "Provider must be one of: openai, anthropic, gemini-api"
  }
}
```

### HTTP 狀態碼

| 狀態碼 | 說明 |
|--------|------|
| 200 | 成功 |
| 201 | 已建立 |
| 202 | 已接受（異步處理） |
| 400 | 錯誤請求 |
| 401 | 未認證 |
| 403 | 禁止存取 |
| 404 | 找不到資源 |
| 405 | 方法不允許 |
| 500 | 伺服器錯誤 |

### 常見錯誤碼

| 錯誤碼 | 說明 |
|--------|------|
| INVALID_PARAMETER | 參數無效 |
| MISSING_PARAMETER | 缺少必要參數 |
| AUTHENTICATION_FAILED | 認證失敗 |
| API_KEY_INVALID | API Key 無效 |
| CLI_NOT_AVAILABLE | CLI 工具不可用 |
| SCAN_IN_PROGRESS | 掃描進行中 |
| BUDGET_EXCEEDED | 預算超支 |

---

## 使用範例

### cURL 範例

```bash
# 取得配置
curl -H "Authorization: Bearer TOKEN" \
     http://localhost:9000/api/owasp/config

# 更新配置
curl -X POST \
     -H "Authorization: Bearer TOKEN" \
     "http://localhost:9000/api/owasp/config/update?provider=openai&model=gpt-4"

# 啟動掃描
curl -X POST \
     -H "Authorization: Bearer TOKEN" \
     "http://localhost:9000/api/owasp/scan/start?projectKey=my-project"

# 查詢進度
curl -H "Authorization: Bearer TOKEN" \
     http://localhost:9000/api/owasp/scan/progress
```

### JavaScript 範例

```javascript
// 使用 Fetch API
async function getScanProgress() {
  const response = await fetch('http://localhost:9000/api/owasp/scan/progress', {
    headers: {
      'Authorization': 'Bearer YOUR_TOKEN'
    }
  });
  const data = await response.json();
  console.log(data);
}

// 啟動掃描
async function startScan(projectKey) {
  const response = await fetch(
    `http://localhost:9000/api/owasp/scan/start?projectKey=${projectKey}`,
    {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer YOUR_TOKEN'
      }
    }
  );
  return await response.json();
}
```

### Python 範例

```python
import requests

# 配置
BASE_URL = 'http://localhost:9000/api/owasp'
TOKEN = 'YOUR_TOKEN'
HEADERS = {'Authorization': f'Bearer {TOKEN}'}

# 取得配置
response = requests.get(f'{BASE_URL}/config', headers=HEADERS)
config = response.json()

# 啟動掃描
response = requests.post(
    f'{BASE_URL}/scan/start',
    params={'projectKey': 'my-project'},
    headers=HEADERS
)
scan = response.json()

# 查詢進度
response = requests.get(f'{BASE_URL}/scan/progress', headers=HEADERS)
progress = response.json()
print(f"Progress: {progress['progressPercent']}%")
```

---

## Rate Limiting

### 限制規則

| API 類別 | 限制 |
|---------|------|
| 配置管理 | 60 次/分鐘 |
| 掃描控制 | 10 次/分鐘 |
| 狀態查詢 | 120 次/分鐘 |
| 報告匯出 | 30 次/分鐘 |

### 標頭

```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1634567890
```

---

## 版本變更

### v1.0.0 (2025-10-20)
- 初始版本發布
- 支援所有核心 API 端點

---

## 支援

- **GitHub Issues**: https://github.com/your-org/sonarqube-ai-owasp-plugin/issues
- **Email**: api-support@your-org.com
- **文件**: https://docs.your-org.com/sonarqube-ai-owasp

---

**© 2025 SonarQube AI OWASP Plugin Team**
