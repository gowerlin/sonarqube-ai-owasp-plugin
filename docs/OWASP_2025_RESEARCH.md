# OWASP Top 10 2025 預測規則研究報告

**研究日期**: 2025-10-20
**研究目的**: Epic 6 Story 6.1 - 為 SonarQube AI OWASP Plugin 準備 OWASP 2025 預備版規則
**資料來源**: OWASP 社群預測、安全趨勢分析、產業報告

---

## 📋 執行摘要

**OWASP Top 10:2025 發布時程**:
- **官方發布**: 2025年11月（OWASP Global AppSec Conf in DC）
- **社群調查截止**: 2025年10月3日
- **當前狀態**: 草案階段，社群預測階段

**主要趨勢識別**:
1. **AI/ML 風險**: 生成式 AI 成為新威脅向量（Prompt Injection, Sensitive Data Exposure）
2. **供應鏈攻擊**: 預測 2025 年 45% 組織將遭受軟體供應鏈攻擊
3. **API 安全**: API 成為通用攻擊入口，"API 即攻擊面"
4. **快速漏洞利用**: 25% 漏洞在公開披露後 24 小時內被利用

**與 OWASP 2021 的主要變化**:
- 從個別編碼錯誤 → 系統性應用風險
- 強調 CI/CD、Artifact Signing、供應鏈風險
- API 和資料流成為核心關注點

---

## 🔍 OWASP Top 10 2025 預測類別

基於社群預測、安全趨勢和產業報告，以下是 OWASP Top 10 2025 的預測類別：

### A01:2025 - Broken Access Control ✨ (保持第一)

**預測依據**: OWASP 2021 第一名，持續為最常見漏洞

**核心風險**:
- 未授權資料存取
- 水平/垂直權限提升
- API 端點授權缺失
- CORS 配置錯誤

**新增重點**:
- **API 授權漏洞**: GraphQL, REST API 授權繞過
- **雲端 IAM 錯誤配置**: AWS S3 bucket 公開、Azure RBAC 缺陷
- **微服務授權**: 服務間通信授權缺失

**CWE 映射**:
- CWE-22 (Path Traversal)
- CWE-284 (Improper Access Control)
- CWE-862 (Missing Authorization)
- CWE-863 (Incorrect Authorization)
- CWE-639 (Authorization Bypass)

**檢測規則建議**:
```java
// 檢測缺少授權註解的 API 端點
@GetMapping("/admin/users")  // ❌ 缺少 @PreAuthorize
public List<User> getAllUsers() { ... }

// 檢測不安全的物件參考
String userId = request.getParameter("id");  // ❌ 直接使用使用者輸入
User user = userRepo.findById(userId);
```

---

### A02:2025 - Cryptographic Failures ✨ (保持第二)

**預測依據**: OWASP 2021 第二名，加密失敗仍為關鍵風險

**核心風險**:
- 敏感資料明文傳輸
- 弱加密演算法
- 硬編碼密碼/API Key
- 不安全隨機數生成器

**新增重點**:
- **量子安全加密**: 準備後量子加密演算法（NIST 標準）
- **雲端金鑰管理**: KMS 配置錯誤、金鑰輪替缺失
- **TLS 1.3 強制**: TLS 1.0/1.1/1.2 已過時

**CWE 映射**:
- CWE-327 (Weak Crypto)
- CWE-329 (Not Using Random IV)
- CWE-330 (Insecure Random)
- CWE-331 (Insufficient Entropy)
- CWE-798 (Hardcoded Credentials)

**檢測規則建議**:
```java
// 檢測弱加密演算法
Cipher.getInstance("DES");  // ❌ DES 已過時
MessageDigest.getInstance("MD5");  // ❌ MD5 不安全

// 檢測硬編碼金鑰
String apiKey = "sk-1234567890abcdef";  // ❌ 硬編碼 API Key
```

---

### A03:2025 - Injection ✨ (保持第三)

**預測依據**: OWASP 2021 第三名，注入攻擊持續演變

