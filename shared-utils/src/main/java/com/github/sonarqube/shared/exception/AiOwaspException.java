package com.github.sonarqube.shared.exception;

/**
 * 插件基礎例外類別
 *
 * @since 1.0.0
 */
public class AiOwaspException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AiOwaspException(String message) {
        super(message);
    }

    public AiOwaspException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiOwaspException(Throwable cause) {
        super(cause);
    }
}
