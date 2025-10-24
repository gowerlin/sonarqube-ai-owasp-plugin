# OWASP 版本切換功能完整實作指南

## 📌 概述

本指南提供完整的 OWASP Top 10 多版本支持實作步驟（2017, 2021, 2025 Preview）。

---

## 1️⃣ 修改 API Controller

**檔案**: `plugin-core/src/main/java/com/github/sonarqube/plugin/api/PdfReportApiController.java`

### 1.1 添加版本參數常量

**位置**: 第 48 行之後

```java
private static final String PARAM_VERSION = "version";
```

### 1.2 在 define() 方法中添加版本參數定義

**位置**: 第 95 行之後（exportAction.createParam(PARAM_PROJECT) 之後）

```java
exportAction.createParam(PARAM_VERSION)
        .setDescription("OWASP Top 10 version (2017, 2021, or 2025)")
        .setRequired(false)
        .setDefaultValue("2021")
        .setPossibleValues("2017", "2021", "2025")
        .setExampleValue("2021");
```

### 1.3 修改 handleExportRequest() 方法

**替換**: 第 108-112 行

```java
// 修改前
private void handleExportRequest(Request request, Response response) {
    String format = request.mandatoryParam(PARAM_FORMAT);
    String projectKey = request.mandatoryParam(PARAM_PROJECT);

    LOG.info("Export request received: format={}, project={}", format, projectKey);

// 修改後
private void handleExportRequest(Request request, Response response) {
    String format = request.mandatoryParam(PARAM_FORMAT);
    String projectKey = request.mandatoryParam(PARAM_PROJECT);
    String owaspVersion = request.param(PARAM_VERSION) != null ?
                          request.param(PARAM_VERSION) : "2021";

    LOG.info("Export request received: format={}, project={}, owaspVersion={}",
             format, projectKey, owaspVersion);
```

### 1.4 修改所有 export 方法調用

**替換**: 第 116-127 行

```java
// 修改前
switch (format.toLowerCase()) {
    case "pdf":
        exportPdfReport(request, response, projectKey);
        break;
    case "html":
        exportHtmlReport(request, response, projectKey);
        break;
    case "json":
        exportJsonReport(request, response, projectKey);
        break;
    case "markdown":
        exportMarkdownReport(request, response, projectKey);
        break;

// 修改後
switch (format.toLowerCase()) {
    case "pdf":
        exportPdfReport(request, response, projectKey, owaspVersion);
        break;
    case "html":
        exportHtmlReport(request, response, projectKey, owaspVersion);
        break;
    case "json":
        exportJsonReport(request, response, projectKey, owaspVersion);
        break;
    case "markdown":
        exportMarkdownReport(request, response, projectKey, owaspVersion);
        break;
```

### 1.5 修改 exportPdfReport() 方法簽名和調用

**替換**: 第 152-156 行

```java
// 修改前
private void exportPdfReport(Request request, Response response, String projectKey) throws IOException {
    LOG.info("Generating PDF report for project: {}", projectKey);

    // 從 SonarQube 查詢實際的安全問題數據
    AnalysisReport report = createReportFromSonarQubeData(projectKey);

// 修改後
private void exportPdfReport(Request request, Response response, String projectKey, String owaspVersion) throws IOException {
    LOG.info("Generating PDF report for project: {} (OWASP {})", projectKey, owaspVersion);

    // 從 SonarQube 查詢實際的安全問題數據
    AnalysisReport report = createReportFromSonarQubeData(projectKey, owaspVersion);
```

### 1.6 修改 exportHtmlReport() 方法簽名和調用

**替換**: 第 198-202 行

