package com.github.sonarqube.rules;

import java.util.Objects;

/**
 * 程式碼範例，包含修正前後的程式碼片段
 * Code example containing before and after code snippets
 *
 * @since 1.0.0
 */
public class CodeExample {
    private final String before;
    private final String after;

    /**
     * 預設建構函數
     */
    public CodeExample() {
        this(null, null);
    }

    /**
     * 參數化建構函數
     *
     * @param before 修正前的程式碼
     * @param after  修正後的程式碼
     */
    public CodeExample(String before, String after) {
        this.before = before;
        this.after = after;
    }

    /**
     * 取得修正前的程式碼
     *
     * @return 修正前的程式碼
     */
    public String getBefore() {
        return before;
    }

    /**
     * 取得修正後的程式碼
     *
     * @return 修正後的程式碼
     */
    public String getAfter() {
        return after;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CodeExample that = (CodeExample) o;
        return Objects.equals(before, that.before) && Objects.equals(after, that.after);
    }

    @Override
    public int hashCode() {
        return Objects.hash(before, after);
    }

    @Override
    public String toString() {
        return "CodeExample{" +
                "before='" + (before != null ? before.substring(0, Math.min(50, before.length())) + "..." : "null") + '\'' +
                ", after='" + (after != null ? after.substring(0, Math.min(50, after.length())) + "..." : "null") + '\'' +
                '}';
    }
}
