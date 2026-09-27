package com.example.fasse_back.support;

import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/** 結合テスト用の HTTP クライアント。JSON の文字列を送り、レスポンスを {@link JsonNode} で返す */
public class ApiClient {

    /** 小数の桁数(例: 200.00)を保ったまま読む */
    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .nodeFactory(JsonNodeFactory.withExactBigDecimals(true))
            .build();

    private final TestRestTemplate rest;
    private final String authorization;

    private ApiClient(TestRestTemplate rest, String authorization) {
        this.rest = rest;
        this.authorization = authorization;
    }

    /** 有効な JWT を付けるクライアント */
    public static ApiClient withJwt(TestRestTemplate rest) {
        return new ApiClient(rest, JwtTestSupport.bearer());
    }

    /** JWT を付けないクライアント */
    public static ApiClient withoutJwt(TestRestTemplate rest) {
        return new ApiClient(rest, null);
    }

    public Response get(String path) {
        return exchange(HttpMethod.GET, path, null);
    }

    public Response post(String path, String body) {
        return exchange(HttpMethod.POST, path, body);
    }

    public Response put(String path, String body) {
        return exchange(HttpMethod.PUT, path, body);
    }

    public Response delete(String path) {
        return exchange(HttpMethod.DELETE, path, null);
    }

    private Response exchange(HttpMethod method, String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        if (authorization != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorization);
        }
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        ResponseEntity<String> response = rest.exchange(path, method, new HttpEntity<>(body, headers), String.class);
        return new Response(response.getStatusCode(), response.getHeaders(), parse(response.getBody()));
    }

    private static JsonNode parse(String body) {
        if (body == null || body.isEmpty()) {
            return null;
        }
        try {
            return JSON.readTree(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("response is not JSON: " + body, e);
        }
    }

    /**
     * @param status  ステータスコード
     * @param headers レスポンスヘッダ
     * @param body    レスポンスボディ(無い場合は null)
     */
    public record Response(HttpStatusCode status, HttpHeaders headers, JsonNode body) {

        public int statusValue() {
            return status.value();
        }
    }
}
