package jp.co.stationery.util;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * セッション管理（インメモリ）。
 * 本システムは社員番号（5桁数字）をセッションに格納し、全操作ログに記録する。
 * Cloud Runでは単一インスタンスを想定（複数インスタンスの場合はRedis等で置換要）。
 */
public final class SessionStore {

    // セッションID（Cookie値）→ 社員番号のマップ
    private static final Map<String, String> STORE = new ConcurrentHashMap<>();

    // セキュアランダム（セッションID生成用）
    private static final SecureRandom RANDOM = new SecureRandom();

    // Cookie名（セッションID保持）
    public static final String COOKIE_NAME = "SID";

    // インスタンス化禁止
    private SessionStore() {
    }

    /**
     * 新規セッションを作成し、セッションIDを返却する。
     *
     * @param syainNo 社員番号（5桁数字）
     * @return セッションID
     */
    public static String create(final String syainNo) {
        // 32バイトのランダム値を生成
        final byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        // 16進数文字列に変換してセッションIDとする
        final StringBuilder sb = new StringBuilder(raw.length * 2);
        for (final byte b : raw) {
            sb.append(String.format("%02x", b));
        }
        final String sid = sb.toString();
        // ストアに登録
        STORE.put(sid, syainNo);
        return sid;
    }

    /**
     * セッションIDから社員番号を取得する。
     *
     * @param sid セッションID（null可）
     * @return 社員番号。セッション無効時はnull
     */
    public static String getSyainNo(final String sid) {
        // null安全
        if (sid == null) {
            return null;
        }
        return STORE.get(sid);
    }

    /**
     * セッションを破棄する（ログアウト時）。
     */
    public static void destroy(final String sid) {
        // null安全
        if (sid != null) {
            STORE.remove(sid);
        }
    }
}
