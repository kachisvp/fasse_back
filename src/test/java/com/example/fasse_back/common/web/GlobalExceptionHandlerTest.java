package com.example.fasse_back.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;

import com.example.fasse_back.common.exception.BadRequestException;

/** JSON 解析エラーの分類と項目名の変換(design.md 6.1 節)。HTTP 経由の検証は各 ControllerTest で行う */
class GlobalExceptionHandlerTest {

    @Test
    void toSnakeCase_convertsNestedIndexedPath() {
        assertThat(GlobalExceptionHandler.toSnakeCase("details[10].itemId")).isEqualTo("details[10].item_id");
        assertThat(GlobalExceptionHandler.toSnakeCase("isActive")).isEqualTo("is_active");
        assertThat(GlobalExceptionHandler.toSnakeCase("subtotal")).isEqualTo("subtotal");
    }

    @Test
    void toBadRequest_withoutCause_isBodyRequired() {
        HttpMessageNotReadableException e = new HttpMessageNotReadableException("Required request body is missing",
                new MockHttpInputMessage(new byte[0]));

        BadRequestException result = GlobalExceptionHandler.toBadRequest(e);

        assertThat(result.getMessage()).isEqualTo("request body is required");
    }

    @Test
    void toBadRequest_unknownCause_isNotValidJson() {
        HttpMessageNotReadableException e = new HttpMessageNotReadableException("I/O error",
                new java.io.IOException("stream closed"), new MockHttpInputMessage(new byte[0]));

        assertThat(GlobalExceptionHandler.toBadRequest(e).getMessage()).isEqualTo("request body is not valid JSON");
    }
}
