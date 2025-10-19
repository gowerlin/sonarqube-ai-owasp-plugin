# SonarQube AI OWASP Security Plugin

## 專案結構

```
sonarqube-ai-owasp-plugin/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── security/
│   │   │           └── plugin/
│   │   │               ├── AiOwaspSecurityPlugin.java
│   │   │               ├── ai/
│   │   │               │   ├── AICodeReviewService.java
│   │   │               │   ├── AIModelConnector.java
│   │   │               │   └── SecurityAnalysisEngine.java
│   │   │               ├── rules/
│   │   │               │   ├── OWASPRulesDefinition.java
│   │   │               │   ├── owasp2021/
│   │   │               │   │   ├── A01BrokenAccessControl.java
│   │   │               │   │   ├── A02CryptographicFailures.java
│   │   │               │   │   ├── A03Injection.java
│   │   │               │   │   ├── A04InsecureDesign.java
│   │   │               │   │   ├── A05SecurityMisconfiguration.java
│   │   │               │   │   ├── A06VulnerableComponents.java
│   │   │               │   │   ├── A07IdentificationAuth.java
│   │   │               │   │   ├── A08DataIntegrity.java
│   │   │               │   │   ├── A09SecurityLogging.java
│   │   │               │   │   └── A10SSRF.java
│   │   │               │   └── owasp2017/
│   │   │               │       └── [2017版本規則]
│   │   │               ├── sensor/
│   │   │               │   ├── SecuritySensor.java
│   │   │               │   └── AIAnalysisSensor.java
│   │   │               ├── report/
│   │   │               │   ├── OWASPReportGenerator.java
│   │   │               │   ├── RemediationSuggestionService.java
│   │   │               │   └── templates/
│   │   │               │       ├── ReportTemplate.java
│   │   │               │       └── FixExampleGenerator.java
│   │   │               └── metrics/
│   │   │                   ├── SecurityMetrics.java
│   │   │                   └── OWASPComplianceMetrics.java
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── plugin.xml
│   │       ├── rules/
│   │       │   ├── owasp-2021-rules.xml
│   │       │   └── owasp-2017-rules.xml
│   │       └── templates/
│   │           ├── report-template.html
│   │           └── fix-examples/
│   └── test/
│       └── java/
│           └── com/security/plugin/
├── config/
│   ├── ai-config.properties
│   └── sonar-project.properties
└── README.md
```

## 核心組件說明

### 1. 主插件類別 (AiOwaspSecurityPlugin.java)
- 註冊所有擴展點
- 配置 AI 服務
- 載入 OWASP 規則

### 2. AI 整合模組 (/ai)
- **AICodeReviewService**: 執行 AI 程式碼審查
- **AIModelConnector**: 連接 AI 模型 API (支援 OpenAI/Claude/本地模型)
- **SecurityAnalysisEngine**: 安全性分析引擎

### 3. OWASP 規則定義 (/rules)
- 實作 OWASP Top 10 2021 和 2017 版本
- 每個規則包含檢測邏輯和修復建議

### 4. 感測器 (/sensor)
- **SecuritySensor**: 執行靜態程式碼分析
- **AIAnalysisSensor**: 觸發 AI 分析

### 5. 報告產生器 (/report)
- **OWASPReportGenerator**: 產生合規性報告
- **RemediationSuggestionService**: 提供修正建議
- **FixExampleGenerator**: 產生修正範例程式碼

### 6. 度量指標 (/metrics)
- 定義安全性相關指標
- OWASP 合規性分數

## 配置需求

### SonarQube 版本
- 最低版本: 8.9 LTS
- 建議版本: 9.9 LTS 或更新

### AI 模型支援
- OpenAI GPT-4
- Anthropic Claude
- 本地部署模型 (透過 Ollama)

### 相依套件
- SonarQube Plugin API
- AI SDK (OpenAI/Anthropic)
- OWASP Dependency Check
- Apache Commons
- Jackson JSON