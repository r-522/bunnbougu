package jp.co.stationery.service;

import jp.co.stationery.dao.JutyuDao;
import jp.co.stationery.dao.ZaikoDao;
import jp.co.stationery.db.DbUtil;
import jp.co.stationery.model.JutyuDetail;
import jp.co.stationery.model.JutyuHeader;
import jp.co.stationery.model.Zaiko;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 出荷業務サービス。
 * 受注明細の出荷数量加算・在庫減算・受注ヘッダ状態更新・TBL_SYUKKA への実績登録を1トランザクションで処理する。
 * 在庫不足時は IllegalStateException を送出してトランザクションをロールバックする。
 */
public final class SyukkaService {

    private final JutyuDao jutyuDao = new JutyuDao();
    private final ZaikoDao zaikoDao = new ZaikoDao();

    /**
     * 出荷登録を実行する。
     */
    public void register(final String juno, final LocalDate sydt, final String sycr, final String sytn,
                         final Map<Integer, Integer> qtyByLine, final String updatedBy) throws SQLException {
        // 受注情報を取得
        final JutyuHeader h = jutyuDao.findByNo(juno);
        if (h == null) {
            throw new IllegalArgumentException("受注が見つかりません: " + juno);
        }
        try (Connection con = DbUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 各明細について出荷処理
                for (final JutyuDetail d : h.details) {
                    final Integer qty = qtyByLine.get(d.juln);
                    if (qty == null || qty <= 0) {
                        continue;
                    }
                    // 既出荷数を取得し、未出荷残量を求める
                    final int alreadyShipped = sumShippedQty(con, juno, d.juln);
                    final int remain = d.juqt - alreadyShipped;
                    final int effective = Math.min(qty, remain);
                    if (effective <= 0) {
                        continue;
                    }
                    // 在庫を取得（同一トランザクション接続を渡してロック整合性を確保）
                    final Zaiko z = zaikoDao.findByCode(d.prdcd, con);
                    final int avail = (z == null) ? 0 : (z.zkqt - z.zkrs);
                    // 在庫不足チェック
                    if (avail < effective) {
                        throw new IllegalStateException(
                            "在庫不足: 商品=" + d.prdcd + " 必要=" + effective + " 有効在庫=" + avail);
                    }
                    // 在庫減算
                    zaikoDao.subtractQuantity(con, d.prdcd, effective, updatedBy);
                    // 出荷実績を登録（スキーマ上 prdcd カラムは無いため (juno, juln) のみで紐付け）
                    insertSyukka(con, juno, d.juln, sydt, effective, sycr, sytn, updatedBy);
                }
                // 受注ヘッダ状態を更新（簡易：未出荷残無し→出荷済(3)、それ以外→出荷準備中(2)）
                final int totalRemain = sumRemainQty(con, juno);
                final String newStatus = (totalRemain <= 0) ? "3" : "2";
                jutyuDao.updateStatus(con, juno, newStatus, updatedBy);
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } catch (IllegalStateException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    // 既出荷数量の合計を取得（指定明細行）
    private int sumShippedQty(final Connection con, final String juno, final int lineNo) throws SQLException {
        final String sql = "SELECT COALESCE(SUM(syqt), 0) FROM TBL_SYUKKA " +
                           "WHERE juno = ? AND juln = ? AND sydf = '0'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, juno);
            ps.setInt(2, lineNo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // 未出荷残量合計（受注ヘッダ全体）
    private int sumRemainQty(final Connection con, final String juno) throws SQLException {
        final String sql =
            "SELECT COALESCE(SUM(d.juqt) - " +
            "       COALESCE((SELECT SUM(s.syqt) FROM TBL_SYUKKA s WHERE s.juno = d.juno AND s.juln = d.juln AND s.sydf='0'), 0), 0) " +
            "FROM TBL_JUTYU_DETAIL d WHERE d.juno = ? AND d.dfdf = '0'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, juno);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // TBL_SYUKKA に出荷実績を1行追加。
    // スキーマ（init.sql）では prdcd カラムは存在しないため、(juno, juln) のみで紐付けする。
    private void insertSyukka(final Connection con, final String juno, final int lineNo,
                              final LocalDate sydt, final int qty, final String sycr, final String sytn,
                              final String updatedBy) throws SQLException {
        final String sql =
            "INSERT INTO TBL_SYUKKA " +
            "(juno, juln, sydt, syqt, sycr, sytn, syct, sycb, syut, syub, sydf) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0')";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setString(1, juno);
            ps.setInt(2, lineNo);
            ps.setDate(3, java.sql.Date.valueOf(sydt));
            ps.setInt(4, qty);
            ps.setString(5, sycr == null ? "" : sycr);
            ps.setString(6, sytn == null ? "" : sytn);
            ps.setTimestamp(7, now);
            ps.setString(8, updatedBy);
            ps.setTimestamp(9, now);
            ps.setString(10, updatedBy);
            ps.executeUpdate();
        }
    }
}
