package jp.co.stationery.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jp.co.stationery.dao.HatyuDao;
import jp.co.stationery.dao.SyohinDao;
import jp.co.stationery.model.HatyuDetail;
import jp.co.stationery.model.HatyuHeader;
import jp.co.stationery.model.Syohin;
import jp.co.stationery.util.Codes;
import jp.co.stationery.util.DateUtil;
import jp.co.stationery.util.HtmlEscape;
import jp.co.stationery.util.HttpUtil;
import jp.co.stationery.util.SousaLogger;
import jp.co.stationery.util.TemplateEngine;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * F08 発注ハンドラ。
 * GET  /hatyu          → 一覧
 * GET  /hatyu/new      → 入力フォーム
 * POST /hatyu          → 登録
 * POST /hatyu/cancel   → 取消
 */
public final class HatyuHandler extends HandlerBase implements HttpHandler {

    private final HatyuDao dao = new HatyuDao();
    private final SyohinDao syohinDao = new SyohinDao();

    @Override
    public void handle(final HttpExchange ex) throws IOException {
        final String syainNo = requireLogin(ex);
        if (syainNo == null) {
            return;
        }
        try {
            final String path = ex.getRequestURI().getPath();
            final String method = ex.getRequestMethod();
            if ("/hatyu".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleList(ex, syainNo);
            } else if ("/hatyu/new".equals(path)) {
                handleForm(ex, syainNo);
            } else if ("/hatyu".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCreate(ex, syainNo);
            } else if ("/hatyu/cancel".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCancel(ex, syainNo);
            } else {
                HttpUtil.sendText(ex, 404, "Not Found");
            }
        } catch (Exception e) {
            SousaLogger.log(SousaLogger.OP_ERROR, "F08", null, e.getMessage(),
                            SousaLogger.RESULT_NG, syainNo);
            sendError(ex, e);
        }
    }

    // 一覧
    private void handleList(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> q = HttpUtil.readQuery(ex);
        final String hano = HttpUtil.param(q, "hano");
        final String trcd = HttpUtil.param(q, "trcd");
        final LocalDate from = DateUtil.parseLocalDate(HttpUtil.param(q, "haf"));
        final LocalDate to = DateUtil.parseLocalDate(HttpUtil.param(q, "hat"));
        final String hast = HttpUtil.param(q, "hast");
        final List<HatyuHeader> list = dao.search(hano, trcd, from, to, hast);
        // 行構築
        final NumberFormat nf = NumberFormat.getNumberInstance(Locale.JAPAN);
        final StringBuilder rows = new StringBuilder();
        if (list.isEmpty()) {
            rows.append("<tr><td colspan=\"7\" class=\"table__center\">該当データはありません</td></tr>");
        } else {
            for (final HatyuHeader h : list) {
                rows.append("<tr>")
                    .append("<td class=\"mono\">").append(HtmlEscape.escape(h.hano)).append("</td>")
                    .append("<td class=\"mono\">").append(h.hadt == null ? "" : h.hadt.toString()).append("</td>")
                    .append("<td>")
                        .append(HtmlEscape.escape(h.hatr)).append(" ")
                        .append(HtmlEscape.escape(h.hatrnm == null ? "" : h.hatrnm))
                    .append("</td>")
                    .append("<td class=\"mono\">").append(h.hank == null ? "" : h.hank.toString()).append("</td>")
                    .append("<td class=\"table__num mono\">")
                        .append(nf.format(h.hasm == null ? BigDecimal.ZERO : h.hasm)).append("</td>")
                    .append("<td>").append(HtmlEscape.escape(Codes.hatyuStateName(h.hast))).append("</td>")
                    .append("<td>")
                        .append("<form method=\"post\" action=\"/hatyu/cancel\" style=\"display:inline\">")
                        .append("<input type=\"hidden\" name=\"hano\" value=\"")
                        .append(HtmlEscape.escape(h.hano)).append("\">")
                        .append("<button class=\"btn btn--sm\" type=\"submit\" ")
                        .append("onclick=\"return confirm('発注を取消しますか？')\">取消</button>")
                        .append("</form>")
                    .append("</td>")
                    .append("</tr>");
            }
        }
        final Map<String, String> params = baseParams(syainNo);
        params.put("rows", rows.toString());
        HttpUtil.sendHtml(ex, 200, TemplateEngine.render("hatyu_list.html", params));
    }

    // フォーム
    private void handleForm(final HttpExchange ex, final String syainNo) throws Exception {
        HttpUtil.sendHtml(ex, 200, TemplateEngine.render("hatyu_form.html", baseParams(syainNo)));
    }

    // 登録
    private void handleCreate(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> form = HttpUtil.readFormBody(ex);
        final HatyuHeader h = new HatyuHeader();
        h.hadt = DateUtil.parseLocalDate(HttpUtil.param(form, "hadt"));
        h.hank = DateUtil.parseLocalDate(HttpUtil.param(form, "hank"));
        h.hatr = HttpUtil.param(form, "hatr");
        h.harm = HttpUtil.param(form, "harm");
        if (h.hadt == null || h.hank == null || h.hatr.isBlank()) {
            SousaLogger.log(SousaLogger.OP_CREATE, "F08", null,
                            "必須項目不足", SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/hatyu/new");
            return;
        }
        // 明細を最大10行まで
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 1; i <= 10; i++) {
            final String prdcd = HttpUtil.param(form, "dt_prdcd_" + i);
            final String qtyStr = HttpUtil.param(form, "dt_qty_" + i);
            final String costStr = HttpUtil.param(form, "dt_cost_" + i);
            if (prdcd.isBlank() || qtyStr.isBlank()) {
                continue;
            }
            int qty;
            try { qty = Integer.parseInt(qtyStr); }
            catch (NumberFormatException e) { continue; }
            if (qty <= 0) continue;
            BigDecimal cost;
            if (!costStr.isBlank()) {
                try { cost = new BigDecimal(costStr); }
                catch (NumberFormatException e) { cost = BigDecimal.ZERO; }
            } else {
                final Syohin p = syohinDao.findByCode(prdcd);
                cost = (p == null || p.prdpr == null) ? BigDecimal.ZERO : p.prdpr;
            }
            final HatyuDetail d = new HatyuDetail();
            d.prdcd = prdcd;
            d.haup = cost;
            d.haqt = qty;
            d.haam = cost.multiply(BigDecimal.valueOf(qty));
            h.details.add(d);
            total = total.add(d.haam);
        }
        h.hasm = total;
        if (h.details.isEmpty()) {
            SousaLogger.log(SousaLogger.OP_CREATE, "F08", null,
                            "明細未入力", SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/hatyu/new");
            return;
        }
        final String hano = dao.insert(h, syainNo);
        SousaLogger.log(SousaLogger.OP_CREATE, "F08", hano,
                        "発注登録 lines=" + h.details.size(),
                        SousaLogger.RESULT_OK, syainNo);
        HttpUtil.redirect(ex, "/hatyu");
    }

    // 取消
    private void handleCancel(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> form = HttpUtil.readFormBody(ex);
        final String hano = HttpUtil.param(form, "hano");
        if (hano.isBlank()) {
            HttpUtil.redirect(ex, "/hatyu");
            return;
        }
        dao.cancel(hano, syainNo);
        SousaLogger.log(SousaLogger.OP_UPDATE, "F08", hano,
                        "発注取消", SousaLogger.RESULT_OK, syainNo);
        HttpUtil.redirect(ex, "/hatyu");
    }

}