**核心風險**:
- SQL Injection
- XSS (Cross-Site Scripting)
- Command Injection
- LDAP/XML/NoSQL Injection

**新增重點**:
- **Prompt Injection**: AI/LLM 提示詞注入攻擊（新興威脅）
- **GraphQL Injection**: GraphQL 查詢注入
- **Server-Side Template Injection (SSTI)**

**CWE 映射**:
- CWE-89 (SQL Injection)
- CWE-79 (XSS)
- CWE-78 (OS Command Injection)
- CWE-77 (Command Injection)
- CWE-917 (Expression Language Injection)
- **CWE-1236 (Improper Neutralization of Formula Elements)** - CSV Injection

**檢測規則建議**:
```java
// 檢測 Prompt Injection 風險
String userInput = request.getParameter("query");
String prompt = "You are a helpful assistant. " + userInput;  // ❌ 直接串接使用者輸入
String response = aiService.analyze(prompt);

// 檢測 GraphQL Injection
String query = "{ user(id: \"" + userId + "\") { name } }";  // ❌ 字串串接
```

---

### A04:2025 - Insecure Design ✨ (保持第四)

**預測依據**: OWASP 2021 新增類別，設計缺陷重要性提升

**核心風險**:
- 缺乏威脅建模
- 不安全的業務邏輯
- 缺少速率限制
- 不受限檔案上傳

**新增重點**:
- **AI/ML 模型設計缺陷**: 模型投毒、資料投毒
- **Zero Trust 架構缺失**: 預設信任內部流量
- **Privacy by Design**: GDPR/CCPA 合規設計

**CWE 映射**:
- CWE-73 (External Control of File)
- CWE-434 (Unrestricted File Upload)
- CWE-841 (Improper Enforcement of Behavioral Workflow)
- CWE-1220 (Insufficient Granularity of Data Element)

**檢測規則建議**:
```java
// 檢測缺少速率限制
@PostMapping("/api/login")  // ❌ 缺少 @RateLimit
public LoginResponse login(@RequestBody LoginRequest req) { ... }

// 檢測不受限檔案上傳
if (!file.isEmpty()) {  // ❌ 缺少檔案類型/大小驗證
    file.transferTo(new File(uploadDir + file.getOriginalFilename()));
}
```

---

### A05:2025 - Security Misconfiguration ✨ (保持第五)

**預測依據**: OWASP 2021 第五名，雲端配置錯誤持續增加

**核心風險**:
- 預設帳號/密碼
- 生產環境除錯模式
- 不必要的功能啟用
- 錯誤訊息洩露資訊

**新增重點**:
- **雲端配置錯誤**: AWS S3, Azure Blob 公開存取
- **容器安全**: Docker, Kubernetes 配置錯誤
- **CORS 配置錯誤**: 允許任意來源 `Access-Control-Allow-Origin: *`

**CWE 映射**:
- CWE-2 (Environment)
- CWE-16 (Configuration)
- CWE-209 (Info Exposure via Error)
- CWE-489 (Debug Mode)
- CWE-611 (XXE)

**檢測規則建議**:
```java
// 檢測生產環境除錯模式
@Configuration
public class AppConfig {
    @Value("${spring.profiles.active}")
    private String activeProfile;

    if (activeProfile.equals("prod") && debug == true) {  // ❌ 生產環境啟用除錯
        ...
    }
}
```

---

### A06:2025 - Vulnerable and Outdated Components 🆕 **擴展範圍**

**預測依據**: OWASP 2021 第六名，供應鏈攻擊升級為系統性風險

**核心風險**:
- 過時的相依套件
- 未修補的已知漏洞
- 不安全的第三方程式庫

**新增重點** 🔥:
- **軟體供應鏈攻擊**: 惡意 npm/PyPI 套件、依賴混淆攻擊
- **CI/CD 管道攻擊**: Jenkins, GitHub Actions 配置錯誤
- **Artifact 完整性**: 缺少簽名驗證、SBOM (Software Bill of Materials)
- **開源軟體風險**: Log4Shell 式的廣泛影響漏洞

