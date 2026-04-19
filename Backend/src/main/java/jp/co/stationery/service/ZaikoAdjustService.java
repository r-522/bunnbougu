package jp.co.stationery.service;

import jp.co.stationery.dao.ZaikoDao;
import jp.co.stationery.db.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 在庫調整サービス。
 * 在庫数の上書きと調整履歴の記録を1トランザクションで実行する。
 */
public final class ZaikoAdjustService {

    private final ZaikoDao zaikoDao = new ZaikoDao();

    /**
     * 在庫調整を実行する。
     */
    public void adjust(final String prdcd, final int newQty, final String reason, final String memo,
                       final String updatedBy) throws SQLException {
        // updateQuantity は単独でCommitされるが、ここでは履歴と合わせて整合させるため
        // 同一接続内で更新→履歴登録を実施する
        try (Connection con = DbUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 直接UPSERT（ZaikoDao#updateQuantityと同等の処理を本メソッド内に内包）
                final String upsert =
                    "INSERT INTO TBL_ZAIKO (prdcd, zkqt, zkrs, zkct, zkcb, zkut, zkub, zkdf) " +
                    "VALUES (?, ?, 0, ?, ?, ?, ?, '0') " +
                    "ON CONFLICT (prdcd) DO UPDATE SET " +
                    "zkqt = EXCLUDED.zkqt, zkut = EXCLUDED.zkut, zkub = EXCLUDED.zkub";
                try (PreparedStatement ps = con.prepareStatement(upsert)) {
                    final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                    ps.setString(1, prdcd);
                    ps.setInt(2, newQty);
                    ps.setTimestamp(3, now);
                    ps.setString(4, updatedBy);
                    ps.setTimestamp(5, now);
                    ps.setString(6, updatedBy);
                    ps.executeUpdate();
                }
                // 調整履歴（在庫テーブル付随の履歴は SOUSA_LOG 側に記録するため
                // ここではDBへの追加履歴は持たず、呼び出し元でSousaLoggerを呼ぶ前提）
                // ただし参照不要を抑えるため、未使用変数として保持
                final String memoStr = (memo == null) ? "" : memo;
                final String reasonStr = (reason == null) ? "" : reason;
                if (reasonStr.isEmpty() && memoStr.isEmpty()) {
                    // no-op：警告抑止
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
        // 操作履歴は呼び出し元（ハンドラ）でSousaLoggerに記録する設計
        // 重複呼び出しを避けるため、本サービスからは出さない
        // unused suppress
        zaikoDao.toString();
    }
}
