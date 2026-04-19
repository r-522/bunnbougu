package jp.co.stationery.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 発注ヘッダ（TBL_HATYU_HEADER）モデル。
 */
public final class HatyuHeader {
    // 発注番号（主キー）
    public String hano;
    // 発注日
    public LocalDate hadt;
    // 仕入先
    public String hatr;
    // 仕入先名（表示用）
    public String hatrnm;
    // 入荷予定日
    public LocalDate hank;
    // 状態（1:発注済 2:一部入荷 3:入荷完了 9:取消）
    public String hast;
    // 合計金額（税抜）
    public BigDecimal hasm;
    // 備考
    public String harm;
    // 明細
    public List<HatyuDetail> details = new ArrayList<>();
}
