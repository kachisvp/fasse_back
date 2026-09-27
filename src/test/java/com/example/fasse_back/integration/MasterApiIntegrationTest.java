package com.example.fasse_back.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.fasse_back.support.ApiClient;
import com.example.fasse_back.support.ApiClient.Response;
import com.example.fasse_back.support.IntegrationTest;
import com.example.fasse_back.support.TestDataLoader;
import com.fasterxml.jackson.databind.JsonNode;

/** マスタ(品目・仕入先・メニュー)と消費税率マスタの CRUD を HTTP で実行する結合テスト */
@IntegrationTest
class MasterApiIntegrationTest {

    private static final String UTC_TIMESTAMP = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z";

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JdbcTemplate jdbc;

    ApiClient api;

    @BeforeEach
    void setUp() {
        TestDataLoader.load(jdbc);
        api = ApiClient.withJwt(rest);
    }

    @Test
    void item_crud() {
        Response list = api.get("/items");
        assertThat(list.statusValue()).isEqualTo(200);
        assertThat(list.body()).hasSize(5);
        assertThat(list.body().get(0).get("standard_price").decimalValue()).isEqualByComparingTo("200.00");
        assertThat(list.body().get(0).get("standard_price").toString()).isEqualTo("200.00");

        Response created = api.post("/items", """
                {"item_name":"玉ねぎ","unit":"kg","standard_price":300,"tax_category":"REDUCED"}
                """);
        assertThat(created.statusValue()).isEqualTo(201);
        JsonNode item = created.body();
        long id = item.get("id").asLong();
        assertThat(item.get("is_active").asBoolean()).isTrue();
        assertThat(item.get("standard_price").toString()).isEqualTo("300.00");
        assertThat(item.get("created_at").asText()).matches(UTC_TIMESTAMP);
        assertThat(item.get("updated_at").asText()).matches(UTC_TIMESTAMP);

        Response updated = api.put("/items/" + id, """
                {"item_name":"新玉ねぎ","unit":"kg","tax_category":"REDUCED"}
                """);
        assertThat(updated.statusValue()).isEqualTo(200);
        assertThat(updated.body().get("item_name").asText()).isEqualTo("新玉ねぎ");
        assertThat(updated.body().get("standard_price").toString()).isEqualTo("300.00");

        assertThat(api.delete("/items/" + id).statusValue()).isEqualTo(204);
        Response deleted = api.get("/items/" + id);
        assertThat(deleted.statusValue()).isEqualTo(200);
        assertThat(deleted.body().get("is_active").asBoolean()).isFalse();

        assertThat(api.get("/items/999").statusValue()).isEqualTo(404);
        assertThat(api.delete("/items/999").statusValue()).isEqualTo(404);
    }

    @Test
    void supplier_crud() {
        assertThat(api.get("/suppliers").body()).hasSize(5);

        Response created = api.post("/suppliers", "{\"supplier_name\":\"新規仕入先\",\"email\":\"new@example.com\"}");
        assertThat(created.statusValue()).isEqualTo(201);
        long id = created.body().get("id").asLong();
        assertThat(created.body().get("postal_code").isNull()).isTrue();

        Response updated = api.put("/suppliers/" + id, "{\"supplier_name\":\"変更後\",\"is_active\":false}");
        assertThat(updated.statusValue()).isEqualTo(200);
        assertThat(updated.body().get("email").asText()).isEqualTo("new@example.com");
        assertThat(updated.body().get("is_active").asBoolean()).isFalse();

        assertThat(api.delete("/suppliers/" + id).statusValue()).isEqualTo(204);
        assertThat(api.get("/suppliers/" + id).body().get("is_active").asBoolean()).isFalse();
    }

