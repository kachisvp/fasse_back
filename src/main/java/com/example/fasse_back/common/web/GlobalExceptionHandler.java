package com.example.fasse_back.common.web;

import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.fasse_back.common.exception.ApiException;
import com.example.fasse_back.common.exception.BadRequestException;
import com.example.fasse_back.common.exception.NotFoundException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * 例外をエラーレスポンス({@code { "message", "requestId" }})に変換する(design.md 6 節)。
 *
 * <p>業務上の想定内エラー(400 / 404 / 405 / 409 / 415)は WARN で理由のみを、
 * 予期しない例外(500)は ERROR でスタックトレースを含めてログに出す。リクエストボディ全体はログに出さない。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    static final String BODY_REQUIRED = "request body is required";
    static final String BODY_NOT_JSON = "request body is not valid JSON";
    static final String BODY_NOT_OBJECT = "request body must be a JSON object";
    static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Pattern CAMEL_CASE_BOUNDARY = Pattern.compile("([a-z0-9])([A-Z])");

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        String reason = e instanceof NotFoundException notFound ? notFound.getDetail() : e.getMessage();
        log.warn("{}: {}", e.getStatus().value(), reason);
        return respond(e.getStatus(), e.getMessage());
    }

    /** ボディが無い・JSON でない・オブジェクトでない・型の不一致 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e) {
        return handleApiException(toBadRequest(e));
    }

    /** Bean Validation の違反(必須項目の欠落、列長・桁数の超過)。違反した全項目を列挙する */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleNotValid(MethodArgumentNotValidException e) {
        TreeSet<String> fields = new TreeSet<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fields.add(toSnakeCase(error.getField()));
        }
        return handleApiException(BadRequestException.invalidFields(fields));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        log.warn("404: no handler for {} /{}", e.getHttpMethod(), e.getResourcePath());
        return respond(HttpStatus.NOT_FOUND, NotFoundException.MESSAGE);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("405: {}", e.getMessage());
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("415: {}", e.getMessage());
        return respond(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported Media Type");
    }

    /** 予期しない例外。内部情報をレスポンスに含めず、固定文言を返す */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("500: unexpected error", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR);
    }

    private static ResponseEntity<ErrorResponse> respond(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(message));
    }

    /** JSON の解析エラーを design.md 6.1 節の 400 に分類する */
    static BadRequestException toBadRequest(HttpMessageNotReadableException e) {
        Throwable cause = e.getCause();
        if (cause == null) {
            // ボディが空(または JSON の null)の場合、Spring が原因例外なしで投げる
            return new BadRequestException(BODY_REQUIRED);
        }
        if (hasCause(cause, JsonParseException.class)) {
            return new BadRequestException(BODY_NOT_JSON);
        }
        if (cause instanceof JsonMappingException mapping) {
            List<JsonMappingException.Reference> path = mapping.getPath();
            if (path.isEmpty()) {
                // ルートがオブジェクトでない(配列・文字列・数値等)
                return new BadRequestException(BODY_NOT_OBJECT);
            }
            return BadRequestException.invalidFields(List.of(toFieldName(path)));
        }
        return new BadRequestException(BODY_NOT_JSON);
    }

    private static boolean hasCause(Throwable t, Class<? extends Throwable> type) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (type.isInstance(c)) {
                return true;
            }
        }
        return false;
    }

    /** JSON のパスを項目名にする(例: {@code details[0].item_id}) */
    private static String toFieldName(List<JsonMappingException.Reference> path) {
        StringBuilder sb = new StringBuilder();
        for (JsonMappingException.Reference ref : path) {
            if (ref.getFieldName() != null) {
                if (!sb.isEmpty()) {
                    sb.append('.');
                }
                sb.append(ref.getFieldName());
            } else {
                sb.append('[').append(ref.getIndex()).append(']');
            }
        }
        return sb.toString();
    }

    /** Java のプロパティパスをスネークケースにする(例: {@code details[0].itemId} → {@code details[0].item_id}) */
    static String toSnakeCase(String propertyPath) {
        return CAMEL_CASE_BOUNDARY.matcher(propertyPath).replaceAll("$1_$2").toLowerCase();
    }
}
