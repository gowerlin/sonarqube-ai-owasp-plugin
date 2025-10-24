# Epic 10: AI 應用層增強與修復

**狀態**: Active
**優先級**: High
**建立日期**: 2025-10-24
**預估工作量**: 3-4 週

---

## Epic 概述

### 目標

修復 AI 基礎設施與應用層之間的資訊流失問題，確保 AI 提供的完整安全分析資訊（包含 Before/After 代碼範例、工作量評估）能夠完整傳遞到規則引擎和最終使用者介面。

### 背景

根據 `docs/AI_APPLICATION_DESIGN_ANALYSIS.md` 分析報告發現：

**問題現況**:
- ✅ AI 基礎設施完整度: 90%（API 整合、提示工程完整）
- ❌ AI 應用層完整度: 30%（資訊在傳遞過程中大量流失）
- ⚠️ PRD FR6 需求滿足度: 27.5%（僅部分滿足智能修復建議需求）

**關鍵問題**:
1. `SecurityIssue` 包含完整的 FR6 欄位（codeExample, effortEstimate）
2. `RuleViolation` 缺少這些欄位，導致資訊無法傳遞
3. `OwaspSensor.buildIssueMessage()` 丟棄了 AI 提供的代碼範例和工作量評估
4. 所有 10 個 OWASP 規則的 `enhanceViolationsWithAi()` 都是空佔位符
5. 規則的 `requiresAi()` 都返回 `false`，未實際使用 AI

**影響評估**:
- 使用者無法看到 Before/After 代碼範例（FR6 需求）
- 使用者無法評估修復工作量（FR6 需求）
- AI 生成的詳細修復建議被降級為純文字
- 產品功能與 PRD 承諾不符（僅 27.5% 滿足度）

### 成功指標

1. **FR6 滿足度** ≥ 90%（從當前 27.5% 提升）
2. **資訊完整性**: AI 提供的 8 個欄位中 ≥ 7 個成功傳遞到使用者
3. **AI 增強覆蓋率**: ≥ 80% 的規則實際使用 AI 增強
4. **使用者可見性**: Before/After 代碼範例和工作量評估在 UI 中可見
5. **效能影響**: AI 增強不導致 >20% 的效能衰退

---

## Stories

### Phase 1: 基礎修復（1-2 週）

#### Story 10.1: 擴展 RuleViolation 資料結構
**目標**: 支援 Before/After 代碼範例和工作量評估

**AC**:
1. `RuleViolation` 新增 `codeExample` 欄位（CodeExample 類型）
2. `RuleViolation` 新增 `effortEstimate` 欄位（String 類型）
3. Builder 模式支援新欄位的設定
4. 向後相容：新欄位為可選（nullable）
5. 單元測試覆蓋率 ≥ 85%

**工作量**: 2-3 小時

#### Story 10.2: 修改 OwaspSensor 保留完整 AI 資訊
**目標**: 停止丟棄 AI 提供的代碼範例和工作量評估

**AC**:
1. `buildIssueMessage()` 改為 `buildEnhancedIssue()`
2. 使用 SonarQube Issue Attributes 儲存額外資訊
3. 保留 `codeExample.before` 和 `codeExample.after`
4. 保留 `effortEstimate`
5. 向後相容：如果 SonarQube 版本不支援 Attributes，優雅降級
6. 整合測試驗證資訊完整性

**工作量**: 4-6 小時

#### Story 10.3: 基礎資訊傳遞測試
**目標**: 驗證 AI 資訊從 API 到 SonarQube Issue 的完整流動

**AC**:
1. 端到端測試：AI API → SecurityIssue → RuleViolation → SonarQube Issue
2. 驗證 8 個 SecurityIssue 欄位全部保留
3. 測試極端情況：空值、超長文字、特殊字元
4. 效能基準測試：與修改前對比 < 10% 衰退
5. 測試覆蓋率 ≥ 80%

**工作量**: 3-4 小時

---

### Phase 2: AI 增強實作（2-3 週）

#### Story 10.4: 實作 AbstractOwaspRule AI 增強邏輯
**目標**: 規則引擎實際使用 AI 生成智能修復建議

**AC**:
1. 實作 `AbstractOwaspRule.enhanceViolationsWithAi()` 方法
2. 呼叫 `context.getAiService()` 進行深入分析
3. 將 AI 的 `fixSuggestion`、`codeExample`、`effortEstimate` 整合到 RuleViolation
4. 錯誤處理：AI 失敗時優雅降級到原始 violation
5. 快取機制：相同代碼片段避免重複 AI 呼叫
6. 單元測試 ≥ 85% 覆蓋率

