# Epic 9: 多元 AI 整合擴充

## Epic 概述

**Epic ID**: Epic 9
**Epic 名稱**: 多元 AI 整合擴充
**優先級**: 高
**預計時程**: 2-3 週
**狀態**: 🚧 進行中

## 業務價值

### 為什麼需要這個功能？

1. **供應商多樣性**: 降低對單一 AI 供應商的依賴風險
2. **成本優化**: 不同 AI 服務有不同定價策略，可依需求選擇
3. **功能互補**: 不同 AI 模型在不同領域有不同優勢
4. **CLI 整合**: 支援本地 CLI 工具，降低 API 成本且提升隱私性

### 預期成果

- 支援 4 種額外的 AI 整合方式
- 統一的 API/CLI 整合架構
- 零中斷升級現有功能
- 完整的文件與測試覆蓋

## 技術目標

### 新增 AI Provider 支援

| Provider | 類型 | 優先級 | 預計工時 |
|----------|------|--------|----------|
| Google Gemini API | API | P0 | 3 天 |
| Google Gemini CLI | CLI | P1 | 2 天 |
| GitHub Copilot CLI | CLI | P1 | 2 天 |
| Claude Code CLI | CLI | P2 | 2 天 |

### 架構改進

1. **雙模式整合架構**
   - API 模式：HTTP REST API 調用
   - CLI 模式：本地命令列工具調用

2. **統一抽象層**
   - `AiService` 介面保持不變
   - 新增 `AiExecutionMode` 枚舉（API/CLI）
   - 新增 `CliExecutor` 介面處理 CLI 調用

3. **配置擴充**
   - 支援 API Key 配置（API 模式）
   - 支援 CLI 路徑配置（CLI 模式）
   - 支援執行模式選擇

## Stories 分解

### Story 9.1: 統一 AI Provider 架構設計 ✅

**描述**: 設計支援 API/CLI 雙模式的統一架構

**驗收標準**:
- [ ] `AiExecutionMode` 枚舉定義（API/CLI）
- [ ] `CliExecutor` 介面設計完成
- [ ] `AiProvider` 枚舉擴充支援 6 種 Provider
- [ ] 架構設計文件完成

**技術設計**:

```java
// AiExecutionMode.java
public enum AiExecutionMode {
    API("api", "API 模式 - HTTP REST API"),
    CLI("cli", "CLI 模式 - 命令列工具");

    private final String code;
    private final String description;
    // ... getters
}

// CliExecutor.java
public interface CliExecutor {
    /**
     * 執行 CLI 命令並獲取回應
     */
    String executeCommand(String[] command, String input) throws IOException;

    /**
     * 測試 CLI 工具是否可用
     */
    boolean isCliAvailable();

    /**
     * 獲取 CLI 工具版本
     */
    String getCliVersion();
}

// 擴充 AiProvider 枚舉
public enum AiProvider {
    OPENAI("openai", "OpenAI", AiExecutionMode.API),
    ANTHROPIC("anthropic", "Anthropic Claude", AiExecutionMode.API),
    GEMINI_API("gemini-api", "Google Gemini API", AiExecutionMode.API),
    GEMINI_CLI("gemini-cli", "Google Gemini CLI", AiExecutionMode.CLI),
    COPILOT_CLI("copilot-cli", "GitHub Copilot CLI", AiExecutionMode.CLI),
    CLAUDE_CLI("claude-cli", "Claude Code CLI", AiExecutionMode.CLI);

    private final String code;
    private final String displayName;
    private final AiExecutionMode executionMode;
    // ... getters
}
```

**預計工時**: 1 天

---

### Story 9.2: Google Gemini API 整合 🚧

**描述**: 實作 Google Gemini API 整合

**相關文件**: https://ai.google.dev/gemini-api/docs?hl=zh-tw

**驗收標準**:
- [ ] `GeminiApiService` 實作完成
- [ ] API 請求/回應模型定義
- [ ] 錯誤處理與重試機制
- [ ] 單元測試覆蓋率 ≥ 80%
- [ ] 整合測試通過

**技術規格**:

