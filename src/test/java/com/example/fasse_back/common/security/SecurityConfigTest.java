package com.example.fasse_back.common.security;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.fasse_back.item.controller.ItemController;
import com.example.fasse_back.item.service.ItemService;
import com.example.fasse_back.support.ApiTest;
import com.example.fasse_back.support.JwtTestSupport;
import com.jayway.jsonpath.JsonPath;

/**
 * JWT 検証の API テスト(docs/specs/authentication/design.md 6 節)。
 * 権限あり(有効な JWT)/なし(ヘッダ欠落・不正な JWT)の双方を検証する。
 */
@ApiTest(ItemController.class)
class SecurityConfigTest {

    private static final String MISSING = "Authorization header is missing or malformed";
    private static final String INVALID = "Invalid or expired token";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ItemService itemService;

    @Test
    void validToken_isAccepted() throws Exception {
        given(itemService.findAll()).willReturn(List.of());

        mvc.perform(get("/items").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk());
        then(itemService).should().findAll();
    }

    @Test
    void lowerCaseBearerScheme_isAccepted() throws Exception {
        mvc.perform(get("/items").header(HttpHeaders.AUTHORIZATION, "bearer " + JwtTestSupport.validToken()))
                .andExpect(status().isOk());
    }

    @Test
    void missingHeader_returns401() throws Exception {
        mvc.perform(get("/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.message").value(MISSING))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
        then(itemService).should(never()).findAll();
    }

    @ParameterizedTest
    @ValueSource(strings = { "Basic dXNlcjpwYXNz", "Token abc", "Bearer" })
    void nonBearerHeader_returns401(String authorization) throws Exception {
        mvc.perform(get("/items").header(HttpHeaders.AUTHORIZATION, authorization))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(MISSING));
        then(itemService).should(never()).findAll();
    }

    static List<String> invalidTokens() {
        return List.of(
                "not-a-jwt",
                "a.b.c",
                JwtTestSupport.tokenSignedByOtherKey(),
                JwtTestSupport.hs256Token(),
                JwtTestSupport.expiredToken(),
                JwtTestSupport.tokenWithoutExp());
    }

    @ParameterizedTest
    @MethodSource("invalidTokens")
    void invalidToken_returns401(String token) throws Exception {
        mvc.perform(get("/items").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.message").value(INVALID))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
        then(itemService).should(never()).findAll();
    }

    @Test
    void unauthorizedResponse_requestIdMatchesHeader() throws Exception {
        MvcResult result = mvc.perform(get("/items")).andReturn();

        String requestId = result.getResponse().getHeader("X-Request-Id");
        String body = result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat((String) JsonPath.read(body, "$.requestId"))
                .isEqualTo(requestId);
    }

    @Test
    void corsPreflight_isAllowedWithoutToken() throws Exception {
        mvc.perform(options("/items")
                .header(HttpHeaders.ORIGIN, "http://localhost:5000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization, Content-Type"))
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
    void corsActualRequest_exposesRequestIdHeader() throws Exception {
        mvc.perform(get("/items").header(HttpHeaders.ORIGIN, "http://localhost:5000")
                .header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, equalTo("X-Request-Id")));
    }
}
