package jp.co.stationery.model;

/**
 * 取引先マスタ（TBL_TORIHIKI）モデル。
 * カラム命名規則: tr[xx] 全小文字2文字。
 */
public final class Torihiki {
    // 取引先コード（主キー）
    public String trcd;
    // 取引先名
    public String trnm;
    // 区分（1:仕入先 2:販売先 3:両方）
    public String trtp;
    // 郵便番号
    public String trzp;
    // 住所
    public String trad;
    // 電話番号
    public String trtl;
    // 担当者名
    public String trpc;
    // メールアドレス
    public String trml;
    // 備考
    public String trrm;
}
