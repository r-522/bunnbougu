package jp.co.stationery.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jp.co.stationery.config.AppConfig;
import jp.co.stationery.util.HttpUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静的アセット（CSS / JS）配信ハンドラ。
 * 認証不要。Frontend/css/* および Frontend/js/* を配信。
 * パストラバーサル防止のため、解決後のパスがTEMPLATE_DIR内にあることを必ず確認する。
 */
public final class StaticHandler implements HttpHandler {

    @Override
    public void handle(final HttpExchange ex) throws IOException {
        // リクエストパス取得
        final String reqPath = ex.getRequestURI().getPath();
        // /css/* と /js/* のみ受け付け
        if (!reqPath.startsWith("/css/") && !reqPath.startsWith("/js/")) {
            HttpUtil.sendText(ex, 404, "Not Found");
            return;
        }
        // テンプレートディレクトリ配下のファイルパスを構築
        final Path base = Paths.get(AppConfig.getTemplateDir()).toAbsolutePath().normalize();
        final Path file = base.resolve("." + reqPath).normalize();
        // パストラバーサル対策
        if (!file.startsWith(base)) {
            HttpUtil.sendText(ex, 403, "Forbidden");
            return;
        }
        if (!Files.exists(file) || Files.isDirectory(file)) {
            HttpUtil.sendText(ex, 404, "Not Found");
            return;
        }
        // 拡張子からContent-Typeを決定
        final String contentType;
        if (reqPath.endsWith(".css")) {
            contentType = "text/css; charset=UTF-8";
        } else if (reqPath.endsWith(".js")) {
            contentType = "application/javascript; charset=UTF-8";
        } else {
            contentType = "application/octet-stream";
        }
        // バイト読み込みして返却
        final byte[] body = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.getResponseHeaders().set("Cache-Control", "public, max-age=300");
        ex.sendResponseHeaders(200, body.length);
        ex.getResponseBody().write(body);
        ex.getResponseBody().close();
    }
}
