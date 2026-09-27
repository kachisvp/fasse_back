package com.example.fasse_back.supplier.controller;

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

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.fasse_back.supplier.dto.SupplierRequest;
import com.example.fasse_back.supplier.dto.SupplierResponse;
import com.example.fasse_back.supplier.service.SupplierService;
import com.example.fasse_back.support.ApiTest;
import com.example.fasse_back.support.JwtTestSupport;

@ApiTest(SupplierController.class)
class SupplierControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SupplierService supplierService;

    private static SupplierResponse sample() {
        return new SupplierResponse(1L, "山田青果", "100-0001", "東京都", "03-1111-1111", "a@example.com", true,
                Instant.parse("2026-07-12T10:30:00Z"), Instant.parse("2026-07-12T10:30:00Z"));
    }

    @Test
    void list_returnsSnakeCaseJson() throws Exception {
        given(supplierService.findAll()).willReturn(List.of(sample()));

        mvc.perform(get("/suppliers").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].supplier_name").value("山田青果"))
                .andExpect(jsonPath("$[0].postal_code").value("100-0001"))
                .andExpect(jsonPath("$[0].phone_number").value("03-1111-1111"))
                .andExpect(jsonPath("$[0].is_active").value(true));
    }

    @Test
    void get_returnsSupplier() throws Exception {
        given(supplierService.findById(1L)).willReturn(sample());

        mvc.perform(get("/suppliers/1").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("a@example.com"));
    }

    @Test
    void get_invalidId_returns400() throws Exception {
        mvc.perform(get("/suppliers/0").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("id must be a positive integer"));
    }

    @Test
    void create_onlyRequiredField_returns201() throws Exception {
        given(supplierService.create(any())).willReturn(sample());

        mvc.perform(post("/suppliers").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"supplier_name\":\"山田青果\"}"))
                .andExpect(status().isCreated());
        then(supplierService).should().create(new SupplierRequest("山田青果", null, null, null, null, null));
    }

    @Test
    void create_missingRequiredAndTooLong_returns400() throws Exception {
        String body = """
                {"postal_code":"%s","address":"%s","phone_number":"%s","email":"%s"}
                """.formatted("1".repeat(11), "a".repeat(256), "0".repeat(31), "e".repeat(256));
        mvc.perform(post("/suppliers").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "invalid or missing fields: address, email, phone_number, postal_code, supplier_name"));
        then(supplierService).should(never()).create(any());
    }

    @Test
    void update_returns200() throws Exception {
        given(supplierService.update(eq(1L), any())).willReturn(sample());

        mvc.perform(put("/suppliers/1").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"supplier_name\":\"山田青果\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mvc.perform(delete("/suppliers/1").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isNoContent());
        then(supplierService).should().delete(1L);
    }

    @Test
    void withoutJwt_returns401() throws Exception {
        mvc.perform(get("/suppliers"))
                .andExpect(status().isUnauthorized());
        then(supplierService).should(never()).findAll();
    }
}
