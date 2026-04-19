package jp.co.stationery.model;

import java.time.LocalDateTime;

/**
 * 在庫（TBL_ZAIKO）モデル。
 * カラム命名規則: zk[xx] 全小文字2文字。
 */
public final class Zaiko {
    // 商品コード（主キー）
    public String prdcd;
    // 商品名（表示用：DAOでJOINして詰める）
    public String prdnm;
    // 単位（表示用）
    public String prdun;
    // 現在庫数
    public int zkqt;
    // 引当済数
    public int zkrs;
    // 最終更新日時
    public LocalDateTime zkut;
}
