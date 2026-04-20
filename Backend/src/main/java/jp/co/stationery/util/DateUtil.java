package jp.co.stationery.util;

import java.time.LocalDate;

/**
 * 日付処理ユーティリティ。
 * 各ハンドラ・画面で重複していた parseDate の集約先。
 */
public final class DateUtil {

    private DateUtil() {
    }

    /**
     * ISO-8601形式（yyyy-MM-dd）の文字列を LocalDate に変換する。
     * null・空文字・形式不正の場合は null を返却する。
     */
    public static LocalDate parseLocalDate(final String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
