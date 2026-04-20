package jp.co.stationery.util;

/**
 * 業務コード→表示名変換の集約ユーティリティ。
 * 各ハンドラに分散していたswitch文を一元化する。
 * 定義変更は本クラスと init.sql のコメントのみで保守する。
 */
public final class Codes {

    private Codes() {
    }

    /** 商品カテゴリ（prdct）。 */
    public static String categoryName(final String code) {
        if (code == null) return "";
        switch (code) {
            case "01": return "筆記具";
            case "02": return "ノート・紙製品";
            case "03": return "ファイル・バインダ";
            case "04": return "接着・修正";
            case "05": return "事務雑貨";
            default:   return code;
        }
    }

    /** 受注状態（just）。 */
    public static String jutyuStateName(final String code) {
        if (code == null) return "";
        switch (code) {
            case "1": return "受付済";
            case "2": return "出荷準備中";
            case "3": return "出荷済";
            case "9": return "取消";
            default:  return code;
        }
    }

    /** 発注状態（hast）。 */
    public static String hatyuStateName(final String code) {
        if (code == null) return "";
        switch (code) {
            case "1": return "発注済";
            case "2": return "一部入荷";
            case "3": return "入荷完了";
            case "9": return "取消";
            default:  return code;
        }
    }

    /** 取引先区分（trtp）。 */
    public static String torihikiTypeName(final String code) {
        if (code == null) return "";
        switch (code) {
            case "1": return "仕入先";
            case "2": return "販売先";
            case "3": return "両方";
            default:  return code;
        }
    }

    /** 社員所属（sybs）。 */
    public static String deptName(final String code) {
        if (code == null) return "";
        switch (code) {
            case "10": return "営業部";
            case "20": return "仕入部";
            case "30": return "物流部";
            case "40": return "情報システム部";
            case "90": return "管理部";
            default:   return code;
        }
    }

    /** 社員役職（sypo）。 */
    public static String positionName(final String code) {
        if (code == null) return "";
        switch (code) {
            case "1": return "担当";
            case "2": return "主任";
            case "3": return "課長";
            case "4": return "部長";
            default:  return code;
        }
    }
}
