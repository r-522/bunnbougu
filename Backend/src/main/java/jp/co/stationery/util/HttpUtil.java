package jp.co.stationery.util;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTPリクエスト/レスポンス補助ユーティリティ。
 * application/x-www-form-urlencodedボディ/クエリ文字列パース、レスポンス送信、Cookie解析を担う。
 */
public final class HttpUtil {

    // インスタンス化禁止
    private HttpUtil() {
    }

    /**
     * リクエストボディ（form-urlencoded）をMapに変換する。
     *
     * @param ex HttpExchange
     * @return フォームパラメータMap
     */
    public static Map<String, String> readFormBody(final HttpExchange ex) throws IOException {
        // ボディをUTF-8で全量読み込み
        try (InputStream is = ex.getRequestBody()) {
            final byte[] raw = is.readAllBytes();
            final String body = new String(raw, StandardCharsets.UTF_8);
            return parseQueryString(body);
        }
    }

    /**
     * クエリ文字列（URL末尾の?以降）をMapに変換する。
     *
     * @param ex HttpExchange
     * @return クエリパラメータMap
     */
    public static Map<String, String> readQuery(final HttpExchange ex) {
        // URIからクエリ部分を取得
        final String q = ex.getRequestURI().getRawQuery();
        return parseQueryString(q);
    }

    /**
     * URLエンコード済クエリ文字列をMapへ変換する。
     * 同一キー複数出現時は最後の値で上書きする（本業務では単値入力のみ想定）。
     */
    public static Map<String, String> parseQueryString(final String q) {
        // 結果マップ
        final Map<String, String> map = new HashMap<>();
        // null/空は空マップ返却
        if (q == null || q.isEmpty()) {
            return map;
        }
        // "&" 区切りで各ペアを処理
        final String[] pairs = q.split("&");
        // for-each で個別パース
        for (final String pair : pairs) {
            // 空要素はスキップ
            if (pair.isEmpty()) {
                continue;
            }
            // "=" で分割
            final int idx = pair.indexOf('=');
            // キーと値の取得（"="なしの場合は値なし扱い）
            final String key;
            final String val;
            if (idx < 0) {
                key = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                val = "";
            } else {
                key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            }
            // 最後の値で上書き
            map.put(key, val);
        }
        return map;
    }

    /**
     * HTMLレスポンスを送信する（Content-Type: text/html; charset=UTF-8）。
     *
     * @param ex     HttpExchange
     * @param status HTTPステータスコード
     * @param html   レスポンスボディ
     */
    public static void sendHtml(final HttpExchange ex, final int status, final String html) throws IOException {
        // バイト配列化
        final byte[] body = html.getBytes(StandardCharsets.UTF_8);
        // レスポンスヘッダ
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        // X-Content-Type-Options：ブラウザのMIMEスニッフィング抑止
        ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        // ヘッダとステータス送信
        ex.sendResponseHeaders(status, body.length);
        // ボディ送信
        ex.getResponseBody().write(body);
        ex.getResponseBody().close();
    }

    /**
     * プレーンテキストレスポンスを送信する。
     */
    public static void sendText(final HttpExchange ex, final int status, final String text) throws IOException {
        final byte[] body = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(status, body.length);
        ex.getResponseBody().write(body);
        ex.getResponseBody().close();
    }

    /**
     * 指定パスへリダイレクト（303 See Other）する。
     */
    public static void redirect(final HttpExchange ex, final String location) throws IOException {
        // Locationヘッダ設定
        ex.getResponseHeaders().set("Location", location);
        // 303（POST→GET遷移に適切）
        ex.sendResponseHeaders(303, -1);
        // ボディ無しでクローズ
        ex.getResponseBody().close();
    }

    /**
     * リクエストCookieから指定名の値を取得する。
     *
     * @return 見つからなければnull
     */
    public static String getCookie(final HttpExchange ex, final String name) {
        // Cookieヘッダ全件を走査
        final Headers headers = ex.getRequestHeaders();
        final List<String> cookies = headers.get("Cookie");
        // 未設定時はnull
        if (cookies == null || cookies.isEmpty()) {
            return null;
        }
        // 各Cookieヘッダを"; "区切りで処理
        for (final String line : cookies) {
            final String[] parts = line.split(";");
            for (final String part : parts) {
                // トリム後"="で分割
                final String p = part.trim();
                final int idx = p.indexOf('=');
                if (idx < 0) {
                    continue;
                }
                // 名前一致時に値返却
                if (name.equals(p.substring(0, idx))) {
                    return p.substring(idx + 1);
                }
            }
        }
        return null;
    }

    /**
     * Set-Cookieヘッダを付与する（HttpOnly/SameSite=Lax/Path=/）。
     */
    public static void setCookie(final HttpExchange ex, final String name, final String value) {
        // セキュア属性付きのCookieヘッダ値を構築
        final String cookie = name + "=" + value + "; HttpOnly; SameSite=Lax; Path=/";
        // Set-Cookieヘッダを追加（既存があっても上書きではなく追加）
        ex.getResponseHeaders().add("Set-Cookie", cookie);
    }

    /**
     * Cookie削除用Set-Cookieヘッダを付与する（Max-Age=0）。
     */
    public static void clearCookie(final HttpExchange ex, final String name) {
        final String cookie = name + "=; HttpOnly; SameSite=Lax; Path=/; Max-Age=0";
        ex.getResponseHeaders().add("Set-Cookie", cookie);
    }

    /**
     * form値取得（null時は空文字）。
     */
    public static String param(final Map<String, String> form, final String key) {
        // null安全
        if (form == null) {
            return "";
        }
        // 値取得、未存在はデフォルト空文字
        return form.getOrDefault(key, "");
    }
}
