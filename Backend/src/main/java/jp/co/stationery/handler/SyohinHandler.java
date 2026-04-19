package jp.co.stationery.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jp.co.stationery.dao.SyohinDao;
import jp.co.stationery.model.Syohin;
import jp.co.stationery.util.HtmlEscape;
import jp.co.stationery.util.HttpUtil;
import jp.co.stationery.util.SousaLogger;
import jp.co.stationery.util.TemplateEngine;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * F03 商品マスタ ハンドラ。
 * GET  /syohin           → 一覧
 * GET  /syohin/new       → 新規登録フォーム
 * POST /syohin           → 新規登録
 * POST /syohin/delete    → 論理削除
 */
public final class SyohinHandler extends HandlerBase implements HttpHandler {

    private final SyohinDao dao = new SyohinDao();

    @Override
    public void handle(final HttpExchange ex) throws IOException {
        // 認証チェック
        final String syainNo = requireLogin(ex);
        if (syainNo == null) {
            return;
        }
        try {
            final String path = ex.getRequestURI().getPath();
            final String method = ex.getRequestMethod();
            if ("/syohin".equals(path) && "GET".equalsIgnoreCase(method)) {
                handleList(ex, syainNo);
            } else if ("/syohin/new".equals(path)) {
                handleForm(ex, syainNo);
            } else if ("/syohin".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleCreate(ex, syainNo);
            } else if ("/syohin/delete".equals(path) && "POST".equalsIgnoreCase(method)) {
                handleDelete(ex, syainNo);
            } else {
                HttpUtil.sendText(ex, 404, "Not Found");
            }
        } catch (Exception e) {
            SousaLogger.log(SousaLogger.OP_ERROR, "F03", null, e.getMessage(),
                            SousaLogger.RESULT_NG, syainNo);
            sendError(ex, e);
        }
    }

    // 一覧表示
    private void handleList(final HttpExchange ex, final String syainNo) throws Exception {
        // 検索条件取得
        final Map<String, String> q = HttpUtil.readQuery(ex);
        final String prdcd = HttpUtil.param(q, "prdcd");
        final String prdnm = HttpUtil.param(q, "prdnm");
        final String prdct = HttpUtil.param(q, "cate");
        // 検索実行
        final List<Syohin> list = dao.search(prdcd, prdnm, prdct);
        // 行HTMLを構築
        final StringBuilder rows = new StringBuilder();
        if (list.isEmpty()) {
            rows.append("<tr><td colspan=\"7\" class=\"table__center\">該当データはありません</td></tr>");
        } else {
            // 単価フォーマット用
            final NumberFormat nf = NumberFormat.getNumberInstance(Locale.JAPAN);
            for (final Syohin p : list) {
                rows.append("<tr>")
                    .append("<td class=\"mono\">").append(HtmlEscape.escape(p.prdcd)).append("</td>")
                    .append("<td>").append(HtmlEscape.escape(p.prdnm)).append("</td>")
                    .append("<td>").append(HtmlEscape.escape(categoryName(p.prdct))).append("</td>")
                    .append("<td class=\"table__num mono\">")
                        .append(nf.format(p.prdpr == null ? BigDecimal.ZERO : p.prdpr)).append("</td>")
                    .append("<td>").append(HtmlEscape.escape(p.prdun)).append("</td>")
                    .append("<td class=\"mono\">-</td>")
                    .append("<td>")
                        .append("<form method=\"post\" action=\"/syohin/delete\" style=\"display:inline\">")
                        .append("<input type=\"hidden\" name=\"prdcd\" value=\"")
                        .append(HtmlEscape.escape(p.prdcd)).append("\">")
                        .append("<button class=\"btn btn--sm\" type=\"submit\" ")
                        .append("onclick=\"return confirm('削除しますか？')\">削除</button>")
                        .append("</form>")
                    .append("</td>")
                    .append("</tr>");
            }
        }
        // テンプレートへ差込
        final Map<String, String> params = baseParams(syainNo);
        params.put("rows", rows.toString());
        HttpUtil.sendHtml(ex, 200, TemplateEngine.render("syohin_list.html", params));
    }

    // 新規登録フォーム表示
    private void handleForm(final HttpExchange ex, final String syainNo) throws Exception {
        HttpUtil.sendHtml(ex, 200, TemplateEngine.render("syohin_form.html", baseParams(syainNo)));
    }

    // 新規登録処理
    private void handleCreate(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> form = HttpUtil.readFormBody(ex);
        // フォーム→モデル
        final Syohin p = new Syohin();
        p.prdcd = HttpUtil.param(form, "prdcd");
        p.prdnm = HttpUtil.param(form, "prdnm");
        p.prdct = HttpUtil.param(form, "prdct");
        // 単価は数値変換（不正時は0扱い）
        final String pr = HttpUtil.param(form, "prdpr");
        try {
            p.prdpr = pr.isBlank() ? BigDecimal.ZERO : new BigDecimal(pr);
        } catch (NumberFormatException e) {
            p.prdpr = BigDecimal.ZERO;
        }
        p.prdun = HttpUtil.param(form, "prdun");
        p.prdrm = HttpUtil.param(form, "prdrm");
        // 必須チェック
        if (p.prdcd.isBlank() || p.prdnm.isBlank() || p.prdct.isBlank() || p.prdun.isBlank()) {
            SousaLogger.log(SousaLogger.OP_CREATE, "F03", p.prdcd,
                            "必須項目不足", SousaLogger.RESULT_NG, syainNo);
            HttpUtil.redirect(ex, "/syohin/new");
            return;
        }
        // 登録
        dao.insert(p, syainNo);
        SousaLogger.log(SousaLogger.OP_CREATE, "F03", p.prdcd,
                        "商品登録", SousaLogger.RESULT_OK, syainNo);
        HttpUtil.redirect(ex, "/syohin");
    }

    // 論理削除
    private void handleDelete(final HttpExchange ex, final String syainNo) throws Exception {
        final Map<String, String> form = HttpUtil.readFormBody(ex);
        final String prdcd = HttpUtil.param(form, "prdcd");
        if (prdcd.isBlank()) {
            HttpUtil.redirect(ex, "/syohin");
            return;
        }
        dao.logicalDelete(prdcd, syainNo);
        SousaLogger.log(SousaLogger.OP_DELETE, "F03", prdcd,
                        "商品論理削除", SousaLogger.RESULT_OK, syainNo);
        HttpUtil.redirect(ex, "/syohin");
    }

    // カテゴリコード→表示名
    private String categoryName(final String code) {
        if (code == null) return "";
        switch (code) {
            case "01": return "筆記具";
            case "02": return "ノート・紙製品";
            case "03": return "ファイル・バインダ";
            case "04": return "接着・修正";
            case "05": return "事務雑貨";
            default: return code;
        }
    }
}
