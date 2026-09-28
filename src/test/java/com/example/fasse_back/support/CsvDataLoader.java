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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@code src/test/resources/testdata/<テーブル名>.csv} の投入処理(docs/specs/local-seed-data/design.md 2 節)。
 * 全テーブルを空にした後、FK の依存順に CSV を投入する。
 *
 * <p>CSV は UTF-8・LF・1 行目がヘッダ(列名)。空欄は NULL として扱う。値にカンマは含めない。
 *
 * <p>接続先の判定は行わない。呼び出し側({@link TestDataLoader}、{@code LocalSeedRunner})で確認すること。
 * {@code TRUNCATE} ではなく {@code DELETE} で空にするため、トランザクション内で呼べばロールバックできる。
 */
public final class CsvDataLoader {

    /** FK の依存順(マスタ → ヘッダ → 明細) */
    public static final List<String> TABLES = List.of(
            "m_item", "m_supplier", "m_menu", "m_tax_rate", "t_slip_no_counter",
            "t_purchase_header", "t_purchase_detail", "t_sales_header", "t_sales_detail");

    private static final Pattern IDENTIFIER = Pattern.compile("[a-z_]+");

    private CsvDataLoader() {
    }

    /**
     * 全テーブルを空にして CSV を投入する
     *
     * @return テーブルごとの投入件数(投入順)
     */
    public static Map<String, Integer> load(JdbcTemplate jdbc) {
        clear(jdbc);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String table : TABLES) {
            counts.put(table, insertCsv(jdbc, table));
        }
        return counts;
    }

    /** 全テーブルを空にする(FK の依存の逆順) */
    public static void clear(JdbcTemplate jdbc) {
        for (String table : TABLES.reversed()) {
            jdbc.update("DELETE FROM " + table);
        }
    }

    private static int insertCsv(JdbcTemplate jdbc, String table) {
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
        return rows.size();
    }

    private static List<String> readLines(String resource) {
        try (InputStream in = CsvDataLoader.class.getResourceAsStream(resource)) {
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
