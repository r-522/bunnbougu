package jp.co.stationery.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jp.co.stationery.dao.SyainDao;
import jp.co.stationery.model.Syain;
import jp.co.stationery.util.HttpUtil;
import jp.co.stationery.util.SessionStore;
import jp.co.stationery.util.SousaLogger;
import jp.co.stationery.util.TemplateEngine;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * F01 ログイン / ログアウト ハンドラ。
 * GET  /login      → ログイン画面表示
 * POST /login      → 社員番号認証→セッション発行→/menu リダイレクト
 * GET  /logout     → セッション破棄→/login リダイレクト
 */
public final class AuthHandler implements HttpHandler {

    private final SyainDao syainDao = new SyainDao();

    @Override
    public void handle(final HttpExchange ex) throws IOException {
        // ルーティング
        final String path = ex.getRequestURI().getPath();
        final String method = ex.getRequestMethod();
        try {
            if ("/logout".equals(path)) {
                handleLogout(ex);
            } else if ("/login".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleLoginPost(ex);
            } else if ("/login".equals(path)) {
                handleLoginGet(ex, "");
            } else {
                HttpUtil.sendText(ex, 404, "Not Found");
            }
        } catch (Exception e) {
            // エラーログ書込み（社員番号は不明）
            SousaLogger.log(SousaLogger.OP_ERROR, "F01", null, e.getMessage(),
                            SousaLogger.RESULT_NG, "-");
            HttpUtil.sendText(ex, 500, "Internal Server Error");
        }
    }

    // ログイン画面表示
    private void handleLoginGet(final HttpExchange ex, final String errMsg) throws IOException {
        final Map<String, String> params = TemplateEngine.newParams();
        // エラーメッセージ差込（テンプレート未対応のため未使用）
        params.put("error", errMsg);
        HttpUtil.sendHtml(ex, 200, TemplateEngine.render("login.html", params));
    }

    // ログイン処理
    private void handleLoginPost(final HttpExchange ex) throws IOException {
        // フォームから社員番号取得
        final Map<String, String> form = HttpUtil.readFormBody(ex);
        final String syainNo = HttpUtil.param(form, "syain_no");
        // 5桁数字以外は不正
        if (syainNo == null || !syainNo.matches("\\d{5}")) {
            SousaLogger.log(SousaLogger.OP_LOGIN, "F01", syainNo,
                            "形式不正", SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/login");
            return;
        }
        // 社員マスタ照合
        Syain s;
        try {
            s = syainDao.findByCode(syainNo);
        } catch (SQLException e) {
            SousaLogger.log(SousaLogger.OP_ERROR, "F01", syainNo, e.getMessage(),
                            SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/login");
            return;
        }
        if (s == null) {
            // 未登録社員
            SousaLogger.log(SousaLogger.OP_LOGIN, "F01", syainNo,
                            "未登録", SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/login");
            return;
        }
        // セッション発行
        final String sid = SessionStore.create(syainNo);
        HttpUtil.setCookie(ex, SessionStore.COOKIE_NAME, sid);
        SousaLogger.log(SousaLogger.OP_LOGIN, "F01", syainNo, "ログイン成功",
                        SousaLogger.RESULT_OK, syainNo);
        HttpUtil.redirect(ex, "/menu");
    }

    // ログアウト処理
    private void handleLogout(final HttpExchange ex) throws IOException {
        // 既存セッション破棄
        final String sid = HttpUtil.getCookie(ex, SessionStore.COOKIE_NAME);
        final String syainNo = SessionStore.getSyainNo(sid);
        SessionStore.destroy(sid);
        HttpUtil.clearCookie(ex, SessionStore.COOKIE_NAME);
        SousaLogger.log(SousaLogger.OP_LOGOUT, "F01", syainNo, "ログアウト",
                        SousaLogger.RESULT_OK, syainNo);
        HttpUtil.redirect(ex, "/login");
    }
}
