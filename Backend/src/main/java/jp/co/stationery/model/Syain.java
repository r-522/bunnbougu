package jp.co.stationery.model;

/**
 * 社員マスタ（TBL_SYAIN）モデル。
 * カラム命名規則: sy[xx] 全小文字2文字。
 */
public final class Syain {
    // 社員番号（5桁数字・主キー）
    public String sycd;
    // 氏名
    public String synm;
    // 氏名カナ
    public String sykn;
    // 所属コード
    public String sybs;
    // 役職コード
    public String sypo;
    // メールアドレス
    public String syml;
}
