package jp.co.stationery.dao;

import jp.co.stationery.db.DbUtil;
import jp.co.stationery.model.JutyuDetail;
import jp.co.stationery.model.JutyuHeader;

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
 * 受注DAO（TBL_JUTYU_HEADER / TBL_JUTYU_DETAIL）。
 */
public final class JutyuDao {

    /** 条件検索（ヘッダのみ）。 */
    public List<JutyuHeader> search(final String juno, final String trcd,
                                    final LocalDate from, final LocalDate to,
                                    final String just) throws SQLException {
        final StringBuilder sql = new StringBuilder(
            "SELECT h.juno, h.judt, h.jutr, t.trnm AS jutrnm, h.junk, h.just, h.jusm, h.jurm " +
            "FROM TBL_JUTYU_HEADER h " +
            "LEFT JOIN TBL_TORIHIKI t ON t.trcd = h.jutr " +
            "WHERE h.judf = '0'");
        final List<Object> binds = new ArrayList<>();
        // 受注番号：前方一致
        if (juno != null && !juno.isBlank()) {
            sql.append(" AND h.juno LIKE ?");
            binds.add(juno + "%");
        }
        // 取引先：完全一致
        if (trcd != null && !trcd.isBlank()) {
            sql.append(" AND h.jutr = ?");
            binds.add(trcd);
        }
        // 開始日
        if (from != null) {
            sql.append(" AND h.judt >= ?");
            binds.add(java.sql.Date.valueOf(from));
        }
        // 終了日
        if (to != null) {
            sql.append(" AND h.judt <= ?");
            binds.add(java.sql.Date.valueOf(to));
        }
        // 状態
        if (just != null && !just.isBlank()) {
            sql.append(" AND h.just = ?");
            binds.add(just);
        }
        sql.append(" ORDER BY h.juno DESC");

        final List<JutyuHeader> list = new ArrayList<>();
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

    /** 受注番号で1件取得（明細込）。 */
    public JutyuHeader findByNo(final String juno) throws SQLException {
        // ヘッダ取得
        final String hs =
            "SELECT h.juno, h.judt, h.jutr, t.trnm AS jutrnm, h.junk, h.just, h.jusm, h.jurm " +
            "FROM TBL_JUTYU_HEADER h LEFT JOIN TBL_TORIHIKI t ON t.trcd = h.jutr " +
            "WHERE h.juno = ? AND h.judf = '0'";
        // 明細取得
        final String ds =
            "SELECT d.juno, d.juln, d.prdcd, p.prdnm, d.juup, d.juqt, d.juam " +
            "FROM TBL_JUTYU_DETAIL d LEFT JOIN TBL_SYOHIN p ON p.prdcd = d.prdcd " +
            "WHERE d.juno = ? AND d.dfdf = '0' ORDER BY d.juln";
        try (Connection con = DbUtil.getConnection()) {
            JutyuHeader h = null;
            try (PreparedStatement ps = con.prepareStatement(hs)) {
                ps.setString(1, juno);
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
                ps.setString(1, juno);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        h.details.add(mapDetail(rs));
                    }
                }
            }
            return h;
        }
    }

    /** 新規登録（ヘッダ＋明細）。トランザクション内で実行。 */
    public String insert(final JutyuHeader h, final String updatedBy) throws SQLException {
        try (Connection con = DbUtil.getConnection()) {
            // 業務整合のためトランザクション開始
            con.setAutoCommit(false);
            try {
                // 受注番号採番：年月 + 4桁連番（シーケンスの代替としてMAX+1）
                final String juno = nextJuno(con, h.judt);
                h.juno = juno;

                // ヘッダINSERT
                final String hsql =
                    "INSERT INTO TBL_JUTYU_HEADER " +
                    "(juno, judt, jutr, junk, just, jusm, jurm, juct, jucb, juut, juub, judf) " +
                    "VALUES (?, ?, ?, ?, '1', ?, ?, ?, ?, ?, ?, '0')";
                try (PreparedStatement ps = con.prepareStatement(hsql)) {
                    final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                    ps.setString(1, juno);
                    ps.setDate(2, java.sql.Date.valueOf(h.judt));
                    ps.setString(3, h.jutr);
                    ps.setDate(4, java.sql.Date.valueOf(h.junk));
                    ps.setBigDecimal(5, h.jusm == null ? BigDecimal.ZERO : h.jusm);
                    ps.setString(6, h.jurm == null ? "" : h.jurm);
                    ps.setTimestamp(7, now);
                    ps.setString(8, updatedBy);
                    ps.setTimestamp(9, now);
                    ps.setString(10, updatedBy);
                    ps.executeUpdate();
                }

                // 明細INSERT
                final String dsql =
                    "INSERT INTO TBL_JUTYU_DETAIL " +
                    "(juno, juln, prdcd, juup, juqt, juam, dfct, dfcb, dfut, dfub, dfdf) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, '0')";
                try (PreparedStatement ps = con.prepareStatement(dsql)) {
                    final Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                    int lineNo = 1;
                    for (final JutyuDetail d : h.details) {
                        ps.setString(1, juno);
                        ps.setInt(2, lineNo++);
                        ps.setString(3, d.prdcd);
                        ps.setBigDecimal(4, d.juup);
                        ps.setInt(5, d.juqt);
                        ps.setBigDecimal(6, d.juam);
                        ps.setTimestamp(7, now);
                        ps.setString(8, updatedBy);
                        ps.setTimestamp(9, now);
                        ps.setString(10, updatedBy);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                // コミット
                con.commit();
                return juno;
            } catch (SQLException e) {
                // 例外時はロールバックして再送出
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /** 取消（状態を9へ更新）。 */
    public void cancel(final String juno, final String updatedBy) throws SQLException {
        final String sql = "UPDATE TBL_JUTYU_HEADER SET just = '9', juut = ?, juub = ? " +
                           "WHERE juno = ? AND judf = '0'";
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, updatedBy);
            ps.setString(3, juno);
            ps.executeUpdate();
        }
    }

    /** 受注番号採番（yyyyMM + 連番3桁、フォーマット: J + yyyyMM + nnn）。 */
    private String nextJuno(final Connection con, final LocalDate judt) throws SQLException {
        final String ym = String.format("%04d%02d", judt.getYear(), judt.getMonthValue());
        final String prefix = "J" + ym;
        // 同月の最大番号を取得
        final String sql = "SELECT COALESCE(MAX(juno), '') FROM TBL_JUTYU_HEADER WHERE juno LIKE ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                final String max = rs.getString(1);
                int seq = 1;
                if (max != null && !max.isEmpty()) {
                    // 末尾3桁を整数化して+1
                    seq = Integer.parseInt(max.substring(prefix.length())) + 1;
                }
                return prefix + String.format("%03d", seq);
            }
        }
    }

    /** 状態更新（出荷完了による自動更新用）。 */
    public void updateStatus(final Connection con, final String juno, final String newStatus, final String updatedBy)
            throws SQLException {
        final String sql = "UPDATE TBL_JUTYU_HEADER SET just = ?, juut = ?, juub = ? WHERE juno = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, updatedBy);
            ps.setString(4, juno);
            ps.executeUpdate();
        }
    }

    // ヘッダ行マッピング
    private JutyuHeader mapHeader(final ResultSet rs) throws SQLException {
        final JutyuHeader h = new JutyuHeader();
        h.juno = rs.getString("juno");
        final java.sql.Date dt = rs.getDate("judt");
        h.judt = dt == null ? null : dt.toLocalDate();
        h.jutr = rs.getString("jutr");
        h.jutrnm = rs.getString("jutrnm");
        final java.sql.Date nk = rs.getDate("junk");
        h.junk = nk == null ? null : nk.toLocalDate();
        h.just = rs.getString("just");
        h.jusm = rs.getBigDecimal("jusm");
        h.jurm = rs.getString("jurm");
        return h;
    }

    // 明細行マッピング
    private JutyuDetail mapDetail(final ResultSet rs) throws SQLException {
        final JutyuDetail d = new JutyuDetail();
        d.juno = rs.getString("juno");
        d.juln = rs.getInt("juln");
        d.prdcd = rs.getString("prdcd");
        d.prdnm = rs.getString("prdnm");
        d.juup = rs.getBigDecimal("juup");
        d.juqt = rs.getInt("juqt");
        d.juam = rs.getBigDecimal("juam");
        return d;
    }
}
