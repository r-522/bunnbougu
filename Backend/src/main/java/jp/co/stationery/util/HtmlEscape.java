package jp.co.stationery.util;

/**
 * HTMLエスケープユーティリティ。
 * XSS対策のため、テンプレートに差し込む全ての動的文字列に本メソッドを通す。
 */
public final class HtmlEscape {

    // インスタンス化禁止
    private HtmlEscape() {
    }

    /**
     * HTML特殊文字をエスケープする。
     *
     * @param s 入力文字列（nullは空文字扱い）
     * @return エスケープ済文字列
     */
    public static String escape(final String s) {
        // nullは空文字として返却（テンプレート差込時の利便性のため）
        if (s == null) {
            return "";
        }
        // StringBuilderで1文字ずつ走査し、特殊文字のみ置換
        final StringBuilder sb = new StringBuilder(s.length());
        // 文字列長分ループ
        for (int i = 0; i < s.length(); i++) {
            // 現在位置の文字を取得
            final char c = s.charAt(i);
            // 特殊文字ごとに置換、それ以外はそのまま追加
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c); break;
            }
        }
        return sb.toString();
    }
}
