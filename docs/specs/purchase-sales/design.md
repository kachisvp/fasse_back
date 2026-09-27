# design.md — 仕入管理・売上管理 WebAPI

## 1. アーキテクチャ

```
Flutter(Web) → Spring Boot(Fargate) → Aurora MySQL Serverless v2
                 ├─ RequestIdFilter      (リクエスト ID 採番・アクセスログ)
                 ├─ Spring Security      (JWT 検証。docs/specs/authentication)
                 ├─ Controller           (入力検証・DTO 変換)
                 ├─ Service              (業務ロジック・トランザクション)
                 └─ Repository(MyBatis)  (SQL)
```

- API 仕様(エンドポイント・スキーマ)の正本は [openapi.yaml](./openapi.yaml)。本書は openapi.yaml で表しきれない振る舞いを定める
- 振る舞いは `fasse_infra` の Lambda 実装を基に定めている。Lambda の現状の振る舞いと異なる点は 9 節にまとめる(置き換えまでの間に Lambda 側を本仕様に合わせるかは `fasse_infra` 側で判断する)

### 1.1 パッケージとクラス

| 機能 | パッケージ | Controller | 主なクラス |
|---|---|---|---|
| 品目 | `item` | `ItemController`(`/items`) | `ItemService`, `ItemMapper`, `ItemRequest`, `ItemResponse`, `Item` |
| 仕入先 | `supplier` | `SupplierController`(`/suppliers`) | 同上の構成 |
| メニュー | `menu` | `MenuController`(`/menus`) | 同上の構成 |
| 消費税率 | `taxrate` | `TaxRateController`(`/tax-rates`) | `TaxRateService`, `TaxRateMapper`, `TaxRateRequest`, `TaxRateUpdateRequest`, `TaxRateResponse`, `TaxRate` |
| 仕入伝票 | `purchase` | `PurchaseController`(`/purchases`) | `PurchaseService`, `PurchaseMapper`, `PurchaseRequest`, `PurchaseDetailRequest`, `PurchaseResponse`, `PurchaseHeaderResponse`, `PurchaseDetailResponse`, `PurchaseHeader`, `PurchaseDetail` |
| 売上伝票 | `sales` | `SalesController`(`/sales`) | 仕入伝票と同じ構成 |
| 伝票番号採番 | `common/slipno` | - | `SlipNoService`, `SlipNoMapper` |
| 共通 | `common/*` | - | `JacksonConfig`, `CorsConfig`, `RequestIdFilter`, `GlobalExceptionHandler`, `ApiException` 系, `TaxCategory`(enum) |

## 2. データモデル

### 2.1 DDL(Flyway `V1__create_purchase_sales_tables.sql`)

