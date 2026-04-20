package jp.co.stationery.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jp.co.stationery.dao.JutyuDao;
import jp.co.stationery.dao.SyohinDao;
import jp.co.stationery.model.JutyuDetail;
import jp.co.stationery.model.JutyuHeader;
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
 * F07 受注ハンドラ。
 * GET  /jutyu          → 一覧
 * GET  /jutyu/new      → 入力フォーム
 * POST /jutyu          → 登録
 * POST /jutyu/cancel   → 取消
 */
public final class JutyuHandler extends HandlerBase implements HttpHandler {

    private final JutyuDao dao = new JutyuDao();
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
            if ("/jutyu".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleList(ex, syainNo);
            } else if ("/jutyu/new".equals(path)) {
                handleForm(ex, syainNo);
            } else if ("/jutyu".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCreate(ex, syainNo);
            } else if ("/jutyu/cancel".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCancel(ex, syainNo);
            } else {
                HttpUtil.sendText(ex, 404, "Not Found");
            }
        } catch (Exception e) {
            SousaLogger.log(SousaLogger.OP_ERROR, "F07", null, e.getMessage(),
                            SousaLogger.RESULT_NG, syainNo);
            sendError(ex, e);
        }
    }

    // 一覧
    private void handleList(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> q = HttpUtil.readQuery(ex);
        final String juno = HttpUtil.param(q, "juno");
        final String trcd = HttpUtil.param(q, "trcd");
        final LocalDate from = DateUtil.parseLocalDate(HttpUtil.param(q, "juf"));
        final LocalDate to = DateUtil.parseLocalDate(HttpUtil.param(q, "jut"));
        final String just = HttpUtil.param(q, "just");
        final List<JutyuHeader> list = dao.search(juno, trcd, from, to, just);
        // 行構築
        final NumberFormat nf = NumberFormat.getNumberInstance(Locale.JAPAN);
        final StringBuilder rows = new StringBuilder();
        if (list.isEmpty()) {
            rows.append("<tr><td colspan=\"7\" class=\"table__center\">該当データはありません</td></tr>");
        } else {
            for (final JutyuHeader h : list) {
                rows.append("<tr>")
                    .append("<td class=\"mono\">").append(HtmlEscape.escape(h.juno)).append("</td>")
                    .append("<td class=\"mono\">").append(h.judt == null ? "" : h.judt.toString()).append("</td>")
                    .append("<td>")
                        .append(HtmlEscape.escape(h.jutr)).append(" ")
                        .append(HtmlEscape.escape(h.jutrnm == null ? "" : h.jutrnm))
                    .append("</td>")
                    .append("<td class=\"mono\">").append(h.junk == null ? "" : h.junk.toString()).append("</td>")
                    .append("<td class=\"table__num mono\">")
                        .append(nf.format(h.jusm == null ? BigDecimal.ZERO : h.jusm)).append("</td>")
                    .append("<td>").append(HtmlEscape.escape(Codes.jutyuStateName(h.just))).append("</td>")
                    .append("<td>")
                        .append("<form method=\"post\" action=\"/jutyu/cancel\" style=\"display:inline\">")
                        .append("<input type=\"hidden\" name=\"juno\" value=\"")
                        .append(HtmlEscape.escape(h.juno)).append("\">")
                        .append("<button class=\"btn btn--sm\" type=\"submit\" ")
                        .append("onclick=\"return confirm('受注を取消しますか？')\">取消</button>")
                        .append("</form>")
                    .append("</td>")
                    .append("</tr>");
            }
        }
        final Map<String, String> params = baseParams(syainNo);
        params.put("rows", rows.toString());
        HttpUtil.sendHtml(ex, 200, TemplateEngine.render("jutyu_list.html", params));
    }

    // フォーム
    private void handleForm(final HttpExchange ex, final String syainNo) throws Exception {
        HttpUtil.sendHtml(ex, 200, TemplateEngine.render("jutyu_form.html", baseParams(syainNo)));
    }

    // 登録
    private void handleCreate(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> form = HttpUtil.readFormBody(ex);
        // ヘッダ情報組立
        final JutyuHeader h = new JutyuHeader();
        h.judt = DateUtil.parseLocalDate(HttpUtil.param(form, "judt"));
        h.junk = DateUtil.parseLocalDate(HttpUtil.param(form, "junk"));
        h.jutr = HttpUtil.param(form, "jutr");
        h.jurm = HttpUtil.param(form, "jurm");
        if (h.judt == null || h.junk == null || h.jutr.isBlank()) {
            SousaLogger.log(SousaLogger.OP_CREATE, "F07", null,
                            "必須項目不足", SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/jutyu/new");
            return;
        }
        // 明細を最大10行まで読む（dt_prdcd_n / dt_qty_n / dt_prdpr_n）
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 1; i <= 10; i++) {
            final String prdcd = HttpUtil.param(form, "dt_prdcd_" + i);
            final String qtyStr = HttpUtil.param(form, "dt_qty_" + i);
            final String prStr = HttpUtil.param(form, "dt_prdpr_" + i);
            // どちらか欠落なら行スキップ
            if (prdcd.isBlank() || qtyStr.isBlank()) {
                continue;
            }
            int qty;
            try {
                qty = Integer.parseInt(qtyStr);
            } catch (NumberFormatException e) {
                continue;
            }
            if (qty <= 0) {
                continue;
            }
            // 単価はフォーム値優先、無ければマスタ参照
            BigDecimal price;
            if (!prStr.isBlank()) {
                try { price = new BigDecimal(prStr); }
                catch (NumberFormatException e) { price = BigDecimal.ZERO; }
            } else {
                final Syohin p = syohinDao.findByCode(prdcd);
                price = (p == null || p.prdpr == null) ? BigDecimal.ZERO : p.prdpr;
            }
            // 明細追加
            final JutyuDetail d = new JutyuDetail();
            d.prdcd = prdcd;
            d.juup = price;
            d.juqt = qty;
            d.juam = price.multiply(BigDecimal.valueOf(qty));
            h.details.add(d);
            total = total.add(d.juam);
        }
        h.jusm = total;
        // 明細1行も無い場合エラー
        if (h.details.isEmpty()) {
            SousaLogger.log(SousaLogger.OP_CREATE, "F07", null,
                            "明細未入力", SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/jutyu/new");
            return;
        }
        // 登録
        final String juno = dao.insert(h, syainNo);
        SousaLogger.log(SousaLogger.OP_CREATE, "F07", juno,
                        "受注登録 lines=" + h.details.size(),
                        SousaLogger.RESULT_OK, syainNo);
        HttpUtil.redirect(ex, "/jutyu");
    }

    // 取消
    private void handleCancel(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> form = HttpUtil.readFormBody(ex);
        final String juno = HttpUtil.param(form, "juno");
        if (juno.isBlank()) {
            HttpUtil.redirect(ex, "/jutyu");
            return;
        }
        dao.cancel(juno, syainNo);
        SousaLogger.log(SousaLogger.OP_UPDATE, "F07", juno,
                        "受注取消", SousaLogger.RESULT_OK, syainNo);
        HttpUtil.redirect(ex, "/jutyu");
    }

}