```java
// 修改前
private void exportHtmlReport(Request request, Response response, String projectKey) throws IOException {
    LOG.info("Generating HTML report for project: {}", projectKey);

    // 從 SonarQube 查詢實際的安全問題數據
    AnalysisReport report = createReportFromSonarQubeData(projectKey);

// 修改後
private void exportHtmlReport(Request request, Response response, String projectKey, String owaspVersion) throws IOException {
    LOG.info("Generating HTML report for project: {} (OWASP {})", projectKey, owaspVersion);

    // 從 SonarQube 查詢實際的安全問題數據
    AnalysisReport report = createReportFromSonarQubeData(projectKey, owaspVersion);
```

### 1.7 修改 exportJsonReport() 方法簽名和調用

**替換**: 第 226-230 行

```java
// 修改前
private void exportJsonReport(Request request, Response response, String projectKey) throws IOException {
    LOG.info("Generating JSON report for project: {}", projectKey);

    // 從 SonarQube 查詢實際的安全問題數據
    AnalysisReport report = createReportFromSonarQubeData(projectKey);

// 修改後
private void exportJsonReport(Request request, Response response, String projectKey, String owaspVersion) throws IOException {
    LOG.info("Generating JSON report for project: {} (OWASP {})", projectKey, owaspVersion);

    // 從 SonarQube 查詢實際的安全問題數據
    AnalysisReport report = createReportFromSonarQubeData(projectKey, owaspVersion);
```

### 1.8 修改 exportMarkdownReport() 方法簽名和調用

**替換**: 第 254-258 行

```java
// 修改前
private void exportMarkdownReport(Request request, Response response, String projectKey) throws IOException {
    LOG.info("Generating Markdown report for project: {}", projectKey);

    // 從 SonarQube 查詢實際的安全問題數據
    AnalysisReport report = createReportFromSonarQubeData(projectKey);

// 修改後
private void exportMarkdownReport(Request request, Response response, String projectKey, String owaspVersion) throws IOException {
    LOG.info("Generating Markdown report for project: {} (OWASP {})", projectKey, owaspVersion);

    // 從 SonarQube 查詢實際的安全問題數據
    AnalysisReport report = createReportFromSonarQubeData(projectKey, owaspVersion);
```

### 1.9 修改 createReportFromSonarQubeData() 方法

**替換**: 第 300-321 行

```java
// 修改前
private AnalysisReport createReportFromSonarQubeData(String projectKey) {
    LOG.info("Retrieving actual analysis data from SonarQube for project: {}", projectKey);

    // 查詢安全問題
    List<SecurityFinding> findings = dataService.getOwaspFindings(projectKey);

    // 計算摘要統計
    ReportSummary summary = dataService.calculateSummary(findings);

    LOG.info("Report created: {} total findings, {} BLOCKER, {} CRITICAL, {} MAJOR",
            summary.getTotalFindings(),
            summary.getBlockerCount(),
            summary.getCriticalCount(),
            summary.getMajorCount());

    return AnalysisReport.builder()
            .projectName(projectKey)
            .owaspVersion("2021")
            .analysisTime(java.time.LocalDateTime.now())
            .findings(findings)
            .summary(summary)
            .build();
}

// 修改後
private AnalysisReport createReportFromSonarQubeData(String projectKey, String owaspVersion) {
    LOG.info("Retrieving actual analysis data from SonarQube for project: {} (OWASP {})",
             projectKey, owaspVersion);

    // 查詢安全問題（使用指定的 OWASP 版本）
    List<SecurityFinding> findings = dataService.getOwaspFindings(projectKey, owaspVersion);

    // 計算摘要統計
    ReportSummary summary = dataService.calculateSummary(findings);

    LOG.info("Report created: {} total findings, {} BLOCKER, {} CRITICAL, {} MAJOR",
            summary.getTotalFindings(),
            summary.getBlockerCount(),
            summary.getCriticalCount(),
            summary.getMajorCount());

    return AnalysisReport.builder()
            .projectName(projectKey)
            .owaspVersion(owaspVersion)  // 使用動態版本
            .analysisTime(java.time.LocalDateTime.now())
            .findings(findings)
            .summary(summary)
            .build();
}
```

