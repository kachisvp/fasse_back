# tasks.md — 開発用 DB へのテストデータ投入

凡例: `[x]` 完了 / `[ ]` 未完了。上から順に着手する(各グループは前のグループに依存する)。

## 仕様の承認

- [ ] requirements.md / design.md のレビュー・承認
- [ ] `docs/steering/` の更新内容(tech.md のコマンド、structure.md のテストデータ)のレビュー・承認

## 1. 投入処理の分割

- [ ] T-101: `TestDataLoader` の CSV 読み込み・削除・投入処理を `CsvDataLoader` に移す。`TestDataLoader` は `_test` の確認後に `CsvDataLoader` を呼ぶだけにする(design.md 2 節)

### テスト

- [ ] T-151: 既存の全テストが変更なしで成功することを確認する(`gradlew.bat test`)

## 2. seedLocal

- [ ] T-201: `SeedGuard`(`confirm` の判定、JDBC URL からのホスト判定)を実装する(design.md 3 節)
- [ ] T-202: `LocalSeedRunner`(最小構成の Spring コンテキスト、1 トランザクションでの削除・投入、件数のログ出力)を実装する(design.md 2.1 節・4 節)
- [ ] T-203: `build.gradle` に `seedLocal` タスクを追加する(design.md 5 節)

### テスト

- [ ] T-251: `SeedGuard` の単体テスト(design.md 6 節)

## 3. 動作確認・ドキュメント

- [ ] T-301: `seedLocal` を `confirm` なしで実行し、DB に接続せずに終了することを確認する
- [ ] T-302: `seedLocal -Pconfirm=yes` を実行し、`fasse` の各テーブルの件数が CSV と一致することを確認する
- [ ] T-303: 投入後にアプリケーションを起動し、伝票(仕入・売上)とマスタを登録して、伝票番号・id が重複しないことを確認する
- [ ] T-304: `README.md` に `seedLocal` の実行方法と注意事項(全件削除されること)を追記する
- [ ] T-305: `gradlew.bat build` が成功することを確認する
