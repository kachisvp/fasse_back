package com.example.fasse_back.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import com.fasterxml.jackson.databind.ObjectMapper;

class JsonAuthenticationEntryPointTest {

    @Test
    void accessDenied_returns401WithJsonBody() throws Exception {
        JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint(new ObjectMapper());
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.handle(new MockHttpServletRequest(), response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getContentAsString()).contains("\"message\":\"Invalid or expired token\"");
    }
}
