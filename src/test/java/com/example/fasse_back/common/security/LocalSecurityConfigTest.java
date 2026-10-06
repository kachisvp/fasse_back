package com.example.fasse_back.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.fasse_back.common.config.CorsConfig;
import com.example.fasse_back.common.config.FasseProperties;
import com.example.fasse_back.common.config.JacksonConfig;
import com.example.fasse_back.common.web.RequestIdFilter;
import com.example.fasse_back.item.controller.ItemController;
import com.example.fasse_back.item.service.ItemService;
import com.example.fasse_back.support.JwtTestSupport;

/**
 * local プロファイルの API テスト(docs/specs/authentication/design.md 6 節)。
 * 認証なしで受け付けること、CORS は local 以外と同じく適用されることを検証する。
 * application-local.yaml は CI に存在しないため、必要なプロパティはここで指定する。
 */
@WebMvcTest(ItemController.class)
@ActiveProfiles("local")
@TestPropertySource(properties = "fasse.cors.allowed-origins=http://localhost:5000")
@EnableConfigurationProperties(FasseProperties.class)
@Import({ SecurityConfig.class, JwtDecoderConfig.class, LocalSecurityConfig.class, JsonAuthenticationEntryPoint.class,
        JacksonConfig.class, CorsConfig.class })
class LocalSecurityConfigTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationContext context;

    @MockitoBean
    ItemService itemService;

    @Test
    void onlyLocalSecurityConfig_isRegistered() {
        assertThat(context.getBeansOfType(LocalSecurityConfig.class)).hasSize(1);
        assertThat(context.getBeansOfType(SecurityConfig.class)).isEmpty();
        assertThat(context.getBeansOfType(JwtDecoder.class)).isEmpty();
    }

    @Test
    void missingHeader_isAccepted() throws Exception {
        given(itemService.findAll()).willReturn(List.of());

        mvc.perform(get("/items"))
                .andExpect(status().isOk());
        then(itemService).should().findAll();
    }

    static List<String> authorizationHeaders() {
        return List.of(
                "Bearer not-a-jwt",
                "Bearer " + JwtTestSupport.expiredToken(),
                "Basic dXNlcjpwYXNz",
                JwtTestSupport.bearer());
    }

    @ParameterizedTest
    @MethodSource("authorizationHeaders")
    void anyAuthorizationHeader_isAccepted(String authorization) throws Exception {
        given(itemService.findAll()).willReturn(List.of());

        mvc.perform(get("/items").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isOk());
    }

    @Test
    void userId_isNotSetInMdc() throws Exception {
        AtomicReference<String> userId = new AtomicReference<>("not-called");
        given(itemService.findAll()).willAnswer(invocation -> {
            userId.set(MDC.get(RequestIdFilter.MDC_USER_ID));
            return List.of();
        });

        mvc.perform(get("/items").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk());
        assertThat(userId.get()).isNull();
    }

    @Test
    void corsPreflight_fromAllowedOrigin_isAllowed() throws Exception {
        mvc.perform(options("/items")
                .header(HttpHeaders.ORIGIN, "http://localhost:5000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5000"));
    }

    @Test
    void corsPreflight_fromUnknownOrigin_isRejected() throws Exception {
        mvc.perform(options("/items")
                .header(HttpHeaders.ORIGIN, "http://evil.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void corsActualRequest_fromUnknownOrigin_isRejected() throws Exception {
        mvc.perform(get("/items").header(HttpHeaders.ORIGIN, "http://evil.example.com"))
                .andExpect(status().isForbidden());
        then(itemService).shouldHaveNoInteractions();
    }
}
