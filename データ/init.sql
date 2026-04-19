-- =====================================================================
-- 文房具基幹システム DDL + 初期データ
-- 対象DB : PostgreSQL（Supabase）
-- 文字コード: UTF-8
-- 命名規則 : テーブル TBL_xxx 全大文字、カラム [略号3]_[略号2] 全小文字
-- 監査項目 : xxxct(created_at) / xxxcb(created_by) / xxxut(updated_at)
--           / xxxub(updated_by) / xxxdf(delete_flag '0'=有効 '1'=削除)
-- =====================================================================

-- ---------------------------------------------------------------------
-- 既存オブジェクトの削除（再実行用）。依存関係の逆順で削除する。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS TBL_SOUSA_LOG     CASCADE;
DROP TABLE IF EXISTS TBL_SYUKKA        CASCADE;
DROP TABLE IF EXISTS TBL_NYUKA         CASCADE;
DROP TABLE IF EXISTS TBL_HATYU_DETAIL  CASCADE;
DROP TABLE IF EXISTS TBL_HATYU_HEADER  CASCADE;
DROP TABLE IF EXISTS TBL_JUTYU_DETAIL  CASCADE;
DROP TABLE IF EXISTS TBL_JUTYU_HEADER  CASCADE;
DROP TABLE IF EXISTS TBL_ZAIKO         CASCADE;
DROP TABLE IF EXISTS TBL_TORIHIKI      CASCADE;
DROP TABLE IF EXISTS TBL_SYOHIN        CASCADE;
DROP TABLE IF EXISTS TBL_SYAIN         CASCADE;

-- ---------------------------------------------------------------------
-- TBL_SYAIN  社員マスタ
--   sycd 社員コード(5桁)/ synm 氏名 / sykn かな / sybs 部署
--   sypo 役職 / syml メール / 監査項目: syct/sycb/syut/syub/sydf
-- ---------------------------------------------------------------------
CREATE TABLE TBL_SYAIN (
  sycd  VARCHAR(5)   PRIMARY KEY,
  synm  VARCHAR(60)  NOT NULL,
  sykn  VARCHAR(120),
  sybs  VARCHAR(40),
  sypo  VARCHAR(20),
  syml  VARCHAR(120),
  syct  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sycb  VARCHAR(5)   NOT NULL,
  syut  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  syub  VARCHAR(5)   NOT NULL,
  sydf  CHAR(1)      NOT NULL DEFAULT '0'
);

-- ---------------------------------------------------------------------
-- TBL_SYOHIN  商品マスタ
--   prdcd コード / prdnm 名称 / prdct カテゴリ(01-05)
--   prdpr 単価 / prdun 単位 / prdrm 備考
--   prdct2 created_at(略号衝突回避) / prdcb / prdut / prdub / prddf
-- ---------------------------------------------------------------------
CREATE TABLE TBL_SYOHIN (
  prdcd  VARCHAR(10)    PRIMARY KEY,
  prdnm  VARCHAR(80)    NOT NULL,
  prdct  CHAR(2),
  prdpr  NUMERIC(10, 2) NOT NULL DEFAULT 0,
  prdun  VARCHAR(10),
  prdrm  VARCHAR(200),
  prdct2 TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  prdcb  VARCHAR(5)     NOT NULL,
  prdut  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  prdub  VARCHAR(5)     NOT NULL,
  prddf  CHAR(1)        NOT NULL DEFAULT '0'
);

-- ---------------------------------------------------------------------
-- TBL_TORIHIKI  取引先マスタ
--   trcd / trnm / trtp 区分(1=仕入先 2=販売先 3=両方)
--   trzp 郵便 / trad 住所 / trtl 電話 / trpc 担当者 / trml メール / trrm 備考
--   trct/trcb/trut/trub/trdf
-- ---------------------------------------------------------------------
CREATE TABLE TBL_TORIHIKI (
  trcd VARCHAR(10)  PRIMARY KEY,
  trnm VARCHAR(80)  NOT NULL,
  trtp CHAR(1)      NOT NULL,
  trzp VARCHAR(8),
  trad VARCHAR(120),
  trtl VARCHAR(20),
  trpc VARCHAR(40),
  trml VARCHAR(120),
  trrm VARCHAR(200),
  trct TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  trcb VARCHAR(5)   NOT NULL,
  trut TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  trub VARCHAR(5)   NOT NULL,
  trdf CHAR(1)      NOT NULL DEFAULT '0'
);