```sql
-- マスタ（IDはBIGINT AUTO_INCREMENT）
CREATE TABLE m_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  item_name VARCHAR(100) NOT NULL,
  unit VARCHAR(20) NOT NULL,
  standard_price DECIMAL(12,2),
  tax_category ENUM('STANDARD', 'REDUCED', 'EXEMPT') NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE m_supplier (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  supplier_name VARCHAR(100) NOT NULL,
  postal_code VARCHAR(10),
  address VARCHAR(255),
  phone_number VARCHAR(30),
  email VARCHAR(255),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE m_menu (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  menu_name VARCHAR(100) NOT NULL,
  category VARCHAR(50) NOT NULL,
  standard_price DECIMAL(12,2) NOT NULL,
  tax_category ENUM('STANDARD', 'REDUCED', 'EXEMPT') NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
);

-- 消費税率マスタ（税区分ごとの税率を期間で管理する）
CREATE TABLE m_tax_rate (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tax_category ENUM('STANDARD', 'REDUCED', 'EXEMPT') NOT NULL,
  description VARCHAR(50) NOT NULL,
  rate DECIMAL(5,4) NOT NULL,
  valid_from DATE NOT NULL,
  valid_to DATE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT uk_m_tax_rate_category_valid_from
    UNIQUE (tax_category, valid_from)
);

-- 伝票番号の採番（counter_name = '<purchase_no|sales_no>#<YYYY-MM-DD>'）
CREATE TABLE t_slip_no_counter (
  counter_name VARCHAR(40) PRIMARY KEY,
  value INT NOT NULL
);

-- 仕入（IDはCHAR(36)=UUID。headerを先に定義する）
CREATE TABLE t_purchase_header (
  id CHAR(36) PRIMARY KEY,
  purchase_no VARCHAR(20) NOT NULL UNIQUE,
  supplier_id BIGINT NOT NULL,
  purchase_date DATE NOT NULL,
  delivery_date DATE,
  subtotal DECIMAL(12,2) NOT NULL,
  tax_amount DECIMAL(12,2) NOT NULL,
  total_amount DECIMAL(12,2) NOT NULL,
  remarks VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_t_purchase_header_supplier
    FOREIGN KEY (supplier_id) REFERENCES m_supplier(id)
);

CREATE INDEX idx_t_purchase_header_purchase_date
  ON t_purchase_header (purchase_date);

CREATE TABLE t_purchase_detail (
  id CHAR(36) PRIMARY KEY,
  purchase_id CHAR(36) NOT NULL,
  item_id BIGINT NOT NULL,
  quantity DECIMAL(10,2) NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  tax_rate DECIMAL(5,4) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_t_purchase_detail_purchase
    FOREIGN KEY (purchase_id) REFERENCES t_purchase_header(id),
  CONSTRAINT fk_t_purchase_detail_item
    FOREIGN KEY (item_id) REFERENCES m_item(id)
);

-- 売上（IDはCHAR(36)=UUID。headerを先に定義する）
CREATE TABLE t_sales_header (
  id CHAR(36) PRIMARY KEY,
  sales_no VARCHAR(20) NOT NULL UNIQUE,
  sales_datetime DATETIME NOT NULL,
  business_date DATE NOT NULL,
  table_no VARCHAR(10),
  customer_count INT NOT NULL DEFAULT 1,
  subtotal DECIMAL(12,2) NOT NULL,
  tax_amount DECIMAL(12,2) NOT NULL,
  discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
  total_amount DECIMAL(12,2) NOT NULL,
  payment_method VARCHAR(20) NOT NULL,
  remarks VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_t_sales_header_business_date
  ON t_sales_header (business_date);

CREATE TABLE t_sales_detail (
  id CHAR(36) PRIMARY KEY,
  sales_id CHAR(36) NOT NULL,
  menu_id BIGINT NOT NULL,
  quantity INT NOT NULL,
  unit_price DECIMAL(12,2) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  tax_rate DECIMAL(5,4) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ON UPDATE CURRENT_TIMESTAMP,

  CONSTRAINT fk_t_sales_detail_sales
    FOREIGN KEY (sales_id) REFERENCES t_sales_header(id),
  CONSTRAINT fk_t_sales_detail_menu
    FOREIGN KEY (menu_id) REFERENCES m_menu(id)
);
```

### 2.2 スキーマ設計上の決定

- マスタ(`m_item`, `m_supplier`, `m_menu`, `m_tax_rate`)の PK は `BIGINT AUTO_INCREMENT`、伝票系の PK は `CHAR(36)`(UUID、アプリケーションで採番)とする
- `m_tax_rate` の API 上のキーは `tax_category` + `valid_from` とし、一意制約 `uk_m_tax_rate_category_valid_from` で保証する。`id` は API に公開しない
- 伝票番号の連番は `t_slip_no_counter` で採番する(4 節)
- 一覧の日付範囲検索のため、`purchase_date` / `business_date` にインデックスを張る
- `m_item` / `m_supplier` / `m_menu` は `is_active` を持ち、削除は論理削除とする(明細から FK 参照されるため)
- `t_sales_header` は `business_date`(営業日)を持つ。深夜営業などで日付をまたぐ取引を、実際の営業日に紐付けるための項目で、クライアントが指定する(サーバーで算出しない)
- 伝票に担当者(`staff_id`)は持たない
- 明細の並び順を表す列は持たない。明細の並び順は保証しない(Lambda 実装と同じ)

### 2.3 消費税の扱い

