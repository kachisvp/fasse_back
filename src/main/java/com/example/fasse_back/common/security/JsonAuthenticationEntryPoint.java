package com.example.fasse_back.common.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.example.fasse_back.common.web.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 認証失敗時に 401 を {@code { "message", "requestId" }} 形式で返す
 * (docs/specs/authentication/design.md 4 節)。
 *
 * <p>ロールによる認可は行わないため、{@link AccessDeniedHandler} も 401 を返す。
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    static final String MISSING_OR_MALFORMED = "Authorization header is missing or malformed";
    static final String INVALID_TOKEN = "Invalid or expired token";

    private static final Logger log = LoggerFactory.getLogger(JsonAuthenticationEntryPoint.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        String message = hasBearerHeader(request) ? INVALID_TOKEN : MISSING_OR_MALFORMED;
        // トークンの値は出さず、理由のみを出す
        log.warn("401: {} ({})", message, authException.getMessage());
        writeUnauthorized(response, message);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        log.warn("401: access denied ({})", accessDeniedException.getMessage());
        writeUnauthorized(response, INVALID_TOKEN);
    }

    private static boolean hasBearerHeader(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return header != null
                && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length());
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(message));
    }
}
