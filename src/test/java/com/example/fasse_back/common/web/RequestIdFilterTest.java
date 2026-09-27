package com.example.fasse_back.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;

class RequestIdFilterTest {

    @Test
    void setsRequestIdToMdcAndHeaderAndClearsAfterward() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> requestIdInChain = new AtomicReference<>();
        AtomicReference<String> errorRequestId = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            requestIdInChain.set(MDC.get(RequestIdFilter.MDC_REQUEST_ID));
            MDC.put(RequestIdFilter.MDC_USER_ID, "user");
            errorRequestId.set(ErrorResponse.of("x").requestId());
        };

        filter.doFilter(new MockHttpServletRequest("GET", "/items"), response, chain);

        String header = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
        assertThat(header).matches("[0-9a-f-]{36}");
        assertThat(requestIdInChain.get()).isEqualTo(header);
        // エラーレスポンスの requestId はヘッダと一致する
        assertThat(errorRequestId.get()).isEqualTo(header);
        assertThat(MDC.get(RequestIdFilter.MDC_REQUEST_ID)).isNull();
        assertThat(MDC.get(RequestIdFilter.MDC_USER_ID)).isNull();
    }

    @Test
    void issuesDifferentIdPerRequest() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        MockHttpServletResponse first = new MockHttpServletResponse();
        MockHttpServletResponse second = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), first, (req, res) -> { });
        filter.doFilter(new MockHttpServletRequest(), second, (req, res) -> { });

        assertThat(first.getHeader(RequestIdFilter.REQUEST_ID_HEADER))
                .isNotEqualTo(second.getHeader(RequestIdFilter.REQUEST_ID_HEADER));
    }

    @Test
    void clearsMdcEvenWhenChainThrows() {
        RequestIdFilter filter = new RequestIdFilter();

        try {
            filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (req, res) -> {
                throw new IllegalStateException("boom");
            });
        } catch (Exception e) {
            assertThat(e).hasMessage("boom");
        }

        assertThat(MDC.get(RequestIdFilter.MDC_REQUEST_ID)).isNull();
    }
}