---

## 2️⃣ 修改數據服務層

**檔案**: `plugin-core/src/main/java/com/github/sonarqube/plugin/service/SonarQubeDataService.java`

### 2.1 修改 getOwaspFindings() 方法簽名

**替換**: 第 70-100 行

```java
// 修改前
public List<SecurityFinding> getOwaspFindings(String projectKey) {
    LOG.info("Querying OWASP findings for project: {}", projectKey);

// 修改後
public List<SecurityFinding> getOwaspFindings(String projectKey, String owaspVersion) {
    LOG.info("Querying OWASP findings for project: {} (OWASP {})", projectKey, owaspVersion);
```

### 2.2 修改 mapToSecurityFinding() 方法中的映射邏輯

**替換**: 第 156-169 行

```java
// 修改前
private SecurityFinding mapToSecurityFinding(Issues.Issue issue) {
    try {
        // 提取 tags
        List<String> tags = issue.getTagsList();

        // 映射到 OWASP 分類
        String ruleKey = issue.getRule();
        String owaspCategory = OwaspCategoryMapper.mapToOwaspCategory(tags, ruleKey);

        if (owaspCategory == null) {
            LOG.debug("Issue {} could not be mapped to OWASP category, using tags as fallback",
                    issue.getKey());
            owaspCategory = getDefaultCategoryByType(issue.getType().name());
        }

// 修改後
private SecurityFinding mapToSecurityFinding(Issues.Issue issue, String owaspVersion) {
    try {
        // 提取 tags
        List<String> tags = issue.getTagsList();

        // 映射到 OWASP 分類（使用指定版本）
        String ruleKey = issue.getRule();
        String owaspCategory = OwaspCategoryMapper.mapToOwaspCategory(tags, ruleKey, owaspVersion);

        if (owaspCategory == null) {
            LOG.debug("Issue {} could not be mapped to OWASP category, using tags as fallback",
                    issue.getKey());
            owaspCategory = getDefaultCategoryByType(issue.getType().name(), owaspVersion);
        }
```

### 2.3 修改 getDefaultCategoryByType() 方法

**替換**: 第 201-212 行

```java
// 修改前
private String getDefaultCategoryByType(String type) {
    switch (type) {
        case "VULNERABILITY":
            return "A03:2021-Injection";
        case "SECURITY_HOTSPOT":
            return "A05:2021-Security Misconfiguration";
        case "BUG":
            return "A04:2021-Insecure Design";
        default:
            return "A09:2021-Security Logging and Monitoring Failures";
    }
}

// 修改後
private String getDefaultCategoryByType(String type, String owaspVersion) {
    String prefix = "2021".equals(owaspVersion) ? "A" :
                    "2017".equals(owaspVersion) ? "A" : "A";
    String format = "2021".equals(owaspVersion) ? "%s%02d:2021-%s" :
                    "2017".equals(owaspVersion) ? "%s%d:2017-%s" :
                    "%s%02d:2025-%s";

    switch (type) {
        case "VULNERABILITY":
            if ("2017".equals(owaspVersion)) {
                return "A1:2017-Injection";
            } else if ("2025".equals(owaspVersion)) {
                return "A03:2025-Injection";
            }
            return "A03:2021-Injection";
        case "SECURITY_HOTSPOT":
            if ("2017".equals(owaspVersion)) {
                return "A6:2017-Security Misconfiguration";
            } else if ("2025".equals(owaspVersion)) {
                return "A05:2025-Security Misconfiguration";
            }
            return "A05:2021-Security Misconfiguration";
        case "BUG":
            if ("2017".equals(owaspVersion)) {
                return "A10:2017-Insufficient Logging and Monitoring";
            } else if ("2025".equals(owaspVersion)) {
                return "A04:2025-Insecure Design";
            }
            return "A04:2021-Insecure Design";
        default:
            if ("2017".equals(owaspVersion)) {
                return "A10:2017-Insufficient Logging and Monitoring";
            } else if ("2025".equals(owaspVersion)) {
                return "A09:2025-Security Logging and Monitoring Failures";
            }
            return "A09:2021-Security Logging and Monitoring Failures";
    }
}
```

