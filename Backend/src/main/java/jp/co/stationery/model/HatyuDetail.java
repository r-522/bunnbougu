package jp.co.stationery.model;

import java.math.BigDecimal;

/**
 * 発注明細（TBL_HATYU_DETAIL）モデル。
 */
public final class HatyuDetail {
    // 発注番号
    public String hano;
    // 明細行番号
    public int haln;
    // 商品コード
    public String prdcd;
    // 商品名（表示用）
    public String prdnm;
    // 仕入単価
    public BigDecimal haup;
    // 発注数量
    public int haqt;
    // 入荷済数量
    public int haiq;
    // 金額
    public BigDecimal haam;
}
