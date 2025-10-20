package com.github.sonarqube.ai.provider.gemini;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Gemini API 回應模型
 *
 * 符合 Gemini API v1beta 規範
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.1.0 (Epic 9, Story 9.2)
 */
public class GeminiApiResponse {

    private List<Candidate> candidates;
    private PromptFeedback promptFeedback;
    private UsageMetadata usageMetadata;

    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    public PromptFeedback getPromptFeedback() {
        return promptFeedback;
    }

    public void setPromptFeedback(PromptFeedback promptFeedback) {
        this.promptFeedback = promptFeedback;
    }

    public UsageMetadata getUsageMetadata() {
        return usageMetadata;
    }

    public void setUsageMetadata(UsageMetadata usageMetadata) {
        this.usageMetadata = usageMetadata;
    }

    /**
     * 取得第一個候選回應的文字內容
     */
    public String getFirstCandidateText() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        Candidate first = candidates.get(0);
        if (first.content == null || first.content.getParts() == null || first.content.getParts().isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (GeminiApiRequest.Part part : first.content.getParts()) {
            if (part.getText() != null) {
                sb.append(part.getText());
            }
        }

        return sb.toString();
    }

    /**
     * Candidate 結構
     */
    public static class Candidate {
        private GeminiApiRequest.Content content;
        private String finishReason; // "STOP", "MAX_TOKENS", "SAFETY", etc.
        private Integer index;
        private List<SafetyRating> safetyRatings;

        public GeminiApiRequest.Content getContent() {
            return content;
        }

        public void setContent(GeminiApiRequest.Content content) {
            this.content = content;
        }

        public String getFinishReason() {
            return finishReason;
        }

        public void setFinishReason(String finishReason) {
            this.finishReason = finishReason;
        }

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }

        public List<SafetyRating> getSafetyRatings() {
            return safetyRatings;
        }

        public void setSafetyRatings(List<SafetyRating> safetyRatings) {
            this.safetyRatings = safetyRatings;
        }
    }

    /**
     * Safety Rating
     */
    public static class SafetyRating {
        private String category;
        private String probability; // "NEGLIGIBLE", "LOW", "MEDIUM", "HIGH"
        private Boolean blocked;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getProbability() {
            return probability;
        }

        public void setProbability(String probability) {
            this.probability = probability;
        }

        public Boolean getBlocked() {
            return blocked;
        }

        public void setBlocked(Boolean blocked) {
            this.blocked = blocked;
        }
    }

    /**
     * Prompt Feedback
     */
    public static class PromptFeedback {
        private List<SafetyRating> safetyRatings;
        private String blockReason;

        public List<SafetyRating> getSafetyRatings() {
            return safetyRatings;
        }

        public void setSafetyRatings(List<SafetyRating> safetyRatings) {
            this.safetyRatings = safetyRatings;
        }

        public String getBlockReason() {
            return blockReason;
        }

        public void setBlockReason(String blockReason) {
            this.blockReason = blockReason;
        }
    }

    /**
     * Usage Metadata
     */
    public static class UsageMetadata {
        private Integer promptTokenCount;
        private Integer candidatesTokenCount;
        private Integer totalTokenCount;

        public Integer getPromptTokenCount() {
            return promptTokenCount;
        }

        public void setPromptTokenCount(Integer promptTokenCount) {
            this.promptTokenCount = promptTokenCount;
        }

        public Integer getCandidatesTokenCount() {
            return candidatesTokenCount;
        }

        public void setCandidatesTokenCount(Integer candidatesTokenCount) {
            this.candidatesTokenCount = candidatesTokenCount;
        }

        public Integer getTotalTokenCount() {
            return totalTokenCount;
        }

        public void setTotalTokenCount(Integer totalTokenCount) {
            this.totalTokenCount = totalTokenCount;
        }
    }
}
