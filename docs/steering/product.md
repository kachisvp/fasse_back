# product.md — プロダクト概要

本ファイルは本リポジトリ(`fasse_back`)に共通する前提を定める。記載内容は常に「現時点の仕様」であり、経緯は持たない(経緯は Git 履歴で追う)。機能単位の要求・設計・タスクは `docs/specs/<feature>/` に記載する。

## 1. プロダクト

**Fasse**: 飲食店(テーブル会計)向けの仕入管理・売上管理システム。

- フロントエンド(Flutter)から WebAPI を経由して DB の値を取得・更新する
- 対象業務
  - 仕入管理: 仕入先マスタ、品目マスタ、仕入伝票(ヘッダ+明細)
  - 売上管理: メニューマスタ、売上伝票(ヘッダ+明細)
  - 消費税管理: 消費税率マスタ(標準/軽減/非課税、適用期間)
- 利用者の認証は JWT(KMS 非対称鍵で署名)で行う。JWT の発行は `fasse_infra` の JWT 発行基盤が担う

## 2. 本リポジトリの役割

`fasse_back` は Fasse の **WebAPI 受口(Spring Boot)** である。`fasse_infra` の API Gateway + Lambda + DynamoDB で提供している WebAPI を、同一の API 仕様で置き換える。

```
現在:     Flutter(Web) → API Gateway → Lambda → DynamoDB          (fasse_infra)
置き換え後: Flutter(Web) → Spring Boot(Fargate) → Aurora MySQL Serverless (fasse_back)
```

- 置き換えの前後で、API 仕様と JWT 認証の仕組み(KMS 公開鍵による検証)は変えない
- 本リポジトリは JWT の **検証のみ** を行い、発行は行わない

## 3. リポジトリ構成

| リポジトリ | 役割 |
|---|---|
| `fasse_front` | Flutter(Web)フロントエンド |
| `fasse_infra` | AWS CDK によるインフラ、JWT 発行基盤、および現行の WebAPI 受口(Lambda) |
| `fasse_back`(本リポジトリ) | Spring Boot による WebAPI 受口(置き換え後) |

## 4. 仕様の正本

| 対象 | 正本 |
|---|---|
| WebAPI 仕様(エンドポイント・スキーマ) | 本リポジトリ `docs/specs/purchase-sales/openapi.yaml`。`fasse_infra`(現行の Lambda)・`fasse_front` はこれを参照する |
| データモデル(DDL) | 本リポジトリ `docs/specs/purchase-sales/design.md` |
| JWT 発行基盤(AccessKey / Cognito / KMS 署名) | `fasse_infra` の `docs/specs/authentication/` |
| WebAPI 受口での JWT 検証(Spring Boot) | 本リポジトリ `docs/specs/authentication/` |

機能ごとの仕様:

- `docs/specs/purchase-sales/`: 仕入管理・売上管理・消費税率マスタの WebAPI
- `docs/specs/authentication/`: WebAPI 受口での JWT 検証
- `docs/specs/local-seed-data/`: 開発用 DB へのテストデータ投入(開発環境向け)

## 5. 今後の拡張

- 食品成分表(`m_food_composition`)を用いた機能を今後追加する予定。DDL とテストデータ(`src/test/resources/FoodCompositionTable.csv`)を保持しているが、現時点では API を提供しない
- コンテナ化・Fargate へのデプロイは別仕様とする
