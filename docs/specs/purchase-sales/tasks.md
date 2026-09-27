# tasks.md — 仕入管理・売上管理 WebAPI

凡例: `[x]` 完了 / `[ ]` 未完了。上から順に着手する(各グループは前のグループに依存する)。

## 仕様の承認

- [ ] `docs/steering/` のレビュー・承認
- [ ] requirements.md / design.md / openapi.yaml のレビュー・承認
- [ ] `docs/specs/authentication/` のレビュー・承認

## 1. プロジェクト基盤

- [ ] T-101: 試作コードを削除する(`RestApiSampleController`、`fasse/`、`user/`、`foodcomposition/`、関連 Mapper XML、`src/test/resources/schema.sql` / `data.sql`)
- [ ] T-102: 食品成分表の DDL を `src/test/resources/ddl/m_food_composition.sql` に移す(`FoodCompositionTable.csv` はそのまま残す)
- [ ] T-103: `build.gradle` を更新する(Spring Boot 3.5 系、thymeleaf と個別指定の jackson-databind を削除、validation / security / oauth2-resource-server / flyway-mysql / JaCoCo を追加)
- [ ] T-104: `application.yaml` とプロファイル別設定(`local` / `test` / `aws`)を作成する。機密情報は環境変数から読む
- [ ] T-105: `.gitignore` に `logs/` を追加する
- [ ] T-106: Flyway マイグレーション `V1__create_purchase_sales_tables.sql` を作成する(design.md 2.1 節)
- [ ] T-107: `JacksonConfig`(スネークケース、未定義項目の無視、型変換の無効化、`BigDecimal`、日時形式)を実装する
- [ ] T-108: `RequestIdFilter`(MDC、`X-Request-Id`、アクセスログ)と `logback-spring.xml` を実装する
- [ ] T-109: `ErrorResponse`・業務例外・`GlobalExceptionHandler` を実装する(design.md 6 節)
- [ ] T-110: `CorsConfig` を実装する
- [ ] T-111: `docs/specs/authentication/tasks.md` の実装タスクを行う

### テスト

- [ ] T-151: テスト用 DB(`fasse_test`)の接続設定と `TestDataLoader`(CSV 投入)を作成する
- [ ] T-152: 全テーブルのテストデータ CSV(各 5 件程度)を作成する(design.md 10.1 節)
- [ ] T-153: `GlobalExceptionHandler` と `JacksonConfig` のテスト(400 の各 `message`、型変換の拒否、500 で内部情報を返さないこと)
- [ ] T-154: `RequestIdFilter` のテスト(`X-Request-Id` の付与、エラーレスポンスの `requestId` と一致すること)

## 2. マスタ(品目・仕入先・メニュー)

- [ ] T-201: 品目(`item`)の Entity / DTO / Mapper / Service / Controller を実装する
- [ ] T-202: 仕入先(`supplier`)を同様に実装する
- [ ] T-203: メニュー(`menu`)を同様に実装する

### テスト

- [ ] T-251: 各 Mapper のテスト(一覧順、取得、登録時の採番、更新、論理削除)
- [ ] T-252: 各 Service のテスト(既定値 `is_active=true`、PUT で省略項目を維持、404)
- [ ] T-253: 各 Controller のテスト(必須・型・ENUM・桁数の 400、パス `id` の 400、201/200/204、JWT なしで 401)

## 3. 消費税率マスタ

- [ ] T-301: 消費税率(`taxrate`)の Entity / DTO / Mapper / Service / Controller を実装する(キーは `tax_category` + `valid_from`、重複は 409、物理削除)

### テスト

- [ ] T-351: Mapper のテスト(絞り込み、並び順、一意制約違反)
- [ ] T-352: Service のテスト(409、404、`valid_to` 省略時に既存値を維持、ボディのキーを無視)
- [ ] T-353: Controller のテスト(パス・クエリの `taxCategory` / `validFrom` 検証、409 の応答)

## 4. 伝票(仕入・売上)

- [ ] T-401: 伝票番号採番(`SlipNoService` / `SlipNoMapper`)を実装する
- [ ] T-402: 仕入伝票(`purchase`)の Entity / DTO / Mapper / Service / Controller を実装する(参照先マスタの確認、明細の全洗い替え)
- [ ] T-403: 売上伝票(`sales`)を同様に実装する(`customer_count` / `discount_amount` の既定値、`sales_datetime` の JST 正規化)

### テスト

- [ ] T-451: `SlipNoMapper` のテスト(同一日付で連番、日付が変わるとリセット)
- [ ] T-452: 伝票 Mapper のテスト(日付範囲の両端を含む、並び順、明細の登録・一括削除)
- [ ] T-453: 伝票 Service のテスト(採番形式、PUT で伝票番号・`created_at` を維持、日付変更で採番し直さない、参照先なしで 400、404)
- [ ] T-454: 伝票 Controller のテスト(`from` / `to` の 400、`details` の検証と添字付き項目名、UUID でない `id` の 400)

## 5. 結合テスト・品質

- [ ] T-501: 全リソースの CRUD を HTTP で実行する結合テストを作成する(JWT あり/なし)
- [ ] T-502: 明細の登録に失敗した場合にヘッダ・採番が残らないこと(ロールバック)の結合テストを作成する
- [ ] T-503: JaCoCo のレポートを設定し、カバレッジが 80% 以上であることを確認する(`common/config` を除く)

## 6. 動作確認・ドキュメント

- [ ] T-601: ローカル起動し、`fasse_infra/postman/` のコレクションを `http://localhost:8080` に向けて全 API を確認する
- [ ] T-602: `README.md` をセットアップ手順(Windows での JDK 21・MySQL 8.4 のインストール、`fasse` / `fasse_test` の作成、環境変数、JWT の取得方法、起動・テストコマンド)。試作用の手順(`schema.sql` / `data.sql` の投入、`/users` の確認等)は削除するに更新する
