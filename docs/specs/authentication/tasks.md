# tasks.md — WebAPI 受口の JWT 検証

前提: `docs/specs/purchase-sales/tasks.md` の「プロジェクト基盤」が完了していること(依存関係・共通エラー応答・リクエスト ID)。

## 実装

- [x] TASK-A01: `spring-boot-starter-security` / `spring-boot-starter-oauth2-resource-server` を導入する
- [x] TASK-A02: `JwtDecoderConfig` を実装する(PEM → `RSAPublicKey`、RS256 限定、clock skew 0、公開鍵未設定・不正時は常に失敗する Decoder と WARN ログ)
- [x] TASK-A03: `SecurityConfig` を実装する(STATELESS、CSRF 無効、CORS preflight 許可、全エンドポイント認証必須)
- [x] TASK-A04: 401 応答用の `AuthenticationEntryPoint` / `AccessDeniedHandler` を実装する(`{ "message", "requestId" }`、`WWW-Authenticate` ヘッダ、WARN ログ)
- [x] TASK-A05: 認証成功後に MDC `userId` へ `sub` を設定する

## テスト

- [x] TASK-A11: テスト用 `JwtTestSupport`(鍵ペア生成・JWT 署名)を作成する
- [x] TASK-A12: `JwtDecoderConfig` の単体テスト(正常、公開鍵未設定、PEM 不正)
- [x] TASK-A13: `@WebMvcTest` で design.md 6 節の各ケースを検証する(権限あり/なしの双方)

## 動作確認

- [x] TASK-A21: `fasse_infra` の stg 環境で取得した JWT と `jwt_public_key.pem` を使い、ローカル起動した API に Postman でアクセスできることを確認する
