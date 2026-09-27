package com.example.fasse_back.menu.controller;

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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.menu.dto.MenuRequest;
import com.example.fasse_back.menu.dto.MenuResponse;
import com.example.fasse_back.menu.service.MenuService;
import com.example.fasse_back.support.ApiTest;
import com.example.fasse_back.support.JwtTestSupport;

@ApiTest(MenuController.class)
class MenuControllerTest {

    private static final String VALID_BODY = """
            {"menu_name":"生ビール","category":"ドリンク","standard_price":600,"tax_category":"STANDARD"}
            """;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    MenuService menuService;

    private static MenuResponse sample() {
        return new MenuResponse(1L, "生ビール", "ドリンク", new BigDecimal("600.00"), TaxCategory.STANDARD, true,
                Instant.parse("2026-07-12T10:30:00Z"), Instant.parse("2026-07-12T10:30:00Z"));
    }

    @Test
    void list_returnsSnakeCaseJson() throws Exception {
        given(menuService.findAll()).willReturn(List.of(sample()));

        mvc.perform(get("/menus").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].menu_name").value("生ビール"))
                .andExpect(jsonPath("$[0].standard_price").value(600.00))
                .andExpect(jsonPath("$[0].tax_category").value("STANDARD"));
    }

    @Test
    void get_returnsMenu() throws Exception {
        given(menuService.findById(1L)).willReturn(sample());

        mvc.perform(get("/menus/1").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("ドリンク"));
    }

    @Test
    void get_invalidId_returns400() throws Exception {
        mvc.perform(get("/menus/a").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("id must be a positive integer"));
    }

    @Test
    void create_returns201() throws Exception {
        given(menuService.create(any())).willReturn(sample());

        mvc.perform(post("/menus").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());
        then(menuService).should().create(new MenuRequest("生ビール", "ドリンク", new BigDecimal("600"),
                TaxCategory.STANDARD, null));
    }

    @Test
    void create_missingRequired_returns400() throws Exception {
        mvc.perform(post("/menus").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "invalid or missing fields: category, menu_name, standard_price, tax_category"));
        then(menuService).should(never()).create(any());
    }

    @Test
    void create_tooLongAndTooManyDigits_returns400() throws Exception {
        String body = """
                {"menu_name":"%s","category":"%s","standard_price":10000000000,"tax_category":"STANDARD"}
                """.formatted("a".repeat(101), "a".repeat(51));
        mvc.perform(post("/menus").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "invalid or missing fields: category, menu_name, standard_price"));
    }

    @Test
    void update_returns200() throws Exception {
        given(menuService.update(eq(1L), any())).willReturn(sample());

        mvc.perform(put("/menus/1").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mvc.perform(delete("/menus/1").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isNoContent());
        then(menuService).should().delete(1L);
    }

    @Test
    void withoutJwt_returns401() throws Exception {
        mvc.perform(delete("/menus/1"))
                .andExpect(status().isUnauthorized());
        then(menuService).should(never()).delete(any(Long.class));
    }
}