### 2.4 修改映射調用以傳遞版本

**找到**: 第 87-91 行的 stream 調用

**替換**:

```java
// 修改前
List<SecurityFinding> findings = response.getIssuesList().stream()
        .filter(this::isSecurityRelated)
        .map(this::mapToSecurityFinding)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());

// 修改後
final String version = owaspVersion;  // 為 lambda 捕獲
List<SecurityFinding> findings = response.getIssuesList().stream()
        .filter(this::isSecurityRelated)
        .map(issue -> mapToSecurityFinding(issue, version))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
```

---

## 3️⃣ 創建或修改 OWASP 類別映射器

**檔案**: `plugin-core/src/main/java/com/github/sonarqube/plugin/service/OwaspCategoryMapper.java`

### 3.1 修改 mapToOwaspCategory() 方法簽名

**如果文件已存在，修改方法簽名添加 version 參數**:

```java
// 修改前
public static String mapToOwaspCategory(List<String> tags, String ruleKey)

// 修改後
public static String mapToOwaspCategory(List<String> tags, String ruleKey, String owaspVersion)
```

### 3.2 在方法內部添加版本分支邏輯

**添加版本檢查邏輯**:

```java
// 根據版本返回不同的映射
if ("2017".equals(owaspVersion)) {
    return mapTo2017Category(tags, ruleKey);
} else if ("2025".equals(owaspVersion)) {
    return mapTo2025Category(tags, ruleKey);
} else {
    return mapTo2021Category(tags, ruleKey);  // 預設 2021
}
```

### 3.3 創建各版本的映射方法

**添加三個映射方法** (如果不存在):