-- ---------------------------------------------------------------------
-- TBL_ZAIKO  在庫
--   prdcd PK / zkqt 現在庫数 / zkrs 引当済数
--   有効在庫 = zkqt - zkrs
--   zkct/zkcb/zkut/zkub/zkdf
-- ---------------------------------------------------------------------
CREATE TABLE TBL_ZAIKO (
  prdcd VARCHAR(10) PRIMARY KEY REFERENCES TBL_SYOHIN(prdcd),
  zkqt  INTEGER     NOT NULL DEFAULT 0,
  zkrs  INTEGER     NOT NULL DEFAULT 0,
  zkct  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  zkcb  VARCHAR(5)  NOT NULL,
  zkut  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  zkub  VARCHAR(5)  NOT NULL,
  zkdf  CHAR(1)     NOT NULL DEFAULT '0'
);

-- ---------------------------------------------------------------------
-- TBL_JUTYU_HEADER  受注ヘッダ
--   juno 受注番号(J + yyyyMM + 3桁) / judt 受注日 / jutr 取引先(販売先)
--   junk 納期 / just 状態(1=受注 2=出荷一部 3=完了 9=取消)
--   jusm 合計 / jurm 備考 / juct/jucb/juut/juub/judf
-- ---------------------------------------------------------------------
CREATE TABLE TBL_JUTYU_HEADER (
  juno VARCHAR(12)    PRIMARY KEY,
  judt DATE           NOT NULL,
  jutr VARCHAR(10)    NOT NULL REFERENCES TBL_TORIHIKI(trcd),
  junk DATE           NOT NULL,
  just CHAR(1)        NOT NULL DEFAULT '1',
  jusm NUMERIC(12, 2) NOT NULL DEFAULT 0,
  jurm VARCHAR(200),
  juct TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  jucb VARCHAR(5)     NOT NULL,
  juut TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  juub VARCHAR(5)     NOT NULL,
  judf CHAR(1)        NOT NULL DEFAULT '0'
);

-- ---------------------------------------------------------------------
-- TBL_JUTYU_DETAIL  受注明細
--   PK: (juno, juln) / prdcd 商品 / juup 単価 / juqt 数量 / juam 金額
--   監査(明細用): dfct/dfcb/dfut/dfub/dfdf
-- ---------------------------------------------------------------------
CREATE TABLE TBL_JUTYU_DETAIL (
  juno  VARCHAR(12)    NOT NULL REFERENCES TBL_JUTYU_HEADER(juno),
  juln  INTEGER        NOT NULL,
  prdcd VARCHAR(10)    NOT NULL REFERENCES TBL_SYOHIN(prdcd),
  juup  NUMERIC(10, 2) NOT NULL DEFAULT 0,
  juqt  INTEGER        NOT NULL DEFAULT 0,
  juam  NUMERIC(12, 2) NOT NULL DEFAULT 0,
  dfct  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dfcb  VARCHAR(5)     NOT NULL,
  dfut  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dfub  VARCHAR(5)     NOT NULL,
  dfdf  CHAR(1)        NOT NULL DEFAULT '0',
  PRIMARY KEY (juno, juln)
);

-- ---------------------------------------------------------------------
-- TBL_HATYU_HEADER  発注ヘッダ
--   hano(H + yyyyMM + 3桁) / hadt 発注日 / hatr 仕入先 / hank 納期
--   hast 状態(1=発注 2=入荷一部 3=完了 9=取消) / hasm 合計 / harm 備考
-- ---------------------------------------------------------------------
CREATE TABLE TBL_HATYU_HEADER (
  hano VARCHAR(12)    PRIMARY KEY,
  hadt DATE           NOT NULL,
  hatr VARCHAR(10)    NOT NULL REFERENCES TBL_TORIHIKI(trcd),
  hank DATE           NOT NULL,
  hast CHAR(1)        NOT NULL DEFAULT '1',
  hasm NUMERIC(12, 2) NOT NULL DEFAULT 0,
  harm VARCHAR(200),
  hact TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  hacb VARCHAR(5)     NOT NULL,
  haut TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  haub VARCHAR(5)     NOT NULL,
  hadf CHAR(1)        NOT NULL DEFAULT '0'
);

