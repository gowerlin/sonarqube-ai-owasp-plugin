package com.github.sonarqube.shared.constants;

/**
 * 插件全域常數定義
 *
 * @since 1.0.0
 */
public final class PluginConstants {

    private PluginConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ============================================================
    // 插件基本資訊
    // ============================================================
    public static final String PLUGIN_KEY = "aiowasp";
    public static final String PLUGIN_NAME = "AI OWASP Security";
    public static final String PLUGIN_VERSION = "1.0.0-SNAPSHOT";

    // ============================================================
    // OWASP 版本
    // ============================================================
    public enum OwaspVersion {
        OWASP_2017("2017", "OWASP Top 10 2017"),
        OWASP_2021("2021", "OWASP Top 10 2021"),
        OWASP_2025("2025", "OWASP Top 10 2025 (Preview)");

        private final String versionCode;
        private final String displayName;

        OwaspVersion(String versionCode, String displayName) {
            this.versionCode = versionCode;
            this.displayName = displayName;
        }

        public String getVersionCode() {
            return versionCode;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static OwaspVersion fromCode(String code) {
            for (OwaspVersion version : values()) {
                if (version.versionCode.equals(code)) {
                    return version;
                }
            }
            throw new IllegalArgumentException("Unknown OWASP version: " + code);
        }
    }

    // ============================================================
    // AI 供應商
    // ============================================================
    public enum AiProvider {
        OPENAI("openai", "OpenAI"),
        ANTHROPIC("anthropic", "Anthropic Claude");

        private final String code;
        private final String displayName;

        AiProvider(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static AiProvider fromCode(String code) {
            for (AiProvider provider : values()) {
                if (provider.code.equalsIgnoreCase(code)) {
                    return provider;
                }
            }
            throw new IllegalArgumentException("Unknown AI provider: " + code);
        }
    }

    // ============================================================
    // 嚴重性等級
    // ============================================================
    public enum Severity {
        CRITICAL("CRITICAL", 4),
        HIGH("HIGH", 3),
        MEDIUM("MEDIUM", 2),
        LOW("LOW", 1);

        private final String level;
        private final int priority;

        Severity(String level, int priority) {
            this.level = level;
            this.priority = priority;
        }

        public String getLevel() {
            return level;
        }

        public int getPriority() {
            return priority;
        }
    }

    // ============================================================
    // 報告格式
    // ============================================================
    public enum ReportFormat {
        HTML("html"),
        JSON("json"),
        BOTH("both");

        private final String format;

        ReportFormat(String format) {
            this.format = format;
        }

        public String getFormat() {
            return format;
        }

        public static ReportFormat fromString(String format) {
            for (ReportFormat rf : values()) {
                if (rf.format.equalsIgnoreCase(format)) {
                    return rf;
                }
            }
            return HTML; // 預設值
        }
    }

    // ============================================================
    // 快取配置
    // ============================================================
    public static final class CacheConfig {
        public static final int DEFAULT_MAX_SIZE = 10000;
        public static final int DEFAULT_EXPIRE_HOURS = 24;
        public static final String CACHE_KEY_SEPARATOR = "::";
    }

    // ============================================================
    // 效能配置
    // ============================================================
    public static final class PerformanceConfig {
        public static final int DEFAULT_PARALLEL_FILES = 3;
        public static final int MAX_PARALLEL_FILES = 10;
        public static final int DEFAULT_TIMEOUT_SECONDS = 60;
        public static final int MAX_TIMEOUT_SECONDS = 300;
    }

    // ============================================================
    // AI 配置預設值
    // ============================================================
    public static final class AiDefaults {
        public static final String DEFAULT_OPENAI_MODEL = "gpt-4";
        public static final String DEFAULT_ANTHROPIC_MODEL = "claude-3-opus-20240229";
        public static final float DEFAULT_TEMPERATURE = 0.3f;
        public static final int DEFAULT_MAX_TOKENS = 2000;
    }
}
