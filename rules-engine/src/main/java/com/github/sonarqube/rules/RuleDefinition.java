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
        private List<String> tags = Collections.emptyList();
        private Owasp2021Category owaspCategory;
        private List<String> cweIds = Collections.emptyList();
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

        public Builder tags(List<String> tags) {
            this.tags = tags != null ? List.copyOf(tags) : Collections.emptyList();
            return this;
        }

        public Builder owaspCategory(Owasp2021Category owaspCategory) {
            this.owaspCategory = owaspCategory;
            return this;
        }

        public Builder cweIds(List<String> cweIds) {
            this.cweIds = cweIds != null ? List.copyOf(cweIds) : Collections.emptyList();
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
