package com.example.fasse_back.purchase.controller;

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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.fasse_back.common.exception.BadRequestException;
import com.example.fasse_back.common.web.RequestParams.DateRange;
import com.example.fasse_back.purchase.dto.PurchaseDetailResponse;
import com.example.fasse_back.purchase.dto.PurchaseHeaderResponse;
import com.example.fasse_back.purchase.dto.PurchaseResponse;
import com.example.fasse_back.purchase.service.PurchaseService;
import com.example.fasse_back.support.ApiTest;
import com.example.fasse_back.support.JwtTestSupport;

@ApiTest(PurchaseController.class)
class PurchaseControllerTest {

    private static final String ID = "a0000000-0000-4000-8000-000000000001";
    private static final String VALID_BODY = """
            {"supplier_id":1,"purchase_date":"2026-07-12","subtotal":200,"tax_amount":16,"total_amount":216,
             "details":[{"item_id":1,"quantity":2.5,"unit_price":80,"amount":200,"tax_rate":0.08}]}
            """;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    PurchaseService purchaseService;

    private static PurchaseHeaderResponse header() {
        return new PurchaseHeaderResponse(ID, "PO-20260712-0001", 1L, LocalDate.of(2026, 7, 12), null,
                new BigDecimal("200.00"), new BigDecimal("16.00"), new BigDecimal("216.00"), null,
                Instant.parse("2026-07-12T10:30:00Z"), Instant.parse("2026-07-12T10:30:00Z"));
    }

    private static PurchaseResponse sample() {
        return new PurchaseResponse(header(), List.of(new PurchaseDetailResponse(
                "b0000000-0000-4000-8000-000000000001", 1L, new BigDecimal("2.50"), new BigDecimal("80.00"),
                new BigDecimal("200.00"), new BigDecimal("0.0800"))));
    }

    @Test
    void list_returnsHeadersWithoutDetails() throws Exception {
        LocalDate date = LocalDate.of(2026, 7, 12);
        given(purchaseService.findHeaders(new DateRange(date, date))).willReturn(List.of(header()));

        mvc.perform(get("/purchases").param("from", "2026-07-12").param("to", "2026-07-12")
                .header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].purchase_no").value("PO-20260712-0001"))
                .andExpect(jsonPath("$[0].purchase_date").value("2026-07-12"))
                .andExpect(jsonPath("$[0].details").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource(value = { "2026-07-01,", ",2026-07-01", "2026/07/01,2026-07-02", "2026-07-01,2026-7-2" },
            ignoreLeadingAndTrailingWhitespace = false)
    void list_invalidRange_returns400(String from, String to) throws Exception {
        var request = get("/purchases").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer());
        if (from != null) {
            request.param("from", from);
        }
        if (to != null) {
            request.param("to", to);
        }
        mvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from and to are required in YYYY-MM-DD format"));
    }

    @Test
    void get_returnsHeaderAndDetailsFlattened() throws Exception {
        given(purchaseService.findById(ID)).willReturn(sample());

        mvc.perform(get("/purchases/" + ID).header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$.purchase_no").value("PO-20260712-0001"))
                .andExpect(jsonPath("$.supplier_id").value(1))
                .andExpect(jsonPath("$.header").doesNotExist())
                .andExpect(jsonPath("$.details", hasSize(1)))
                .andExpect(jsonPath("$.details[0].item_id").value(1))
                .andExpect(jsonPath("$.details[0].quantity").value(2.5))
                .andExpect(jsonPath("$.details[0].tax_rate").value(0.08))
                .andExpect(jsonPath("$.details[0].purchase_id").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = { "1", "not-a-uuid", "a0000000-0000-4000-8000-00000000000" })
    void get_invalidId_returns400(String id) throws Exception {
        mvc.perform(get("/purchases/" + id).header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("id must be a UUID"));
    }

    @Test
    void create_returns201() throws Exception {
        given(purchaseService.create(any())).willReturn(sample());

        mvc.perform(post("/purchases").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.purchase_no").value("PO-20260712-0001"));
    }

    @Test
    void create_invalidDetails_listsIndexedFieldNames() throws Exception {
        String body = """
                {"supplier_id":1,"purchase_date":"2026-07-12","subtotal":200,"tax_amount":16,"total_amount":216,
                 "remarks":"%s",
                 "details":[{"item_id":1,"quantity":2,"unit_price":80,"amount":200,"tax_rate":0.08},
                            {"quantity":123456789,"unit_price":80,"amount":200,"tax_rate":10},
                            null]}
                """.formatted("a".repeat(501));
        mvc.perform(post("/purchases").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid or missing fields: details[1].item_id, "
                        + "details[1].quantity, details[1].tax_rate, details[2], remarks"));
        then(purchaseService).should(never()).create(any());
    }

    @Test
    void create_detailTypeMismatch_showsIndexedFieldName() throws Exception {
        String body = """
                {"supplier_id":1,"purchase_date":"2026-07-12","subtotal":200,"tax_amount":16,"total_amount":216,
                 "details":[{"item_id":1.5,"quantity":2,"unit_price":80,"amount":200,"tax_rate":0.08}]}
                """;
        mvc.perform(post("/purchases").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid or missing fields: details[0].item_id"));
    }

    @Test
    void create_detailsNotArray_returns400() throws Exception {
        String body = """
                {"supplier_id":1,"purchase_date":"2026-07-12","subtotal":200,"tax_amount":16,"total_amount":216,
                 "details":{"item_id":1}}
                """;
        mvc.perform(post("/purchases").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid or missing fields: details"));
    }

    @Test
    void create_missingHeaderFields_returns400() throws Exception {
        mvc.perform(post("/purchases").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid or missing fields: details, purchase_date, "
                        + "subtotal, supplier_id, tax_amount, total_amount"));
    }

    @Test
    void create_invalidReference_returns400() throws Exception {
        given(purchaseService.create(any())).willThrow(BadRequestException.invalidReference(List.of("supplier_id")));

        mvc.perform(post("/purchases").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid reference: supplier_id"));
    }

    @Test
    void update_returns200() throws Exception {
        given(purchaseService.update(eq(ID), any())).willReturn(sample());

        mvc.perform(put("/purchases/" + ID).header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void update_invalidId_returns400() throws Exception {
        mvc.perform(put("/purchases/1").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("id must be a UUID"));
    }

    @Test
    void delete_returns204() throws Exception {
        mvc.perform(delete("/purchases/" + ID).header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isNoContent());
        then(purchaseService).should().delete(ID);
    }

    @Test
    void withoutJwt_returns401() throws Exception {
        mvc.perform(get("/purchases/" + ID))
                .andExpect(status().isUnauthorized());
        then(purchaseService).should(never()).findById(any());
    }
}
