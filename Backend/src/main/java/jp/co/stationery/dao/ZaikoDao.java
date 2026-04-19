package jp.co.stationery.dao;

import jp.co.stationery.db.DbUtil;
import jp.co.stationery.model.Zaiko;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 在庫DAO（TBL_ZAIKO）。
 * 在庫数は「現在庫数」と「引当済数」の2値で管理し、有効在庫は(zkqt - zkrs)で算出する。
 */
public final class ZaikoDao {

    /** 商品コードで1件取得（商品マスタをJOIN）。 */
    public Zaiko findByCode(final String prdcd, final Connection conOpt) throws SQLException {
        // 接続は呼び出し元から受けとることで、業務トランザクション内の更新ロックにも利用できる
        // 呼び出し元がnullなら新規接続を開き、呼び出し元が渡してきたならクローズしない
        final boolean closeCon = (conOpt == null);
        final Connection con = closeCon ? DbUtil.getConnection() : conOpt;
        try {
            final String sql =
                "SELECT z.prdcd, p.prdnm, p.prdun, z.zkqt, z.zkrs, z.zkut " +
                "FROM TBL_ZAIKO z " +
                "INNER JOIN TBL_SYOHIN p ON p.prdcd = z.prdcd AND p.prddf = '0' " +
                "WHERE z.prdcd = ? AND z.zkdf = '0'";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, prdcd);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapRow(rs);
                    }
                    return null;
                }
            }
        } finally {
            // 自前で開いた場合のみクローズ
            if (closeCon) {
                con.close();
            }
        }
    }

    /** 条件付き検索（商品JOIN、状態はクライアント側で判定）。 */
    public List<Zaiko> search(final String prdcd, final String prdnm) throws SQLException {
        final StringBuilder sql = new StringBuilder(
            "SELECT z.prdcd, p.prdnm, p.prdun, z.zkqt, z.zkrs, z.zkut " +
            "FROM TBL_ZAIKO z " +
            "INNER JOIN TBL_SYOHIN p ON p.prdcd = z.prdcd AND p.prddf = '0' " +
            "WHERE z.zkdf = '0'");
        final List<Object> binds = new ArrayList<>();
        // 商品コード：前方一致
        if (prdcd != null && !prdcd.isBlank()) {
            sql.append(" AND z.prdcd LIKE ?");
            binds.add(prdcd + "%");
        }
        // 商品名：部分一致
        if (prdnm != null && !prdnm.isBlank()) {
            sql.append(" AND p.prdnm LIKE ?");
            binds.add("%" + prdnm + "%");
        }
        sql.append(" ORDER BY z.prdcd ASC");

        final List<Zaiko> list = new ArrayList<>();
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

    /** 在庫数直接更新（調整機能）。 */
    public void updateQuantity(final String prdcd, final int newQty, final String updatedBy) throws SQLException {
        // 在庫レコードが無ければINSERT、あればUPDATE（PostgreSQLのUPSERT構文を利用）
        final String sql =
            "INSERT INTO TBL_ZAIKO (prdcd, zkqt, zkrs, zkct, zkcb, zkut, zkub, zkdf) " +
            "VALUES (?, ?, 0, ?, ?, ?, ?, '0') " +
            "ON CONFLICT (prdcd) DO UPDATE SET " +
            "zkqt = EXCLUDED.zkqt, zkut = EXCLUDED.zkut, zkub = EXCLUDED.zkub";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setString(1, prdcd);
            ps.setInt(2, newQty);
            ps.setTimestamp(3, now);
            ps.setString(4, updatedBy);
            ps.setTimestamp(5, now);
            ps.setString(6, updatedBy);
            ps.executeUpdate();
        }
    }

    /** 入荷による在庫加算（トランザクション内での更新）。 */
    public void addQuantity(final Connection con, final String prdcd, final int qty, final String updatedBy)
            throws SQLException {
        // 在庫レコードが無ければ作成、あれば加算
        final String sql =
            "INSERT INTO TBL_ZAIKO (prdcd, zkqt, zkrs, zkct, zkcb, zkut, zkub, zkdf) " +
            "VALUES (?, ?, 0, ?, ?, ?, ?, '0') " +
            "ON CONFLICT (prdcd) DO UPDATE SET " +
            "zkqt = TBL_ZAIKO.zkqt + EXCLUDED.zkqt, zkut = EXCLUDED.zkut, zkub = EXCLUDED.zkub";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setString(1, prdcd);
            ps.setInt(2, qty);
            ps.setTimestamp(3, now);
            ps.setString(4, updatedBy);
            ps.setTimestamp(5, now);
            ps.setString(6, updatedBy);
            ps.executeUpdate();
        }
    }

    /** 出荷による在庫減算（トランザクション内での更新）。 */
    public void subtractQuantity(final Connection con, final String prdcd, final int qty, final String updatedBy)
            throws SQLException {
        // 在庫不足は呼び出し元でチェック済である前提
        final String sql =
            "UPDATE TBL_ZAIKO SET zkqt = zkqt - ?, zkut = ?, zkub = ? WHERE prdcd = ? AND zkdf = '0'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, qty);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, updatedBy);
            ps.setString(4, prdcd);
            ps.executeUpdate();
        }
    }

    // ResultSet→モデル変換
    private Zaiko mapRow(final ResultSet rs) throws SQLException {
        final Zaiko z = new Zaiko();
        z.prdcd = rs.getString("prdcd");
        z.prdnm = rs.getString("prdnm");
        z.prdun = rs.getString("prdun");
        z.zkqt = rs.getInt("zkqt");
        z.zkrs = rs.getInt("zkrs");
        final Timestamp ts = rs.getTimestamp("zkut");
        z.zkut = ts == null ? null : ts.toLocalDateTime();
        return z;
    }
}
