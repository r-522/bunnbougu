package jp.co.stationery.main;

import com.sun.net.httpserver.HttpServer;
import jp.co.stationery.config.AppConfig;
import jp.co.stationery.handler.AuthHandler;
import jp.co.stationery.handler.HatyuHandler;
import jp.co.stationery.handler.HealthHandler;
import jp.co.stationery.handler.JutyuHandler;
import jp.co.stationery.handler.LogHandler;
import jp.co.stationery.handler.MenuHandler;
import jp.co.stationery.handler.NyukaHandler;
import jp.co.stationery.handler.StaticHandler;
import jp.co.stationery.handler.SyainHandler;
import jp.co.stationery.handler.SyohinHandler;
import jp.co.stationery.handler.SyukkaHandler;
import jp.co.stationery.handler.TorihikiHandler;
import jp.co.stationery.handler.ZaikoHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * 文房具基幹システム エントリポイント。
 * com.sun.net.httpserver.HttpServer を使用し、各機能のハンドラを登録して起動する。
 * Cloud Run要件：PORT環境変数で待機、/healthz でヘルスチェック応答。
 */
public final class Main {

    private Main() {
    }

    public static void main(final String[] args) throws Exception {
        // ポート番号取得（Cloud Run: PORT、ローカル: 8080）
        final int port = AppConfig.getPort();
        // HTTPサーバ生成（バックログ0でデフォルト）
        final HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // ヘルスチェック（認証不要）
        server.createContext("/healthz", new HealthHandler());
        // ルート → メニューへリダイレクトしたいが、最小実装としてメニューハンドラを当てる
        server.createContext("/", new MenuHandler());

        // F01 認証
        server.createContext("/login", new AuthHandler());
        server.createContext("/logout", new AuthHandler());

        // F02 メニュー
        server.createContext("/menu", new MenuHandler());

        // 静的アセット
        server.createContext("/css/", new StaticHandler());
        server.createContext("/js/", new StaticHandler());

        // F03 商品マスタ
        final SyohinHandler syohin = new SyohinHandler();
        server.createContext("/syohin", syohin);

        // F04 取引先マスタ
        final TorihikiHandler torihiki = new TorihikiHandler();
        server.createContext("/torihiki", torihiki);

        // F05 社員マスタ
        final SyainHandler syain = new SyainHandler();
        server.createContext("/syain", syain);

        // F06 在庫
        final ZaikoHandler zaiko = new ZaikoHandler();
        server.createContext("/zaiko", zaiko);

        // F07 受注
        final JutyuHandler jutyu = new JutyuHandler();
        server.createContext("/jutyu", jutyu);

        // F08 発注
        final HatyuHandler hatyu = new HatyuHandler();
        server.createContext("/hatyu", hatyu);

        // F09 入荷
        server.createContext("/nyuka", new NyukaHandler());

        // F10 出荷
        server.createContext("/syukka", new SyukkaHandler());

        // F11 操作履歴
        server.createContext("/log", new LogHandler());

        // 並列処理のためスレッドプールを設定（CPU数の倍）
        server.setExecutor(Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors() * 2)));

        // 起動
        server.start();
        // 起動ログ
        System.out.println("Stationery Core System started on port " + port);
        System.out.println("Template dir: " + AppConfig.getTemplateDir());
    }
}
