package com.example.fasse_back.common.web;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * リクエストごとにリクエスト ID(UUID)を採番し、MDC とレスポンスヘッダ {@code X-Request-Id} に設定する。
 * 完了時にアクセスログ(メソッド、パス、ステータス、処理時間)を INFO で出力する。
 *
 * <p>Spring Security のフィルタより前に動かすため、最優先の順序で登録する。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_USER_ID = "userId";

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        long start = System.nanoTime();
        MDC.put(MDC_REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.info("{} {} {} {}ms", request.getMethod(), request.getRequestURI(), response.getStatus(), elapsedMs);
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_REQUEST_ID);
        }
    }
}
