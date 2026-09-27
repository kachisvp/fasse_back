package com.example.fasse_back.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * 結合テスト用アノテーション(アプリケーション全体を起動し、テスト用 DB {@code fasse_test} に接続する)。
 * テストデータは各テストの前に {@link TestDataLoader} で投入する(コミットされる)。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = JwtTestKeyInitializer.class)
public @interface IntegrationTest {
}
