package jp.co.stationery.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jp.co.stationery.dao.TorihikiDao;
import jp.co.stationery.model.Torihiki;
import jp.co.stationery.util.Codes;
import jp.co.stationery.util.HtmlEscape;
import jp.co.stationery.util.HttpUtil;
import jp.co.stationery.util.SousaLogger;
import jp.co.stationery.util.TemplateEngine;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * F04 取引先マスタ ハンドラ。
 * GET  /torihiki         → 一覧
 * GET  /torihiki/new     → 新規登録フォーム
 * POST /torihiki         → 新規登録
 * POST /torihiki/delete  → 論理削除
 */
public final class TorihikiHandler extends HandlerBase implements HttpHandler {

    private final TorihikiDao dao = new TorihikiDao();

    @Override
    public void handle(final HttpExchange ex) throws IOException {
        final String syainNo = requireLogin(ex);
        if (syainNo == null) {
            return;
        }
        try {
            final String path = ex.getRequestURI().getPath();
            final String method = ex.getRequestMethod();
            if ("/torihiki".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleList(ex, syainNo);
            } else if ("/torihiki/new".equals(path)) {
                handleForm(ex, syainNo);
            } else if ("/torihiki".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCreate(ex, syainNo);
            } else if ("/torihiki/delete".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleDelete(ex, syainNo);
            } else {
                HttpUtil.sendText(ex, 404, "Not Found");
            }
        } catch (Exception e) {
            SousaLogger.log(SousaLogger.OP_ERROR, "F04", null, e.getMessage(),
                            SousaLogger.RESULT_NG, syainNo);
            sendError(ex, e);
        }
    }

    // 一覧
    private void handleList(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> q = HttpUtil.readQuery(ex);
        final String trcd = HttpUtil.param(q, "trcd");
        final String trnm = HttpUtil.param(q, "trnm");
        final String trtp = HttpUtil.param(q, "trtp");
        final List<Torihiki> list = dao.search(trcd, trnm, trtp);
        // 行HTMLを構築
        final StringBuilder rows = new StringBuilder();
        if (list.isEmpty()) {
            rows.append("<tr><td colspan=\"7\" class=\"table__center\">該当データはありません</td></tr>");
        } else {
            for (final Torihiki t : list) {
                rows.append("<tr>")
                    .append("<td class=\"mono\">").append(HtmlEscape.escape(t.trcd)).append("</td>")
                    .append("<td>").append(HtmlEscape.escape(t.trnm)).append("</td>")
                    .append("<td>").append(HtmlEscape.escape(Codes.torihikiTypeName(t.trtp))).append("</td>")
                    .append("<td class=\"mono\">").append(HtmlEscape.escape(t.trtl)).append("</td>")
                    .append("<td>").append(HtmlEscape.escape(t.trpc)).append("</td>")
                    .append("<td class=\"mono\">-</td>")
                    .append("<td>")
                        .append("<form method=\"post\" action=\"/torihiki/delete\" style=\"display:inline\">")
                        .append("<input type=\"hidden\" name=\"trcd\" value=\"")
                        .append(HtmlEscape.escape(t.trcd)).append("\">")
                        .append("<button class=\"btn btn--sm\" type=\"submit\" ")
                        .append("onclick=\"return confirm('削除しますか？')\">削除</button>")
                        .append("</form>")
                    .append("</td>")
                    .append("</tr>");
            }
        }
        final Map<String, String> params = baseParams(syainNo);
        params.put("rows", rows.toString());
        HttpUtil.sendHtml(ex, 200, TemplateEngine.render("torihiki_list.html", params));
    }

    // フォーム
    private void handleForm(final HttpExchange ex, final String syainNo) throws Exception {
        HttpUtil.sendHtml(ex, 200, TemplateEngine.render("torihiki_form.html", baseParams(syainNo)));
    }

    // 登録
    private void handleCreate(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> form = HttpUtil.readFormBody(ex);
        final Torihiki t = new Torihiki();
        t.trcd = HttpUtil.param(form, "trcd");
        t.trnm = HttpUtil.param(form, "trnm");
        t.trtp = HttpUtil.param(form, "trtp");
        t.trzp = HttpUtil.param(form, "trzp");
        t.trad = HttpUtil.param(form, "trad");
        t.trtl = HttpUtil.param(form, "trtl");
        t.trpc = HttpUtil.param(form, "trpc");
        t.trml = HttpUtil.param(form, "trml");
        t.trrm = HttpUtil.param(form, "trrm");
        if (t.trcd.isBlank() || t.trnm.isBlank() || t.trtp.isBlank()) {
            SousaLogger.log(SousaLogger.OP_CREATE, "F04", t.trcd,
                            "必須項目不足", SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/torihiki/new");
            return;
        }
        dao.insert(t, syainNo);
        SousaLogger.log(SousaLogger.OP_CREATE, "F04", t.trcd,
                        "取引先登録", SousaLogger.RESULT_OK, syainNo);
        HttpUtil.redirect(ex, "/torihiki");
    }

    // 削除
    private void handleDelete(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> form = HttpUtil.readFormBody(ex);
        final String trcd = HttpUtil.param(form, "trcd");
        if (trcd.isBlank()) {
            HttpUtil.redirect(ex, "/torihiki");
            return;
        }
        dao.logicalDelete(trcd, syainNo);
        SousaLogger.log(SousaLogger.OP_DELETE, "F04", trcd,
                        "取引先論理削除", SousaLogger.RESULT_OK, syainNo);
        HttpUtil.redirect(ex, "/torihiki");
    }

}
