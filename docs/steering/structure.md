# structure.md — ディレクトリ構成・コーディング規約

## 1. ディレクトリ構成

```
fasse_back/
├── build.gradle
├── docs/
│   ├── steering/                         # product / tech / structure
│   └── specs/<feature>/                  # requirements / design / tasks (+ openapi.yaml)
└── src/
    ├── main/
    │   ├── java/com/example/fasse_back/
    │   │   ├── FasseBackApplication.java
    │   │   ├── common/                   # cross-cutting concerns
    │   │   │   ├── config/               # Jackson, CORS, etc.
    │   │   │   ├── security/             # JWT verification, 401 handling
    │   │   │   ├── web/                  # request ID filter, error handling
    │   │   │   └── exception/            # business exceptions
    │   │   └── <feature>/                # item, supplier, menu, taxrate, purchase, sales
    │   │       ├── controller/           # REST endpoints
    │   │       ├── service/              # business logic, transactions
    │   │       ├── repository/           # MyBatis mapper interfaces
    │   │       ├── dto/                  # request / response DTOs
    │   │       └── entity/               # DB row objects
    │   └── resources/
    │       ├── application.yaml              # common settings (env var references only, committed)
    │       ├── application-<profile>.yaml    # local / test only, git-ignored (see README.md)
    │       ├── logback-spring.xml
    │       ├── db/migration/             # Flyway migrations (V<n>__<desc>.sql)
    │       └── com/example/fasse_back/<feature>/repository/  # MyBatis mapper XML
    └── test/
        ├── java/com/example/fasse_back/  # mirrors main
        └── resources/
            ├── testdata/<table>.csv      # test data per table
            ├── FoodCompositionTable.csv  # reserved for future food composition feature
            └── ddl/m_food_composition.sql
```

## 2. ドキュメントの置き場所

| 種類 | 場所 |
|---|---|
| プロジェクト共通の前提 | `docs/steering/` |
| 機能仕様 | `docs/specs/<feature>/`(`<feature>` は英語のケバブケース) |
| API 仕様 | `docs/specs/<feature>/openapi.yaml` |
| セットアップ手順 | `README.md` |

## 3. 命名規約

| 対象 | 規約 | 例 |
|---|---|---|
| パッケージ | 小文字、機能名 | `taxrate`, `purchase` |
| Controller / Service / Mapper | `<Entity>Controller` / `<Entity>Service` / `<Entity>Mapper` | `ItemController` |
| リクエスト DTO | `<Entity>Request` | `ItemRequest` |
| レスポンス DTO | `<Entity>Response` | `ItemResponse` |
| Entity | テーブルに対応する名詞 | `Item`, `PurchaseHeader` |
| テーブル | マスタは `m_`、トランザクションは `t_` | `m_item`, `t_purchase_header` |
| JSON 項目名 | スネークケース(Jackson の `SNAKE_CASE` を全体に適用) | `item_name` |
| テストデータ | `src/test/resources/testdata/<テーブル名>.csv` | `testdata/m_item.csv` |

## 4. コーディング規約

- **レイヤー分離**: Controller は入出力の変換と検証のみ、Service は業務ロジックとトランザクション(`@Transactional`)、Repository(MyBatis Mapper)は SQL のみを担う
- **DTO と Entity の分離**: Entity を API レスポンスに直接返さない。変換は Service 層で行う
- **入力検証**: リクエスト DTO に Bean Validation のアノテーションを付け、Controller で `@Valid` を付けて検証する
- **SQL**: Mapper XML に記述し、パラメータは `#{}` のみを使う
- **例外**: 業務上の想定内エラー(400/404/409)は `common/exception` の例外を投げ、`@RestControllerAdvice` でレスポンスに変換する。空の `catch` を書かない
- **ログ**: SLF4J の `Logger` を使う。`System.out.println` を使わない。トークン・パスワード・リクエストボディ全体を出力しない
- **コメント・ドキュメント**: 日本語で書く。識別子・ファイル名は英語で書く

## 5. テストデータ

- 各テーブルのテストデータは、`src/test/resources/testdata/<テーブル名>.csv` に **5 件程度** 用意する
- CSV の形式
  - 文字コード UTF-8、改行 LF、1 行目はヘッダ(列名はテーブルの列名と同じスネークケース)
  - 空欄は `NULL` として扱う
  - `created_at` / `updated_at` など DB の既定値に任せる列は省略してよい
- FK の依存順(マスタ → ヘッダ → 明細)に投入する。投入処理はテスト用ユーティリティで共通化する
- 食品成分表のテストデータ `src/test/resources/FoodCompositionTable.csv` と DDL `src/test/resources/ddl/m_food_composition.sql` は今後の拡張用に保持する(現時点ではテストから使用しない)