-- ---------------------------------------------------------------------
-- TBL_HATYU_DETAIL  発注明細
--   PK: (hano, haln) / haup 単価 / haqt 数量 / haiq 入荷済数 / haam 金額
--   監査(明細用): dhct/dhcb/dhut/dhub/dhdf
-- ---------------------------------------------------------------------
CREATE TABLE TBL_HATYU_DETAIL (
  hano  VARCHAR(12)    NOT NULL REFERENCES TBL_HATYU_HEADER(hano),
  haln  INTEGER        NOT NULL,
  prdcd VARCHAR(10)    NOT NULL REFERENCES TBL_SYOHIN(prdcd),
  haup  NUMERIC(10, 2) NOT NULL DEFAULT 0,
  haqt  INTEGER        NOT NULL DEFAULT 0,
  haiq  INTEGER        NOT NULL DEFAULT 0,
  haam  NUMERIC(12, 2) NOT NULL DEFAULT 0,
  dhct  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dhcb  VARCHAR(5)     NOT NULL,
  dhut  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  dhub  VARCHAR(5)     NOT NULL,
  dhdf  CHAR(1)        NOT NULL DEFAULT '0',
  PRIMARY KEY (hano, haln)
);

-- ---------------------------------------------------------------------
-- TBL_NYUKA  入荷明細（発注明細単位の入荷実績）
--   nyid シリアルPK / hano + haln で発注明細紐付け / nydt 入荷日 / nyqt 入荷数
-- ---------------------------------------------------------------------
CREATE TABLE TBL_NYUKA (
  nyid SERIAL      PRIMARY KEY,
  hano VARCHAR(12) NOT NULL,
  haln INTEGER     NOT NULL,
  nydt DATE        NOT NULL,
  nyqt INTEGER     NOT NULL,
  nyct TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  nycb VARCHAR(5)  NOT NULL,
  nyut TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  nyub VARCHAR(5)  NOT NULL,
  nydf CHAR(1)     NOT NULL DEFAULT '0',
  FOREIGN KEY (hano, haln) REFERENCES TBL_HATYU_DETAIL(hano, haln)
);

-- ---------------------------------------------------------------------
-- TBL_SYUKKA  出荷明細（受注明細単位の出荷実績）
--   syid シリアルPK / juno + juln で受注明細紐付け / sydt 出荷日 / syqt 出荷数
--   sycr 配送業者 / sytn 追跡番号
-- ---------------------------------------------------------------------
CREATE TABLE TBL_SYUKKA (
  syid SERIAL      PRIMARY KEY,
  juno VARCHAR(12) NOT NULL,
  juln INTEGER     NOT NULL,
  sydt DATE        NOT NULL,
  syqt INTEGER     NOT NULL,
  sycr VARCHAR(40),
  sytn VARCHAR(40),
  syct TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sycb VARCHAR(5)  NOT NULL,
  syut TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
  syub VARCHAR(5)  NOT NULL,
  sydf CHAR(1)     NOT NULL DEFAULT '0',
  FOREIGN KEY (juno, juln) REFERENCES TBL_JUTYU_DETAIL(juno, juln)
);

