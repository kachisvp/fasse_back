# Spring Boot - Agent Instructions

ルートの `CLAUDE.md` の方針（言語、セキュリティ、仕様駆動開発）を前提とする。

## 仕様駆動開発

- API・機能の追加修正を行う前に、このフォルダ配下（または対象機能配下）に `requirements.md` / `design.md` / `tasks.md` を作成・更新し、承認を得ること
- `design.md` にはAPI仕様（エンドポイント、リクエスト/レスポンス）、テーブル設計、レイヤー構成を記載すること

## セキュリティ

- Spring Securityを用いて認証・認可を実装し、エンドポイントごとにアクセス制御を明示すること
- パスワードは必ずハッシュ化（BCrypt等）して保存すること
- SQLはJPA/MyBatisのプレースホルダを使用し、文字列連結によるSQL構築（SQLインジェクションの原因）を避けること
- `application.yml` / `application.properties` に機密情報を直接記載せず、環境変数や外部シークレット管理（Vault, AWS Secrets Manager等）を利用すること
- CORS設定は許可するオリジンを必要最小限に限定すること
- 例外ハンドリングでスタックトレースや内部情報をレスポンスに含めないこと
- 依存ライブラリのバージョンを最新の安定版に保ち、既知のCVEに注意すること

## 実装方針

- レイヤー構成（Controller / Service / Repository）の責務を明確に分離すること
- DTOとEntityを分離し、Entityを直接APIレスポンスに露出させないこと
- バリデーションは `@Valid` とBean Validationアノテーションを活用すること
- ログには機密情報（パスワード、トークン等）を出力しないこと

## ログ出力

- `System.out.println` 等の握りつぶされる標準出力デバッグを残さないこと。SLF4J（Logback）等の標準ロガーを使用し、ログレベル（ERROR/WARN/INFO/DEBUG）を適切に使い分けること
- ログはファイルまたはログ収集基盤（CloudWatch Logs等）に出力し、環境ごとに保持期間・ローテーション設定（`logback-spring.xml`等）を行うこと
- MDC（Mapped Diagnostic Context）等を用いて、リクエストID・ユーザーID等のトレース情報をログに付与し、事象追跡を容易にすること
- パスワード、トークン、個人情報等の機密情報はログに出力しないこと（マスキング処理を検討すること）

## 例外処理

- 例外を空の`catch`ブロックで握りつぶさないこと。捕捉した例外は必ずログ出力し、必要に応じて呼び出し元に伝播させること
- `@RestControllerAdvice` / `@ExceptionHandler` を用いて共通の例外ハンドリングを実装し、未処理例外によるスタックトレースの直接露出を防ぐこと
- クライアントへのエラーレスポンスは、事象が把握できる分かりやすいメッセージ・エラーコードを含め、内部の技術詳細（スタックトレース、SQL文等）は含めないこと
- サーバー内部の詳細（スタックトレース、発生箇所、原因例外）はログファイル・ログ収集基盤に記録し、事後調査できるようにすること
- ビジネス例外（バリデーションエラー等）とシステム例外（DB接続エラー等）を区別し、それぞれ適切なHTTPステータスコードとログレベルを割り当てること

## テスト自動化

- Service層・ドメインロジックはJUnit 5によるユニットテストで検証し、外部依存はMockito等でモック化すること
- Repository層は `@DataJpaTest` を用い、実際のクエリ結果を検証すること（H2等のインメモリDB、またはTestcontainersを使用）
- Controller層は `@WebMvcTest` またはMockMvc/RestAssuredを用いたAPIレベルのテストを行い、リクエスト/レスポンス仕様（`design.md`）との整合性を確認すること
- 認証・認可のロジックは、権限あり/なし双方のケースを含めてテストすること
- 結合テストが必要な場合はTestcontainersで実DBに近い環境を構築し、`@SpringBootTest` で検証すること
- CI（GitHub Actions等）で `./gradlew test`（またはMaven）を自動実行し、失敗時はマージをブロックすること
- JaCoCo等でカバレッジを計測し、著しい低下がないか継続的に確認すること
- 新規機能追加・バグ修正時は、対応するテストコードを同一PR内に含めること

## コマンド例

```bash
# テスト実行
./gradlew test

# ビルド（テストを含む）
./gradlew build

# ビルド（テストをスキップ）
./gradlew build -x test

# アプリケーション起動
./gradlew bootRun

# 静的解析（Checkstyle等を導入している場合）
./gradlew check

# カバレッジレポート生成（JaCoCoを導入している場合）
./gradlew jacocoTestReport
```

Mavenを使用する場合は以下を参考にすること。

```bash
mvn test
mvn package
mvn spring-boot:run
```
