package jp.co.stationery.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jp.co.stationery.util.HttpUtil;

import java.io.IOException;

/**
 * Cloud Run用ヘルスチェックエンドポイント。
 * 認証不要。常に 200 を返す（DB疎通までは見ない）。
 */
public final class HealthHandler implements HttpHandler {

    @Override
    public void handle(final HttpExchange ex) throws IOException {
        HttpUtil.sendText(ex, 200, "OK");
    }
}
