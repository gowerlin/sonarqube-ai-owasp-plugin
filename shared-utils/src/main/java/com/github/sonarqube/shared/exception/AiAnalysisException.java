package com.github.sonarqube.shared.exception;

/**
 * AI 分析相關例外
 *
 * @since 1.0.0
 */
public class AiAnalysisException extends AiOwaspException {

    private static final long serialVersionUID = 1L;

    public AiAnalysisException(String message) {
        super(message);
    }

    public AiAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
