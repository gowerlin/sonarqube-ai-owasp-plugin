package com.github.sonarqube.version;

/**
 * OWASP 版本枚舉
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 1.0.0
 */
public enum OwaspVersion {
    /**
     * OWASP Top 10 2017 版本
     */
    OWASP_2017("2017", "OWASP Top 10 2017"),

    /**
     * OWASP Top 10 2021 版本（預設）
     */
    OWASP_2021("2021", "OWASP Top 10 2021");

    private final String version;
    private final String displayName;

    OwaspVersion(String version, String displayName) {
        this.version = version;
        this.displayName = displayName;
    }

    public String getVersion() {
        return version;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static OwaspVersion fromVersion(String version) {
        if (version == null) {
            return OWASP_2021; // 預設值
        }

        for (OwaspVersion v : values()) {
            if (v.version.equals(version)) {
                return v;
            }
        }

        return OWASP_2021;
    }

    public static OwaspVersion getDefault() {
        return OWASP_2021;
    }
}
