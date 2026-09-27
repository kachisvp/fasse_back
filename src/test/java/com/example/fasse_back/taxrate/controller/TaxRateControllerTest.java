package com.example.fasse_back.taxrate.controller;

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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.common.exception.ConflictException;
import com.example.fasse_back.support.ApiTest;
import com.example.fasse_back.support.JwtTestSupport;
import com.example.fasse_back.taxrate.dto.TaxRateRequest;
import com.example.fasse_back.taxrate.dto.TaxRateResponse;
import com.example.fasse_back.taxrate.dto.TaxRateUpdateRequest;
import com.example.fasse_back.taxrate.service.TaxRateService;

@ApiTest(TaxRateController.class)
class TaxRateControllerTest {

    private static final LocalDate VALID_FROM = LocalDate.of(2019, 10, 1);
    private static final String VALID_BODY = """
            {"tax_category":"STANDARD","description":"標準税率","rate":0.10,"valid_from":"2019-10-01"}
            """;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    TaxRateService taxRateService;

    private static TaxRateResponse sample() {
        return new TaxRateResponse(TaxCategory.STANDARD, "標準税率", new BigDecimal("0.1000"), VALID_FROM, null,
                Instant.parse("2026-07-12T10:30:00Z"), Instant.parse("2026-07-12T10:30:00Z"));
    }

    @Test
    void list_withoutFilter_returnsAllWithoutId() throws Exception {
        given(taxRateService.findAll(null)).willReturn(List.of(sample()));

        mvc.perform(get("/tax-rates").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tax_category").value("STANDARD"))
                .andExpect(jsonPath("$[0].rate").value(0.1))
                .andExpect(jsonPath("$[0].valid_from").value("2019-10-01"))
                .andExpect(jsonPath("$[0].valid_to").isEmpty())
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    void list_withFilter() throws Exception {
        given(taxRateService.findAll(TaxCategory.REDUCED)).willReturn(List.of());

        mvc.perform(get("/tax-rates").param("tax_category", "REDUCED")
                .header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk());
        then(taxRateService).should().findAll(TaxCategory.REDUCED);
    }

    @Test
    void list_invalidFilter_returns400() throws Exception {
        mvc.perform(get("/tax-rates").param("tax_category", "FOOD")
                .header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("tax_category must be one of STANDARD, REDUCED, EXEMPT"));
    }

    @Test
    void get_returnsTaxRate() throws Exception {
        given(taxRateService.findByKey(TaxCategory.STANDARD, VALID_FROM)).willReturn(sample());

        mvc.perform(get("/tax-rates/STANDARD/2019-10-01").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("標準税率"));
    }

    @Test
    void get_invalidTaxCategory_returns400() throws Exception {
        mvc.perform(get("/tax-rates/standard/2019-10-01").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("taxCategory must be one of STANDARD, REDUCED, EXEMPT"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "2019-1-1", "20191001", "2019-02-30", "x" })
    void get_invalidValidFrom_returns400(String validFrom) throws Exception {
        mvc.perform(get("/tax-rates/STANDARD/" + validFrom).header(HttpHeaders.AUTHORIZATION,
                JwtTestSupport.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("validFrom must be in YYYY-MM-DD format"));
    }

    @Test
    void create_returns201() throws Exception {
        given(taxRateService.create(any())).willReturn(sample());

        mvc.perform(post("/tax-rates").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());
        then(taxRateService).should().create(
                new TaxRateRequest(TaxCategory.STANDARD, "標準税率", new BigDecimal("0.10"), VALID_FROM, null));
    }

    @Test
    void create_duplicate_returns409() throws Exception {
        given(taxRateService.create(any()))
                .willThrow(new ConflictException("tax rate already exists: STANDARD/2019-10-01"));

        mvc.perform(post("/tax-rates").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("tax rate already exists: STANDARD/2019-10-01"));
    }

    @Test
    void create_invalidFields_returns400() throws Exception {
        mvc.perform(post("/tax-rates").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tax_category\":\"STANDARD\",\"description\":\"%s\",\"rate\":10}"
                        .formatted("a".repeat(51))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid or missing fields: description, rate, valid_from"));
    }

    @Test
    void create_invalidDateFormat_returns400() throws Exception {
        mvc.perform(post("/tax-rates").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tax_category":"STANDARD","description":"a","rate":0.1,"valid_from":"2019-10-01T00:00:00"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid or missing fields: valid_from"));
    }

    @Test
    void update_ignoresKeysInBody() throws Exception {
        given(taxRateService.update(eq(TaxCategory.STANDARD), eq(VALID_FROM), any())).willReturn(sample());

        mvc.perform(put("/tax-rates/STANDARD/2019-10-01").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tax_category":"EXEMPT","valid_from":"2000-01-01","description":"標準","rate":0.1}
                        """))
                .andExpect(status().isOk());
        then(taxRateService).should().update(TaxCategory.STANDARD, VALID_FROM,
                new TaxRateUpdateRequest("標準", new BigDecimal("0.1"), null));
    }

    @Test
    void update_missingFields_returns400() throws Exception {
        mvc.perform(put("/tax-rates/STANDARD/2019-10-01").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"valid_to\":\"2030-03-31\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid or missing fields: description, rate"));
    }

    @Test
    void delete_returns204() throws Exception {
        mvc.perform(delete("/tax-rates/EXEMPT/2019-10-01").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isNoContent());
        then(taxRateService).should().delete(TaxCategory.EXEMPT, VALID_FROM);
    }

    @Test
    void withoutJwt_returns401() throws Exception {
        mvc.perform(get("/tax-rates"))
                .andExpect(status().isUnauthorized());
        then(taxRateService).should(never()).findAll(any());
    }
}
