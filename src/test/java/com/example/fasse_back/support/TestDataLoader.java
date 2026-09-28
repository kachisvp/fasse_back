package com.example.fasse_back.support;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 自動テスト用のテストデータ投入(docs/specs/purchase-sales/design.md 10.2 節)。
 * 投入処理は {@link CsvDataLoader} に委ねる。
 *
 * <p>誤って開発用 DB のデータを消さないよう、データベース名が {@code _test} で終わる場合のみ実行する。
 * テストのトランザクション内で呼べば、テスト終了時にロールバックされる。
 */
public final class TestDataLoader {

    private TestDataLoader() {
    }

    /** 全テーブルを空にしてテストデータを投入する */
    public static void load(JdbcTemplate jdbc) {
        requireTestDatabase(jdbc);
        CsvDataLoader.load(jdbc);
    }

    /** 全テーブルを空にする */
    public static void clear(JdbcTemplate jdbc) {
        requireTestDatabase(jdbc);
        CsvDataLoader.clear(jdbc);
    }

    private static void requireTestDatabase(JdbcTemplate jdbc) {
        String database = jdbc.queryForObject("SELECT DATABASE()", String.class);
        if (database == null || !database.endsWith("_test")) {
            throw new IllegalStateException("test data can only be loaded into a *_test database: " + database);
        }
    }
}
