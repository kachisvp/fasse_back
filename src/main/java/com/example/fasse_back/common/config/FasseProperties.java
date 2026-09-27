package com.example.fasse_back.common.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * アプリケーション固有の設定値(application*.yaml の {@code fasse.*})。
 *
 * @param jwt  JWT 検証の設定
 * @param cors CORS の設定
 */
@ConfigurationProperties(prefix = "fasse")
public record FasseProperties(Jwt jwt, Cors cors) {

    /**
     * @param publicKeyPem KMS 公開鍵(PEM 文字列)。未設定・不正の場合は全 API が 401 を返す
     */
    public record Jwt(String publicKeyPem) {
    }

    /**
     * @param allowedOrigins CORS で許可するオリジン
     */
    public record Cors(List<String> allowedOrigins) {
    }
}
