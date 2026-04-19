package jp.co.stationery.service;

import jp.co.stationery.dao.HatyuDao;
import jp.co.stationery.dao.ZaikoDao;
import jp.co.stationery.db.DbUtil;
import jp.co.stationery.model.HatyuDetail;
import jp.co.stationery.model.HatyuHeader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 入荷業務サービス。
 * 発注明細の入荷済数量を加算、在庫加算、発注ヘッダ状態の再計算、
 * TBL_NYUKA への実績登録を1トランザクションで処理する。
 */
public final class NyukaService {

    private final HatyuDao hatyuDao = new HatyuDao();
    private final ZaikoDao zaikoDao = new ZaikoDao();

    /**
     * 入荷登録を実行する。
     *
     * @param hano      発注番号
     * @param nydt      入荷日
     * @param nyno      納品書番号（任意）
     * @param qtyByLine 行番号→入荷数量
     * @param updatedBy 実行社員番号
     */
    public void register(final String hano, final LocalDate nydt, final String nyno,
                         final Map<Integer, Integer> qtyByLine, final String updatedBy) throws SQLException {
        // 発注情報を取得（明細込）
        final HatyuHeader h = hatyuDao.findByNo(hano);
        if (h == null) {
            throw new IllegalArgumentException("発注が見つかりません: " + hano);
        }
        // トランザクション開始
        try (Connection con = DbUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 各明細行の入荷数量を反映
                for (final HatyuDetail d : h.details) {
                    final Integer qty = qtyByLine.get(d.haln);
                    if (qty == null || qty <= 0) {
                        continue;
                    }
                    // 過剰入荷ガード：残数以下とする
                    final int remain = d.haqt - d.haiq;
                    final int effective = Math.min(qty, remain);
                    if (effective <= 0) {
                        continue;
                    }
                    // 発注明細の入荷済数量を加算
                    hatyuDao.applyNyuka(con, hano, d.haln, effective, updatedBy);
                    // 在庫加算
                    zaikoDao.addQuantity(con, d.prdcd, effective, updatedBy);
                    // TBL_NYUKA レコード登録
                    insertNyuka(con, hano, d.haln, d.prdcd, nydt, effective, nyno, updatedBy);
                }
                // 発注ヘッダの状態を再計算
                hatyuDao.refreshStatus(con, hano, updatedBy);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    // TBL_NYUKA に入荷実績を1行追加
    private void insertNyuka(final Connection con, final String hano, final int lineNo, final String prdcd,
                             final LocalDate nydt, final int qty, final String nyno, final String updatedBy)
            throws SQLException {
        final String sql =
            "INSERT INTO TBL_NYUKA " +
            "(hano, haln, prdcd, nydt, nyqt, nyno, nyct, nycb, nyut, nyub, nydf) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0')";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setString(1, hano);
            ps.setInt(2, lineNo);
            ps.setString(3, prdcd);
            ps.setDate(4, java.sql.Date.valueOf(nydt));
            ps.setInt(5, qty);
            ps.setString(6, nyno == null ? "" : nyno);
            ps.setTimestamp(7, now);
            ps.setString(8, updatedBy);
            ps.setTimestamp(9, now);
            ps.setString(10, updatedBy);
            ps.executeUpdate();
        }
    }

    // 未使用import削減のためListを明示利用
    @SuppressWarnings("unused")
    private static List<HatyuDetail> dummy() { return null; }
}
