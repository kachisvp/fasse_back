# tech.md — 技術スタック・技術方針

## 1. 技術スタック

| 層 | 技術 |
|---|---|
| 言語 | Java 21 |
| フレームワーク | Spring Boot 3.5 系(Spring Web / Spring Security / Bean Validation) |
| 永続化 | MyBatis(`mybatis-spring-boot-starter`)。JPA は使わない |
| DB | Aurora MySQL Serverless v2(MySQL 8.0 互換)。ローカルは MySQL 8.4 |
| スキーマ管理 | Flyway(`src/main/resources/db/migration/`) |
| 認証(JWT 検証) | `spring-boot-starter-oauth2-resource-server`(Nimbus)。KMS 公開鍵(PEM)で RS256 署名を検証 |
| ログ | SLF4J + Logback(`logback-spring.xml`)。AWS 上は構造化ログ(JSON)を標準出力 → CloudWatch Logs |
| ビルド | Gradle(Wrapper) |
| テスト | JUnit 5 / Mockito / Spring Boot Test / MyBatis Test / JaCoCo |
| 実行基盤 | AWS Fargate(コンテナ)。デプロイは別仕様 |

## 2. 環境

| 環境 | 位置づけ | DB | JWT 公開鍵 |
|---|---|---|---|
| local | 開発者 PC(Windows)上での実行 | ローカルの MySQL 8.4、データベース `fasse` | `fasse_infra` の stg と同じ公開鍵 PEM |
| test | 自動テスト(`gradlew.bat test`) | ローカルの MySQL 8.4、データベース `fasse_test` | テスト内で生成した鍵ペア |
| dev / stg | AWS(Fargate + Aurora)。`fasse_infra` の環境名に合わせる | Aurora MySQL Serverless v2 | `fasse_infra` の stg と同じ公開鍵 PEM |

- 環境は Spring Profile(`local` / `test` / `aws`)で切り替える。dev / stg の差分は環境変数で与える
- テストはデータを削除・投入するため、`local` とは別のデータベース(`fasse_test`)を使う
- ローカル(MySQL 8.4)と Aurora(MySQL 8.0 互換)のバージョン差があるため、SQL・DDL は両方で動く構文に限る
- 環境ごとに変わる値・機密情報は `application.yaml` に直接書かず、環境変数で与える

| 環境変数 | 内容 |
|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 接続先 DB。AWS 上は Secrets Manager から ECS タスク定義経由で注入する |
| `TEST_DB_URL` / `TEST_DB_USERNAME` / `TEST_DB_PASSWORD` | テスト用 DB(`fasse_test`) |
| `JWT_PUBLIC_KEY_PEM` | KMS 公開鍵(PEM 文字列)。未設定の場合、全 API が 401 を返す(フェイルクローズ) |
| `CORS_ALLOWED_ORIGINS` | CORS で許可するオリジン(カンマ区切り) |

### ローカル開発での JWT

- 本リポジトリは JWT を発行しない。ローカル開発では `fasse_infra` の stg 環境の `POST /auth/token`(AccessKey 経路)で JWT を取得し、`Authorization: Bearer <JWT>` を付けて呼び出す(有効期限 30 日)
- 公開鍵 PEM は秘密情報ではないため、`fasse_infra/jwt_public_key.pem` の内容を `JWT_PUBLIC_KEY_PEM` に設定してよい
- 自動テストでは、テストコード内で RSA 鍵ペアを生成して JWT を署名する(AWS への接続は不要)

## 3. 技術方針

- API 仕様・入力検証・エラー応答の正本は本リポジトリの `docs/specs/purchase-sales/openapi.yaml` と `design.md` とする。`fasse_infra` の Lambda 実装を基に定めており、置き換えまでの間 Lambda もこの仕様に従う
- JWT は KMS 公開鍵(PEM)を起動時に読み込んで検証し、リクエストの都度 KMS にアクセスしない
- SQL は MyBatis の Mapper XML にプレースホルダ(`#{}`)で記述し、`${}` による文字列埋め込みは使わない
- 依存ライブラリは Spring Boot の依存管理(BOM)に従い、個別にバージョンを固定しない
- セキュリティ、ログ、例外処理、テストのルールは、ルートおよび本リポジトリの `CLAUDE.md` に従う

## 4. 開発環境

- Windows
- JDK 21
- MySQL 8.4(ローカルにインストール)

インストール・初期設定の手順は `README.md` に記載する。

## 5. よく使うコマンド

```bash
gradlew.bat bootRun                           # SPRING_PROFILES_ACTIVE=local
gradlew.bat test                              # unit + integration tests (requires local MySQL)
gradlew.bat build                             # build with tests
gradlew.bat jacocoTestReport                  # coverage report
```

- 動作確認は Postman で行う。`fasse_infra/postman/` のコレクションを、ベース URL を `http://localhost:8080` に変えた環境で利用する

## 6. 開発プロセス

- 仕様駆動開発: 機能ごとに `docs/specs/<feature>/` に `requirements.md` / `design.md` / `tasks.md` を作成し、承認を得てから実装する
- 実装が仕様と食い違う場合は、先に仕様を更新してから実装を直す
- コミット前に `gradlew.bat build` を実行し、テストが失敗する変更はコミットしない
