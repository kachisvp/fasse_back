# requirements.md — 仕入管理・売上管理 WebAPI

## 背景

- Fasse の WebAPI は現在 `fasse_infra` の API Gateway + Lambda + DynamoDB で提供している
- WebAPI 受口を Spring Boot(Fargate)に、データストアを Aurora MySQL Serverless に置き換える。本仕様はその Spring Boot 側の実装を対象とする
- 置き換えの前後でフロントエンドの改修が不要となるよう、API 仕様([openapi.yaml](./openapi.yaml))と振る舞いを Lambda 実装に合わせる

## 目的

飲食店(テーブル会計)を対象に、仕入(仕入先からの仕入伝票)と売上(伝票)を管理する WebAPI を提供する。

## スコープ

- マスタ: 品目(`/items`)、仕入先(`/suppliers`)、メニュー(`/menus`)の CRUD
- 消費税率マスタ(`/tax-rates`)の CRUD
- 仕入伝票(`/purchases`)、売上伝票(`/sales`)の CRUD(ヘッダ+明細)
- 全 API の JWT 認証(`docs/specs/authentication`)
- DB スキーマのマイグレーション(Flyway)とテストデータ

## ユーザーストーリー

- 仕入担当者として、仕入伝票をヘッダ+明細で登録したい
- 店舗スタッフとして、売上伝票を営業日(`business_date`)を指定して登録したい(深夜営業などで日付をまたぐ取引を正しい営業日に紐付けるため)
- 管理者として、仕入/売上を日付範囲で一覧確認したい(売上は営業日単位)
- 管理者として、消費税率(標準/軽減/非課税)を税区分・適用期間を指定して管理したい(将来の税率改定に備えるため)

## 受け入れ基準

### 機能

- [ ] openapi.yaml の全エンドポイント(12 パス)が、定義どおりのリクエスト・レスポンス・ステータスコードで動作する
- [ ] マスタ(品目・仕入先・メニュー)の削除は論理削除(`is_active` を `false` に更新)である
- [ ] 消費税率マスタは `tax_category` + `valid_from` をキーに CRUD でき、削除は物理削除である。同一キーの登録は 409 を返す
- [ ] 品目・メニューマスタは税区分(`STANDARD` / `REDUCED` / `EXEMPT`)を保持する
- [ ] 仕入/売上明細は登録時点の税率を `tax_rate` としてスナップショット保持する
- [ ] 伝票番号(`purchase_no` / `sales_no`)はサーバーが `PO-YYYYMMDD-NNNN` / `SO-YYYYMMDD-NNNN` 形式で採番し、一意である。連番は `purchase_date` / `business_date` 単位でリセットする
- [ ] ヘッダと明細は 1 トランザクションで登録・更新・削除される(片方だけ登録される状態にならない)
- [ ] 伝票更新時、明細は送信された内容で全洗い替えされる
- [ ] 伝票一覧は `from` / `to`(両端を含む)で日付範囲指定でき、仕入は `purchase_date`、売上は `business_date` を基準にする

### 入力検証・エラー応答

- [ ] design.md「入力検証・エラー応答」の条件で 400 を返し、`message` に不備のある項目名を含む
- [ ] 存在しない ID の取得・更新・削除は 404 を返す
- [ ] 予期しない例外は 500(`message` は固定文言 `Internal Server Error`)を返し、スタックトレース等をレスポンスに含めない
- [ ] 全エラーレスポンスが `{ "message", "requestId" }` 形式である
- [ ] サーバー管理項目(`id`、伝票番号、`created_at`、`updated_at`、明細の `id`)はリクエストで上書きできない

### テスト・品質

- [ ] Service 層の単体テスト、Mapper の DB テスト、Controller の API テスト、結合テストがあり、`./gradlew test` がすべて成功する
- [ ] JaCoCo の命令カバレッジ(`common/config` を除く)が 80% 以上である
- [ ] 各テーブルのテストデータが `src/test/resources/testdata/<テーブル名>.csv` に 5 件程度ある

## スコープ外

- 仕入先/メニュー単位の集計・一覧、集計・レポーティング機能
- 金額の整合性検証(`quantity × unit_price = amount`、明細合計 = `subtotal` 等)。クライアントの責務とする
- DynamoDB からのデータ移行
- コンテナ化・Fargate へのデプロイ・API Gateway の統合切り替え(別仕様)
- 食品成分表(`m_food_composition`)の API