**CWE 映射**:
- CWE-1035 (2013 Software Development)
- CWE-1104 (Untrusted Pointer Dereference)
- **CWE-1329 (Reliance on Uncontrolled Component)** - 新增
- **CWE-1395 (Dependency on Vulnerable Third-Party Component)** - 新增

**檢測規則建議**:
```xml
<!-- 檢測過時/不穩定相依套件 -->
<dependency>
    <groupId>log4j</groupId>
    <artifactId>log4j</artifactId>
    <version>1.2.17</version>  <!-- ❌ Log4j 1.x 已過時 -->
</dependency>

<dependency>
    <version>3.0.0-SNAPSHOT</version>  <!-- ❌ 不穩定版本 -->
</dependency>
```

---

### A07:2025 - Identity and Authentication Failures ✨ (保持第七)

**預測依據**: OWASP 2021 第七名，認證失敗仍為關鍵弱點

**核心風險**:
- 弱密碼策略
- 缺少 MFA (Multi-Factor Authentication)
- Session 固定攻擊
- 憑證填充攻擊

**新增重點**:
- **Passkey/WebAuthn**: FIDO2 無密碼認證推廣
- **OAuth 2.1/OIDC 安全**: 授權碼流程強制 PKCE
- **生物識別**: 人臉/指紋識別安全性

**CWE 映射**:
- CWE-287 (Authentication)
- CWE-306 (Missing Authentication)
- CWE-384 (Session Fixation)
- CWE-521 (Weak Password)
- CWE-798 (Hardcoded Credentials)

**檢測規則建議**:
```java
// 檢測缺少 MFA
@PostMapping("/login")
public LoginResponse login(@RequestBody LoginRequest req) {
    if (isValidPassword(req)) {
        // ❌ 缺少 MFA 檢查
        return generateToken(req.getUsername());
    }
}
```

---

### A08:2025 - Software and Data Integrity Failures ✨ (保持第八)

**預測依據**: OWASP 2021 第八名，CI/CD 攻擊增加

**核心風險**:
- 不安全的反序列化
- CI/CD 管道未受保護
- 自動更新未驗證

**新增重點**:
- **Artifact 簽名驗證**: 缺少 Sigstore, cosign 驗證
- **SBOM 要求**: 軟體物料清單 (SPDX, CycloneDX)
- **Container Image 簽名**: Docker Content Trust

**CWE 映射**:
- CWE-502 (Deserialization)
- CWE-494 (Download of Code Without Integrity Check)
- CWE-829 (Untrusted Control Sphere)
- CWE-915 (Improperly Controlled Modification)

**檢測規則建議**:
```java
// 檢測不安全的反序列化
ObjectInputStream ois = new ObjectInputStream(inputStream);
Object obj = ois.readObject();  // ❌ 未驗證來源
```

---

### A09:2025 - Security Logging and Monitoring Failures ✨ (保持第九)

**預測依據**: OWASP 2021 第九名，監控缺失導致攻擊延遲發現

**核心風險**:
- 缺少安全事件記錄
- 日誌注入攻擊
- 不足的監控告警

**新增重點**:
- **SIEM 整合**: Splunk, ELK Stack 日誌分析
- **異常行為偵測**: ML-based anomaly detection
- **雲端日誌**: CloudTrail, Azure Monitor

**CWE 映射**:
- CWE-117 (Log Injection)
- CWE-223 (Omission of Security-relevant Information)
- CWE-532 (Info Exposure via Log)
- CWE-778 (Insufficient Logging)

**檢測規則建議**:
```java
// 檢測日誌注入
log.info("User login: " + username);  // ❌ 直接串接使用者輸入

// 檢測缺少安全事件記錄
public void deleteUser(String userId) {
    userRepo.deleteById(userId);  // ❌ 缺少審計日誌
}
```

---

### A10:2025 - Server-Side Request Forgery (SSRF) 🔄 **或** AI/ML Vulnerabilities 🆕

**預測依據**:

**選項 1: SSRF (保守預測)**
- OWASP 2021 第十名，雲端環境 SSRF 風險增加