```java
// GeminiApiService.java
public class GeminiApiService implements AiService {
    private static final String API_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/";
    private final AiConfig config;
    private final HttpClient httpClient;

    @Override
    public AiResponse analyzeCode(AiRequest request) throws AiException {
        // 1. 建構 Gemini API 請求
        // 2. 發送 HTTP POST 請求
        // 3. 解析回應
        // 4. 轉換為統一 AiResponse 格式
    }

    @Override
    public String getProviderName() {
        return "Google Gemini";
    }

    @Override
    public String getModelName() {
        return config.getModel().getModelId(); // e.g., "gemini-1.5-pro"
    }
}

// GeminiApiRequest.java
public class GeminiApiRequest {
    private Contents contents;
    private GenerationConfig generationConfig;
    private List<SafetySetting> safetySettings;
    // ... standard Gemini API format
}

// GeminiApiResponse.java
public class GeminiApiResponse {
    private List<Candidate> candidates;
    private PromptFeedback promptFeedback;
    // ... standard Gemini API format
}
```

**API 端點**:
- 模型列表: `GET /v1beta/models`
- 生成內容: `POST /v1beta/models/{model}:generateContent`

**支援模型**:
- `gemini-1.5-pro`: 最新專業版
- `gemini-1.5-flash`: 快速版

**預計工時**: 3 天

---

### Story 9.3: CLI 整合框架實作 🔜

**描述**: 實作統一的 CLI 整合框架

**驗收標準**:
- [ ] `AbstractCliExecutor` 抽象基類實作
- [ ] `ProcessCliExecutor` 實作（使用 `ProcessBuilder`）
- [ ] CLI 輸入/輸出處理
- [ ] 錯誤處理與超時控制
- [ ] 單元測試覆蓋率 ≥ 80%

**技術設計**:

```java
// AbstractCliExecutor.java
public abstract class AbstractCliExecutor implements CliExecutor {
    protected final String cliPath;
    protected final int timeoutSeconds;

    @Override
    public String executeCommand(String[] command, String input) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // 寫入輸入
        if (input != null) {
            try (OutputStream os = process.getOutputStream()) {
                os.write(input.getBytes(StandardCharsets.UTF_8));
            }
        }

        // 讀取輸出
        String output = readProcessOutput(process);

        // 等待完成（帶超時）
        boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!completed) {
            process.destroy();
            throw new IOException("CLI execution timeout");
        }

        return output;
    }

    protected abstract String[] buildCommand(String prompt);
}

// ProcessCliExecutor.java
public class ProcessCliExecutor extends AbstractCliExecutor {
    // 實作具體的命令建構邏輯
}
```

**預計工時**: 2 天

---

### Story 9.4: Google Gemini CLI 整合 🔜

**描述**: 實作 Google Gemini CLI 工具整合

**相關文件**: https://github.com/google-gemini/gemini-cli

**驗收標準**:
- [ ] `GeminiCliService` 實作完成
- [ ] CLI 命令參數正確傳遞
- [ ] 輸出解析與轉換
- [ ] 單元測試與整合測試通過

**CLI 使用範例**:

```bash
# 安裝
npm install -g @google/generative-ai-cli

# 使用
gemini chat "分析這段代碼的安全問題: [CODE]"
```

**技術實作**:

```java
public class GeminiCliService implements AiService {
    private final CliExecutor executor;
    private final AiConfig config;

    @Override
    public AiResponse analyzeCode(AiRequest request) throws AiException {
        // 1. 建構 CLI 命令
        String[] command = {
            config.getCliPath(), // gemini
            "chat",
            request.getPrompt()
        };

        // 2. 執行 CLI
        String output = executor.executeCommand(command, null);

        // 3. 解析輸出並轉換為 AiResponse
        return parseCliOutput(output);
    }
}
```

**預計工時**: 2 天

---

### Story 9.5: GitHub Copilot CLI 整合 🔜

**描述**: 實作 GitHub Copilot CLI 工具整合

**相關文件**: https://github.com/github/copilot-cli

**驗收標準**:
- [ ] `CopilotCliService` 實作完成
- [ ] 支援 `gh copilot suggest` 命令
- [ ] 輸出解析與轉換
- [ ] 單元測試與整合測試通過

**CLI 使用範例**:

```bash
# 安裝
gh extension install github/gh-copilot

# 使用
gh copilot suggest "分析這段代碼的安全問題"
```

**技術實作**:

```java
public class CopilotCliService implements AiService {
    private final CliExecutor executor;
    private final AiConfig config;

    @Override
    public AiResponse analyzeCode(AiRequest request) throws AiException {
        // 1. 建構 CLI 命令
        String[] command = {
            "gh", "copilot", "suggest",
            "-t", "security",
            request.getPrompt()
        };

        // 2. 執行 CLI
        String output = executor.executeCommand(command, null);

        // 3. 解析輸出並轉換為 AiResponse
        return parseCliOutput(output);
    }
}
```

**預計工時**: 2 天

---

### Story 9.6: Claude Code CLI 整合 🔜

**描述**: 實作 Claude Code CLI 工具整合

**相關文件**: https://docs.claude.com/en/docs/claude-code/overview

**驗收標準**:
- [ ] `ClaudeCliService` 實作完成
- [ ] 支援 `claude` CLI 命令
- [ ] 輸出解析與轉換
- [ ] 單元測試與整合測試通過

**CLI 使用範例**:

```bash
# 安裝
npm install -g @anthropic-ai/claude-code

# 使用
claude analyze "分析這段代碼的安全問題: [CODE]"
```

**技術實作**:

```java
public class ClaudeCliService implements AiService {
    private final CliExecutor executor;
    private final AiConfig config;

    @Override
    public AiResponse analyzeCode(AiRequest request) throws AiException {
        // 1. 建構 CLI 命令
        String[] command = {
            config.getCliPath(), // claude
            "analyze",
            "--format", "json",
            request.getPrompt()
        };

        // 2. 執行 CLI
        String output = executor.executeCommand(command, null);

        // 3. 解析 JSON 輸出並轉換為 AiResponse
        return parseJsonOutput(output);
    }
}
```

**預計工時**: 2 天

---

### Story 9.7: 配置管理更新 🔜

**描述**: 更新 SonarQube 配置管理支援新的 AI Providers

**驗收標準**:
- [ ] `AiOwaspPlugin` 新增 6 種 Provider 配置屬性
- [ ] 支援 API Key 與 CLI Path 配置
- [ ] 支援執行模式選擇（API/CLI）
- [ ] 配置驗證邏輯更新
- [ ] 文件更新

**配置屬性**:

```java
// 在 AiOwaspPlugin.java 中新增
PropertyDefinition.builder("aiowasp.ai.provider")
    .name("AI Provider")
    .description("選擇 AI 服務提供者")
    .type(PropertyType.SINGLE_SELECT_LIST)
    .options(
        "openai", "anthropic",
        "gemini-api", "gemini-cli",
        "copilot-cli", "claude-cli"
    )
    .defaultValue("openai")
    .build(),

PropertyDefinition.builder("aiowasp.ai.executionMode")
    .name("AI Execution Mode")
    .description("AI 服務執行模式")
    .type(PropertyType.SINGLE_SELECT_LIST)
    .options("api", "cli")
    .defaultValue("api")
    .build(),

PropertyDefinition.builder("aiowasp.ai.gemini.apiKey")
    .name("Google Gemini API Key")
    .description("Gemini API 金鑰")
    .type(PropertyType.PASSWORD)
    .build(),

PropertyDefinition.builder("aiowasp.ai.gemini.cliPath")
    .name("Gemini CLI Path")
    .description("Gemini CLI 工具路徑")
    .defaultValue("/usr/local/bin/gemini")
    .build(),

PropertyDefinition.builder("aiowasp.ai.copilot.cliPath")
    .name("GitHub Copilot CLI Path")
    .description("gh copilot CLI 路徑")
    .defaultValue("/usr/local/bin/gh")
    .build(),

PropertyDefinition.builder("aiowasp.ai.claude.cliPath")
    .name("Claude CLI Path")
    .description("Claude CLI 工具路徑")
    .defaultValue("/usr/local/bin/claude")
    .build()
```

**預計工時**: 1 天

---

### Story 9.8: 整合測試與文件 🔜

**描述**: 完整的整合測試與使用文件

