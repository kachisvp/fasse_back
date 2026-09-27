package com.example.fasse_back.common.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.fasse_back.common.web.RequestIdFilter;

/**
 * CORS の設定(design.md 8.2 節)。許可するオリジンは {@code fasse.cors.allowed-origins} で指定する。
 */
@Configuration
public class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource(FasseProperties properties) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(properties.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setExposedHeaders(List.of(RequestIdFilter.REQUEST_ID_HEADER));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