- `tax_category`(`STANDARD` / `REDUCED` / `EXEMPT`)は消費税法で定まる固定区分のため、独立したマスタにせず ENUM とする。Java では `TaxCategory` enum で表す
- `m_tax_rate` は区分ごとに `valid_from` / `valid_to` で有効期間を管理する。税率改定時は既存行の `valid_to` を設定し、新しい行を POST する運用を推奨する
- 明細の `tax_rate` は登録時点の税率のスナップショットであり、`m_tax_rate` を FK 参照しない。そのため `m_tax_rate` の削除は物理削除でよい
- ある日付に有効な税率の取得 API は提供しない。クライアントは `GET /tax-rates?tax_category=<区分>` で取得して選ぶ
- ヘッダの `tax_amount` の計算(インボイス制度に沿った税率ごとの端数処理)はクライアントの責務であり、サーバーは検証しない

## 3. リソースごとの振る舞い

### 3.1 マスタ(品目・仕入先・メニュー)

| 操作 | 振る舞い |
|---|---|
| `GET /xxx` | 全件(`is_active=false` を含む)を `id` 昇順で返す |
| `GET /xxx/{id}` | 1 件を返す。無ければ 404 |
| `POST /xxx` | 検証後に登録し、201 と登録結果を返す。`is_active` 省略時は `true` |
| `PUT /xxx/{id}` | 無ければ 404。リクエストの項目で上書きし、省略した任意項目は既存の値を維持する。200 と更新結果を返す |
| `DELETE /xxx/{id}` | 無ければ 404。`is_active=false`、`updated_at` を現在時刻に更新する。204 |

### 3.2 消費税率マスタ

| 操作 | 振る舞い |
|---|---|
| `GET /tax-rates` | 全件、または `tax_category` クエリで絞り込んだ行を `tax_category`(ENUM 定義順)・`valid_from` 昇順で返す |
| `GET /tax-rates/{taxCategory}/{validFrom}` | 1 件を返す。無ければ 404 |
| `POST /tax-rates` | 登録し 201。同じ `tax_category` + `valid_from` が既にあれば 409 |
| `PUT /tax-rates/{taxCategory}/{validFrom}` | 無ければ 404。`TaxRateUpdateRequest`(`description`, `rate` 必須、`valid_to` 任意)で更新する。`valid_to` を省略した場合は既存の値を維持する。キーはパスの値を使い、ボディの `tax_category` / `valid_from` は無視する |
| `DELETE /tax-rates/{taxCategory}/{validFrom}` | 無ければ 404。物理削除し 204 |

### 3.3 仕入伝票・売上伝票

仕入・売上は同じ振る舞いとし、以下の読み替えで共通化する。

| 項目 | 仕入 | 売上 |
|---|---|---|
| 基準日 | `purchase_date` | `business_date` |
| 伝票番号 | `purchase_no`(`PO-`) | `sales_no`(`SO-`) |
| 明細の外部キー | `purchase_id` | `sales_id` |
| 明細の参照マスタ | `item_id` → `m_item` | `menu_id` → `m_menu` |
| ヘッダの参照マスタ | `supplier_id` → `m_supplier` | なし |

| 操作 | 振る舞い |
|---|---|
| `GET /xxx?from=&to=` | 基準日が `from` 以上 `to` 以下のヘッダ(明細なし)を、基準日・伝票番号の昇順で返す。`from > to` の場合は空配列 |
| `GET /xxx/{id}` | ヘッダ+明細を返す。無ければ 404 |
| `POST /xxx` | 1 トランザクションで、伝票番号の採番 → ヘッダ登録 → 明細登録を行う。201 とヘッダ+明細を返す |
| `PUT /xxx/{id}` | 無ければ 404。1 トランザクションでヘッダ更新 → 既存明細の全削除 → リクエストの明細の全登録を行う(全洗い替え)。伝票番号・`created_at` は維持し、基準日を変更しても伝票番号は採番し直さない。ヘッダの省略した任意項目は既存の値を維持する。200 とヘッダ+明細を返す |
| `DELETE /xxx/{id}` | 無ければ 404。1 トランザクションで明細の全削除 → ヘッダ削除を行う(物理削除)。204 |

