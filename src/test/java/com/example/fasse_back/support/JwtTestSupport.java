package com.example.fasse_back.support;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * テスト用の JWT を発行する(docs/specs/authentication/design.md 6 節)。
 * テスト実行中は同じ鍵ペアを使い、公開鍵は {@link JwtTestKeyInitializer} がプロパティに設定する。
 */
public final class JwtTestSupport {

    public static final String SUBJECT = "test-user";

    private static final KeyPair KEY_PAIR = generateKeyPair();
    private static final KeyPair OTHER_KEY_PAIR = generateKeyPair();

    private JwtTestSupport() {
    }

    /** テスト用公開鍵(PEM) */
    public static String publicKeyPem() {
        return toPem(KEY_PAIR);
    }

    /** 有効な JWT(1 時間後に期限切れ) */
    public static String validToken() {
        return sign(claims(Instant.now().plus(1, ChronoUnit.HOURS)).build(), (RSAPrivateKey) KEY_PAIR.getPrivate());
    }

    /** {@code Authorization} ヘッダの値 */
    public static String bearer() {
        return "Bearer " + validToken();
    }

    /** 期限切れの JWT */
    public static String expiredToken() {
        return sign(claims(Instant.now().minus(1, ChronoUnit.MINUTES)).build(),
                (RSAPrivateKey) KEY_PAIR.getPrivate());
    }

    /** {@code exp} の無い JWT */
    public static String tokenWithoutExp() {
        return sign(new JWTClaimsSet.Builder().subject(SUBJECT).issueTime(new Date()).build(),
                (RSAPrivateKey) KEY_PAIR.getPrivate());
    }

    /** 別の鍵で署名した JWT(署名不正) */
    public static String tokenSignedByOtherKey() {
        return sign(claims(Instant.now().plus(1, ChronoUnit.HOURS)).build(),
                (RSAPrivateKey) OTHER_KEY_PAIR.getPrivate());
    }

    /** HS256 で署名した JWT({@code alg} 不一致) */
    public static String hs256Token() {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),
                    claims(Instant.now().plus(1, ChronoUnit.HOURS)).build());
            jwt.sign(new MACSigner(new byte[32]));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 公開鍵を PEM 形式にする */
    public static String toPem(KeyPair keyPair) {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(keyPair.getPublic().getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + base64 + "\n-----END PUBLIC KEY-----\n";
    }

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 指定した秘密鍵で RS256 署名した有効な JWT */
    public static String validTokenSignedBy(KeyPair keyPair) {
        return sign(claims(Instant.now().plus(1, ChronoUnit.HOURS)).build(), (RSAPrivateKey) keyPair.getPrivate());
    }

    private static JWTClaimsSet.Builder claims(Instant expiresAt) {
        return new JWTClaimsSet.Builder()
                .subject(SUBJECT)
                .issuer("fasse-test")
                .issueTime(new Date())
                .expirationTime(Date.from(expiresAt));
    }

    private static String sign(JWTClaimsSet claims, RSAPrivateKey privateKey) {
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(), claims);
            jwt.sign(new RSASSASigner(privateKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
