package jp.co.stationery.config;

/**
 * アプリケーション設定。
 * 環境変数からDB接続情報・ポート番号・テンプレートディレクトリを取得する。
 * Cloud Runでは環境変数PORT/DB_URL/DB_USER/DB_PASSWORDを設定する前提。
 */
public final class AppConfig {

    // インスタンス化を禁止（ユーティリティクラス）
    private AppConfig() {
    }

    // HTTPサーバのポート番号（Cloud Runの仕様に合わせデフォルト8080）
    public static int getPort() {
        // 環境変数PORTから取得、未設定時は8080
        final String port = System.getenv("PORT");
        // null or 空文字の場合は8080を返却
        if (port == null || port.isBlank()) {
            return 8080;
        }
        // 数値変換、失敗時は例外を伝播（起動時に気付けるようにする）
        return Integer.parseInt(port);
    }

    // JDBC接続URL（例: jdbc:postgresql://host:5432/dbname?sslmode=require）
    public static String getDbUrl() {
        // 必須設定：未設定時は明示的に例外送出
        return requireEnv("DB_URL");
    }

    // DBユーザ名
    public static String getDbUser() {
        return requireEnv("DB_USER");
    }

    // DBパスワード
    public static String getDbPassword() {
        return requireEnv("DB_PASSWORD");
    }

    // テンプレートディレクトリ（Frontend/配下のHTML）
    public static String getTemplateDir() {
        // 環境変数TEMPLATE_DIR、未設定時は"Frontend"
        final String dir = System.getenv("TEMPLATE_DIR");
        return (dir == null || dir.isBlank()) ? "Frontend" : dir;
    }

    // 必須環境変数取得：未設定時はIllegalStateExceptionを送出
    private static String requireEnv(final String key) {
        // 環境変数を取得
        final String value = System.getenv(key);
        // null or 空文字は起動時エラーとして扱う
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("環境変数が未設定です: " + key);
        }
        return value;
    }
}
