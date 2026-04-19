package jp.co.stationery.dao;

import jp.co.stationery.db.DbUtil;
import jp.co.stationery.model.HatyuDetail;
import jp.co.stationery.model.HatyuHeader;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 発注DAO（TBL_HATYU_HEADER / TBL_HATYU_DETAIL）。
 */
public final class HatyuDao {

    /** 条件検索（ヘッダのみ）。 */
    public List<HatyuHeader> search(final String hano, final String trcd,
                                    final LocalDate from, final LocalDate to,
                                    final String hast) throws SQLException {
        final StringBuilder sql = new StringBuilder(
            "SELECT h.hano, h.hadt, h.hatr, t.trnm AS hatrnm, h.hank, h.hast, h.hasm, h.harm " +
            "FROM TBL_HATYU_HEADER h " +
            "LEFT JOIN TBL_TORIHIKI t ON t.trcd = h.hatr " +
            "WHERE h.hadf = '0'");
        final List<Object> binds = new ArrayList<>();
        if (hano != null && !hano.isBlank()) {
            sql.append(" AND h.hano LIKE ?");
            binds.add(hano + "%");
        }
        if (trcd != null && !trcd.isBlank()) {
            sql.append(" AND h.hatr = ?");
            binds.add(trcd);
        }
        if (from != null) {
            sql.append(" AND h.hadt >= ?");
            binds.add(java.sql.Date.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND h.hadt <= ?");
            binds.add(java.sql.Date.valueOf(to));
        }
        if (hast != null && !hast.isBlank()) {
            sql.append(" AND h.hast = ?");
            binds.add(hast);
        }
        sql.append(" ORDER BY h.hano DESC");

        final List<HatyuHeader> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < binds.size(); i++) {
                ps.setObject(i + 1, binds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapHeader(rs));
                }
            }
        }
        return list;
    }

    /** 発注番号で1件取得（明細込）。 */
    public HatyuHeader findByNo(final String hano) throws SQLException {
        final String hs =
            "SELECT h.hano, h.hadt, h.hatr, t.trnm AS hatrnm, h.hank, h.hast, h.hasm, h.harm " +
            "FROM TBL_HATYU_HEADER h LEFT JOIN TBL_TORIHIKI t ON t.trcd = h.hatr " +
            "WHERE h.hano = ? AND h.hadf = '0'";
        final String ds =
            "SELECT d.hano, d.haln, d.prdcd, p.prdnm, d.haup, d.haqt, d.haiq, d.haam " +
            "FROM TBL_HATYU_DETAIL d LEFT JOIN TBL_SYOHIN p ON p.prdcd = d.prdcd " +
            "WHERE d.hano = ? AND d.dhdf = '0' ORDER BY d.haln";
        try (Connection con = DbUtil.getConnection()) {
            HatyuHeader h = null;
            try (PreparedStatement ps = con.prepareStatement(hs)) {
                ps.setString(1, hano);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        h = mapHeader(rs);
                    }
                }
            }
            if (h == null) {
                return null;
            }
            try (PreparedStatement ps = con.prepareStatement(ds)) {
                ps.setString(1, hano);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        h.details.add(mapDetail(rs));
                    }
                }
            }
            return h;
        }
    }

    /** 新規登録（ヘッダ＋明細）。 */
    public String insert(final HatyuHeader h, final String updatedBy) throws SQLException {
        try (Connection con = DbUtil.getConnection()) {
            con.setAutoCommit(false);
            try {
                // 発注番号採番（H + yyyyMM + 3桁）
                final String hano = nextHano(con, h.hadt);
                h.hano = hano;

                final String hsql =
                    "INSERT INTO TBL_HATYU_HEADER " +
                    "(hano, hadt, hatr, hank, hast, hasm, harm, hact, hacb, haut, haub, hadf) " +
                    "VALUES (?, ?, ?, ?, '1', ?, ?, ?, ?, ?, ?, '0')";
                try (PreparedStatement ps = con.prepareStatement(hsql)) {
                    final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                    ps.setString(1, hano);
                    ps.setDate(2, java.sql.Date.valueOf(h.hadt));
                    ps.setString(3, h.hatr);
                    ps.setDate(4, java.sql.Date.valueOf(h.hank));
                    ps.setBigDecimal(5, h.hasm == null ? BigDecimal.ZERO : h.hasm);
                    ps.setString(6, h.harm == null ? "" : h.harm);
                    ps.setTimestamp(7, now);
                    ps.setString(8, updatedBy);
                    ps.setTimestamp(9, now);
                    ps.setString(10, updatedBy);
                    ps.executeUpdate();
                }

                final String dsql =
                    "INSERT INTO TBL_HATYU_DETAIL " +
                    "(hano, haln, prdcd, haup, haqt, haiq, haam, dhct, dhcb, dhut, dhub, dhdf) " +
                    "VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, '0')";
                try (PreparedStatement ps = con.prepareStatement(dsql)) {
                    final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                    int lineNo = 1;
                    for (final HatyuDetail d : h.details) {
                        ps.setString(1, hano);
                        ps.setInt(2, lineNo++);
                        ps.setString(3, d.prdcd);
                        ps.setBigDecimal(4, d.haup);
                        ps.setInt(5, d.haqt);
                        ps.setBigDecimal(6, d.haam);
                        ps.setTimestamp(7, now);
                        ps.setString(8, updatedBy);
                        ps.setTimestamp(9, now);
                        ps.setString(10, updatedBy);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                con.commit();
                return hano;
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /** 取消（状態を9へ更新）。 */
    public void cancel(final String hano, final String updatedBy) throws SQLException {
        final String sql = "UPDATE TBL_HATYU_HEADER SET hast = '9', haut = ?, haub = ? " +
                           "WHERE hano = ? AND hadf = '0'";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, updatedBy);
            ps.setString(3, hano);
            ps.executeUpdate();
        }
    }

    /** 明細の入荷済数を加算し、ヘッダ状態を更新する（入荷処理）。 */
    public void applyNyuka(final Connection con,
                           final String hano, final int lineNo, final int addQty,
                           final String updatedBy) throws SQLException {
        // 明細の入荷済数を加算
        final String sql =
            "UPDATE TBL_HATYU_DETAIL SET haiq = haiq + ?, dhut = ?, dhub = ? " +
            "WHERE hano = ? AND haln = ? AND dhdf = '0'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, addQty);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, updatedBy);
            ps.setString(4, hano);
            ps.setInt(5, lineNo);
            ps.executeUpdate();
        }
    }

    /** 全明細が入荷完了しているかを判定し、ヘッダ状態を更新する。 */
    public void refreshStatus(final Connection con, final String hano, final String updatedBy) throws SQLException {
        // 未入荷数合計を取得
        final String sumSql =
            "SELECT COALESCE(SUM(haqt - haiq), 0), COALESCE(SUM(haiq), 0) " +
            "FROM TBL_HATYU_DETAIL WHERE hano = ? AND dhdf = '0'";
        int remain = 0;
        int received = 0;
        try (PreparedStatement ps = con.prepareStatement(sumSql)) {
            ps.setString(1, hano);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                remain = rs.getInt(1);
                received = rs.getInt(2);
            }
        }
        // 状態判定：完了/一部/発注済
        final String newStatus;
        if (remain <= 0) {
            newStatus = "3";
        } else if (received > 0) {
            newStatus = "2";
        } else {
            newStatus = "1";
        }
        // ヘッダ更新
        final String uSql = "UPDATE TBL_HATYU_HEADER SET hast = ?, haut = ?, haub = ? WHERE hano = ?";
        try (PreparedStatement ps = con.prepareStatement(uSql)) {
            ps.setString(1, newStatus);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, updatedBy);
            ps.setString(4, hano);
            ps.executeUpdate();
        }
    }

    /** 発注番号採番。 */
    private String nextHano(final Connection con, final LocalDate hadt) throws SQLException {
        final String ym = String.format("%04d%02d", hadt.getYear(), hadt.getMonthValue());
        final String prefix = "H" + ym;
        final String sql = "SELECT COALESCE(MAX(hano), '') FROM TBL_HATYU_HEADER WHERE hano LIKE ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                final String max = rs.getString(1);
                int seq = 1;
                if (max != null && !max.isEmpty()) {
                    seq = Integer.parseInt(max.substring(prefix.length())) + 1;
                }
                return prefix + String.format("%03d", seq);
            }
        }
    }

    // ヘッダマッピング
    private HatyuHeader mapHeader(final ResultSet rs) throws SQLException {
        final HatyuHeader h = new HatyuHeader();
        h.hano = rs.getString("hano");
        final java.sql.Date dt = rs.getDate("hadt");
        h.hadt = dt == null ? null : dt.toLocalDate();
        h.hatr = rs.getString("hatr");
        h.hatrnm = rs.getString("hatrnm");
        final java.sql.Date nk = rs.getDate("hank");
        h.hank = nk == null ? null : nk.toLocalDate();
        h.hast = rs.getString("hast");
        h.hasm = rs.getBigDecimal("hasm");
        h.harm = rs.getString("harm");
        return h;
    }

    // 明細マッピング
    private HatyuDetail mapDetail(final ResultSet rs) throws SQLException {
        final HatyuDetail d = new HatyuDetail();
        d.hano = rs.getString("hano");
        d.haln = rs.getInt("haln");
        d.prdcd = rs.getString("prdcd");
        d.prdnm = rs.getString("prdnm");
        d.haup = rs.getBigDecimal("haup");
        d.haqt = rs.getInt("haqt");
        d.haiq = rs.getInt("haiq");
        d.haam = rs.getBigDecimal("haam");
        return d;
    }
}