    @Test
    void menu_crud() {
        assertThat(api.get("/menus").body()).hasSize(5);

        Response created = api.post("/menus", """
                {"menu_name":"ハイボール","category":"ドリンク","standard_price":500,"tax_category":"STANDARD",
                 "is_active":false}
                """);
        assertThat(created.statusValue()).isEqualTo(201);
        long id = created.body().get("id").asLong();
        assertThat(created.body().get("is_active").asBoolean()).isFalse();

        Response updated = api.put("/menus/" + id, """
                {"menu_name":"ハイボール","category":"ドリンク","standard_price":550,"tax_category":"STANDARD"}
                """);
        assertThat(updated.body().get("standard_price").toString()).isEqualTo("550.00");
        assertThat(updated.body().get("is_active").asBoolean()).isFalse();

        assertThat(api.delete("/menus/" + id).statusValue()).isEqualTo(204);
    }

    @Test
    void taxRate_crud() {
        Response standard = api.get("/tax-rates?tax_category=STANDARD");
        assertThat(standard.body()).hasSize(3);
        assertThat(standard.body().get(2).get("rate").toString()).isEqualTo("0.1000");
        assertThat(standard.body().get(2).get("valid_to").isNull()).isTrue();

        String body = """
                {"tax_category":"STANDARD","description":"標準税率","rate":0.12,"valid_from":"2030-04-01"}
                """;
        Response created = api.post("/tax-rates", body);
        assertThat(created.statusValue()).isEqualTo(201);
        assertThat(created.body().has("id")).isFalse();

        Response duplicate = api.post("/tax-rates", body);
        assertThat(duplicate.statusValue()).isEqualTo(409);
        assertThat(duplicate.body().get("message").asText())
                .isEqualTo("tax rate already exists: STANDARD/2030-04-01");

        Response updated = api.put("/tax-rates/STANDARD/2019-10-01",
                "{\"description\":\"標準税率\",\"rate\":0.10,\"valid_to\":\"2030-03-31\"}");
        assertThat(updated.statusValue()).isEqualTo(200);
        assertThat(updated.body().get("valid_to").asText()).isEqualTo("2030-03-31");

        Response kept = api.put("/tax-rates/STANDARD/2019-10-01", "{\"description\":\"標準\",\"rate\":0.10}");
        assertThat(kept.body().get("valid_to").asText()).isEqualTo("2030-03-31");

        assertThat(api.delete("/tax-rates/STANDARD/2030-04-01").statusValue()).isEqualTo(204);
        assertThat(api.get("/tax-rates/STANDARD/2030-04-01").statusValue()).isEqualTo(404);
    }

    @ParameterizedTest
    @ValueSource(strings = { "/items", "/suppliers", "/menus", "/tax-rates", "/purchases?from=2026-07-01&to=2026-07-31",
            "/sales?from=2026-07-01&to=2026-07-31" })
    void withoutJwt_allResourcesReturn401(String path) {
        Response response = ApiClient.withoutJwt(rest).get(path);

        assertThat(response.statusValue()).isEqualTo(401);
        assertThat(response.body().get("message").asText()).isEqualTo("Authorization header is missing or malformed");
        assertThat(response.body().get("requestId").asText())
                .isEqualTo(response.headers().getFirst("X-Request-Id"));
    }

    @Test
    void withoutJwt_writeIsRejectedAndNothingChanges() {
        Response response = ApiClient.withoutJwt(rest).post("/items",
                "{\"item_name\":\"a\",\"unit\":\"b\",\"tax_category\":\"STANDARD\"}");

        assertThat(response.statusValue()).isEqualTo(401);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM m_item", Integer.class)).isEqualTo(5);
    }

    @Test
    void errorResponse_requestIdMatchesHeader() {
        Response response = api.get("/items/999");

        assertThat(response.statusValue()).isEqualTo(404);
        assertThat(response.body().get("message").asText()).isEqualTo("Not Found");
        assertThat(response.body().get("requestId").asText()).isEqualTo(response.headers().getFirst("X-Request-Id"));
    }
}
