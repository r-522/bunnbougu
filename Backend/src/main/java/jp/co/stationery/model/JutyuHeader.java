package jp.co.stationery.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 受注ヘッダ（TBL_JUTYU_HEADER）モデル。
 * 明細は TBL_JUTYU_DETAIL とし、DTOとして本オブジェクトに紐付ける。
 */
public final class JutyuHeader {
    // 受注番号（主キー）
    public String juno;
    // 受注日
    public LocalDate judt;
    // 販売先（取引先コード）
    public String jutr;
    // 販売先名（表示用）
    public String jutrnm;
    // 納期
    public LocalDate junk;
    // 状態（1:受付済 2:出荷準備中 3:出荷済 9:取消）
    public String just;
    // 合計金額（税抜）
    public BigDecimal jusm;
    // 備考
    public String jurm;
    // 明細
    public List<JutyuDetail> details = new ArrayList<>();
}