```java
private static String mapTo2017Category(List<String> tags, String ruleKey) {
    // OWASP Top 10 2017 映射邏輯
    // A1:2017-Injection
    // A2:2017-Broken Authentication
    // A3:2017-Sensitive Data Exposure
    // A4:2017-XML External Entities (XXE)
    // A5:2017-Broken Access Control
    // A6:2017-Security Misconfiguration
    // A7:2017-Cross-Site Scripting (XSS)
    // A8:2017-Insecure Deserialization
    // A9:2017-Using Components with Known Vulnerabilities
    // A10:2017-Insufficient Logging and Monitoring

    String lowerRuleKey = ruleKey.toLowerCase();

    if (lowerRuleKey.contains("injection") || lowerRuleKey.contains("sql")) {
        return "A1:2017-Injection";
    }
    if (lowerRuleKey.contains("auth") || lowerRuleKey.contains("session")) {
        return "A2:2017-Broken Authentication";
    }
    if (lowerRuleKey.contains("crypto") || lowerRuleKey.contains("encrypt")) {
        return "A3:2017-Sensitive Data Exposure";
    }
    if (lowerRuleKey.contains("xxe") || lowerRuleKey.contains("xml")) {
        return "A4:2017-XML External Entities (XXE)";
    }
    if (lowerRuleKey.contains("access") || lowerRuleKey.contains("permission")) {
        return "A5:2017-Broken Access Control";
    }
    if (lowerRuleKey.contains("config") || lowerRuleKey.contains("setting")) {
        return "A6:2017-Security Misconfiguration";
    }
    if (lowerRuleKey.contains("xss") || lowerRuleKey.contains("script")) {
        return "A7:2017-Cross-Site Scripting (XSS)";
    }
    if (lowerRuleKey.contains("deserial")) {
        return "A8:2017-Insecure Deserialization";
    }
    if (lowerRuleKey.contains("component") || lowerRuleKey.contains("dependency")) {
        return "A9:2017-Using Components with Known Vulnerabilities";
    }

    return "A10:2017-Insufficient Logging and Monitoring";
}

private static String mapTo2021Category(List<String> tags, String ruleKey) {
    // OWASP Top 10 2021 映射邏輯（保持原有邏輯）
    String lowerRuleKey = ruleKey.toLowerCase();

    if (lowerRuleKey.contains("access") || lowerRuleKey.contains("permission")) {
        return "A01:2021-Broken Access Control";
    }
    if (lowerRuleKey.contains("crypto") || lowerRuleKey.contains("encrypt")) {
        return "A02:2021-Cryptographic Failures";
    }
    if (lowerRuleKey.contains("injection") || lowerRuleKey.contains("sql")) {
        return "A03:2021-Injection";
    }
    if (lowerRuleKey.contains("design") || lowerRuleKey.contains("architecture")) {
        return "A04:2021-Insecure Design";
    }
    if (lowerRuleKey.contains("config") || lowerRuleKey.contains("setting")) {
        return "A05:2021-Security Misconfiguration";
    }
    if (lowerRuleKey.contains("component") || lowerRuleKey.contains("dependency")) {
        return "A06:2021-Vulnerable and Outdated Components";
    }
    if (lowerRuleKey.contains("auth") || lowerRuleKey.contains("session")) {
        return "A07:2021-Identification and Authentication Failures";
    }
    if (lowerRuleKey.contains("integrity") || lowerRuleKey.contains("deserialization")) {
        return "A08:2021-Software and Data Integrity Failures";
    }
    if (lowerRuleKey.contains("log") || lowerRuleKey.contains("monitor")) {
        return "A09:2021-Security Logging and Monitoring Failures";
    }
    if (lowerRuleKey.contains("ssrf") || lowerRuleKey.contains("request")) {
        return "A10:2021-Server-Side Request Forgery (SSRF)";
    }

    return null;  // 無法映射
}

private static String mapTo2025Category(List<String> tags, String ruleKey) {
    // OWASP Top 10 2025 Preview 映射邏輯
    // A10 改為 "Insecure Use of AI"

    String lowerRuleKey = ruleKey.toLowerCase();

    // A01-A09 與 2021 相同
    if (lowerRuleKey.contains("access") || lowerRuleKey.contains("permission")) {
        return "A01:2025-Broken Access Control";
    }
    if (lowerRuleKey.contains("crypto") || lowerRuleKey.contains("encrypt")) {
        return "A02:2025-Cryptographic Failures";
    }
    if (lowerRuleKey.contains("injection") || lowerRuleKey.contains("sql")) {
        return "A03:2025-Injection";
    }
    if (lowerRuleKey.contains("design") || lowerRuleKey.contains("architecture")) {
        return "A04:2025-Insecure Design";
    }
    if (lowerRuleKey.contains("config") || lowerRuleKey.contains("setting")) {
        return "A05:2025-Security Misconfiguration";
    }
    if (lowerRuleKey.contains("component") || lowerRuleKey.contains("dependency")) {
        return "A06:2025-Vulnerable and Outdated Components";
    }
    if (lowerRuleKey.contains("auth") || lowerRuleKey.contains("session")) {
        return "A07:2025-Identification and Authentication Failures";
    }
    if (lowerRuleKey.contains("integrity") || lowerRuleKey.contains("deserialization")) {
        return "A08:2025-Software and Data Integrity Failures";
    }
    if (lowerRuleKey.contains("log") || lowerRuleKey.contains("monitor")) {
        return "A09:2025-Security Logging and Monitoring Failures";
    }

    // A10 新增：不安全的 AI 使用
    if (lowerRuleKey.contains("ai") || lowerRuleKey.contains("ml") ||
        lowerRuleKey.contains("model") || lowerRuleKey.contains("llm")) {
        return "A10:2025-Insecure Use of AI";
    }

    return null;  // 無法映射
}
```

---

## 4️⃣ 前端 UI 修改