**選項 2: AI/ML Vulnerabilities (激進預測)** 🔥
- 生成式 AI 快速普及，AI 特有風險成為獨立類別

#### 選項 1: A10:2025 - Server-Side Request Forgery (SSRF)

**核心風險**:
- 伺服器端請求偽造
- 內網探測
- 雲端元資料洩露

**新增重點**:
- **雲端元資料攻擊**: AWS IMDS, Azure Metadata Service
- **Kubernetes API 攻擊**: 未受保護的 K8s API Server

**CWE 映射**:
- CWE-918 (SSRF)

**檢測規則建議**:
```java
// 檢測 SSRF 風險
String url = request.getParameter("imageUrl");
URL resource = new URL(url);  // ❌ 未驗證 URL
InputStream stream = resource.openStream();
```

#### 選項 2: A10:2025 - AI/ML Vulnerabilities 🆕 (激進預測)

**核心風險**:
- Prompt Injection (提示詞注入)
- Training Data Poisoning (訓練資料投毒)
- Model Theft (模型竊取)
- Sensitive Data Exposure via LLM

**來源**: OWASP Top 10 for LLM Applications 2025

**新增重點** 🔥:
- **Prompt Injection**: 使用者輸入繞過系統提示詞
- **Training Data Poisoning**: 惡意資料污染訓練集
- **Model Inversion**: 從模型輸出反推訓練資料
- **Excessive Agency**: LLM 被賦予過多權限（執行系統命令等）

**CWE 映射**:
- **CWE-1236 (Improper Neutralization of Formula Elements)** - Prompt Injection
- **CWE-20 (Improper Input Validation)** - LLM Input
- **CWE-200 (Information Exposure)** - Training Data Leak

**檢測規則建議**:
```java
// 檢測 Prompt Injection 風險
String userInput = request.getParameter("query");
String systemPrompt = "You are a helpful assistant.";
String fullPrompt = systemPrompt + "\n\nUser: " + userInput;  // ❌ 未隔離系統提示詞
String response = llmService.generate(fullPrompt);

// 檢測 LLM 過度授權
@Tool("execute_shell_command")  // ❌ LLM 可執行系統命令
public String executeCommand(String cmd) {
    return Runtime.getRuntime().exec(cmd);
}
```

---

## 📊 OWASP 2025 vs 2021 對照分析

| 2021 類別 | 2025 預測類別 | 變化 | 說明 |
|-----------|---------------|------|------|
| A01 - Broken Access Control | A01 - Broken Access Control | ✨ 保持 | API/雲端授權風險增加 |
| A02 - Cryptographic Failures | A02 - Cryptographic Failures | ✨ 保持 | 量子安全加密、雲端 KMS |
| A03 - Injection | A03 - Injection | ✨ 保持 | 新增 Prompt Injection |
| A04 - Insecure Design | A04 - Insecure Design | ✨ 保持 | AI/ML 設計缺陷、Zero Trust |
| A05 - Security Misconfiguration | A05 - Security Misconfiguration | ✨ 保持 | 雲端/容器配置錯誤 |
| A06 - Vulnerable Components | A06 - Vulnerable Components | 🆕 擴展 | **供應鏈攻擊升級** |
| A07 - Authentication Failures | A07 - Identity & Auth Failures | ✨ 保持 | Passkey/WebAuthn |
| A08 - Data Integrity Failures | A08 - Data Integrity Failures | ✨ 保持 | Artifact 簽名、SBOM |
| A09 - Logging Failures | A09 - Logging Failures | ✨ 保持 | SIEM 整合、異常偵測 |
| A10 - SSRF | A10 - SSRF **或** AI/ML 🔥 | 🔄/🆕 | **爭議：SSRF vs AI/ML** |

**關鍵變化總結**:
1. **A06 擴展**: 從「過時元件」擴展至「軟體供應鏈攻擊」
2. **A10 爭議**: SSRF（保守）vs AI/ML Vulnerabilities（激進）
3. **橫向主題**: AI/ML、雲端、API、供應鏈貫穿所有類別

