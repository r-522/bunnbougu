package jp.co.stationery.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jp.co.stationery.service.NyukaService;
import jp.co.stationery.util.DateUtil;
import jp.co.stationery.util.HttpUtil;
import jp.co.stationery.util.SousaLogger;
import jp.co.stationery.util.TemplateEngine;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * F09 入荷登録ハンドラ。
 * GET  /nyuka  → フォーム表示
 * POST /nyuka  → 入荷実績登録
 */
public final class NyukaHandler extends HandlerBase implements HttpHandler {

    private final NyukaService service = new NyukaService();

    @Override
    public void handle(final HttpExchange ex) throws IOException {
        final String syainNo = requireLogin(ex);
        if (syainNo == null) {
            return;
        }
        try {
            final String method = ex.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                handleForm(ex, syainNo);
            } else if ("POST".equalsIgnoreCase(method)) {
                handlePost(ex, syainNo);
            } else {
                HttpUtil.sendText(ex, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            SousaLogger.log(SousaLogger.OP_ERROR, "F09", null, e.getMessage(),
                            SousaLogger.RESULT_NG, syainNo);
            sendError(ex, e);
        }
    }

    private void handleForm(final HttpExchange ex, final String syainNo) throws Exception {
        // テンプレートのモック値そのままで表示（hano絞込はクエリ引数のみ反映）
        HttpUtil.sendHtml(ex, 200, TemplateEngine.render("nyuka.html", baseParams(syainNo)));
    }

    private void handlePost(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> form = HttpUtil.readFormBody(ex);
        final String hano = HttpUtil.param(form, "hano");
        final LocalDate nydt = DateUtil.parseLocalDate(HttpUtil.param(form, "nydt"));
        // 納品書番号はフォーム受領のみ（現行スキーマでは保存対象外）
        if (hano.isBlank() || nydt == null) {
            SousaLogger.log(SousaLogger.OP_CREATE, "F09", hano,
                            "必須項目不足", SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/nyuka");
            return;
        }
        // 行番号→数量マップを組立（dt_nyqty_n）
        final Map<Integer, Integer> qtyByLine = new HashMap<>();
        for (int i = 1; i <= 20; i++) {
            final String v = HttpUtil.param(form, "dt_nyqty_" + i);
            if (v.isBlank()) continue;
            try {
                final int q = Integer.parseInt(v);
                if (q > 0) qtyByLine.put(i, q);
            } catch (NumberFormatException e) {
                // 不正値は無視
            }
        }
        if (qtyByLine.isEmpty()) {
            SousaLogger.log(SousaLogger.OP_CREATE, "F09", hano,
                            "入荷数未入力", SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/nyuka");
            return;
        }
        // サービス実行
        try {
            service.register(hano, nydt, qtyByLine, syainNo);
            SousaLogger.log(SousaLogger.OP_CREATE, "F09", hano,
                            "入荷登録 lines=" + qtyByLine.size(),
                            SousaLogger.RESULT_OK, syainNo);
        } catch (IllegalArgumentException e) {
            SousaLogger.log(SousaLogger.OP_CREATE, "F09", hano, e.getMessage(),
                            SousaLogger.RESULT_NG, syainNo);
        }
        HttpUtil.redirect(ex, "/hatyu");
    }
}
