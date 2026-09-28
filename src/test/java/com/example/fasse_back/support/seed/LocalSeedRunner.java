package com.example.fasse_back.support.seed;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.fasse_back.support.CsvDataLoader;

/**
 * 開発用 DB(local プロファイルの接続先)へのテストデータ投入(docs/specs/local-seed-data/design.md)。
 * Gradle タスク {@code seedLocal} から実行する({@code gradlew.bat seedLocal -Pconfirm=yes})。
 *
 * <ol>
 * <li>確認({@code confirm=yes})が無ければ、DB に接続せずに終了する</li>
 * <li>DB 関連の自動設定のみで Spring を起動する(起動時に Flyway のマイグレーションが適用される)</li>
 * <li>接続先がローカルでなければ、データを変更せずに終了する</li>
 * <li>1 トランザクションで全テーブルを削除し、CSV を投入する</li>
 * </ol>
 *
 * <p>アプリケーションのコンポーネントスキャンに含まれないよう、{@code @Configuration} は付けない。
 */
@ImportAutoConfiguration({ DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class, FlywayAutoConfiguration.class })
public class LocalSeedRunner {

    private static final Logger log = LoggerFactory.getLogger(LocalSeedRunner.class);

    public static void main(String[] args) {
        if (!SeedGuard.isConfirmed(args)) {
            log.warn(SeedGuard.CONFIRM_REQUIRED_MESSAGE);
            System.exit(1);
        }
        System.exit(run());
    }

    /** @return 終了コード(成功 0、失敗 1) */
    private static int run() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(LocalSeedRunner.class)
                .web(WebApplicationType.NONE)
                .logStartupInfo(false)
                .run()) {
            return seed(context);
        } catch (RuntimeException e) {
            log.error("seed failed. The local database was not changed.", e);
            return 1;
        }
    }

    private static int seed(ConfigurableApplicationContext context) {
        String url = context.getEnvironment().getProperty("spring.datasource.url");
        Optional<SeedGuard.Target> target = SeedGuard.localTarget(url);
        if (target.isEmpty()) {
            String host = SeedGuard.parse(url).map(SeedGuard.Target::host).orElse("unknown");
            log.error("seed is allowed only for local database: host={}", host);
            return 1;
        }
        log.info("seeding local database: host={}, database={}", target.get().host(), target.get().database());

        JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
        TransactionTemplate transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        Map<String, Integer> counts = transaction.execute(status -> CsvDataLoader.load(jdbc));

        counts.forEach((table, count) -> log.info("{}: {} rows", table, count));
        log.info("seed completed");
        return 0;
    }
}
