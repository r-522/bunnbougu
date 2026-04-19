package jp.co.stationery.dao;

import jp.co.stationery.db.DbUtil;
import jp.co.stationery.model.Torihiki;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 取引先マスタDAO（TBL_TORIHIKI）。
 */
public final class TorihikiDao {

    /** 取引先コードで1件取得。 */
    public Torihiki findByCode(final String trcd) throws SQLException {
        final String sql =
            "SELECT trcd, trnm, trtp, trzp, trad, trtl, trpc, trml, trrm " +
            "FROM TBL_TORIHIKI WHERE trcd = ? AND trdf = '0'";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, trcd);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
                return null;
            }
        }
    }

    /** 条件付き検索。 */
    public List<Torihiki> search(final String trcd, final String trnm, final String trtp) throws SQLException {
        final StringBuilder sql = new StringBuilder(
            "SELECT trcd, trnm, trtp, trzp, trad, trtl, trpc, trml, trrm " +
            "FROM TBL_TORIHIKI WHERE trdf = '0'");
        final List<Object> binds = new ArrayList<>();
        // コード：前方一致
        if (trcd != null && !trcd.isBlank()) {
            sql.append(" AND trcd LIKE ?");
            binds.add(trcd + "%");
        }
        // 名称：部分一致
        if (trnm != null && !trnm.isBlank()) {
            sql.append(" AND trnm LIKE ?");
            binds.add("%" + trnm + "%");
        }
        // 区分：完全一致（「両方」は3として扱う）
        if (trtp != null && !trtp.isBlank()) {
            sql.append(" AND (trtp = ? OR trtp = '3')");
            binds.add(trtp);
        }
        sql.append(" ORDER BY trcd ASC");
        final List<Torihiki> list = new ArrayList<>();
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

    /** 区分別（仕入先/販売先）取得：プルダウン用。 */
    public List<Torihiki> listByType(final String trtp) throws SQLException {
        // "1"=仕入先, "2"=販売先。"3"=両方は両方とも含まれる。
        final String sql =
            "SELECT trcd, trnm, trtp, trzp, trad, trtl, trpc, trml, trrm " +
            "FROM TBL_TORIHIKI WHERE trdf = '0' AND (trtp = ? OR trtp = '3') ORDER BY trcd";
        final List<Torihiki> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, trtp);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /** 新規登録。 */
    public void insert(final Torihiki t, final String updatedBy) throws SQLException {
        final String sql =
            "INSERT INTO TBL_TORIHIKI " +
            "(trcd, trnm, trtp, trzp, trad, trtl, trpc, trml, trrm, trct, trcb, trut, trub, trdf) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0')";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setString(1, t.trcd);
            ps.setString(2, t.trnm);
            ps.setString(3, t.trtp);
            ps.setString(4, t.trzp);
            ps.setString(5, t.trad);
            ps.setString(6, t.trtl);
            ps.setString(7, t.trpc);
            ps.setString(8, t.trml);
            ps.setString(9, t.trrm);
            ps.setTimestamp(10, now);
            ps.setString(11, updatedBy);
            ps.setTimestamp(12, now);
            ps.setString(13, updatedBy);
            ps.executeUpdate();
        }
    }

    /** 更新。 */
    public void update(final Torihiki t, final String updatedBy) throws SQLException {
        final String sql =
            "UPDATE TBL_TORIHIKI SET " +
            "trnm = ?, trtp = ?, trzp = ?, trad = ?, trtl = ?, trpc = ?, trml = ?, trrm = ?, trut = ?, trub = ? " +
            "WHERE trcd = ? AND trdf = '0'";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setString(1, t.trnm);
            ps.setString(2, t.trtp);
            ps.setString(3, t.trzp);
            ps.setString(4, t.trad);
            ps.setString(5, t.trtl);
            ps.setString(6, t.trpc);
            ps.setString(7, t.trml);
            ps.setString(8, t.trrm);
            ps.setTimestamp(9, now);
            ps.setString(10, updatedBy);
            ps.setString(11, t.trcd);
            ps.executeUpdate();
        }
    }

    /** 論理削除。 */
    public void logicalDelete(final String trcd, final String updatedBy) throws SQLException {
        final String sql =
            "UPDATE TBL_TORIHIKI SET trdf = '1', trut = ?, trub = ? WHERE trcd = ? AND trdf = '0'";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, updatedBy);
            ps.setString(3, trcd);
            ps.executeUpdate();
        }
    }

    // ResultSet→モデル変換
    private Torihiki mapRow(final ResultSet rs) throws SQLException {
        final Torihiki t = new Torihiki();
        t.trcd = rs.getString("trcd");
        t.trnm = rs.getString("trnm");
        t.trtp = rs.getString("trtp");
        t.trzp = rs.getString("trzp");
        t.trad = rs.getString("trad");
        t.trtl = rs.getString("trtl");
        t.trpc = rs.getString("trpc");
        t.trml = rs.getString("trml");
        t.trrm = rs.getString("trrm");
        return t;
    }
}
