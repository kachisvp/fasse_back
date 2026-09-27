package com.example.fasse_back.support;

import java.util.Map;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

/** テスト用公開鍵を {@code fasse.jwt.public-key-pem} に設定する */
public class JwtTestKeyInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("jwtTestKey",
                Map.of("fasse.jwt.public-key-pem", JwtTestSupport.publicKeyPem())));
    }
}