- 売上ヘッダの `customer_count` 省略時は `1`、`discount_amount` 省略時は `0` とする(POST のみ。PUT では既存の値を維持)
- ヘッダ・明細の `id` は `UUID.randomUUID()` で採番する
- 参照先マスタ(`supplier_id` / `item_id` / `menu_id`)の存在は Service で事前に確認し、無ければ 400(`invalid reference: supplier_id, details[1].item_id` の形式)。論理削除済みのマスタは参照してよい

### 3.4 レスポンスの作り方

- 登録・更新後は DB から読み直した値でレスポンスを作る(`created_at` / `updated_at` を DB の値と一致させるため)
- レスポンス DTO は openapi.yaml のスキーマの項目のみを持つ(`m_tax_rate.id`、明細の `purchase_id` / `sales_id`、`created_at` / `updated_at` の明細分は返さない)

## 4. 伝票番号の採番

- 形式: `<PO|SO>-<基準日 YYYYMMDD>-<連番 4 桁ゼロ埋め>`(例: `PO-20260712-0001`)。連番が 9999 を超えた場合は桁を増やす
- `SlipNoMapper` に次の 2 メソッドを定義し、`SlipNoService` が伝票登録と同じトランザクション内で順に呼んで連番を得る(1 回の Mapper 呼び出しで複数の SQL を実行しないため、接続設定 `allowMultiQueries` は使わない)

  | メソッド | SQL |
  |---|---|
  | `increment(counterName)` | `INSERT INTO t_slip_no_counter (counter_name, value) VALUES (#{counterName}, LAST_INSERT_ID(1)) ON DUPLICATE KEY UPDATE value = LAST_INSERT_ID(value + 1)` |
  | `selectLastInsertId()` | `SELECT LAST_INSERT_ID()` |

- `LAST_INSERT_ID()` は DB 接続ごとの値である。同じトランザクション内の Mapper 呼び出しは同じ接続を使うため、`selectLastInsertId()` は直前の `increment` で設定した値を返す
- 行ロックはトランザクション終了まで保持されるため、同じ基準日の同時登録でも番号は重複しない。伝票登録がロールバックされた場合は連番も戻る

## 5. JSON・日付の扱い

- JSON の項目名はスネークケース(Jackson `PropertyNamingStrategies.SNAKE_CASE`)
- 金額・数量・税率は `BigDecimal` で扱い、JSON の数値として返す(小数部の桁数は DB の列定義に従う。例: `1200.00`, `0.1000`)
- 業務日付(`purchase_date`, `delivery_date`, `business_date`, `valid_from`, `valid_to`)は `LocalDate`、`YYYY-MM-DD`
- `sales_datetime` は入力でタイムゾーンのオフセットを必須とする(例: `2026-07-12T19:30:00+09:00`)。JST(+09:00)に変換して `DATETIME` に保存し、`+09:00` 付きで返す
- `created_at` / `updated_at` は UTC とし、`2026-07-12T10:30:00.000Z` 形式で返す。JDBC 接続のタイムゾーンを UTC に固定する(`connectionTimeZone=UTC`、`forceConnectionTimeZoneToSession=true`)

## 6. 入力検証・エラー応答

### 6.1 400 を返す条件

| 条件 | `message` |
|---|---|
| ボディが無い | `request body is required` |
| ボディが JSON として解析できない | `request body is not valid JSON` |
| ボディが JSON オブジェクトでない | `request body must be a JSON object` |
| 必須項目の欠落(`null` を含む)、型の不一致、ENUM 外の値、日付・日時の形式不正、`details` が配列でない、DB の列長・整数部桁数の超過 | `invalid or missing fields: <項目名>, ...` |
| 参照先マスタが存在しない | `invalid reference: <項目名>, ...` |
| `from` / `to` が無い、または形式不正 | `from and to are required in YYYY-MM-DD format` |
| マスタのパス `id` が正の整数でない | `id must be a positive integer` |
| 伝票のパス `id` が UUID 形式でない | `id must be a UUID` |
| `taxCategory` が ENUM 外(パス・クエリ) | `taxCategory must be one of STANDARD, REDUCED, EXEMPT` / `tax_category must be one of ...` |
| パス `validFrom` の形式不正 | `validFrom must be in YYYY-MM-DD format` |

