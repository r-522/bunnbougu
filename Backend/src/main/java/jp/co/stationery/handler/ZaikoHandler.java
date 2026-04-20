package jp.co.stationery.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jp.co.stationery.dao.ZaikoDao;
import jp.co.stationery.model.Zaiko;
import jp.co.stationery.service.ZaikoAdjustService;
import jp.co.stationery.util.HtmlEscape;
import jp.co.stationery.util.HttpUtil;
import jp.co.stationery.util.SousaLogger;
import jp.co.stationery.util.TemplateEngine;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * F06 在庫照会・調整 ハンドラ。
 * GET  /zaiko             → 在庫一覧
 * GET  /zaiko/adjust?prdcd=... → 調整フォーム
 * POST /zaiko/adjust      → 調整実行
 */
public final class ZaikoHandler extends HandlerBase implements HttpHandler {

    private final ZaikoDao dao = new ZaikoDao();
    private final ZaikoAdjustService adjustService = new ZaikoAdjustService();

    @Override
    public void handle(final HttpExchange ex) throws IOException {
        final String syainNo = requireLogin(ex);
        if (syainNo == null) {
            return;
        }
        try {
            final String path = ex.getRequestURI().getPath();
            final String method = ex.getRequestMethod();
            if ("/zaiko".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleList(ex, syainNo);
            } else if ("/zaiko/adjust".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleAdjustForm(ex, syainNo);
            } else if ("/zaiko/adjust".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleAdjustPost(ex, syainNo);
            } else {
                HttpUtil.sendText(ex, 404, "Not Found");
            }
        } catch (Exception e) {
            SousaLogger.log(SousaLogger.OP_ERROR, "F06", null, e.getMessage(),
                            SousaLogger.RESULT_NG, syainNo);
            sendError(ex, e);
        }
    }

    // 一覧
    private void handleList(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> q = HttpUtil.readQuery(ex);
        final String prdcd = HttpUtil.param(q, "prdcd");
        final String prdnm = HttpUtil.param(q, "prdnm");
        final List<Zaiko> list = dao.search(prdcd, prdnm);
        // 行構築
        final NumberFormat nf = NumberFormat.getNumberInstance(Locale.JAPAN);
        final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        final StringBuilder rows = new StringBuilder();
        if (list.isEmpty()) {
            rows.append("<tr><td colspan=\"9\" class=\"table__center\">該当データはありません</td></tr>");
        } else {
            for (final Zaiko z : list) {
                final int avail = z.zkqt - z.zkrs;
                final String state = (avail <= 0) ? "欠品" : (avail <= 10 ? "在庫僅少" : "通常");
                rows.append("<tr>")
                    .append("<td class=\"mono\">").append(HtmlEscape.escape(z.prdcd)).append("</td>")
                    .append("<td>").append(HtmlEscape.escape(z.prdnm)).append("</td>")
                    .append("<td class=\"table__num mono\">").append(nf.format(z.zkqt)).append("</td>")
                    .append("<td class=\"table__num mono\">").append(nf.format(z.zkrs)).append("</td>")
                    .append("<td class=\"table__num mono\">").append(nf.format(avail)).append("</td>")
                    .append("<td>").append(HtmlEscape.escape(z.prdun)).append("</td>")
                    .append("<td>").append(state).append("</td>")
                    .append("<td class=\"mono\">")
                        .append(z.zkut == null ? "-" : z.zkut.format(dtf)).append("</td>")
                    .append("<td>")
                        .append("<a class=\"btn btn--sm\" href=\"/zaiko/adjust?prdcd=")
                        .append(HtmlEscape.escape(z.prdcd)).append("\">調整</a>")
                    .append("</td>")
                    .append("</tr>");
            }
        }
        final Map<String, String> params = baseParams(syainNo);
        params.put("rows", rows.toString());
        HttpUtil.sendHtml(ex, 200, TemplateEngine.render("zaiko_list.html", params));
    }

    // 調整フォーム
    private void handleAdjustForm(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> q = HttpUtil.readQuery(ex);
        final String prdcd = HttpUtil.param(q, "prdcd");
        if (prdcd.isBlank()) {
            HttpUtil.redirect(ex, "/zaiko");
            return;
        }
        HttpUtil.sendHtml(ex, 200,
            TemplateEngine.render("zaiko_adjust.html", baseParams(syainNo)));
    }

    // 調整実行
    private void handleAdjustPost(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> form = HttpUtil.readFormBody(ex);
        final String prdcd = HttpUtil.param(form, "prdcd");
        final String zkaqStr = HttpUtil.param(form, "zkaq");
        final String reason = HttpUtil.param(form, "zkrs");
        final String memo = HttpUtil.param(form, "zkrm");
        // バリデーション
        if (prdcd.isBlank() || zkaqStr.isBlank() || reason.isBlank()) {
            SousaLogger.log(SousaLogger.OP_UPDATE, "F06", prdcd,
                            "必須項目不足", SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/zaiko/adjust?prdcd=" + java.net.URLEncoder.encode(prdcd,
                              java.nio.charset.StandardCharsets.UTF_8));
            return;
        }
        int newQty;
        try {
            newQty = Integer.parseInt(zkaqStr);
            if (newQty < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            SousaLogger.log(SousaLogger.OP_UPDATE, "F06", prdcd,
                            "数値不正", SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/zaiko");
            return;
        }
        // 調整実行
        adjustService.adjust(prdcd, newQty, reason, memo, syainNo);
        SousaLogger.log(SousaLogger.OP_UPDATE, "F06", prdcd,
                        "在庫調整 reason=" + reason + " qty=" + newQty,
                        SousaLogger.RESULT_OK, syainNo);
        HttpUtil.redirect(ex, "/zaiko");
    }
}
