package com.example.fasse_back.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security の構成(docs/specs/authentication/design.md 3 節)。
 *
 * <ul>
 * <li>セッションを使わず、CSRF は無効(Cookie を使わない Bearer 認証のため)</li>
 * <li>CORS の preflight({@code OPTIONS})のみ認証なしで許可し、それ以外は全て認証必須</li>
 * <li>ロール・スコープによる認可は行わない</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JsonAuthenticationEntryPoint entryPoint)
            throws Exception {
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(entryPoint))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(entryPoint))
                .addFilterAfter(new UserIdMdcFilter(), BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
