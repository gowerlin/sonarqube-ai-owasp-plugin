# SonarQube AI OWASP Security Plugin - 開發者指南

**版本**: 1.0.0
**最後更新**: 2025-10-20

---

## 專案架構

### 模組結構

```
sonarqube-ai-owasp-plugin/
├── plugin-core/              # 核心外掛程式碼
│   ├── src/main/java/       # Java 原始碼
│   │   └── com/github/sonarqube/plugin/
│   │       ├── AiOwaspPlugin.java           # Plugin 主入口
│   │       ├── ai/                          # AI 整合層
│   │       │   ├── AiProviderFactory.java   # AI Provider 工廠
│   │       │   ├── CliExecutor.java         # CLI 抽象基類
│   │       │   ├── GeminiCliExecutor.java
│   │       │   ├── CopilotCliExecutor.java
│   │       │   └── ClaudeCliExecutor.java
│   │       ├── api/                         # Web Service API
│   │       │   ├── ConfigurationApiController.java
│   │       │   ├── ScanProgressApiController.java
│   │       │   ├── CliStatusApiController.java
│   │       │   ├── OwaspVersionApiController.java
│   │       │   └── PdfReportApiController.java
│   │       ├── cache/                       # 快取管理
│   │       │   └── IntelligentCacheManager.java
│   │       ├── parallel/                    # 並行執行
│   │       │   └── ParallelAnalysisExecutor.java
│   │       ├── incremental/                 # 增量掃描
│   │       │   └── IncrementalScanManager.java
│   │       ├── cost/                        # 成本估算
│   │       │   └── CostEstimator.java
│   │       └── web/                         # Web UI
│   │           └── OwaspReportPageDefinition.java
│   ├── src/main/resources/
│   │   └── static/
│   │       ├── owasp-report.html            # 基礎報告頁面
│   │       └── owasp-report-advanced.html   # React 進階報告
│   └── src/test/java/                       # 測試程式碼
├── docs/                     # 文件
└── pom.xml                   # Maven 配置
```

### 核心類別說明

#### 1. AiOwaspPlugin.java
Plugin 主入口，負責：
- 註冊配置屬性（17 個）
- 註冊 Web Service（5 個）
- 註冊 Web Page（1 個）

#### 2. AI 整合層
- **AiProviderFactory**: 統一管理 API/CLI 模式
- **CliExecutor**: CLI 工具抽象基類
- **具體實作**: Gemini/Copilot/Claude CLI 執行器

#### 3. 效能優化層
- **IntelligentCacheManager**: SHA-256 檔案 hash 快取
- **ParallelAnalysisExecutor**: 並行任務執行
- **IncrementalScanManager**: Git diff 增量掃描

#### 4. API 層
- **ConfigurationApiController**: `/api/owasp/config`
- **CliStatusApiController**: `/api/owasp/cli`
- **ScanProgressApiController**: `/api/owasp/scan`

---

## 開發環境設定

### 必要工具

```bash
# Java 17+
java -version

# Maven 3.8+
mvn -version

# SonarQube 9.9+ (開發測試用)
docker run -d --name sonarqube -p 9000:9000 sonarqube:10-community
```

### 建置專案

```bash
# Clone 專案
git clone https://github.com/gowerlin/sonarqube-ai-owasp-plugin
cd sonarqube-ai-owasp-plugin

# 編譯
mvn clean compile

# 執行測試
mvn test

# 建置 JAR
mvn clean package

# 輸出: plugin-core/target/sonarqube-ai-owasp-plugin-1.0.0.jar
```

### 本地開發測試

```bash
# 複製 JAR 到 SonarQube
cp plugin-core/target/sonarqube-ai-owasp-plugin-1.0.0.jar \
   $SONARQUBE_HOME/extensions/plugins/

# 重啟 SonarQube
$SONARQUBE_HOME/bin/sonar.sh restart

# 查看日誌
tail -f $SONARQUBE_HOME/logs/sonar.log
```

---

## 開發指南

### 新增 AI Provider

1. **建立 Executor 類別**
```java
public class NewAiCliExecutor extends CliExecutor {
    public NewAiCliExecutor(String cliPath, int timeoutSeconds) {
        super(cliPath, timeoutSeconds);
    }

    @Override
    public String analyze(String prompt, String codeSnippet)
            throws CliExecutionException {
        // 實作分析邏輯
    }
}
```

2. **註冊到 Factory**
```java
// AiProviderFactory.java
public enum ProviderType {
    // 新增
    NEW_AI_CLI("new-ai-cli");
}

private static CliExecutor createCliExecutor(ProviderConfig config) {
    switch (config.getType()) {
        case NEW_AI_CLI:
            return new NewAiCliExecutor(...);
    }
}
```

3. **更新 Plugin 配置**
```java
// AiOwaspPlugin.java
context.addExtension(
    PropertyDefinition.builder("sonar.aiowasp.cli.newai.path")
        .name("New AI CLI Path")
        .defaultValue("/usr/local/bin/newai")
        .build()
);
```

### 新增 Web Service

