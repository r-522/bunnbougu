package jp.co.stationery.db;

import jp.co.stationery.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DBコネクション取得ユーティリティ。
 * 本システムは同時接続数が低負荷なのでコネクションプールは用いず、都度接続とする。
 * SQLは全てPreparedStatementを利用する前提（文字列結合SQL禁止）。
 */
public final class DbUtil {

    // インスタンス化禁止
    private DbUtil() {
    }

    // ドライバロード（static初期化で一度だけ実行）
    static {
        try {
            // PostgreSQL JDBCドライバのクラスを明示的にロード
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            // ドライバが見つからない＝起動不能。RuntimeExceptionで伝播
            throw new IllegalStateException("PostgreSQL JDBCドライバが見つかりません", e);
        }
    }

    /**
     * DB接続を取得する。
     * 呼び出し側はtry-with-resourcesで必ずクローズすること。
     *
     * @return 新規Connection
     * @throws SQLException 接続失敗時
     */
    public static Connection getConnection() throws SQLException {
        // 接続プロパティ：ユーザ・パスワード・SSL（本番はsslmode=requireをURLで指定）
        final Properties props = new Properties();
        props.setProperty("user", AppConfig.getDbUser());
        props.setProperty("password", AppConfig.getDbPassword());
        // DriverManager経由で接続取得
        return DriverManager.getConnection(AppConfig.getDbUrl(), props);
    }
}
