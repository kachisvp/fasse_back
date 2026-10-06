# tasks.md — WebAPI 受口の JWT 検証

前提: `docs/specs/purchase-sales/tasks.md` の「プロジェクト基盤」が完了していること(依存関係・共通エラー応答・リクエスト ID)。

## 実装

- [x] TASK-A01: `spring-boot-starter-security` / `spring-boot-starter-oauth2-resource-server` を導入する
- [x] TASK-A02: `JwtDecoderConfig` を実装する(PEM → `RSAPublicKey`、RS256 限定、clock skew 0、公開鍵未設定・不正時は常に失敗する Decoder と WARN ログ)
- [x] TASK-A03: `SecurityConfig` を実装する(STATELESS、CSRF 無効、CORS preflight 許可、全エンドポイント認証必須)
- [x] TASK-A04: 401 応答用の `AuthenticationEntryPoint` / `AccessDeniedHandler` を実装する(`{ "message", "requestId" }`、`WWW-Authenticate` ヘッダ、WARN ログ)
- [x] TASK-A05: 認証成功後に MDC `userId` へ `sub` を設定する
- [x] TASK-A06: `SecurityConfig` / `JwtDecoderConfig` を `@Profile("!local")` とし、`LocalSecurityConfig`(`@Profile("local")`、全リクエスト許可、CORS 適用、WARN ログ)を実装する(design.md 3.2 節)

## テスト

- [x] TASK-A11: テスト用 `JwtTestSupport`(鍵ペア生成・JWT 署名)を作成する
- [x] TASK-A12: `JwtDecoderConfig` の単体テスト(正常、公開鍵未設定、PEM 不正)
- [x] TASK-A13: `@WebMvcTest` で design.md 6 節の各ケースを検証する(権限あり/なしの双方)
- [x] TASK-A14: `LocalSecurityConfig` の API テスト(ヘッダ欠落・不正な JWT で 2xx、CORS の許可・拒否)。TASK-A06 に依存
- [x] TASK-A15: プロファイルによる構成の切り替えのテスト(`local` / `test` / `aws`)。TASK-A06 に依存

## 動作確認

- [x] TASK-A22: local プロファイルで起動し、`Authorization` ヘッダ無しで Postman からアクセスできること、起動時に WARN ログが出ることを確認する
- [x] TASK-A23: `README.md` の `application-local.yaml` の記載例と「JWT の取得と API の呼び出し」を、認証なしの運用に合わせて更新する
