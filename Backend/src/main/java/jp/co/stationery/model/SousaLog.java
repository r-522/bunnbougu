package jp.co.stationery.model;

import java.time.LocalDateTime;

/**
 * 操作履歴（TBL_SOUSA_LOG）モデル。
 */
public final class SousaLog {
    // ID（SERIAL）
    public long slid;
    // 操作区分
    public String slop;
    // 対象機能
    public String sltg;
    // 対象キー
    public String slky;
    // 内容
    public String slms;
    // 結果
    public String slrs;
    // 実行社員番号
    public String slcb;
    // 実行日時
    public LocalDateTime slct;
    // 実行社員氏名（表示用：DAOでJOIN取得）
    public String synm;
}