**檔案**: `plugin-core/src/main/resources/static/report.html`

### 4.1 添加版本選擇器 CSS（在 `/* Filter Panel */` 之前）

```css
/* Version Selector */
.version-selector {
    background: white;
    padding: 20px 25px;
    border-radius: 8px;
    margin-bottom: 20px;
    box-shadow: 0 2px 4px rgba(0,0,0,0.08);
    display: flex;
    align-items: center;
    gap: 15px;
}

.version-selector label {
    font-weight: 600;
    color: #444;
    font-size: 1.1rem;
}

.version-selector select {
    padding: 10px 15px;
    border: 2px solid #667eea;
    border-radius: 6px;
    background: white;
    color: #333;
    font-size: 1rem;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s;
    min-width: 200px;
}

.version-selector select:hover {
    border-color: #764ba2;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2);
}

.version-selector select:focus {
    outline: none;
    border-color: #764ba2;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}
```

### 4.2 添加版本選擇器 HTML（在 `<!-- Filter Panel -->` 之前）

```html
<!-- OWASP Version Selector -->
<div class="version-selector">
    <label for="owaspVersionSelect">🔄 OWASP 版本:</label>
    <select id="owaspVersionSelect">
        <option value="2017">OWASP Top 10 2017</option>
        <option value="2021" selected>OWASP Top 10 2021</option>
        <option value="2025">OWASP Top 10 2025 Preview</option>
    </select>
</div>
```

### 4.3 添加 JavaScript 版本定義（在 `// Global state` 之後）

```javascript
// OWASP 版本定義
const OWASP_VERSIONS = {
    '2017': {
        'A01': 'A1: 注入攻擊',
        'A02': 'A2: 無效的身份驗證',
        'A03': 'A3: 敏感數據洩露',
        'A04': 'A4: XML 外部實體 (XXE)',
        'A05': 'A5: 無效的存取控制',
        'A06': 'A6: 安全設定錯誤',
        'A07': 'A7: 跨站腳本 (XSS)',
        'A08': 'A8: 不安全的反序列化',
        'A09': 'A9: 使用已知漏洞的元件',
        'A10': 'A10: 記錄與監控不足'
    },
    '2021': {
        'A01': 'A01: 存取控制失效',
        'A02': 'A02: 加密機制失效',
        'A03': 'A03: 注入攻擊',
        'A04': 'A04: 不安全設計',
        'A05': 'A05: 安全設定缺陷',
        'A06': 'A06: 危險或過時的元件',
        'A07': 'A07: 身份識別及驗證失效',
        'A08': 'A08: 軟體及資料完整性失效',
        'A09': 'A09: 安全記錄及監控失效',
        'A10': 'A10: 伺服器端請求偽造 (SSRF)'
    },
    '2025': {
        'A01': 'A01: 存取控制失效',
        'A02': 'A02: 加密機制失效',
        'A03': 'A03: 注入攻擊',
        'A04': 'A04: 不安全設計',
        'A05': 'A05: 安全設定缺陷',
        'A06': 'A06: 危險或過時的元件',
        'A07': 'A07: 身份識別及驗證失效',
        'A08': 'A08: 軟體及資料完整性失效',
        'A09': 'A09: 安全記錄及監控失效',
        'A10': 'A10: 不安全的 AI 使用'
    }
};

// 當前選擇的 OWASP 版本
let currentOwaspVersion = '2021';

// 更新 OWASP 分類選項
function updateOwaspCategoryOptions() {
    const owaspFilter = document.getElementById('owaspFilter');
    const categories = OWASP_VERSIONS[currentOwaspVersion];

    const currentValue = owaspFilter.value;
    owaspFilter.innerHTML = '<option value="">所有分類</option>';

    Object.entries(categories).forEach(([key, label]) => {
        const option = document.createElement('option');
        option.value = key;
        option.textContent = label;
        owaspFilter.appendChild(option);
    });

    if (currentValue && categories[currentValue]) {
        owaspFilter.value = currentValue;
    }

    console.log(`[OWASP Report] Updated category options for version ${currentOwaspVersion}`);
}
```

