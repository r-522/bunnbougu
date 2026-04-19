package jp.co.stationery.dao;

import jp.co.stationery.db.DbUtil;
import jp.co.stationery.model.SousaLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作履歴DAO（TBL_SOUSA_LOG）。
 * 参照専用（書込みはSousaLoggerが担う）。
 */
public final class SousaLogDao {

    /** 条件付き参照。 */
    public List<SousaLog> search(final LocalDate from, final LocalDate to,
                                 final String sycd, final String lgop, final String lgtg) throws SQLException {
        final StringBuilder sql = new StringBuilder(
            "SELECT l.slid, l.slop, l.sltg, l.slky, l.slms, l.slrs, l.slcb, l.slct, s.synm " +
            "FROM TBL_SOUSA_LOG l " +
            "LEFT JOIN TBL_SYAIN s ON s.sycd = l.slcb " +
            "WHERE 1=1");
        final List<Object> binds = new ArrayList<>();
        // 期間開始（当日の00:00:00以降）
        if (from != null) {
            sql.append(" AND l.slct >= ?");
            binds.add(java.sql.Timestamp.valueOf(from.atStartOfDay()));
        }
        // 期間終了（翌日0:00未満）
        if (to != null) {
            sql.append(" AND l.slct < ?");
            binds.add(java.sql.Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
        }
        // 社員番号
        if (sycd != null && !sycd.isBlank()) {
            sql.append(" AND l.slcb = ?");
            binds.add(sycd);
        }
        // 操作区分
        if (lgop != null && !lgop.isBlank()) {
            sql.append(" AND l.slop = ?");
            binds.add(lgop);
        }
        // 対象機能
        if (lgtg != null && !lgtg.isBlank()) {
            sql.append(" AND l.sltg = ?");
            binds.add(lgtg);
        }
        // 新しい順
        sql.append(" ORDER BY l.slct DESC LIMIT 500");

        final List<SousaLog> list = new ArrayList<>();
        try (Connection con = DbUtil.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            for (int i = 0; i < binds.size(); i++) {
                ps.setObject(i + 1, binds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final SousaLog l = new SousaLog();
                    l.slid = rs.getLong("slid");
                    l.slop = rs.getString("slop");
                    l.sltg = rs.getString("sltg");
                    l.slky = rs.getString("slky");
                    l.slms = rs.getString("slms");
                    l.slrs = rs.getString("slrs");
                    l.slcb = rs.getString("slcb");
                    final java.sql.Timestamp ts = rs.getTimestamp("slct");
                    l.slct = ts == null ? null : ts.toLocalDateTime();
                    l.synm = rs.getString("synm");
                    list.add(l);
                }
            }
        }
        return list;
    }
}
