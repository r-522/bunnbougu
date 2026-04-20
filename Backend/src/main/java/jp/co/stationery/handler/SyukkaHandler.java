package jp.co.stationery.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jp.co.stationery.service.SyukkaService;
import jp.co.stationery.util.DateUtil;
import jp.co.stationery.util.HttpUtil;
import jp.co.stationery.util.SousaLogger;
import jp.co.stationery.util.TemplateEngine;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * F10 出荷登録ハンドラ。
 * GET  /syukka  → フォーム表示
 * POST /syukka  → 出荷実績登録
 */
public final class SyukkaHandler extends HandlerBase implements HttpHandler {

    private final SyukkaService service = new SyukkaService();

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
            SousaLogger.log(SousaLogger.OP_ERROR, "F10", null, e.getMessage(),
                            SousaLogger.RESULT_NG, syainNo);
            sendError(ex, e);
        }
    }

    private void handleForm(final HttpExchange ex, final String syainNo) throws Exception {
        HttpUtil.sendHtml(ex, 200, TemplateEngine.render("syukka.html", baseParams(syainNo)));
    }

    private void handlePost(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> form = HttpUtil.readFormBody(ex);
        final String juno = HttpUtil.param(form, "juno");
        final LocalDate sydt = DateUtil.parseLocalDate(HttpUtil.param(form, "sydt"));
        final String sycr = HttpUtil.param(form, "sycr");
        final String sytn = HttpUtil.param(form, "sytn");
        if (juno.isBlank() || sydt == null) {
            SousaLogger.log(SousaLogger.OP_CREATE, "F10", juno,
                            "必須項目不足", SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/syukka");
            return;
        }
        // 行番号→出荷数（dt_syqty_n）
        final Map<Integer, Integer> qtyByLine = new HashMap<>();
        for (int i = 1; i <= 20; i++) {
            final String v = HttpUtil.param(form, "dt_syqty_" + i);
            if (v.isBlank()) continue;
            try {
                final int q = Integer.parseInt(v);
                if (q > 0) qtyByLine.put(i, q);
            } catch (NumberFormatException e) {
                // 不正値は無視
            }
        }
        if (qtyByLine.isEmpty()) {
            SousaLogger.log(SousaLogger.OP_CREATE, "F10", juno,
                            "出荷数未入力", SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/syukka");
            return;
        }
        // サービス実行
        try {
            service.register(juno, sydt, sycr, sytn, qtyByLine, syainNo);
            SousaLogger.log(SousaLogger.OP_CREATE, "F10", juno,
                            "出荷登録 lines=" + qtyByLine.size(),
                            SousaLogger.RESULT_OK, syainNo);
        } catch (IllegalArgumentException | IllegalStateException e) {
            // 在庫不足含む業務例外
            SousaLogger.log(SousaLogger.OP_CREATE, "F10", juno, e.getMessage(),
                            SousaLogger.RESULT_NG, syainNo);
        }
        HttpUtil.redirect(ex, "/jutyu");
    }

}