### 4.4 添加版本切換事件監聽器（在 `DOMContentLoaded` 事件中）

```javascript
// 版本切換事件監聽器
document.addEventListener('DOMContentLoaded', function() {
    const versionSelect = document.getElementById('owaspVersionSelect');

    versionSelect.addEventListener('change', function() {
        currentOwaspVersion = this.value;
        console.log(`[OWASP Report] Switched to OWASP Top 10 ${currentOwaspVersion}`);

        // 更新分類選項
        updateOwaspCategoryOptions();

        // 重新載入報告
        loadReport();
    });

    // 初始化分類選項
    updateOwaspCategoryOptions();
});
```

### 4.5 修改 downloadReport() 函數（添加版本參數）

```javascript
// 修改前
const downloadUrl = `/api/owasp/report/export?format=${format}&project=${encodeURIComponent(projectKey)}`;

// 修改後
const downloadUrl = `/api/owasp/report/export?format=${format}&project=${encodeURIComponent(projectKey)}&version=${currentOwaspVersion}`;
```

### 4.6 修改 loadReport() 函數中的 fetch URL（添加版本參數）

```javascript
// 修改前
fetch(`/api/owasp/report/export?format=json&project=${encodeURIComponent(projectKey)}`)

// 修改後
fetch(`/api/owasp/report/export?format=json&project=${encodeURIComponent(projectKey)}&version=${currentOwaspVersion}`)
```

---

## 5️⃣ 建置與部署

### 5.1 重新建置插件

```bash
cd D:/ForgejoGit/Security_Plugin_for_SonarQube
mvn clean package -Dmaven.test.skip=true
```

### 5.2 停止 SonarQube

```bash
cd E:/sonarqube-community-25.10.0.114319/bin/windows-x86-64
cmd.exe /c ServiceStop.bat
```

### 5.3 部署新插件

```bash
cp D:/ForgejoGit/Security_Plugin_for_SonarQube/plugin-core/target/sonar-aiowasp-plugin-1.0.0-SNAPSHOT.jar E:/sonarqube-community-25.10.0.114319/extensions/plugins/
```

### 5.4 清除緩存並啟動

```bash
rm -rf E:/sonarqube-community-25.10.0.114319/data/web/deploy/
cd E:/sonarqube-community-25.10.0.114319/bin/windows-x86-64
cmd.exe /c ServiceStart.bat
```

---

## ✅ 驗證清單

完成所有修改後，請驗證以下功能：

1. ✅ API 端點接受 `version` 參數
   ```bash
   curl -u "admin:P@ssw0rd" "http://localhost:9000/api/owasp/report/export?format=json&project=NCCS2.CallCenterWeb.backend&version=2021"
   ```

2. ✅ 前端版本選擇器顯示三個選項（2017, 2021, 2025）

3. ✅ 切換版本時，OWASP 分類選項動態更新

4. ✅ 不同版本的 A10 分類正確：
   - 2017: "A10: 記錄與監控不足"
   - 2021: "A10: 伺服器端請求偽造 (SSRF)"
   - 2025: "A10: 不安全的 AI 使用"

5. ✅ 匯出功能（PDF/HTML/JSON/Markdown）包含正確的版本資訊

6. ✅ 報告中的 owaspVersion 欄位動態顯示選擇的版本

---

## 📊 預期結果

完成實作後，您應該能夠：

1. 在報告頁面選擇 OWASP Top 10 版本（2017/2021/2025）
2. 根據選擇的版本查看不同的分類對應
3. 匯出的報告包含正確的版本資訊
4. 後端 API 支持版本參數並返回對應的數據

---

**實作指南版本**: 1.0
**最後更新**: 2025-10-24
**作者**: Claude Code AI Assistant
