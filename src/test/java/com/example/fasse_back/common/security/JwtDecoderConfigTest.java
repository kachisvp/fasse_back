package com.example.fasse_back.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.example.fasse_back.common.config.FasseProperties;
import com.example.fasse_back.support.JwtTestSupport;

class JwtDecoderConfigTest {

    @Test
    void validKey_decodesValidToken() {
        JwtDecoder decoder = JwtDecoderConfig.createDecoder(JwtTestSupport.publicKeyPem());

        assertThat(decoder.decode(JwtTestSupport.validToken()).getSubject()).isEqualTo(JwtTestSupport.SUBJECT);
    }

    @Test
    void validKeyWithoutLineBreaks_decodesValidToken() {
        String singleLine = JwtTestSupport.publicKeyPem().replace("\n", "");

        assertThat(JwtDecoderConfig.createDecoder(singleLine).decode(JwtTestSupport.validToken())).isNotNull();
    }

    @Test
    void beanMethod_readsPropertyAndHandlesMissingJwtSection() {
        JwtDecoderConfig config = new JwtDecoderConfig();

        assertThat(config.jwtDecoder(new FasseProperties(new FasseProperties.Jwt(JwtTestSupport.publicKeyPem()),
                null)).decode(JwtTestSupport.validToken())).isNotNull();
        JwtDecoder withoutSection = config.jwtDecoder(new FasseProperties(null, null));
        assertThatThrownBy(() -> withoutSection.decode(JwtTestSupport.validToken()))
                .isInstanceOf(JwtException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "  " })
    void missingKey_alwaysRejects(String pem) {
        JwtDecoder decoder = JwtDecoderConfig.createDecoder(pem);

        assertThatThrownBy(() -> decoder.decode(JwtTestSupport.validToken())).isInstanceOf(JwtException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "-----BEGIN PUBLIC KEY-----\nnot base64!\n-----END PUBLIC KEY-----",
            "-----BEGIN PUBLIC KEY-----\nAAAA\n-----END PUBLIC KEY-----",
    })
    void invalidKey_alwaysRejects(String pem) {
        JwtDecoder decoder = JwtDecoderConfig.createDecoder(pem);

        assertThatThrownBy(() -> decoder.decode(JwtTestSupport.validToken())).isInstanceOf(JwtException.class);
    }

    @Test
    void expiredToken_isRejected() {
        JwtDecoder decoder = JwtDecoderConfig.createDecoder(JwtTestSupport.publicKeyPem());

        assertThatThrownBy(() -> decoder.decode(JwtTestSupport.expiredToken())).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenWithoutExp_isRejected() {
        JwtDecoder decoder = JwtDecoderConfig.createDecoder(JwtTestSupport.publicKeyPem());

        assertThatThrownBy(() -> decoder.decode(JwtTestSupport.tokenWithoutExp())).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenSignedByOtherKey_isRejected() {
        JwtDecoder decoder = JwtDecoderConfig.createDecoder(JwtTestSupport.publicKeyPem());

        assertThatThrownBy(() -> decoder.decode(JwtTestSupport.tokenSignedByOtherKey()))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void hs256Token_isRejected() {
        JwtDecoder decoder = JwtDecoderConfig.createDecoder(JwtTestSupport.publicKeyPem());

        assertThatThrownBy(() -> decoder.decode(JwtTestSupport.hs256Token())).isInstanceOf(JwtException.class);
    }
}
