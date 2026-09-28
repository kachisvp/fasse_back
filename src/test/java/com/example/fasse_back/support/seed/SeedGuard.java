package com.example.fasse_back.support.seed;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code seedLocal} の実行可否の判定(docs/specs/local-seed-data/design.md 3 節)。
 * 全件削除を伴うため、明示的な確認と、接続先がローカルであることを確認する。
 */
public final class SeedGuard {

    /** 確認の指定が無い場合に表示するメッセージ */
    public static final String CONFIRM_REQUIRED_MESSAGE = "seedLocal deletes all data in the local database and "
            + "loads src/test/resources/testdata/. Run with -Pconfirm=yes to proceed.";

    private static final String CONFIRM_VALUE = "yes";
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "::1");

    /** {@code jdbc:mysql://<ホスト>[:<ポート>]/<DB 名>[?<パラメータ>]}。IPv6 のホストは {@code [::1]} の形式 */
    private static final Pattern MYSQL_URL =
            Pattern.compile("jdbc:mysql://(\\[[^\\]]+\\]|[^:/?\\[\\]]+)(?::\\d+)?/([^?/]*)(?:\\?.*)?");

    private SeedGuard() {
    }

    /** 引数(Gradle プロパティ {@code confirm})で実行が確認されているか */
    public static boolean isConfirmed(String[] args) {
        return args != null && args.length > 0 && CONFIRM_VALUE.equals(args[0]);
    }

    /**
     * 接続先がローカルの DB であれば、その接続先を返す
     *
     * @param jdbcUrl {@code spring.datasource.url}
     * @return ローカルでない、または解析できない場合は空
     */
    public static Optional<Target> localTarget(String jdbcUrl) {
        return parse(jdbcUrl).filter(target -> LOCAL_HOSTS.contains(target.host().toLowerCase(Locale.ROOT)));
    }

    /**
     * JDBC URL からホストと DB 名を取り出す(ユーザー名・パスワード・パラメータは含めない)
     *
     * @return 解析できない場合は空
     */
    public static Optional<Target> parse(String jdbcUrl) {
        if (jdbcUrl == null) {
            return Optional.empty();
        }
        Matcher matcher = MYSQL_URL.matcher(jdbcUrl);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String host = matcher.group(1);
        if (host.startsWith("[")) {
            host = host.substring(1, host.length() - 1);
        }
        return Optional.of(new Target(host, matcher.group(2)));
    }

    /**
     * 投入先の DB
     *
     * @param host     ホスト名
     * @param database データベース名
     */
    public record Target(String host, String database) {
    }
}
