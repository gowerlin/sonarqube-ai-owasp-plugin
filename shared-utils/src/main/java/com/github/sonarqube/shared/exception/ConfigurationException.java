package com.github.sonarqube.shared.exception;

/**
 * 配置相關例外
 *
 * @since 1.0.0
 */
public class ConfigurationException extends AiOwaspException {

    private static final long serialVersionUID = 1L;

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
