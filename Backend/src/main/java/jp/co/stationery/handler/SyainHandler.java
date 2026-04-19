package jp.co.stationery.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jp.co.stationery.dao.SyainDao;
import jp.co.stationery.model.Syain;
import jp.co.stationery.util.HtmlEscape;
import jp.co.stationery.util.HttpUtil;
import jp.co.stationery.util.SousaLogger;
import jp.co.stationery.util.TemplateEngine;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * F05 社員マスタ ハンドラ。
 * GET  /syain          → 一覧
 * GET  /syain/new      → 新規登録フォーム
 * POST /syain          → 新規登録
 * POST /syain/delete   → 論理削除
 */
public final class SyainHandler extends HandlerBase implements HttpHandler {

    private final SyainDao dao = new SyainDao();

    @Override
    public void handle(final HttpExchange ex) throws IOException {
        final String syainNo = requireLogin(ex);
        if (syainNo == null) {
            return;
        }
        try {
            final String path = ex.getRequestURI().getPath();
            final String method = ex.getRequestMethod();
            if ("/syain".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleList(ex, syainNo);
            } else if ("/syain/new".equals(path)) {
                handleForm(ex, syainNo);
            } else if ("/syain".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCreate(ex, syainNo);
            } else if ("/syain/delete".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleDelete(ex, syainNo);
            } else {
                HttpUtil.sendText(ex, 404, "Not Found");
            }
        } catch (Exception e) {
            SousaLogger.log(SousaLogger.OP_ERROR, "F05", null, e.getMessage(),
                            SousaLogger.RESULT_NG, syainNo);
            sendError(ex, e);
        }
    }

    private void handleList(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> q = HttpUtil.readQuery(ex);
        final String sycd = HttpUtil.param(q, "sycd");
        final String synm = HttpUtil.param(q, "synm");
        final String sybs = HttpUtil.param(q, "sybs");
        final List<Syain> list = dao.search(sycd, synm, sybs);
        // 行構築
        final StringBuilder rows = new StringBuilder();
        if (list.isEmpty()) {
            rows.append("<tr><td colspan=\"7\" class=\"table__center\">該当データはありません</td></tr>");
        } else {
            for (final Syain s : list) {
                rows.append("<tr>")
                    .append("<td class=\"mono\">").append(HtmlEscape.escape(s.sycd)).append("</td>")
                    .append("<td>").append(HtmlEscape.escape(s.synm)).append("</td>")
                    .append("<td>").append(HtmlEscape.escape(s.sykn)).append("</td>")
                    .append("<td>").append(HtmlEscape.escape(deptName(s.sybs))).append("</td>")
                    .append("<td>").append(HtmlEscape.escape(positionName(s.sypo))).append("</td>")
                    .append("<td class=\"mono\">-</td>")
                    .append("<td>")
                        .append("<form method=\"post\" action=\"/syain/delete\" style=\"display:inline\">")
                        .append("<input type=\"hidden\" name=\"sycd\" value=\"")
                        .append(HtmlEscape.escape(s.sycd)).append("\">")
                        .append("<button class=\"btn btn--sm\" type=\"submit\" ")
                        .append("onclick=\"return confirm('削除しますか？')\">削除</button>")
                        .append("</form>")
                    .append("</td>")
                    .append("</tr>");
            }
        }
        final Map<String, String> params = baseParams(syainNo);
        params.put("rows", rows.toString());
        HttpUtil.sendHtml(ex, 200, TemplateEngine.render("syain_list.html", params));
    }

    private void handleForm(final HttpExchange ex, final String syainNo) throws Exception {
        HttpUtil.sendHtml(ex, 200, TemplateEngine.render("syain_form.html", baseParams(syainNo)));
    }

    private void handleCreate(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> form = HttpUtil.readFormBody(ex);
        final Syain s = new Syain();
        s.sycd = HttpUtil.param(form, "sycd");
        s.synm = HttpUtil.param(form, "synm");
        s.sykn = HttpUtil.param(form, "sykn");
        s.sybs = HttpUtil.param(form, "sybs");
        s.sypo = HttpUtil.param(form, "sypo");
        s.syml = HttpUtil.param(form, "syml");
        // 必須チェック
        if (!s.sycd.matches("\\d{5}") || s.synm.isBlank() || s.sybs.isBlank()) {
            SousaLogger.log(SousaLogger.OP_CREATE, "F05", s.sycd,
                            "必須/形式不正", SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/syain/new");
            return;
        }
        dao.insert(s, syainNo);
        SousaLogger.log(SousaLogger.OP_CREATE, "F05", s.sycd,
                        "社員登録", SousaLogger.RESULT_OK, syainNo);
        HttpUtil.redirect(ex, "/syain");
    }

    private void handleDelete(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> form = HttpUtil.readFormBody(ex);
        final String sycd = HttpUtil.param(form, "sycd");
        if (sycd.isBlank()) {
            HttpUtil.redirect(ex, "/syain");
            return;
        }
        dao.logicalDelete(sycd, syainNo);
        SousaLogger.log(SousaLogger.OP_DELETE, "F05", sycd,
                        "社員論理削除", SousaLogger.RESULT_OK, syainNo);
        HttpUtil.redirect(ex, "/syain");
    }

    private String deptName(final String code) {
        if (code == null) return "";
        switch (code) {
            case "10": return "営業部";
            case "20": return "仕入部";
            case "30": return "物流部";
            case "40": return "情報システム部";
            case "90": return "管理部";
            default: return code;
        }
    }

    private String positionName(final String code) {
        if (code == null) return "";
        switch (code) {
            case "1": return "担当";
            case "2": return "主任";
            case "3": return "課長";
            case "4": return "部長";
            default: return code;
        }
    }
}
