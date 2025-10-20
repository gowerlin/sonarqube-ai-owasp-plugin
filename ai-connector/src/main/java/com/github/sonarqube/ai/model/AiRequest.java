package com.github.sonarqube.ai.model;

import java.util.Objects;

/**
 * AI 請求封裝
 *
 * 包含代碼分析所需的所有輸入參數。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public class AiRequest {

    private final String code;
    private final String fileName;
    private final String language;
    private final String owaspVersion;
    private final String analysisType;
    private final String additionalContext;

    private AiRequest(Builder builder) {
        this.code = builder.code;
        this.fileName = builder.fileName;
        this.language = builder.language;
        this.owaspVersion = builder.owaspVersion;
        this.analysisType = builder.analysisType;
        this.additionalContext = builder.additionalContext;
    }

    public String getCode() {
        return code;
    }

    /**
     * 取得代碼片段（別名方法，指向 getCode()）
     *
     * @return 代碼內容
     */
    public String getCodeSnippet() {
        return code;
    }

    public String getFileName() {
        return fileName;
    }

    public String getLanguage() {
        return language;
    }

    public String getOwaspVersion() {
        return owaspVersion;
    }

    public String getAnalysisType() {
        return analysisType;
    }

    public String getAdditionalContext() {
        return additionalContext;
    }

    /**
     * 建立新的 Builder 實例
     *
     * @param code 待分析的代碼
     * @return Builder 實例
     */
    public static Builder builder(String code) {
        return new Builder(code);
    }

    /**
     * AI 請求建構器
     */
    public static class Builder {
        private final String code;
        private String fileName;
        private String language = "java";
        private String owaspVersion = "2021";
        private String analysisType = "security";
        private String additionalContext;

        public Builder(String code) {
            Objects.requireNonNull(code, "Code cannot be null");
            this.code = code;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder owaspVersion(String owaspVersion) {
            this.owaspVersion = owaspVersion;
            return this;
        }

        public Builder analysisType(String analysisType) {
            this.analysisType = analysisType;
            return this;
        }

        public Builder additionalContext(String additionalContext) {
            this.additionalContext = additionalContext;
            return this;
        }

        public AiRequest build() {
            return new AiRequest(this);
        }
    }

    @Override
    public String toString() {
        return String.format("AiRequest[file=%s, lang=%s, owasp=%s, codeLength=%d]",
            fileName, language, owaspVersion, code != null ? code.length() : 0);
    }
}
