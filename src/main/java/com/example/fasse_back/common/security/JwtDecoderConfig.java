package com.example.fasse_back.common.security;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.example.fasse_back.common.config.FasseProperties;

/**
 * JWT の検証器(docs/specs/authentication/design.md 3 節)。
 *
 * <ul>
 * <li>KMS 公開鍵(PEM)を起動時に読み込み、RS256 の署名のみを受け付ける</li>
 * <li>{@code exp} は必須とし、時刻の許容誤差は 0 秒とする。{@code iss} / {@code aud} は検証しない</li>
 * <li>公開鍵が未設定・不正な場合は起動を止めず、常に検証失敗とする(フェイルクローズ)</li>
 * </ul>
 *
 * local プロファイルでは JWT を検証しないため、本構成は読み込まない(design.md 3.2 節)。
 */
@Configuration
@Profile("!local")
public class JwtDecoderConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtDecoderConfig.class);

    @Bean
    JwtDecoder jwtDecoder(FasseProperties properties) {
        return createDecoder(properties.jwt() == null ? null : properties.jwt().publicKeyPem());
    }

    static JwtDecoder createDecoder(String publicKeyPem) {
        if (publicKeyPem == null || publicKeyPem.isBlank()) {
            log.warn("JWT public key is not configured. All requests will be rejected with 401.");
            return rejectingDecoder("JWT public key is not configured");
        }
        RSAPublicKey publicKey;
        try {
            publicKey = parsePublicKey(publicKeyPem);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            // 公開鍵の値はログに出さない
            log.warn("JWT public key is invalid ({}). All requests will be rejected with 401.",
                    e.getClass().getSimpleName());
            return rejectingDecoder("JWT public key is invalid");
        }
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey)
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO),
                new JwtClaimValidator<Instant>(JwtClaimNames.EXP, Objects::nonNull)));
        return decoder;
    }

    /** PEM(X.509 SubjectPublicKeyInfo)の RSA 公開鍵を読み込む */
    static RSAPublicKey parsePublicKey(String pem) throws GeneralSecurityException {
        String base64 = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(base64);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
    }

    private static JwtDecoder rejectingDecoder(String reason) {
        return token -> {
            throw new BadJwtException(reason);
        };
    }
}
