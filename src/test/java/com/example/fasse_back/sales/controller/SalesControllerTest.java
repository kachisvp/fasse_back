package com.example.fasse_back.sales.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.fasse_back.common.web.RequestParams.DateRange;
import com.example.fasse_back.sales.dto.SalesDetailResponse;
import com.example.fasse_back.sales.dto.SalesHeaderResponse;
import com.example.fasse_back.sales.dto.SalesRequest;
import com.example.fasse_back.sales.dto.SalesResponse;
import com.example.fasse_back.sales.service.SalesService;
import com.example.fasse_back.support.ApiTest;
import com.example.fasse_back.support.JwtTestSupport;

@ApiTest(SalesController.class)
class SalesControllerTest {

    private static final String ID = "c0000000-0000-4000-8000-000000000001";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SalesService salesService;

    private static String body(String salesDatetime) {
        return """
                {"sales_datetime":"%s","business_date":"2026-07-12","subtotal":1200,"tax_amount":120,
                 "total_amount":1320,"payment_method":"CASH",
                 "details":[{"menu_id":1,"quantity":2,"unit_price":600,"amount":1200,"tax_rate":0.1}]}
                """.formatted(salesDatetime);
    }

    private static SalesHeaderResponse header() {
        return new SalesHeaderResponse(ID, "SO-20260712-0001", OffsetDateTime.parse("2026-07-12T19:30:00+09:00"),
                LocalDate.of(2026, 7, 12), "A1", 2, new BigDecimal("1200.00"), new BigDecimal("120.00"),
                new BigDecimal("0.00"), new BigDecimal("1320.00"), "CASH", null,
                Instant.parse("2026-07-12T10:30:00Z"), Instant.parse("2026-07-12T10:30:00Z"));
    }

    private static SalesResponse sample() {
        return new SalesResponse(header(), List.of(new SalesDetailResponse("d0000000-0000-4000-8000-000000000001",
                1L, 2, new BigDecimal("600.00"), new BigDecimal("1200.00"), new BigDecimal("0.1000"))));
    }

    @Test
    void list_returnsSalesDatetimeWithJstOffset() throws Exception {
        LocalDate date = LocalDate.of(2026, 7, 12);
        given(salesService.findHeaders(new DateRange(date, date))).willReturn(List.of(header()));

        mvc.perform(get("/sales").param("from", "2026-07-12").param("to", "2026-07-12")
                .header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sales_no").value("SO-20260712-0001"))
                .andExpect(jsonPath("$[0].sales_datetime").value("2026-07-12T19:30:00+09:00"))
                .andExpect(jsonPath("$[0].business_date").value("2026-07-12"))
                .andExpect(jsonPath("$[0].customer_count").value(2))
                .andExpect(jsonPath("$[0].discount_amount").value(0.00))
                .andExpect(jsonPath("$[0].created_at").value("2026-07-12T10:30:00.000Z"));
    }

    @Test
    void list_missingRange_returns400() throws Exception {
        mvc.perform(get("/sales").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from and to are required in YYYY-MM-DD format"));
    }

    @Test
    void get_returnsHeaderAndDetailsFlattened() throws Exception {
        given(salesService.findById(ID)).willReturn(sample());

        mvc.perform(get("/sales/" + ID).header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sales_no").value("SO-20260712-0001"))
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0].menu_id").value(1))
                .andExpect(jsonPath("$.details[0].sales_id").doesNotExist());
    }

    @Test
    void get_invalidId_returns400() throws Exception {
        mvc.perform(get("/sales/123").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("id must be a UUID"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "2026-07-12T19:30:00+09:00", "2026-07-12T10:30:00Z" })
    void create_acceptsDatetimeWithOffset(String salesDatetime) throws Exception {
        given(salesService.create(any())).willReturn(sample());

        mvc.perform(post("/sales").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body(salesDatetime)))
                .andExpect(status().isCreated());

        ArgumentCaptor<SalesRequest> captor = ArgumentCaptor.forClass(SalesRequest.class);
        then(salesService).should().create(captor.capture());
        assertSameInstant(captor.getValue().salesDatetime(), OffsetDateTime.parse("2026-07-12T19:30:00+09:00"));
    }

    private static void assertSameInstant(OffsetDateTime actual, OffsetDateTime expected) {
        org.assertj.core.api.Assertions.assertThat(actual.toInstant()).isEqualTo(expected.toInstant());
    }

    @ParameterizedTest
    @ValueSource(strings = { "2026-07-12T19:30:00", "2026-07-12", "2026/07/12 19:30" })
    void create_datetimeWithoutOffset_returns400(String salesDatetime) throws Exception {
        mvc.perform(post("/sales").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body(salesDatetime)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid or missing fields: sales_datetime"));
        then(salesService).should(never()).create(any());
    }

    @Test
    void create_invalidFields_returns400() throws Exception {
        String body = """
                {"sales_datetime":"2026-07-12T19:30:00+09:00","business_date":"2026-07-12","subtotal":1200,
                 "tax_amount":120,"total_amount":1320,"table_no":"%s","payment_method":"%s",
                 "details":[{"menu_id":1,"unit_price":600,"amount":1200,"tax_rate":0.1}]}
                """.formatted("1".repeat(11), "p".repeat(21));
        mvc.perform(post("/sales").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "invalid or missing fields: details[0].quantity, payment_method, table_no"));
    }

    @Test
    void create_decimalQuantity_returns400() throws Exception {
        String body = body("2026-07-12T19:30:00+09:00").replace("\"quantity\":2", "\"quantity\":1.5");
        mvc.perform(post("/sales").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid or missing fields: details[0].quantity"));
    }

    @Test
    void update_returns200() throws Exception {
        given(salesService.update(eq(ID), any())).willReturn(sample());

        mvc.perform(put("/sales/" + ID).header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body("2026-07-12T19:30:00+09:00")))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mvc.perform(delete("/sales/" + ID).header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isNoContent());
        then(salesService).should().delete(ID);
    }

    @Test
    void withoutJwt_returns401() throws Exception {
        mvc.perform(post("/sales").contentType(MediaType.APPLICATION_JSON).content(body("2026-07-12T19:30:00Z")))
                .andExpect(status().isUnauthorized());
        then(salesService).should(never()).create(any());
    }
}
