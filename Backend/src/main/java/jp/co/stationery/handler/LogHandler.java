package jp.co.stationery.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jp.co.stationery.dao.SousaLogDao;
import jp.co.stationery.model.SousaLog;
import jp.co.stationery.util.HtmlEscape;
import jp.co.stationery.util.HttpUtil;
import jp.co.stationery.util.SousaLogger;
import jp.co.stationery.util.TemplateEngine;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * F11 操作履歴照会ハンドラ。
 * GET /log → 検索結果一覧表示。
 */
public final class LogHandler extends HandlerBase implements HttpHandler {

    private final SousaLogDao dao = new SousaLogDao();

    @Override
    public void handle(final HttpExchange ex) throws IOException {
        final String syainNo = requireLogin(ex);
        if (syainNo == null) {
            return;
        }
        try {
            final Map<String, String> q = HttpUtil.readQuery(ex);
            final LocalDate from = parseDate(HttpUtil.param(q, "logf"));
            final LocalDate to = parseDate(HttpUtil.param(q, "logt"));
            final String sycd = HttpUtil.param(q, "sycd");
            final String lgop = HttpUtil.param(q, "lgop");
            final String lgtg = HttpUtil.param(q, "lgtg");
            final List<SousaLog> list = dao.search(from, to, sycd, lgop, lgtg);
            // 行構築
            final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            final StringBuilder rows = new StringBuilder();
            if (list.isEmpty()) {
                rows.append("<tr><td colspan=\"8\" class=\"table__center\">該当データはありません</td></tr>");
            } else {
                for (final SousaLog l : list) {
                    rows.append("<tr>")
                        .append("<td class=\"mono\">")
                            .append(l.slct == null ? "" : l.slct.format(dtf)).append("</td>")
                        .append("<td class=\"mono\">").append(HtmlEscape.escape(l.slcb)).append("</td>")
                        .append("<td>").append(HtmlEscape.escape(l.synm)).append("</td>")
                        .append("<td>").append(HtmlEscape.escape(l.slop)).append("</td>")
                        .append("<td>").append(HtmlEscape.escape(l.sltg)).append("</td>")
                        .append("<td class=\"mono\">").append(HtmlEscape.escape(l.slky)).append("</td>")
                        .append("<td>").append(HtmlEscape.escape(l.slms)).append("</td>")
                        .append("<td>").append(HtmlEscape.escape(l.slrs)).append("</td>")
                        .append("</tr>");
                }
            }
            final Map<String, String> params = baseParams(syainNo);
            params.put("rows", rows.toString());
            HttpUtil.sendHtml(ex, 200, TemplateEngine.render("log.html", params));
        } catch (Exception e) {
            SousaLogger.log(SousaLogger.OP_ERROR, "F11", null, e.getMessage(),
                            SousaLogger.RESULT_NG, syainNo);
            sendError(ex, e);
        }
    }

    private LocalDate parseDate(final String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }
}
