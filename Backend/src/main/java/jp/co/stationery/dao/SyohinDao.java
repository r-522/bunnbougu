package jp.co.stationery.dao;

import jp.co.stationery.db.DbUtil;
import jp.co.stationery.model.Syohin;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品マスタDAO（TBL_SYOHIN）。
 */
public final class SyohinDao {

    /**
     * 商品コードで1件取得。
     */
    public Syohin findByCode(final String prdcd) throws SQLException {
        final String sql =
            "SELECT prdcd, prdnm, prdct, prdpr, prdun, prdrm " +
            "FROM TBL_SYOHIN WHERE prdcd = ? AND prddf = '0'";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, prdcd);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    /**
     * 条件付き検索。
     */
    public List<Syohin> search(final String prdcd, final String prdnm, final String prdct) throws SQLException {
        final StringBuilder sql = new StringBuilder(
            "SELECT prdcd, prdnm, prdct, prdpr, prdun, prdrm " +
            "FROM TBL_SYOHIN WHERE prddf = '0'");
        final List<Object> binds = new ArrayList<>();
        // 商品コード：前方一致
        if (prdcd != null && !prdcd.isBlank()) {
            sql.append(" AND prdcd LIKE ?");
            binds.add(prdcd + "%");
        }
        // 商品名：部分一致
        if (prdnm != null && !prdnm.isBlank()) {
            sql.append(" AND prdnm LIKE ?");
            binds.add("%" + prdnm + "%");
        }
        // カテゴリ：完全一致
        if (prdct != null && !prdct.isBlank()) {
            sql.append(" AND prdct = ?");
            binds.add(prdct);
        }
        sql.append(" ORDER BY prdcd ASC");

        final List<Syohin> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < binds.size(); i++) {
                ps.setObject(i + 1, binds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 新規登録。
     */
    public void insert(final Syohin p, final String updatedBy) throws SQLException {
        final String sql =
            "INSERT INTO TBL_SYOHIN " +
            "(prdcd, prdnm, prdct, prdpr, prdun, prdrm, prdct2, prdcb, prdut, prdub, prddf) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0')";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setString(1, p.prdcd);
            ps.setString(2, p.prdnm);
            ps.setString(3, p.prdct);
            ps.setBigDecimal(4, p.prdpr);
            ps.setString(5, p.prdun);
            ps.setString(6, p.prdrm);
            ps.setTimestamp(7, now);
            ps.setString(8, updatedBy);
            ps.setTimestamp(9, now);
            ps.setString(10, updatedBy);
            ps.executeUpdate();
        }
    }

    /**
     * 更新。
     */
    public void update(final Syohin p, final String updatedBy) throws SQLException {
        final String sql =
            "UPDATE TBL_SYOHIN SET " +
            "prdnm = ?, prdct = ?, prdpr = ?, prdun = ?, prdrm = ?, prdut = ?, prdub = ? " +
            "WHERE prdcd = ? AND prddf = '0'";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setString(1, p.prdnm);
            ps.setString(2, p.prdct);
            ps.setBigDecimal(3, p.prdpr);
            ps.setString(4, p.prdun);
            ps.setString(5, p.prdrm);
            ps.setTimestamp(6, now);
            ps.setString(7, updatedBy);
            ps.setString(8, p.prdcd);
            ps.executeUpdate();
        }
    }

    /**
     * 論理削除。
     */
    public void logicalDelete(final String prdcd, final String updatedBy) throws SQLException {
        final String sql =
            "UPDATE TBL_SYOHIN SET prddf = '1', prdut = ?, prdub = ? WHERE prdcd = ? AND prddf = '0'";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, updatedBy);
            ps.setString(3, prdcd);
            ps.executeUpdate();
        }
    }

    // ResultSet→モデル変換
    private Syohin mapRow(final ResultSet rs) throws SQLException {
        final Syohin p = new Syohin();
        p.prdcd = rs.getString("prdcd");
        p.prdnm = rs.getString("prdnm");
        p.prdct = rs.getString("prdct");
        // BigDecimalでnull安全に受け取る
        final BigDecimal pr = rs.getBigDecimal("prdpr");
        p.prdpr = pr == null ? BigDecimal.ZERO : pr;
        p.prdun = rs.getString("prdun");
        p.prdrm = rs.getString("prdrm");
        return p;
    }
}
