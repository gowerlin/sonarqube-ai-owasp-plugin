# 貢獻指南

感謝您考慮為 **SonarQube AI OWASP Security Plugin** 貢獻！

---

## 🌟 貢獻方式

### 報告問題（Bug Reports）
1. 檢查 [GitHub Issues](https://github.com/your-org/sonarqube-ai-owasp-plugin/issues) 確認問題未被報告
2. 建立新 Issue，包含：
   - **問題描述**：清晰簡潔的說明
   - **重現步驟**：詳細的操作步驟
   - **預期行為 vs 實際行為**
   - **環境資訊**：SonarQube 版本、Java 版本、OS
   - **錯誤訊息**：完整的堆疊追蹤或日誌
   - **截圖**（可選）：UI 問題時特別有用

### 功能建議（Feature Requests）
1. 建立 Issue，標記為 `enhancement`
2. 描述：
   - **使用場景**：什麼情況下需要此功能？
   - **期望行為**：功能應該如何運作？
   - **替代方案**：目前的解決方法是什麼？
   - **優先級**：此功能對您的重要性

### 提交 Pull Request
1. **Fork 專案**
2. **建立分支**：
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **編寫代碼**：遵循代碼規範（見下方）
4. **編寫測試**：確保測試覆蓋率 ≥ 80%
5. **提交變更**：使用 Conventional Commits 格式
6. **推送分支**：
   ```bash
   git push origin feature/your-feature-name
   ```
7. **建立 Pull Request**

---

## 📝 代碼規範

### Java 代碼風格
- 遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 使用 4 空格縮排（不使用 Tab）
- 每行最多 120 字元
- 類別、方法必須有 Javadoc 註解

**範例**：
```java
/**
 * AI 模型連接器介面
 *
 * @since 1.0.0
 */
public interface AiModelProvider {
    /**
     * 分析代碼並檢測 OWASP 漏洞
     *
     * @param codeContext 代碼上下文
     * @param owaspVersion OWASP 版本
     * @return AI 分析結果
     * @throws AiAnalysisException 分析失敗時拋出
     */
    AiAnalysisResult analyzeCode(CodeContext codeContext, OwaspVersion owaspVersion)
        throws AiAnalysisException;
}
```

### Commit 訊息格式
使用 [Conventional Commits](https://www.conventionalcommits.org/) 格式：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 類型**：
- `feat`: 新功能
- `fix`: 錯誤修復
- `docs`: 文件變更
- `style`: 代碼格式（不影響功能）
- `refactor`: 重構（既非新功能也非修復）
- `test`: 測試相關
- `chore`: 建構流程或輔助工具

**Scope 範圍**：
- `plugin-core`: 插件核心
- `ai-connector`: AI 連接器
- `rules-engine`: 規則引擎
- `report-generator`: 報告生成
- `version-manager`: 版本管理
- `config-manager`: 配置管理
- `shared-utils`: 共用工具

**範例**：
```
feat(ai-connector): add Claude 3 Opus support

- Implement Claude API client
- Add configuration properties
- Update AI provider factory
- Add unit tests for Claude integration

Closes #123
```

---

## 🧪 測試要求

### 單元測試
- 所有新代碼必須包含單元測試
- 測試覆蓋率 ≥ 80%
- 使用 **JUnit 5** 和 **Mockito**

```java
@Test
void testAiProviderSelection() {
    // Given
    ConfigurationService config = mock(ConfigurationService.class);
    when(config.getAiProvider()).thenReturn("openai");

    AiProviderFactory factory = new AiProviderFactory(config);

    // When
    AiModelProvider provider = factory.createProvider();

    // Then
    assertThat(provider).isInstanceOf(OpenAiProvider.class);
}
```

### 整合測試
- 使用 **Testcontainers** 測試與 SonarQube 的整合
- 測試覆蓋率 ≥ 70%

```java
@Testcontainers
class SonarQubeIntegrationTest {
    @Container
    static SonarQubeContainer sonarqube = new SonarQubeContainer("sonarqube:9.9-community");

    @Test
    void testPluginLoadsSuccessfully() {
        // 測試插件可成功載入
    }
}
```

### 執行測試
```bash
# 單元測試
mvn test

# 整合測試
mvn verify -Pintegration-tests

# 覆蓋率報告
mvn jacoco:report
open target/site/jacoco/index.html
```

---

## 🏗️ 開發環境設置

### 使用 Docker（推薦）
```bash
# 1. Clone 專案
git clone https://github.com/your-org/sonarqube-ai-owasp-plugin.git
cd sonarqube-ai-owasp-plugin

# 2. 啟動開發環境
make start

# 3. 編譯專案
make build

# 4. 執行測試
make test
```

### 手動設置
**系統需求**：
- Java 11+ (建議 OpenJDK 17)
- Maven 3.8+
- Docker & Docker Compose（用於本地 SonarQube）

**步驟**：
```bash
# 1. 安裝 Java 11+
java -version  # 確認版本

# 2. 安裝 Maven 3.8+
mvn -version  # 確認版本

# 3. Clone 專案
git clone https://github.com/your-org/sonarqube-ai-owasp-plugin.git
cd sonarqube-ai-owasp-plugin

# 4. 編譯專案
mvn clean compile

# 5. 執行測試
mvn test

# 6. 啟動本地 SonarQube（Docker）
docker-compose up -d
```

---

## 🔍 代碼審查流程

Pull Request 將經過以下審查：

### 1. 自動化檢查
- ✅ CI 建構成功
- ✅ 測試全部通過
- ✅ 代碼覆蓋率達標（≥ 80%）
- ✅ Checkstyle 通過
- ✅ SpotBugs 無高風險問題

### 2. 人工審查
- 代碼品質與可讀性
- 遵循專案架構模式
- 測試完整性
- 文件更新（README、Javadoc、CHANGELOG）

### 3. 審查時間
- 一般 PR：1-3 個工作日
- 緊急修復：24 小時內

---

## 📋 Pull Request Checklist

提交 PR 前，請確認：

- [ ] 代碼遵循 Java 代碼風格指南
- [ ] 所有測試通過（`mvn test`）
- [ ] 測試覆蓋率 ≥ 80%（`mvn jacoco:report`）
- [ ] Javadoc 註解完整
- [ ] Commit 訊息遵循 Conventional Commits 格式
- [ ] README.md 已更新（如有 API 變更）
- [ ] CHANGELOG.md 已新增條目
- [ ] 無編譯警告
- [ ] 本地測試通過（`make test` 或 `mvn verify`）

---

## 🌐 社群行為準則

我們致力於營造友善、包容的社群環境。參與者應：

- ✅ 尊重不同觀點和經驗
- ✅ 提供建設性反饋
- ✅ 接受批評並專注於最佳實踐
- ✅ 展現同理心和善意

❌ 不接受的行為：
- 騷擾、攻擊性言論
- 發布他人隱私資訊
- 其他不專業或不受歡迎的行為

違反者將被移出專案。

---

## 📞 聯絡方式

- **問題討論**: [GitHub Discussions](https://github.com/your-org/sonarqube-ai-owasp-plugin/discussions)
- **即時聊天**: [Discord](#) 或 [Slack](#)
- **Email**: dev@your-org.com

---

感謝您的貢獻！🎉
