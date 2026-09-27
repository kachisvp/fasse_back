package com.example.fasse_back.item.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.common.exception.NotFoundException;
import com.example.fasse_back.item.dto.ItemRequest;
import com.example.fasse_back.item.dto.ItemResponse;
import com.example.fasse_back.item.service.ItemService;
import com.example.fasse_back.support.ApiTest;
import com.example.fasse_back.support.JwtTestSupport;

@ApiTest(ItemController.class)
class ItemControllerTest {

    private static final String VALID_BODY = """
            {"item_name":"キャベツ","unit":"玉","standard_price":200,"tax_category":"REDUCED"}
            """;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    ItemService itemService;

    private static ItemResponse sample() {
        return new ItemResponse(1L, "キャベツ", "玉", new BigDecimal("200.00"), TaxCategory.REDUCED, true,
                Instant.parse("2026-07-12T10:30:00Z"), Instant.parse("2026-07-12T10:31:00.123Z"));
    }

    @Test
    void list_returnsSnakeCaseJsonWithUtcTimestamps() throws Exception {
        given(itemService.findAll()).willReturn(List.of(sample()));

        mvc.perform(get("/items").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].item_name").value("キャベツ"))
                .andExpect(jsonPath("$[0].standard_price").value(200.00))
                .andExpect(jsonPath("$[0].tax_category").value("REDUCED"))
                .andExpect(jsonPath("$[0].is_active").value(true))
                .andExpect(jsonPath("$[0].created_at").value("2026-07-12T10:30:00.000Z"))
                .andExpect(jsonPath("$[0].updated_at").value("2026-07-12T10:31:00.123Z"))
                .andExpect(header().string("X-Request-Id", notNullValue()));
    }

    @Test
    void get_returnsItem() throws Exception {
        given(itemService.findById(1L)).willReturn(sample());

        mvc.perform(get("/items/1").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unit").value("玉"));
    }

    @Test
    void get_notFound_returns404() throws Exception {
        given(itemService.findById(9L)).willThrow(new NotFoundException("item not found: id=9"));

        mvc.perform(get("/items/9").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Not Found"))
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = { "0", "-1", "abc", "1.5", "99999999999999999999" })
    void get_invalidId_returns400(String id) throws Exception {
        mvc.perform(get("/items/" + id).header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("id must be a positive integer"));
    }

    @Test
    void create_returns201() throws Exception {
        given(itemService.create(any())).willReturn(sample());

        mvc.perform(post("/items").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
        then(itemService).should().create(new ItemRequest("キャベツ", "玉", new BigDecimal("200"),
                TaxCategory.REDUCED, null));
    }

    @Test
    void create_ignoresServerManagedAndUnknownFields() throws Exception {
        given(itemService.create(any())).willReturn(sample());

        mvc.perform(post("/items").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"id":99,"created_at":"x","unknown":1,"item_name":"a","unit":"b",
                         "tax_category":"STANDARD","is_active":false}
                        """))
                .andExpect(status().isCreated());
        then(itemService).should().create(new ItemRequest("a", "b", null, TaxCategory.STANDARD, false));
    }

    @Test
    void create_missingFields_listsAllFields() throws Exception {
        mvc.perform(post("/items").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content("{\"unit\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid or missing fields: item_name, tax_category, unit"));
        then(itemService).should(never()).create(any());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            // 型の不一致(暗黙の型変換をしない)
            "{\"item_name\":\"a\",\"unit\":\"b\",\"tax_category\":\"STANDARD\",\"standard_price\":\"100\"}|standard_price",
            "{\"item_name\":1,\"unit\":\"b\",\"tax_category\":\"STANDARD\"}|item_name",
            "{\"item_name\":\"a\",\"unit\":true,\"tax_category\":\"STANDARD\"}|unit",
            "{\"item_name\":\"a\",\"unit\":\"b\",\"tax_category\":\"STANDARD\",\"is_active\":1}|is_active",
            "{\"item_name\":\"a\",\"unit\":\"b\",\"tax_category\":\"STANDARD\",\"is_active\":\"true\"}|is_active",
            // ENUM 外の値
            "{\"item_name\":\"a\",\"unit\":\"b\",\"tax_category\":\"FREE\"}|tax_category",
            "{\"item_name\":\"a\",\"unit\":\"b\",\"tax_category\":0}|tax_category",
            "{\"item_name\":\"a\",\"unit\":\"b\",\"tax_category\":\"\"}|tax_category",
    })
    void create_typeMismatch_returns400(String body, String field) throws Exception {
        mvc.perform(post("/items").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid or missing fields: " + field));
    }

    @Test
    void create_tooLong_returns400() throws Exception {
        String body = """
                {"item_name":"%s","unit":"%s","standard_price":12345678901,"tax_category":"STANDARD"}
                """.formatted("あ".repeat(101), "a".repeat(20));
        mvc.perform(post("/items").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("invalid or missing fields: item_name, standard_price"));
    }

    @Test
    void create_maxLength_isAccepted() throws Exception {
        given(itemService.create(any())).willReturn(sample());
        String body = """
                {"item_name":"%s","unit":"%s","standard_price":1234567890.99,"tax_category":"STANDARD"}
                """.formatted("あ".repeat(100), "a".repeat(20));
        mvc.perform(post("/items").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "{\"item_name\":|request body is not valid JSON",
            "not json|request body is not valid JSON",
            "{\"item_name\":tru}|request body is not valid JSON",
            "[]|request body must be a JSON object",
            "\"text\"|request body must be a JSON object",
            "123|request body must be a JSON object",
    })
    void create_invalidBody_returns400(String body, String message) throws Exception {
        mvc.perform(post("/items").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void create_emptyBody_returns400() throws Exception {
        mvc.perform(post("/items").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("request body is required"));
    }

    @Test
    void create_notJsonContentType_returns415() throws Exception {
        mvc.perform(post("/items").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.TEXT_PLAIN).content(VALID_BODY))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Unsupported Media Type"));
    }

    @Test
    void update_returns200() throws Exception {
        given(itemService.update(eq(1L), any())).willReturn(sample());

        mvc.perform(put("/items/1").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_invalidId_returns400() throws Exception {
        mvc.perform(put("/items/x").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("id must be a positive integer"));
    }

    @Test
    void delete_returns204() throws Exception {
        mvc.perform(delete("/items/1").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isNoContent());
        then(itemService).should().delete(1L);
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        willThrow(new NotFoundException("item not found: id=9")).given(itemService).delete(9L);

        mvc.perform(delete("/items/9").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void unsupportedMethod_returns405() throws Exception {
        mvc.perform(post("/items/1").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Method Not Allowed"));
    }

    @Test
    void unknownPath_returns404() throws Exception {
        mvc.perform(get("/unknown").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Not Found"));
    }

    @Test
    void unexpectedException_returns500WithoutInternalDetails() throws Exception {
        given(itemService.findAll()).willThrow(new IllegalStateException("secret internal detail"));

        mvc.perform(get("/items").header(HttpHeaders.AUTHORIZATION, JwtTestSupport.bearer()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal Server Error"))
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }

    @Nested
    class WithoutJwt {

        @Test
        void list_returns401() throws Exception {
            mvc.perform(get("/items"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Authorization header is missing or malformed"));
            then(itemService).should(never()).findAll();
        }

        @Test
        void create_returns401() throws Exception {
            mvc.perform(post("/items").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                    .andExpect(status().isUnauthorized());
            then(itemService).should(never()).create(any());
        }
    }
}
