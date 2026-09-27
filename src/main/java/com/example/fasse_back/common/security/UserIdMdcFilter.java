package com.example.fasse_back.common.security;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.fasse_back.common.web.RequestIdFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 認証成功後、JWT の {@code sub} を MDC の {@code userId} に設定する(REQ-A06)。
 * MDC の削除は {@link RequestIdFilter} がリクエスト完了時に行う。
 */
public class UserIdMdcFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            MDC.put(RequestIdFilter.MDC_USER_ID, jwtAuthentication.getToken().getSubject());
        }
        chain.doFilter(request, response);
    }
}
