package jp.co.stationery.util;

import jp.co.stationery.db.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 操作履歴ロガー。
 * 全操作（ログイン/登録/更新/削除/エラー）を TBL_SOUSA_LOG に永続化する。
 * READMEの「チェック例外は握り潰さない、必ずTBL_SOUSA_LOGに記録」要件を満たす。
 */
public final class SousaLogger {

    // 操作区分定数
    public static final String OP_LOGIN  = "LOGIN";
    public static final String OP_LOGOUT = "LOGOUT";
    public static final String OP_CREATE = "CREATE";
    public static final String OP_UPDATE = "UPDATE";
    public static final String OP_DELETE = "DELETE";
    public static final String OP_ERROR  = "ERROR";

    // 結果区分定数
    public static final String RESULT_OK = "OK";
    public static final String RESULT_NG = "NG";

    // ログ挿入SQL（PreparedStatement利用）
    private static final String INSERT_SQL =
        "INSERT INTO TBL_SOUSA_LOG " +
        "(slop, sltg, slky, slms, slrs, slcb, slct) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?)";

    // インスタンス化禁止
    private SousaLogger() {
    }

    /**
     * 操作ログを記録する。DBエラー時は標準エラー出力にのみ出してアプリ本流は止めない。
     *
     * @param op       操作区分（OP_*定数）
     * @param target   対象機能（例: "F07"）
     * @param key      対象キー（例: 受注番号）。null許容
     * @param message  操作内容
     * @param result   RESULT_OK / RESULT_NG
     * @param syainNo  実行社員番号。null時は"-"
     */
    public static void log(final String op,
                           final String target,
                           final String key,
                           final String message,
                           final String result,
                           final String syainNo) {
        // try-with-resourcesでConnection/PreparedStatementを確実にクローズ
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(INSERT_SQL)) {
            // 各パラメータを順にバインド
            ps.setString(1, op);
            ps.setString(2, target);
            // 対象キーは最大40桁程度を想定、nullはそのままsetNull
            if (key == null) {
                ps.setNull(3, java.sql.Types.VARCHAR);
            } else {
                ps.setString(3, key);
            }
            ps.setString(4, message == null ? "" : message);
            ps.setString(5, result);
            // 実行社員番号（未ログイン時は"-"で代用）
            ps.setString(6, syainNo == null ? "-" : syainNo);
            // タイムスタンプは現在時刻
            ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now()));
            // 実行
            ps.executeUpdate();
        } catch (SQLException e) {
            // ログ書込自体の失敗はコンソールに出力してアプリは継続
            System.err.println("操作履歴の書込に失敗: " + e.getMessage());
        }
    }
}