- 項目名はスネークケースで、明細は `details[0].item_id` のように添字付きで示す
- 型の検証は厳密に行い、暗黙の型変換をしない(例: 文字列 `"100"` を数値項目に、数値を文字列項目に、`1.5` を整数項目に受け付けない)。Jackson の型変換(coercion)を無効化して実現する
- 必須項目・桁数は Bean Validation(`@NotNull`, `@Size`, `@Digits` 等)で検証し、違反した全項目を `message` に列挙する。型の不一致は JSON の解析時点で検出するため、最初の 1 項目のみを示す
- スキーマに無い項目は無視する(`FAIL_ON_UNKNOWN_PROPERTIES=false`)。サーバー管理項目(`id`、伝票番号、`created_at`、`updated_at`)はリクエスト DTO に持たないため、送られても無視される

### 6.2 その他のステータス

| ステータス | 条件 | `message` |
|---|---|---|
| 401 | JWT 検証 NG(`docs/specs/authentication`) | 同仕様による |
| 404 | ID に該当する行が無い、存在しないパス | `Not Found` |
| 405 | 未対応の HTTP メソッド | `Method Not Allowed` |
| 409 | 消費税率の一意キー重複 | `tax rate already exists: <tax_category>/<valid_from>` |
| 415 | `Content-Type` が JSON でない | `Unsupported Media Type` |
| 500 | 予期しない例外 | `Internal Server Error`(固定) |

### 6.3 実装

- エラーレスポンスは `ErrorResponse(message, requestId)` とし、`@RestControllerAdvice` の `GlobalExceptionHandler` で生成する
- 業務例外: `BadRequestException`(400)、`NotFoundException`(404)、`ConflictException`(409)
- ログ: 400 / 404 / 405 / 409 / 415 は WARN(理由のみ)、500 は ERROR(スタックトレースを含む)。リクエストボディ全体・トークンは出力しない

## 7. トランザクション

- Service のメソッド単位で `@Transactional` を付ける(参照系は `readOnly = true`)
- 伝票の登録・更新・削除は、採番・ヘッダ・明細を 1 トランザクションで行う
- 一意制約違反(`m_tax_rate`)は `DuplicateKeyException` を 409 に変換する

## 8. 横断的な設定

### 8.1 ログ

- `RequestIdFilter` がリクエストごとに `requestId`(UUID)を採番して MDC とレスポンスヘッダ `X-Request-Id` に設定し、完了時に INFO でアクセスログ(メソッド、パス、ステータス、処理時間)を出す
- `logback-spring.xml`
  - `local`: コンソール + ファイル(`logs/fasse_back.log`、日次ローテーション、7 日保持)。パターンに `requestId` / `userId` を含める
  - `aws`: 構造化ログ(JSON)を標準出力し、CloudWatch Logs に送る(保持期間はインフラ側で設定する)
  - `test`: コンソールのみ
- ログレベルは既定で INFO。`logging.level.root` で変更できる

### 8.2 CORS

- `fasse.cors.allowed-origins` で許可するオリジンを指定する(例: `http://localhost:5000`, CloudFront のドメイン)
- 許可メソッド: `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`。許可ヘッダ: `Authorization`, `Content-Type`。公開ヘッダ: `X-Request-Id`

### 8.3 設定値

設定ファイルの役割は `docs/steering/tech.md` 2 節による。`application.yaml` は環境変数を参照し、`application-local.yaml` / `application-test.yaml` は値を直接記載して上書きする。

| プロパティ | `application.yaml`(dev / stg) | `application-local.yaml` | `application-test.yaml` |
|---|---|---|---|
| `spring.datasource.url` | `${DB_URL}` | `jdbc:mysql://localhost:3306/fasse?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true` | `jdbc:mysql://localhost:3306/fasse_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true` |
| `spring.datasource.username` / `password` | `${DB_USERNAME}` / `${DB_PASSWORD}` | 開発者ごとの値 | 開発者ごとの値 |
| `fasse.jwt.public-key-pem` | `${JWT_PUBLIC_KEY_PEM:}`(未設定なら全 API が 401) | `fasse_infra/jwt_public_key.pem` の内容 | 空(テストコードで生成した公開鍵を設定する) |
| `fasse.cors.allowed-origins` | `${CORS_ALLOWED_ORIGINS}` | `http://localhost:5000` | `http://localhost:5000` |
| `logging.level.root` | `${LOG_LEVEL:INFO}` | 継承 | 継承 |

