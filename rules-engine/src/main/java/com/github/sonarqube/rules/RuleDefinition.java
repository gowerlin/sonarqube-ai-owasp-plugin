package com.github.sonarqube.rules;

import com.github.sonarqube.rules.owasp.Owasp2021Category;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 規則定義
 *
 * 定義單一安全規則的所有資訊，包括 ID、名稱、描述、嚴重性等。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class RuleDefinition {

    private final String ruleKey;
    private final String name;
    private final String description;
    private final RuleSeverity severity;
    private final RuleType type;
    private final List<String> tags;
    private final Owasp2021Category owaspCategory;
    private final List<String> cweIds;
    private final String language;
    private final String remediationFunction;
    private final String remediationCost;

    private RuleDefinition(Builder builder) {
        this.ruleKey = builder.ruleKey;
        this.name = builder.name;
        this.description = builder.description;
        this.severity = builder.severity;
        this.type = builder.type;
        this.tags = Collections.unmodifiableList(builder.tags);
        this.owaspCategory = builder.owaspCategory;
        this.cweIds = Collections.unmodifiableList(builder.cweIds);
        this.language = builder.language;
        this.remediationFunction = builder.remediationFunction;
        this.remediationCost = builder.remediationCost;
    }

    /**
     * 規則嚴重性枚舉
     */
    public enum RuleSeverity {
        BLOCKER,   // 阻斷性
        CRITICAL,  // 嚴重
        MAJOR,     // 主要
        MINOR,     // 次要
        INFO       // 資訊
    }

    /**
     * 規則類型枚舉
     */
    public enum RuleType {
        VULNERABILITY,      // 漏洞
        SECURITY_HOTSPOT,   // 安全熱點
        BUG,               // 錯誤
        CODE_SMELL         // 代碼異味
    }

    public static Builder builder(String ruleKey) {
        return new Builder(ruleKey);
    }

    public static class Builder {
        private final String ruleKey;
        private String name;
        private String description;
        private RuleSeverity severity = RuleSeverity.MAJOR;
        private RuleType type = RuleType.VULNERABILITY;
        private final java.util.ArrayList<String> tags = new java.util.ArrayList<>();
        private Owasp2021Category owaspCategory;
        private final java.util.ArrayList<String> cweIds = new java.util.ArrayList<>();
        private String language;
        private String remediationFunction = "CONSTANT_ISSUE";
        private String remediationCost = "10min";

        private Builder(String ruleKey) {
            this.ruleKey = ruleKey;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder severity(RuleSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder type(RuleType type) {
            this.type = type;
            return this;
        }

        /**
         * 設定標籤列表（替換現有的）
         *
         * @param tags 標籤列表
         * @return Builder 實例
         */
        public Builder tags(List<String> tags) {
            this.tags.clear();
            if (tags != null) {
                this.tags.addAll(tags);
            }
            return this;
        }

        /**
         * 添加單一標籤（支援連續呼叫）
         *
         * @param tag 標籤名稱（例如 "owasp-2021", "security"）
         * @return Builder 實例
         */
        public Builder tag(String tag) {
            if (tag != null && !tag.trim().isEmpty()) {
                this.tags.add(tag);
            }
            return this;
        }

        public Builder owaspCategory(Owasp2021Category owaspCategory) {
            this.owaspCategory = owaspCategory;
            return this;
        }

        /**
         * 設定 OWASP 分類（透過 String ID）
         *
         * @param categoryId OWASP 分類 ID（例如 "A01", "A03:2021"）
         * @return Builder 實例
         */
        public Builder owaspCategory(String categoryId) {
            if (categoryId != null) {
                this.owaspCategory = Owasp2021Category.fromCategoryId(categoryId);
            }
            return this;
        }

        /**
         * 設定 CWE ID 列表（替換現有的）
         *
         * @param cweIds CWE ID 列表
         * @return Builder 實例
         */
        public Builder cweIds(List<String> cweIds) {
            this.cweIds.clear();
            if (cweIds != null) {
                this.cweIds.addAll(cweIds);
            }
            return this;
        }

        /**
         * 添加單一 CWE ID（支援連續呼叫）
         *
         * @param cweId CWE ID（例如 "CWE-20"）
         * @return Builder 實例
         */
        public Builder cweId(String cweId) {
            if (cweId != null && !cweId.trim().isEmpty()) {
                this.cweIds.add(cweId);
            }
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder remediationFunction(String remediationFunction) {
            this.remediationFunction = remediationFunction;
            return this;
        }

        public Builder remediationCost(String remediationCost) {
            this.remediationCost = remediationCost;
            return this;
        }

        public RuleDefinition build() {
            Objects.requireNonNull(ruleKey, "Rule key cannot be null");
            Objects.requireNonNull(name, "Rule name cannot be null");
            Objects.requireNonNull(description, "Rule description cannot be null");
            return new RuleDefinition(this);
        }
    }

    // Getters

    public String getRuleKey() {
        return ruleKey;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public RuleSeverity getSeverity() {
        return severity;
    }

    public RuleType getType() {
        return type;
    }

    public List<String> getTags() {
        return tags;
    }

    public Owasp2021Category getOwaspCategory() {
        return owaspCategory;
    }

    public List<String> getCweIds() {
        return cweIds;
    }

    public String getLanguage() {
        return language;
    }

    public String getRemediationFunction() {
        return remediationFunction;
    }

    public String getRemediationCost() {
        return remediationCost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RuleDefinition that = (RuleDefinition) o;
        return Objects.equals(ruleKey, that.ruleKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleKey);
    }

    @Override
    public String toString() {
        return "RuleDefinition{" +
            "ruleKey='" + ruleKey + '\'' +
            ", name='" + name + '\'' +
            ", severity=" + severity +
            ", type=" + type +
            ", language='" + language + '\'' +
            ", owaspCategory=" + owaspCategory +
            '}';
    }
}
