package com.github.sonarqube.report.exception;

/**
 * Report Generation Exception
 *
 * <p>自定義異常，用於 PDF 報表生成過程中的錯誤處理。</p>
 *
 * @author SonarQube AI OWASP Plugin Team
 * @since 2.0.0 (Story 1.7)
 */
public class ReportGenerationException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ErrorCode errorCode;

    /**
     * 錯誤代碼列舉
     */
    public enum ErrorCode {
        /**
         * 超時錯誤
         */
        TIMEOUT("PDF generation timeout after 60 seconds"),

        /**
         * 記憶體不足錯誤
         */
        OUT_OF_MEMORY("Insufficient memory for PDF generation. Increase JVM heap size or reduce report size."),

        /**
         * iText PDF 生成錯誤
         */
        PDF_GENERATION_FAILED("PDF generation failed due to iText library error"),

        /**
         * 檔案 I/O 錯誤
         */
        FILE_IO_ERROR("File I/O error during PDF generation"),

        /**
         * 無效輸入資料
         */
        INVALID_INPUT("Invalid input data for PDF generation"),

        /**
         * 未預期的錯誤
         */
        UNEXPECTED_ERROR("Unexpected error during PDF generation");

        private final String defaultMessage;

        ErrorCode(String defaultMessage) {
            this.defaultMessage = defaultMessage;
        }

        public String getDefaultMessage() {
            return defaultMessage;
        }
    }

    /**
     * Constructor with error code
     *
     * @param errorCode 錯誤代碼
     */
    public ReportGenerationException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    /**
     * Constructor with error code and custom message
     *
     * @param errorCode 錯誤代碼
     * @param message   自定義錯誤訊息
     */
    public ReportGenerationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructor with error code and cause
     *
     * @param errorCode 錯誤代碼
     * @param cause     原始異常
     */
    public ReportGenerationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
    }

    /**
     * Constructor with error code, custom message, and cause
     *
     * @param errorCode 錯誤代碼
     * @param message   自定義錯誤訊息
     * @param cause     原始異常
     */
    public ReportGenerationException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 取得錯誤代碼
     *
     * @return 錯誤代碼
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 取得使用者友善的錯誤訊息
     *
     * @return 使用者友善的錯誤訊息
     */
    public String getUserFriendlyMessage() {
        return errorCode.getDefaultMessage();
    }

    @Override
    public String toString() {
        return String.format("ReportGenerationException[errorCode=%s, message=%s]",
                errorCode, getMessage());
    }
}
