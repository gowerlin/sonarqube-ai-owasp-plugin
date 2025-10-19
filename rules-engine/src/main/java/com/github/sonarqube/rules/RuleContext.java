package com.github.sonarqube.rules;

import com.github.sonarqube.ai.AiService;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 規則執行上下文
 *
 * 包含執行規則檢查所需的所有資訊，包括程式碼、檔案資訊、AI 服務、配置等。
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 3, Story 3.1)
 */
public class RuleContext {

    private final String code;
    private final String language;
    private final String fileName;
    private final Path filePath;
    private final String owaspVersion;
    private final AiService aiService;
    private final Map<String, Object> metadata;

    private RuleContext(Builder builder) {
        this.code = builder.code;
        this.language = builder.language;
        this.fileName = builder.fileName;
        this.filePath = builder.filePath;
        this.owaspVersion = builder.owaspVersion;
        this.aiService = builder.aiService;
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
    }

    public String getCode() {
        return code;
    }

    public String getLanguage() {
        return language;
    }

    public String getFileName() {
        return fileName;
    }

    public Path getFilePath() {
        return filePath;
    }

    public String getOwaspVersion() {
        return owaspVersion;
    }

    public AiService getAiService() {
        return aiService;
    }

    /**
     * 獲取元資料
     *
     * @param key 元資料鍵
     * @return 元資料值，如果不存在返回 null
     */
    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    /**
     * 獲取元資料（帶預設值）
     *
     * @param key 元資料鍵
     * @param defaultValue 預設值
     * @param <T> 值類型
     * @return 元資料值，如果不存在返回預設值
     */
    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, T defaultValue) {
        Object value = metadata.get(key);
        return value != null ? (T) value : defaultValue;
    }

    /**
     * 獲取所有元資料
     *
     * @return 不可變的元資料 Map
     */
    public Map<String, Object> getAllMetadata() {
        return metadata;
    }

    /**
     * 檢查是否有 AI 服務可用
     *
     * @return true 如果 AI 服務可用
     */
    public boolean hasAiService() {
        return aiService != null;
    }

    /**
     * 建立 Builder
     *
     * @param code 程式碼
     * @param language 程式語言
     * @return Builder 實例
     */
    public static Builder builder(String code, String language) {
        return new Builder(code, language);
    }

    public static class Builder {
        private final String code;
        private final String language;
        private String fileName;
        private Path filePath;
        private String owaspVersion = "2021";
        private AiService aiService;
        private final Map<String, Object> metadata = new HashMap<>();

        private Builder(String code, String language) {
            this.code = Objects.requireNonNull(code, "Code cannot be null");
            this.language = Objects.requireNonNull(language, "Language cannot be null");
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder filePath(Path filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder owaspVersion(String owaspVersion) {
            this.owaspVersion = owaspVersion;
            return this;
        }

        public Builder aiService(AiService aiService) {
            this.aiService = aiService;
            return this;
        }

        /**
         * 添加元資料
         *
         * @param key 鍵
         * @param value 值
         * @return Builder 實例
         */
        public Builder metadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        /**
         * 批次添加元資料
         *
         * @param metadata 元資料 Map
         * @return Builder 實例
         */
        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
            return this;
        }

        public RuleContext build() {
            return new RuleContext(this);
        }
    }

    @Override
    public String toString() {
        return "RuleContext{" +
            "language='" + language + '\'' +
            ", fileName='" + fileName + '\'' +
            ", owaspVersion='" + owaspVersion + '\'' +
            ", hasAi=" + hasAiService() +
            ", codeLength=" + (code != null ? code.length() : 0) +
            '}';
    }
}
