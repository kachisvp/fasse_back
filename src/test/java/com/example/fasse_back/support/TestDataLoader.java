package com.example.fasse_back.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * テストデータの投入(design.md 10.2 節)。
 * 全テーブルを空にした後、{@code src/test/resources/testdata/<テーブル名>.csv} を FK の依存順に投入する。
 *
 * <p>CSV は UTF-8・LF・1 行目がヘッダ(列名)。空欄は NULL として扱う。値にカンマは含めない。
 *
 * <p>誤って開発用 DB のデータを消さないよう、データベース名が {@code _test} で終わる場合のみ実行する。
 * DELETE で空にするため、テストのトランザクション内で呼べばテスト終了時にロールバックされる。
 */
public final class TestDataLoader {

    /** FK の依存順(マスタ → ヘッダ → 明細) */
    public static final List<String> TABLES = List.of(
            "m_item", "m_supplier", "m_menu", "m_tax_rate", "t_slip_no_counter",
            "t_purchase_header", "t_purchase_detail", "t_sales_header", "t_sales_detail");

    private static final Pattern IDENTIFIER = Pattern.compile("[a-z_]+");

    private TestDataLoader() {
    }

    /** 全テーブルを空にしてテストデータを投入する */
    public static void load(JdbcTemplate jdbc) {
        clear(jdbc);
        for (String table : TABLES) {
            insertCsv(jdbc, table);
        }
    }

    /** 全テーブルを空にする */
    public static void clear(JdbcTemplate jdbc) {
        String database = jdbc.queryForObject("SELECT DATABASE()", String.class);
        if (database == null || !database.endsWith("_test")) {
            throw new IllegalStateException("test data can only be loaded into a *_test database: " + database);
        }
        for (String table : TABLES.reversed()) {
            jdbc.update("DELETE FROM " + table);
        }
    }

    private static void insertCsv(JdbcTemplate jdbc, String table) {
        List<String> lines = readLines("/testdata/" + table + ".csv");
        String[] columns = lines.get(0).split(",", -1);
        for (String column : columns) {
            if (!IDENTIFIER.matcher(column).matches()) {
                throw new IllegalStateException("invalid column name in " + table + ".csv: " + column);
            }
        }
        String placeholders = String.join(",", Collections.nCopies(columns.length, "?"));
        String sql = "INSERT INTO " + table + " (" + String.join(",", columns) + ") VALUES (" + placeholders + ")";
        List<Object[]> rows = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            String[] values = line.split(",", -1);
            if (values.length != columns.length) {
                throw new IllegalStateException("column count mismatch in " + table + ".csv: " + line);
            }
            rows.add(Arrays.stream(values).map(v -> v.isEmpty() ? null : v).toArray());
        }
        jdbc.batchUpdate(sql, rows);
    }

    private static List<String> readLines(String resource) {
        try (InputStream in = TestDataLoader.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("test data not found: " + resource);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return reader.lines().filter(line -> !line.isBlank()).toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