```java
public class MyApiController implements WebService {
    @Override
    public void define(Context context) {
        NewController controller = context.createController("api/owasp/myapi")
            .setDescription("My Custom API")
            .setSince("1.1.0");

        controller.createAction("action")
            .setDescription("Action description")
            .setHandler(new MyHandler())
            .createParam("param")
            .setRequired(true);

        controller.done();
    }

    private class MyHandler implements RequestHandler {
        @Override
        public void handle(Request request, Response response) {
            // 處理邏輯
        }
    }
}
```

### 撰寫測試

#### Unit Test
```java
@Test
@DisplayName("測試功能描述")
void testFeature() {
    // Given
    MyClass instance = new MyClass();

    // When
    String result = instance.method();

    // Then
    assertEquals("expected", result);
}
```

#### Integration Test
```java
@Test
@DisplayName("測試 API 整合")
void testApiIntegration() {
    Plugin.Context context = new Plugin.Context(...);
    MyApiController controller = new MyApiController();

    controller.define(context);

    WebService.Controller apiController = context.controller("api/myapi");
    assertNotNull(apiController);
}
```

---

## 效能優化建議

### 1. 並行處理

```java
// 使用 ParallelAnalysisExecutor
ParallelAnalysisExecutor executor = new ParallelAnalysisExecutor(5, 10);

List<AnalysisTask<String>> tasks = createTasks();
List<String> results = executor.executeParallel(tasks);
```

### 2. 快取策略

```java
// 使用 IntelligentCacheManager
IntelligentCacheManager cache = new IntelligentCacheManager(true, 1000);

String result = cache.get(filePath);
if (result == null) {
    result = performAnalysis(filePath);
    cache.put(filePath, result);
}
```

### 3. 增量掃描

```java
// 使用 IncrementalScanManager
IncrementalScanManager scanner = new IncrementalScanManager();
List<String> changedFiles = scanner.getChangedFiles(projectPath, "HEAD~1");
```

---

## 發布流程

### 版本號規則

遵循 Semantic Versioning 2.0.0：
- **Major**: 不相容的 API 變更
- **Minor**: 新功能，向後相容
- **Patch**: Bug 修復

### 發布檢查清單

- [ ] 所有測試通過 (`mvn test`)
- [ ] 程式碼品質檢查 (`mvn sonar:sonar`)
- [ ] 更新 CHANGELOG.md
- [ ] 更新版本號 (pom.xml)
- [ ] 建立 Git tag
- [ ] 建置發布版本 (`mvn clean package`)
- [ ] 上傳到 GitHub Releases

```bash
# 建立版本 tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# 建置發布版本
mvn clean package -DskipTests

# 上傳 JAR 到 GitHub Releases
gh release create v1.0.0 \
  plugin-core/target/sonarqube-ai-owasp-plugin-1.0.0.jar \
  --title "v1.0.0" \
  --notes "Release notes..."
```

---

## 程式碼風格

### Java 規範

```java
// 類別命名：PascalCase
public class MyFeatureManager { }

// 方法命名：camelCase
public void processData() { }

// 常數命名：UPPER_SNAKE_CASE
public static final String DEFAULT_MODEL = "gpt-4";

// 私有欄位：camelCase with underscore prefix (optional)
private final String apiKey;

// 註解：使用 JavaDoc
/**
 * 分析程式碼片段並返回安全建議
 *
 * @param codeSnippet 要分析的程式碼
 * @return 安全分析結果
 * @throws AnalysisException 當分析失敗時
 */
public String analyze(String codeSnippet) throws AnalysisException {
    // 實作...
}
```

### 日誌規範

```java
private static final Logger LOG = LoggerFactory.getLogger(MyClass.class);

// DEBUG: 詳細除錯資訊
LOG.debug("處理檔案: {}, 大小: {} bytes", filePath, fileSize);

// INFO: 一般資訊
LOG.info("掃描完成: 發現 {} 個問題", issueCount);

// WARN: 警告但不影響執行
LOG.warn("快取命中率低: {}%", hitRate);

// ERROR: 錯誤需要關注
LOG.error("API 呼叫失敗: {}", errorMessage, exception);
```

---

## 貢獻指南

### Pull Request 流程

1. Fork 專案
2. 建立功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交變更 (`git commit -m 'Add amazing feature'`)
4. 推送分支 (`git push origin feature/amazing-feature`)
5. 開啟 Pull Request

### Commit Message 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type**:
- `feat`: 新功能
- `fix`: Bug 修復
- `docs`: 文件變更
- `style`: 程式碼格式（不影響功能）
- `refactor`: 重構
- `test`: 測試相關
- `chore`: 建置或輔助工具

**範例**:
```
feat(ai): add support for Gemini 1.5 Flash model

- Add Gemini Flash to supported models list
- Update cost calculator with Flash pricing
- Add configuration option for Flash model

Closes #123
```

---

## 授權

MIT License - 詳見 LICENSE 檔案

---

## 聯絡方式

- **GitHub Issues**: https://github.com/gowerlin/sonarqube-ai-owasp-plugin/issues
- **GitHub**: https://github.com/gowerlin
