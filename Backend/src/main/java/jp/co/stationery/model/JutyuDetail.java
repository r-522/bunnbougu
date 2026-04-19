package jp.co.stationery.model;

import java.math.BigDecimal;

/**
 * 受注明細（TBL_JUTYU_DETAIL）モデル。
 */
public final class JutyuDetail {
    // 受注番号
    public String juno;
    // 明細行番号
    public int juln;
    // 商品コード
    public String prdcd;
    // 商品名（表示用）
    public String prdnm;
    // 単価
    public BigDecimal juup;
    // 数量
    public int juqt;
    // 金額（単価×数量）
    public BigDecimal juam;
}
