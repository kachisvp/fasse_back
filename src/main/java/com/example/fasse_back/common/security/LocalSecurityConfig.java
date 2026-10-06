package com.example.fasse_back.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * local プロファイル用の Spring Security の構成(docs/specs/authentication/design.md 3.2 節)。
 *
 * <ul>
 * <li>認証を行わず、全リクエストを許可する({@code Authorization} ヘッダは参照しない)</li>
 * <li>CORS は local 以外と同じ設定を適用する</li>
 * <li>MDC の {@code userId} は設定しない</li>
 * </ul>
 *
 * 認証の無効化は local プロファイルに限り、プロパティでは切り替えない(REQ-A09)。
 */
@Configuration
@Profile("local")
public class LocalSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(LocalSecurityConfig.class);

    @Bean
    SecurityFilterChain localSecurityFilterChain(HttpSecurity http) throws Exception {
        log.warn("Authentication is DISABLED (local profile). Never use this profile outside a developer PC.");
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
