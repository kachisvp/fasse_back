package com.example.fasse_back.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

/**
 * Mapper の DB テスト用アノテーション({@code @MybatisTest} + テスト用 DB {@code fasse_test})。
 * スキーマは Flyway で作成し、各テストはトランザクション内で実行されテスト終了時にロールバックされる。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@ActiveProfiles("test")
public @interface MapperTest {
}
