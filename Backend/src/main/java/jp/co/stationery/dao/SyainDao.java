package jp.co.stationery.dao;

import jp.co.stationery.db.DbUtil;
import jp.co.stationery.model.Syain;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 社員マスタDAO（TBL_SYAIN）。
 * 全てPreparedStatementを使用し、SQLインジェクションを防止する。
 */
public final class SyainDao {

    /**
     * 社員番号で1件検索する（論理削除されていない者のみ）。
     */
    public Syain findByCode(final String sycd) throws SQLException {
        // 参照SQL。sydf='0' が有効レコード
        final String sql =
            "SELECT sycd, synm, sykn, sybs, sypo, syml " +
            "FROM TBL_SYAIN WHERE sycd = ? AND sydf = '0'";
        // try-with-resourcesで接続・PreparedStatement・ResultSetを確実にクローズ
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // 第1パラメータに社員番号をバインド
            ps.setString(1, sycd);
            // クエリ実行
            try (ResultSet rs = ps.executeQuery()) {
                // 先頭1件のみ取得（主キー検索のため1件以下）
                if (rs.next()) {
                    return mapRow(rs);
                }
                // 該当なしはnull
                return null;
            }
        }
    }

    /**
     * 条件付き全件検索（検索画面用）。
     */
    public List<Syain> search(final String sycd, final String synm, final String sybs) throws SQLException {
        // 動的条件をWHERE句に積むが、値は必ずプレースホルダを使用
        final StringBuilder sql = new StringBuilder(
            "SELECT sycd, synm, sykn, sybs, sypo, syml " +
            "FROM TBL_SYAIN WHERE sydf = '0'");
        // バインド値リストに条件が加わる順に積む
        final List<Object> binds = new ArrayList<>();
        // 社員番号条件（前方一致）
        if (sycd != null && !sycd.isBlank()) {
            sql.append(" AND sycd LIKE ?");
            binds.add(sycd + "%");
        }
        // 氏名条件（部分一致）
        if (synm != null && !synm.isBlank()) {
            sql.append(" AND synm LIKE ?");
            binds.add("%" + synm + "%");
        }
        // 所属条件（完全一致）
        if (sybs != null && !sybs.isBlank()) {
            sql.append(" AND sybs = ?");
            binds.add(sybs);
        }
        // 主キー順に並べる
        sql.append(" ORDER BY sycd ASC");

        // 実行
        final List<Syain> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            // バインド値を順番に設定
            for (int i = 0; i < binds.size(); i++) {
                ps.setObject(i + 1, binds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                // 全行ループしてモデル化
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 新規登録（論理削除フラグは'0'で作成）。
     */
    public void insert(final Syain s, final String updatedBy) throws SQLException {
        final String sql =
            "INSERT INTO TBL_SYAIN " +
            "(sycd, synm, sykn, sybs, sypo, syml, syct, sycb, syut, syub, sydf) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0')";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            // 現在時刻を監査項目に設定
            final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            // 各パラメータをバインド
            ps.setString(1, s.sycd);
            ps.setString(2, s.synm);
            ps.setString(3, s.sykn);
            ps.setString(4, s.sybs);
            ps.setString(5, s.sypo);
            ps.setString(6, s.syml);
            ps.setTimestamp(7, now);
            ps.setString(8, updatedBy);
            ps.setTimestamp(9, now);
            ps.setString(10, updatedBy);
            ps.executeUpdate();
        }
    }

    /**
     * 更新（監査項目も更新）。
     */
    public void update(final Syain s, final String updatedBy) throws SQLException {
        final String sql =
            "UPDATE TBL_SYAIN SET " +
            "synm = ?, sykn = ?, sybs = ?, sypo = ?, syml = ?, syut = ?, syub = ? " +
            "WHERE sycd = ? AND sydf = '0'";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setString(1, s.synm);
            ps.setString(2, s.sykn);
            ps.setString(3, s.sybs);
            ps.setString(4, s.sypo);
            ps.setString(5, s.syml);
            ps.setTimestamp(6, now);
            ps.setString(7, updatedBy);
            ps.setString(8, s.sycd);
            ps.executeUpdate();
        }
    }

    /**
     * 論理削除（sydfを'1'へ更新）。
     */
    public void logicalDelete(final String sycd, final String updatedBy) throws SQLException {
        final String sql =
            "UPDATE TBL_SYAIN SET sydf = '1', syut = ?, syub = ? WHERE sycd = ? AND sydf = '0'";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            ps.setTimestamp(1, now);
            ps.setString(2, updatedBy);
            ps.setString(3, sycd);
            ps.executeUpdate();
        }
    }

    // ResultSet→Syain変換（内部利用）
    private Syain mapRow(final ResultSet rs) throws SQLException {
        final Syain s = new Syain();
        s.sycd = rs.getString("sycd");
        s.synm = rs.getString("synm");
        s.sykn = rs.getString("sykn");
        s.sybs = rs.getString("sybs");
        s.sypo = rs.getString("sypo");
        s.syml = rs.getString("syml");
        return s;
    }
}