**工作量**: 8-12 小時

#### Story 10.5: 更新 OWASP 規則啟用 AI 增強
**目標**: 10 個 OWASP 2021 規則全部啟用 AI

**AC**:
1. 修改 10 個規則的 `requiresAi()` 返回 `true`
2. 確認所有規則的 `enhanceViolationsWithAi()` 正確呼叫
3. 規則優先級：先啟用高風險規則（Injection, Broken Access Control）
4. 配置選項：允許用戶選擇性啟用/停用規則 AI 增強
5. 整合測試：每個規則至少 3 個測試案例

**受影響規則**:
- InjectionRule
- BrokenAccessControlRule
- CryptographicFailuresRule
- AuthenticationFailuresRule
- DataIntegrityFailuresRule
- InsecureDesignRule
- SecurityMisconfigurationRule
- SecurityLoggingFailuresRule
- SsrfRule
- VulnerableComponentsRule

**工作量**: 6-8 小時

#### Story 10.6: AI 增強整合測試與效能調優
**目標**: 確保 AI 增強不影響整體效能和穩定性

**AC**:
1. 端到端測試：完整掃描流程（100+ 檔案）
2. 效能測試：AI 增強 vs 無 AI 對比 < 20% 衰退
3. 並行測試：驗證多執行緒安全性
4. 快取測試：驗證重複掃描性能提升 ≥ 50%
5. 錯誤恢復測試：AI API 失敗、超時、速率限制
6. 負載測試：1000 個檔案掃描穩定性
7. 測試報告：包含效能基準、資源使用、錯誤率

**工作量**: 6-8 小時

---

### Phase 3: UI 優化（3-4 週 - 可選，未包含在本 Epic）

**說明**: UI 層的增強（代碼對比視圖、結構化修復建議展示）將在後續 Epic 中處理，本 Epic 聚焦於資料層和邏輯層的修復。

---

## 依賴關係

**前置條件**:
- Epic 2 Story 2.1-2.3 已完成（AI 連接器、OpenAI 整合、提示詞工程）
- Epic 3 Story 3.1-3.2 已完成（規則引擎架構、OWASP 2021 規則）

**阻塞項目**:
- 無（本 Epic 獨立執行，不阻塞其他 Epics）

**並行建議**:
- 可與 Epic 1 Story 1.8（測試與文檔）並行
- 不建議與 Epic 3-4（規則引擎修改）並行，避免衝突

---

## 風險評估

| 風險項目 | 機率 | 影響 | 緩解措施 |
|---------|------|------|---------|
| SonarQube API 不支援 Issue Attributes | 中 | 高 | 研究替代方案（自訂擴展點、外部儲存） |
| AI 回應延遲影響掃描效能 | 高 | 中 | 實作智能快取、批次處理、異步分析 |
| 資料模型重構影響現有功能 | 低 | 高 | 階段性重構、保持向後相容、完整回歸測試 |
| AI 成本超支 | 中 | 中 | Token 用量監控、快取策略、用量限制 |
| 工作量低估 | 中 | 中 | 保留 30% 緩衝時間 |

---

## 驗收標準

**Epic 完成條件**:
1. ✅ 所有 6 個 Stories 狀態為 "Done"
2. ✅ FR6 滿足度 ≥ 90%（經驗證測試確認）
3. ✅ AI 增強覆蓋率 ≥ 80% 的規則
4. ✅ 效能測試通過（< 20% 衰退）
5. ✅ 整合測試套件通過率 100%
6. ✅ 回歸測試通過率 ≥ 95%
7. ✅ 代碼審查通過（無 blocker issues）
8. ✅ 文檔更新完成（API 文檔、架構文檔、測試報告）

**不包含在本 Epic**:
- UI 層增強（代碼對比視圖、工作量評估展示）→ 留待未來 Epic
- OWASP 2017/2025 規則 AI 增強 → Epic 4/6 處理

---

## 參考文件

- **分析報告**: `docs/AI_APPLICATION_DESIGN_ANALYSIS.md`
- **PRD FR6**: `docs/prd.md` Line 47
- **架構文檔**: `docs/architecture.md`（待更新）
- **AI 資料模型**: `ai-connector/src/main/java/com/github/sonarqube/ai/model/SecurityIssue.java`
- **規則引擎**: `rules-engine/src/main/java/com/github/sonarqube/rules/`

---

**Epic Owner**: Development Team
**Last Updated**: 2025-10-24
