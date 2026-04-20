package jp.co.stationery.handler;

import com.sun.net.httpserver.HttpExchange;
import jp.co.stationery.dao.SyainDao;
import jp.co.stationery.model.Syain;
import jp.co.stationery.util.HttpUtil;
import jp.co.stationery.util.SessionStore;
import jp.co.stationery.util.TemplateEngine;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * 全ハンドラ共通の基底クラス。
 * 認証チェック・ヘッダ用ユーザ情報差込・例外処理などを集約する。
 */
public abstract class HandlerBase {

    // 社員氏名解決用（簡易キャッシュなし、毎回DBアクセスは実用上問題ない頻度）
    private static final SyainDao SYAIN_DAO = new SyainDao();

    /**
     * ログイン中の社員番号を返す。未ログイン時は /login へリダイレクトしてnullを返す。
     */
    protected String requireLogin(final HttpExchange ex) throws IOException {
        // CookieからSIDを取り出し、社員番号を解決
        final String sid = HttpUtil.getCookie(ex, SessionStore.COOKIE_NAME);
        final String syainNo = SessionStore.getSyainNo(sid);
        if (syainNo == null) {
            // 未ログイン時はログイン画面へリダイレクト
            HttpUtil.redirect(ex, "/login");
            return null;
        }
        return syainNo;
    }

    /**
     * テンプレート差込用の共通パラメータマップを生成（ヘッダ社員番号・氏名を埋める）。
     */
    protected Map<String, String> baseParams(final String syainNo) {
        final Map<String, String> params = TemplateEngine.newParams();
        params.put("syain_no", syainNo == null ? "" : syainNo);
        // 氏名はDB参照、エラーなら空文字
        try {
            if (syainNo != null) {
                final Syain s = SYAIN_DAO.findByCode(syainNo);
                params.put("syain_name", (s == null || s.synm == null) ? "" : s.synm);
            } else {
                params.put("syain_name", "");
            }
        } catch (SQLException e) {
            params.put("syain_name", "");
        }
        return params;
    }

    /**
     * 例外時の汎用エラーページを返す（500）。
     * スタックトレースやメッセージはブラウザに露出させず、標準エラー出力とTBL_SOUSA_LOGで追跡する。
     */
    protected void sendError(final HttpExchange ex, final Throwable t) throws IOException {
        // サーバログには詳細を残す（Cloud Runのログで確認可能）
        t.printStackTrace();
        // ブラウザには内部情報を含まない静的HTMLのみ返す
        final String html =
            "<!doctype html><html lang=\"ja\"><head><meta charset=\"UTF-8\">" +
            "<title>エラー - 文房具基幹システム</title>" +
            "<link rel=\"stylesheet\" href=\"/css/common.css\"></head><body>" +
            "<header class=\"header\"><div class=\"header__inner\">" +
            "<span class=\"header__title\">文房具基幹システム</span></div></header>" +
            "<main class=\"main\">" +
            "<h1 class=\"h1\">エラーが発生しました</h1>" +
            "<div class=\"msg msg--error\">処理中に予期せぬエラーが発生しました。" +
            "お手数ですが時間をおいて再度お試しください。</div>" +
            "<p><a href=\"/menu\">メニューへ戻る</a></p>" +
            "</main></body></html>";
        HttpUtil.sendHtml(ex, 500, html);
    }
}
