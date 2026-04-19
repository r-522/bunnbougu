package jp.co.stationery.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jp.co.stationery.util.HttpUtil;
import jp.co.stationery.util.TemplateEngine;

import java.io.IOException;

/**
 * F02 メニューハンドラ。
 * GET /menu または / → メインメニュー画面表示。
 */
public final class MenuHandler extends HandlerBase implements HttpHandler {

    @Override
    public void handle(final HttpExchange ex) throws IOException {
        // 認証チェック
        final String syainNo = requireLogin(ex);
        if (syainNo == null) {
            return;
        }
        // メニューHTMLを描画
        try {
            HttpUtil.sendHtml(ex, 200, TemplateEngine.render("menu.html", baseParams(syainNo)));
        } catch (Exception e) {
            sendError(ex, e);
        }
    }
}
