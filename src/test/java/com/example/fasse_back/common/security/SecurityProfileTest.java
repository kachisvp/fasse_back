package com.example.fasse_back.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * プロファイルによる Spring Security の構成の切り替え(docs/specs/authentication/design.md 3 節、REQ-A09)。
 * 構成クラスの登録時点で {@code @Profile} が評価されるため、コンテキストの起動(refresh)は行わない。
 * プロファイル未指定の場合は環境変数 {@code SPRING_PROFILES_ACTIVE}(開発者 PC では {@code local})に依存するため対象外とする。
 */
class SecurityProfileTest {

    static Stream<Arguments> profiles() {
        return Stream.of(
                Arguments.of((Object) new String[] { "test" }, false),
                Arguments.of((Object) new String[] { "aws" }, false),
                Arguments.of((Object) new String[] { "local" }, true));
    }

    @ParameterizedTest
    @MethodSource("profiles")
    void securityConfig_isSwitchedByProfile(String[] activeProfiles, boolean authDisabled) {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles(activeProfiles);
            context.register(SecurityConfig.class, JwtDecoderConfig.class, LocalSecurityConfig.class);

            assertThat(context.containsBeanDefinition("localSecurityConfig")).isEqualTo(authDisabled);
            assertThat(context.containsBeanDefinition("securityConfig")).isEqualTo(!authDisabled);
            assertThat(context.containsBeanDefinition("jwtDecoderConfig")).isEqualTo(!authDisabled);
        }
    }
}
