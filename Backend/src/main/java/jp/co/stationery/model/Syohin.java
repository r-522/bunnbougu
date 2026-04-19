package jp.co.stationery.model;

import java.math.BigDecimal;

/**
 * 商品マスタ（TBL_SYOHIN）モデル。
 * カラム命名規則: prd[xx] 全小文字2文字。
 */
public final class Syohin {
    // 商品コード（主キー）
    public String prdcd;
    // 商品名
    public String prdnm;
    // カテゴリ
    public String prdct;
    // 単価（税抜）
    public BigDecimal prdpr;
    // 単位
    public String prdun;
    // 備考
    public String prdrm;
}