## 9. Lambda 実装と意図的に異なる点

| 項目 | Lambda | Spring Boot | 理由 |
|---|---|---|---|
| 消費税率の同一キー登録 | 上書き(PutItem) | 409 | RDB の一意制約で表す。黙って上書きすると誤登録に気付けないため |
| 参照先マスタが存在しない | 登録される | 400 | FK 制約があるため。500 にしないよう事前に確認する |
| 伝票のパス `id` が UUID でない | 404 | 400 | openapi.yaml の定義(`format: uuid`)に合わせる |
| 項目に `null` を指定 | 型不一致で 400 | 省略と同じ扱い(必須なら 400、任意なら POST は未設定・PUT は既存値を維持) | Java の DTO では省略と `null` を区別しないため |
| 型不一致の項目名 | 全項目を列挙 | 最初の 1 項目 | JSON 解析時に検出するため |
| DB の列長・桁数の超過 | 保存される | 400 | RDB の列定義に収まらないため |
| 数値の表記 | 入力値のまま(`1200`) | 列の小数部桁数に従う(`1200.00`) | `DECIMAL` 型のため。数値としては同じ値 |
| `sales_datetime` のオフセット | 入力値のまま | `+09:00` に正規化 | `DATETIME` に JST で保存するため |
| 一覧の並び順 | マスタは不定 | `id` 昇順 | RDB で順序を明示できるため |
| CORS | `*` | 許可オリジンを限定 | `CLAUDE.md` のセキュリティ方針 |

## 10. テスト設計

| 層 | 方式 | 対象 |
|---|---|---|
| Service | JUnit 5 + Mockito(Mapper をモック) | 既存値の維持、既定値、404、参照先確認、伝票番号の組み立て、明細の全洗い替えの呼び出し順 |
| Repository | `@MybatisTest` + テスト用 DB(MySQL 8.4、`fasse_test`) | 各 SQL の結果、日付範囲検索、採番 SQL の連番・日付単位のリセット、一意制約 |
| Controller | `@WebMvcTest` + Spring Security | 6.1 節の各 400 条件、レスポンス形式(スネークケース・日時形式)、ステータスコード、JWT あり/なし |
| 結合 | `@SpringBootTest(RANDOM_PORT)` + テスト用 DB | 各リソースの CRUD を HTTP で一通り実行、トランザクションのロールバック(10.1 節)、500 の応答形式 |

### 10.1 ロールバックの検証

- 入力検証と参照先マスタの確認により、通常の入力では明細の登録は失敗しない。そのため、テストで明細の登録失敗を人為的に起こす
- 結合テストで伝票 Mapper(`PurchaseMapper` / `SalesMapper`)を `@MockitoSpyBean` に差し替え、明細の登録メソッドだけが `RuntimeException` を投げるよう設定する(他のメソッドは実際の SQL を実行する)
- `POST` で明細を含む伝票を登録し、次を検証する
  - 500 と `{ "message": "Internal Server Error", "requestId" }` が返る
  - ヘッダ・明細が DB に残っていない
  - `t_slip_no_counter` の値が登録前と同じである(採番も戻る)
- `PUT` でも同様に、ヘッダ・明細が更新前の状態のまま残ることを検証する

### 10.2 テストデータ

- `src/test/resources/testdata/<テーブル名>.csv` に各テーブル 5 件程度を置く(形式は `docs/steering/structure.md` 5 節)
  - `m_item.csv`, `m_supplier.csv`, `m_menu.csv`, `m_tax_rate.csv`, `t_slip_no_counter.csv`, `t_purchase_header.csv`, `t_purchase_detail.csv`, `t_sales_header.csv`, `t_sales_detail.csv`
  - `t_slip_no_counter.csv` は、ヘッダの伝票番号と矛盾しない値にする
- テスト用ユーティリティ `TestDataLoader` が、全テーブルを空にした後、FK の依存順に CSV を投入する
- テスト用 DB のスキーマは Flyway で作成する(テスト開始時に `fasse_test` へマイグレーションを適用する)

## 11. 未確定事項

なし