-- ---------------------------------------------------------------------
-- TBL_SOUSA_LOG  操作履歴（監査ログ）
--   slid BIGSERIAL PK / slop 操作区分 / sltg 対象機能 / slky 対象キー
--   slms メッセージ / slrs 結果(OK/NG) / slcb 実施者社員番号 / slct 実施日時
-- ---------------------------------------------------------------------
CREATE TABLE TBL_SOUSA_LOG (
  slid BIGSERIAL  PRIMARY KEY,
  slop VARCHAR(10) NOT NULL,
  sltg VARCHAR(10) NOT NULL,
  slky VARCHAR(40),
  slms VARCHAR(500),
  slrs CHAR(2)     NOT NULL,
  slcb VARCHAR(5)  NOT NULL,
  slct TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 検索高速化用インデックス
CREATE INDEX idx_sousa_log_slct ON TBL_SOUSA_LOG (slct DESC);
CREATE INDEX idx_sousa_log_slcb ON TBL_SOUSA_LOG (slcb);
CREATE INDEX idx_jutyu_header_judt ON TBL_JUTYU_HEADER (judt);
CREATE INDEX idx_hatyu_header_hadt ON TBL_HATYU_HEADER (hadt);

-- =====================================================================
-- 初期データ INSERT
-- =====================================================================

-- 社員マスタ（管理者・業務担当の最低限）
INSERT INTO TBL_SYAIN (sycd, synm, sykn, sybs, sypo, syml, sycb, syub) VALUES
  ('00001', '管理 花子', 'カンリ ハナコ', '01', '01', 'admin@example.co.jp', '00001', '00001'),
  ('12345', '業務 太郎', 'ギョウム タロウ', '02', '03', 'gyomu@example.co.jp', '00001', '00001'),
  ('22345', '在庫 次郎', 'ザイコ ジロウ', '03', '03', 'zaiko@example.co.jp', '00001', '00001'),
  ('33345', '出荷 三郎', 'シュッカ サブロウ', '04', '03', 'syukka@example.co.jp', '00001', '00001');

-- 商品マスタ（カテゴリ: 01=筆記用具 02=ノート 03=ファイル 04=事務用品 05=その他）
INSERT INTO TBL_SYOHIN (prdcd, prdnm, prdct, prdpr, prdun, prdrm, prdcb, prdub) VALUES
  ('P0001', '鉛筆 HB',           '01',   80, '本', '標準鉛筆',          '00001', '00001'),
  ('P0002', 'ボールペン 黒 0.5', '01',  150, '本', '油性',              '00001', '00001'),
  ('P0003', 'A4ノート 50枚',     '02',  280, '冊', '横罫',              '00001', '00001'),
  ('P0004', 'クリアファイル A4', '03',   40, '枚', '透明',              '00001', '00001'),
  ('P0005', '消しゴム',          '04',  100, '個', 'プラスチック消しゴム','00001', '00001');

-- 取引先マスタ（1=仕入先 2=販売先 3=両方）
INSERT INTO TBL_TORIHIKI (trcd, trnm, trtp, trzp, trad, trtl, trpc, trml, trrm, trcb, trub) VALUES
  ('T0001', '東京文具卸 株式会社',     '1', '101-0001', '東京都千代田区神田1-1-1',   '03-0000-0001', '仕入 一郎', 'shire@tokyo-bungu.co.jp', '主要仕入先', '00001', '00001'),
  ('T0002', '関西ステーショナリー株式会社', '1', '530-0001', '大阪市北区梅田2-2-2',     '06-0000-0002', '仕入 二郎', 'osaka@kansai-st.co.jp',   '副次仕入先', '00001', '00001'),
  ('T0101', '山田事務機 株式会社',     '2', '160-0023', '東京都新宿区西新宿3-3-3',   '03-0000-0101', '販売 花子', 'sales@yamada-jimu.co.jp', '主要販売先', '00001', '00001'),
  ('T0102', '佐藤商事 株式会社',       '2', '231-0001', '横浜市中区新港4-4-4',       '045-000-0102', '販売 桜子', 'sales@sato-shoji.co.jp',  '販売先',     '00001', '00001'),
  ('T0201', '中央オフィスサプライ株式会社', '3', '460-0001', '名古屋市中区栄5-5-5',     '052-000-0201', '中央 健',   'info@chuo-os.co.jp',      '仕入販売両方','00001', '00001');

-- 在庫初期値（商品マスタに対応）
INSERT INTO TBL_ZAIKO (prdcd, zkqt, zkrs, zkcb, zkub) VALUES
  ('P0001', 500, 0, '00001', '00001'),
  ('P0002', 300, 0, '00001', '00001'),
  ('P0003', 120, 0, '00001', '00001'),
  ('P0004',  80, 0, '00001', '00001'),
  ('P0005', 200, 0, '00001', '00001');

-- =====================================================================
-- 完了
-- =====================================================================
