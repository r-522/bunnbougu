package jp.co.stationery.util;

import jp.co.stationery.config.AppConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 簡易テンプレートエンジン。
 * Frontend/配下のHTMLをそのまま利用し、${キー} 形式のプレースホルダを差し替える。
 * - JSPは禁止のため利用しない。
 * - 全差込値は原則HtmlEscape経由でエスケープ済みの前提。
 * - 差込済HTMLはキャッシュしない（テンプレート本体のみプロセス起動時に一度だけ読込）。
 */
public final class TemplateEngine {

    // テンプレート本体キャッシュ（ファイル名→HTML文字列）
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    // インスタンス化禁止
    private TemplateEngine() {
    }

    /**
     * テンプレートを読み込み、プレースホルダを差し替えて返却する。
     *
     * @param templateName テンプレートファイル名（例: "login.html"）
     * @param params       差込パラメータ（キー→値）。valueは事前にエスケープ済である必要はない（本メソッド内でescapeする）。
     * @return 差込済HTML文字列
     */
    public static String render(final String templateName, final Map<String, String> params) {
        // テンプレート本体を取得（キャッシュ利用）
        final String tmpl = load(templateName);
        // paramsがnullなら差込なしでそのまま返す
        if (params == null || params.isEmpty()) {
            return tmpl;
        }
        // 結果バッファをStringBuilderで構築
        final StringBuilder out = new StringBuilder(tmpl.length() + 256);
        // 先頭から${...}を順次探索して置換
        int idx = 0;
        while (idx < tmpl.length()) {
            // プレースホルダ開始位置を探す
            final int start = tmpl.indexOf("${", idx);
            // 開始が無ければ残りを出力して終了
            if (start < 0) {
                out.append(tmpl, idx, tmpl.length());
                break;
            }
            // 開始前の部分をそのまま出力
            out.append(tmpl, idx, start);
            // 終端"}"を探す
            final int end = tmpl.indexOf('}', start + 2);
            // 不正なプレースホルダはそのままテンプレート末尾まで出力
            if (end < 0) {
                out.append(tmpl, start, tmpl.length());
                break;
            }
            // キーを取り出す（${KEY} のKEY部分）
            final String key = tmpl.substring(start + 2, end);
            // "raw:" プレフィックスでエスケープ回避（明細HTML断片など、事前生成済のHTMLを埋め込む用途）
            if (key.startsWith("raw:")) {
                // プレフィックス除去後のキーで値取得
                final String realKey = key.substring(4);
                // 値があれば出力、無ければ空文字
                out.append(params.getOrDefault(realKey, ""));
            } else {
                // 通常キー：値取得→HTMLエスケープして出力
                out.append(HtmlEscape.escape(params.getOrDefault(key, "")));
            }
            // 次の走査開始位置を更新
            idx = end + 1;
        }
        return out.toString();
    }

    // テンプレートファイルをUTF-8で読み込みキャッシュする
    private static String load(final String templateName) {
        // キャッシュヒット時は即返却
        final String cached = CACHE.get(templateName);
        if (cached != null) {
            return cached;
        }
        // ファイルパスを解決（TEMPLATE_DIR + "/" + templateName）
        final Path path = Paths.get(AppConfig.getTemplateDir(), templateName);
        try {
            // ファイル全体をUTF-8で読み込み
            final String content = Files.readString(path, StandardCharsets.UTF_8);
            // キャッシュへ格納
            CACHE.put(templateName, content);
            return content;
        } catch (IOException e) {
            // テンプレート未存在はサーバ設定エラーとして扱う
            throw new IllegalStateException("テンプレート読込失敗: " + path, e);
        }
    }

    /**
     * 空の差込マップを生成するヘルパ。
     */
    public static Map<String, String> newParams() {
        return new HashMap<>();
    }
}
