# requirements.md — WebAPI 受口の JWT 検証

## 背景・目的

Fasse の WebAPI は全エンドポイントで JWT 認証を必須とする。JWT は `fasse_infra` の JWT 発行基盤(AccessKey 経路・Cognito 経路)が KMS 非対称鍵で署名した単一形式に統一されている。本仕様は、Spring Boot の WebAPI 受口で、`fasse_infra` の Lambda と同じ検証を行うための要件を定める。

JWT 発行基盤の要件は `fasse_infra` の `docs/specs/authentication/` を参照する。

## スコープ

- 対象: Spring Boot の WebAPI 受口での JWT 検証
- 対象外: JWT の発行(AccessKey 照合、Cognito ID Token 検証、KMS 署名)。ローカル開発でも発行は行わず、`fasse_infra` の stg 環境で取得した JWT を使う

## 機能要件

- REQ-A01: `docs/specs/purchase-sales/openapi.yaml` の全エンドポイントで、`Authorization: Bearer <JWT>` による認証を必須とする
- REQ-A02: 検証する JWT の発行者は、JWT 発行基盤(KMS 署名)の単一発行者に限る。Cognito が発行した JWT を直接受け付けない
- REQ-A03: 署名は、あらかじめ設置した KMS 公開鍵(PEM、RSA 2048)で、RS256 に限って検証する。リクエストの都度 KMS にアクセスしない
- REQ-A04: 署名に加えて有効期限(`exp`)を検証する。`exp` が無いトークンは不正とする
- REQ-A05: 以下の場合は 401 を返す。レスポンスは他のエラーと同じ形式(`{ "message", "requestId" }`)とする
  - `Authorization` ヘッダが無い、または `Bearer ` で始まらない
  - JWT の形式が不正、`alg` が RS256 でない、署名が不正、有効期限切れ
  - 公開鍵が設定されていない、または PEM として解釈できない(フェイルクローズ)
- REQ-A06: 検証に成功した JWT の `sub` をログのトレース情報(ユーザー ID)として付与する
- REQ-A07: dev / stg / local は同一の公開鍵・同一のコードで検証する。環境ごとの差は公開鍵の設定値のみとする

## 非機能要件

- NFR-A01: トークンの値・公開鍵の値をログに出力しない
- NFR-A02: 公開鍵はリポジトリに含めない(公開鍵自体は秘密情報ではないが、環境ごとの運用値として扱う)。dev / stg は環境変数、local は `.gitignore` 対象の `application-local.yaml` で与える(`docs/steering/tech.md` 2 節)

## 受け入れ基準

- [ ] 有効な JWT 付きのリクエストが受け付けられる
- [ ] REQ-A05 の各ケースで 401 と `{ "message", "requestId" }` が返る
- [ ] 公開鍵が未設定でもアプリケーションは起動し、全 API が 401 を返す
- [ ] 上記を権限あり/なしの双方のケースで自動テストしている
