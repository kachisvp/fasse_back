package com.example.fasse_back.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.example.fasse_back.common.config.CorsConfig;
import com.example.fasse_back.common.config.FasseProperties;
import com.example.fasse_back.common.config.JacksonConfig;
import com.example.fasse_back.common.security.JsonAuthenticationEntryPoint;
import com.example.fasse_back.common.security.JwtDecoderConfig;
import com.example.fasse_back.common.security.SecurityConfig;

/**
 * Controller の API テスト用アノテーション({@code @WebMvcTest} + Spring Security + JWT 検証)。
 * Service はテストクラス側で {@code @MockitoBean} にする。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WebMvcTest
@ActiveProfiles("test")
@ContextConfiguration(initializers = JwtTestKeyInitializer.class)
@EnableConfigurationProperties(FasseProperties.class)
@Import({ SecurityConfig.class, JwtDecoderConfig.class, JsonAuthenticationEntryPoint.class,
        JacksonConfig.class, CorsConfig.class })
public @interface ApiTest {

    /** テスト対象の Controller */
    @AliasFor(annotation = WebMvcTest.class, attribute = "controllers")
    Class<?>[] value() default {};
}