**驗收標準**:
- [ ] 6 種 Provider 整合測試通過
- [ ] API/CLI 模式切換測試
- [ ] 錯誤處理測試
- [ ] 效能測試（CLI vs API）
- [ ] 使用文件更新（README, Architecture）
- [ ] API 文件更新

**測試範圍**:

```java
@Test
void testAllProviders() {
    // 測試所有 6 種 Provider
    for (AiProvider provider : AiProvider.values()) {
        AiConfig config = createConfig(provider);
        AiService service = AiServiceFactory.createService(config);

        assertTrue(service.testConnection());

        AiResponse response = service.analyzeCode(createTestRequest());
        assertNotNull(response);
        assertTrue(response.hasFindings());
    }
}

@Test
void testApiVsCliPerformance() {
    // 比較 API 模式與 CLI 模式的效能差異
}

@Test
void testCliTimeout() {
    // 測試 CLI 超時處理
}
```

**文件更新**:
- README.md: 新增 6 種 Provider 使用說明
- docs/architecture.md: 更新 AI 整合架構圖
- docs/configuration.md: 新增配置範例

**預計工時**: 2 天

---

## 技術風險與緩解措施

### 風險 1: CLI 工具依賴性

**風險描述**: CLI 模式需要使用者手動安裝 CLI 工具

**緩解措施**:
- 提供完整的安裝文件
- 在配置介面提供 CLI 可用性檢查
- 如果 CLI 不可用，自動降級到 API 模式（如果有配置）

### 風險 2: CLI 輸出格式變更

**風險描述**: CLI 工具更新可能改變輸出格式

**緩解措施**:
- 使用版本檢查確保相容性
- 實作彈性的輸出解析器
- 提供格式驗證與錯誤報告

### 風險 3: API 費用成本

**風險描述**: 不同 AI API 有不同計費方式

**緩解措施**:
- 文件中明確說明各 Provider 的費用模型
- 建議使用 CLI 模式降低成本（本地執行）
- 提供快取機制減少 API 調用次數

---

## 測試策略

### 單元測試

- 每個新類別 ≥ 80% 覆蓋率
- Mock HTTP Client 測試 API 模式
- Mock ProcessBuilder 測試 CLI 模式

### 整合測試

- 真實 API 調用測試（需要 API Key）
- 真實 CLI 執行測試（需要安裝 CLI 工具）
- 端到端流程測試

### 效能測試

- API 模式回應時間測試
- CLI 模式回應時間測試
- 並行請求處理測試

---

## 交付標準

### 程式碼品質

- [ ] 所有單元測試通過
- [ ] 所有整合測試通過
- [ ] 程式碼覆蓋率 ≥ 80%
- [ ] SonarQube 品質門檻通過
- [ ] 無 Critical/Blocker 問題

### 文件完整性

- [ ] README.md 更新
- [ ] Architecture.md 更新
- [ ] API 文件生成
- [ ] 配置範例提供

### 功能完整性

- [ ] 6 種 Provider 全部實作並測試通過
- [ ] API/CLI 雙模式正常運作
- [ ] 向後相容（不影響現有 OpenAI/Claude 功能）

---

## 時程規劃

| Week | Stories | 交付物 |
|------|---------|--------|
| Week 1 | 9.1, 9.2 | 架構設計完成、Gemini API 實作 |
| Week 2 | 9.3, 9.4, 9.5 | CLI 框架、Gemini CLI、Copilot CLI |
| Week 3 | 9.6, 9.7, 9.8 | Claude CLI、配置更新、測試文件 |

---

## 驗收標準

### Epic 完成定義 (Definition of Done)

- [x] 所有 8 個 Stories 完成
- [ ] 6 種 AI Provider 全部實作並測試通過
- [ ] API/CLI 雙模式架構完整實現
- [ ] 所有測試通過（單元、整合、效能）
- [ ] 程式碼覆蓋率 ≥ 80%
- [ ] 文件完整更新
- [ ] SonarQube 品質門檻通過
- [ ] 向後相容性驗證通過

---

**建立日期**: 2025-10-20
**最後更新**: 2025-10-20
**Epic Owner**: SonarQube AI OWASP Plugin Team
