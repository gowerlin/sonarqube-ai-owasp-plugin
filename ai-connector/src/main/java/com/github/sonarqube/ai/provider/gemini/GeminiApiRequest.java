package com.github.sonarqube.ai.provider.gemini;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Gemini API 請求模型
 *
 * 符合 Gemini API v1beta 規範
 * 參考: https://ai.google.dev/gemini-api/docs
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9, Story 9.2)
 */
public class GeminiApiRequest {

    private List<Content> contents;
    private GenerationConfig generationConfig;
    private List<SafetySetting> safetySettings;

    public GeminiApiRequest() {
        this.contents = new ArrayList<>();
        this.safetySettings = new ArrayList<>();
    }

    public List<Content> getContents() {
        return contents;
    }

    public void setContents(List<Content> contents) {
        this.contents = contents;
    }

    public void addContent(Content content) {
        this.contents.add(content);
    }

    public GenerationConfig getGenerationConfig() {
        return generationConfig;
    }

    public void setGenerationConfig(GenerationConfig generationConfig) {
        this.generationConfig = generationConfig;
    }

    public List<SafetySetting> getSafetySettings() {
        return safetySettings;
    }

    public void setSafetySettings(List<SafetySetting> safetySettings) {
        this.safetySettings = safetySettings;
    }

    /**
     * Content 結構
     */
    public static class Content {
        private List<Part> parts;
        private String role; // "user" or "model"

        public Content() {
            this.parts = new ArrayList<>();
        }

        public Content(String role, String text) {
            this();
            this.role = role;
            this.addTextPart(text);
        }

        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }

        public void addTextPart(String text) {
            Part part = new Part();
            part.setText(text);
            this.parts.add(part);
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    /**
     * Part 結構（文字或其他內容）
     */
    public static class Part {
        private String text;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    /**
     * 生成配置
     */
    public static class GenerationConfig {
        private Float temperature;
        private Integer maxOutputTokens;
        private Float topP;
        private Integer topK;
        private List<String> stopSequences;

        public Float getTemperature() {
            return temperature;
        }

        public void setTemperature(Float temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxOutputTokens() {
            return maxOutputTokens;
        }

        public void setMaxOutputTokens(Integer maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }

        public Float getTopP() {
            return topP;
        }

        public void setTopP(Float topP) {
            this.topP = topP;
        }

        public Integer getTopK() {
            return topK;
        }

        public void setTopK(Integer topK) {
            this.topK = topK;
        }

        public List<String> getStopSequences() {
            return stopSequences;
        }

        public void setStopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
        }
    }

    /**
     * 安全設定
     */
    public static class SafetySetting {
        private String category; // "HARM_CATEGORY_*"
        private String threshold; // "BLOCK_*"

        public SafetySetting() {
        }

        public SafetySetting(String category, String threshold) {
            this.category = category;
            this.threshold = threshold;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getThreshold() {
            return threshold;
        }

        public void setThreshold(String threshold) {
            this.threshold = threshold;
        }
    }

    /**
     * Builder for fluent API
     */
    public static class Builder {
        private final GeminiApiRequest request;

        public Builder() {
            this.request = new GeminiApiRequest();
        }

        public Builder addUserMessage(String text) {
            request.addContent(new Content("user", text));
            return this;
        }

        public Builder temperature(float temperature) {
            if (request.generationConfig == null) {
                request.generationConfig = new GenerationConfig();
            }
            request.generationConfig.setTemperature(temperature);
            return this;
        }

        public Builder maxTokens(int maxTokens) {
            if (request.generationConfig == null) {
                request.generationConfig = new GenerationConfig();
            }
            request.generationConfig.setMaxOutputTokens(maxTokens);
            return this;
        }

        public Builder addSafetySetting(String category, String threshold) {
            request.safetySettings.add(new SafetySetting(category, threshold));
            return this;
        }

        public GeminiApiRequest build() {
            return request;
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
