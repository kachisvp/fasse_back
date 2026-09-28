# design.md — 開発用 DB へのテストデータ投入

## 1. 全体像

```
gradlew.bat seedLocal -Pconfirm=yes
  └─ JavaExec: LocalSeedRunner(テストのクラスパスで実行、プロファイル local)
       1. 実行可否の判定(confirm の指定)      … NG なら DB に接続せず終了
       2. Spring コンテキスト起動(DB 関連の自動設定のみ)
       3. 実行可否の判定(接続先ホスト)         … NG ならエラー終了
       4. Flyway マイグレーション適用
       5. 1 トランザクションで全テーブル削除 → CSV 投入(CsvDataLoader)
       6. テーブルごとの投入件数を INFO で出力
```

- 実行クラスと CSV 投入処理はテストのソースセット(`src/test/`)に置き、アプリケーションの jar には含めない
- 接続先は `application.yaml` + `application-local.yaml` から読む(アプリケーションのローカル起動と同じ接続先)

## 2. クラス構成

| クラス | 場所 | 役割 |
|---|---|---|
| `CsvDataLoader` | `src/test/java/.../support/` | CSV 投入の本体。全テーブルの削除と、FK の依存順での CSV 投入を行う。接続先の判定は行わない |
| `TestDataLoader` | 同上(既存) | 自動テスト用。データベース名が `_test` で終わることを確認してから `CsvDataLoader` を呼ぶ |
| `LocalSeedRunner` | `src/test/java/.../support/seed/` | `seedLocal` の実行クラス(`main` メソッド)。1 節の手順を行う |
| `SeedGuard` | 同上 | 実行可否の判定(`confirm` の指定、接続先ホスト)。単体テストの対象 |

- 既存の `TestDataLoader` の CSV 読み込み・投入処理を `CsvDataLoader` に移し、`TestDataLoader` は `_test` の確認後に `CsvDataLoader` を呼ぶだけにする。自動テストからの使い方(`TestDataLoader.load(jdbc)`)は変えない
- 投入対象のテーブルと順序は `CsvDataLoader.TABLES`(`docs/specs/purchase-sales/design.md` 10.2 節の 9 テーブル)とする

### 2.1 Spring コンテキスト

`LocalSeedRunner` は、アプリケーション全体(Web・Security・MyBatis)は起動せず、DB 関連の自動設定のみを有効にした最小構成で起動する。

- 有効にする自動設定: `DataSourceAutoConfiguration`、`DataSourceTransactionManagerAutoConfiguration`、`JdbcTemplateAutoConfiguration`、`FlywayAutoConfiguration`
- `spring.main.web-application-type=none`
- プロファイルは `local` に固定する(Gradle タスクで `spring.profiles.active=local` を指定する)
- Flyway の自動設定により、起動時に未適用のマイグレーションが適用される

## 3. 実行可否の判定(SeedGuard)

| 判定 | タイミング | 条件 | NG の場合 |
|---|---|---|---|
| 確認 | DB 接続前 | 引数(Gradle プロパティ `confirm`)が `yes` | 下記のメッセージを表示し、終了コード 1 で終了 |
| 接続先 | Spring コンテキスト起動後、データ変更前 | `spring.datasource.url` のホストが `localhost` / `127.0.0.1` / `::1`(`[::1]`) | `seed is allowed only for local database: host=<ホスト>` を ERROR で出力し、終了コード 1 で終了 |

- 確認 NG 時のメッセージ: `seedLocal deletes all data in the local database and loads src/test/resources/testdata/. Run with -Pconfirm=yes to proceed.`
- 接続先の判定は JDBC URL(`jdbc:mysql://<ホスト>[:<ポート>]/<DB 名>?...`)からホストを取り出して行う。解析できない URL は NG とする
- ログに出すのは接続先のホスト名と DB 名のみとし、ユーザー名・パスワード・URL のパラメータは出力しない

### 3.1 Flyway の適用タイミング

Flyway のマイグレーションは Spring コンテキストの起動時(接続先の判定より前)に適用される。マイグレーションはテーブル定義の作成のみでデータを変更しないため、判定前に適用されても 3 節の「データを変更せずにエラー終了する」を満たすものとする。

## 4. データ投入

- 1 トランザクション(`TransactionTemplate`)内で、`CsvDataLoader.TABLES` の逆順に `DELETE FROM <テーブル>` → 正順に CSV を投入する
- 途中で失敗した場合はロールバックし、例外のスタックトレースを ERROR で出力して終了コード 1 で終了する
- `DELETE` を使う(`TRUNCATE` は暗黙のコミットを伴い、ロールバックできないため)。`AUTO_INCREMENT` の値はリセットされないが、CSV の id は明示指定で投入され、以降の採番は既存の最大値より大きい値になるため、id は重複しない
- 伝票番号の採番カウンタ(`t_slip_no_counter`)は、テストデータのヘッダの伝票番号と整合する値で投入されるため、投入後の伝票登録で番号は重複しない
- 完了時にテーブルごとの投入件数を INFO で出力する(例: `m_item: 5 rows`)

## 5. Gradle タスク

```groovy
tasks.register('seedLocal', JavaExec) {
	group = 'application'
	description = 'ローカル DB(fasse)の全データを削除し、src/test/resources/testdata/ を投入する'
	classpath = sourceSets.test.runtimeClasspath
	mainClass = 'com.example.fasse_back.support.seed.LocalSeedRunner'
	systemProperty 'spring.profiles.active', 'local'
	args(project.findProperty('confirm') ?: '')
}
```

- 実行例: `gradlew.bat seedLocal -Pconfirm=yes`
- `SPRING_PROFILES_ACTIVE` 環境変数の値に関わらず、`local` プロファイルで実行する

## 6. テスト設計

| 対象 | 方式 | 内容 |
|---|---|---|
| `SeedGuard` | JUnit 5 単体テスト | `confirm` が `yes` / 未指定 / その他の値、接続先が `localhost` / `127.0.0.1` / `[::1]` / ポート有無 / リモートホスト / 解析できない URL |
| `CsvDataLoader` / `TestDataLoader` | 既存の Mapper テスト・結合テスト | 既存テストが `TestDataLoader.load(jdbc)` 経由で `CsvDataLoader` を使うため、分割後も全テストが成功することで確認する |
| `LocalSeedRunner` | 手動確認 | `seedLocal` を `confirm` なし・ありで実行し、`fasse` の件数と、投入後の伝票登録で伝票番号が重複しないことを確認する |

- `LocalSeedRunner` はテストのソースセットに置くため、JaCoCo のカバレッジ計測の対象外である