---

## 🎯 實作建議：OWASP 2025 預備規則集

### 規則優先級

**P0 (高優先級) - 必須實現**:
1. ✅ A01 - Broken Access Control (API/雲端授權)
2. ✅ A03 - Injection (Prompt Injection 🔥)
3. 🆕 A06 - Supply Chain Attacks (惡意相依套件、CI/CD 攻擊)
4. 🆕 A10 - AI/ML Vulnerabilities (Prompt Injection, Training Data Poisoning)

**P1 (中優先級) - 建議實現**:
5. ✅ A02 - Cryptographic Failures (量子安全加密)
6. ✅ A04 - Insecure Design (AI/ML 設計缺陷)
7. ✅ A08 - Data Integrity (Artifact 簽名、SBOM)

**P2 (低優先級) - 可選實現**:
8. ✅ A05 - Security Misconfiguration (雲端配置)
9. ✅ A07 - Authentication Failures (Passkey)
10. ✅ A09 - Logging Failures (SIEM)

### 技術實作路徑

#### 階段 1: 基礎擴展 (2-3 天)
- 複製 OWASP 2021 規則至 `owasp2025` 套件
- 標記所有規則為 `@Preview` 狀態
- 更新 `owaspVersion = "2025"`

#### 階段 2: 核心新增 (4-6 天)
- **Prompt Injection Rule**: 檢測 AI/LLM 提示詞注入風險
- **Supply Chain Rule**: 檢測惡意相依套件、CI/CD 配置錯誤
- **AI/ML Security Rule**: 訓練資料投毒、模型竊取

#### 階段 3: 配置檔更新機制 (2-3 天)
- 建立 `owasp2025-rules.yaml` 配置檔
- 實現熱載入機制（無需重新編譯插件）
- 支援規則啟用/停用、嚴重性調整

---

## 📝 研究結論

### 確定性預測 (高信心度 > 80%)
1. ✅ **A01-A09 保持**: OWASP 2021 前九名類別基本保持，但重點擴展
2. ✅ **供應鏈升級**: A06 從「過時元件」擴展至「軟體供應鏈攻擊」
3. ✅ **AI/ML 橫向整合**: Prompt Injection 納入 A03 Injection

### 不確定性預測 (中信心度 40-60%)
4. 🔄 **A10 爭議**: SSRF (保守) vs AI/ML Vulnerabilities (激進)
   - **保守策略**: 保持 SSRF，將 AI/ML 分散至各類別
   - **激進策略**: 新增 AI/ML 獨立類別（參考 OWASP Top 10 for LLM）

### 實作策略建議
**建議採用「雙軌制」**:
- **主線**: 實現 A01-A09 + A10:SSRF（與 OWASP 2021 相容）
- **Preview 分支**: 實現 A10:AI/ML Vulnerabilities（供激進用戶選用）
- **等待官方發布**: 2025年11月根據官方版本快速調整

---

## 📚 參考資料

1. **OWASP Foundation**: OWASP Top Ten 2025 Community Survey ([https://owasp.org/blog/2025/09/26/Top10Survey](https://owasp.org/blog/2025/09/26/Top10Survey))
2. **Zoonou Blog**: Our predictions for the 2025 OWASP top 10
3. **TCM Security**: OWASP Top 10 Prediction | 2025
4. **Penta Security**: OWASP Top 10 2025 Prediction
5. **ZeroPath Blog**: OWASP 2021 vs 2025: What to Expect
6. **Hadrian Blog**: A look ahead at the OWASP Top Ten for 2025
7. **Avatao**: OWASP Top 10 2025 Preview: Risks, Shifts, and Quick Wins
8. **ResearchGate**: Evolution of Application Security based on OWASP Top 10 and CWE/SANS Top 25 with Predictions for the 2025 OWASP Top 10

---

**文件版本**: 1.0
**建立日期**: 2025-10-20
**作者**: SonarQube AI OWASP Plugin Team (Epic 6 Story 6.1)
**下一步**: Story 6.2 - 實現 OWASP 2025 預備規則集
