package com.example.fasse_back.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.fasse_back.support.ApiClient;
import com.example.fasse_back.support.ApiClient.Response;
import com.example.fasse_back.support.IntegrationTest;
import com.example.fasse_back.support.TestDataLoader;
import com.fasterxml.jackson.databind.JsonNode;

/** 伝票(仕入・売上)の CRUD を HTTP で実行する結合テスト */
@IntegrationTest
class SlipApiIntegrationTest {

    private static final String PURCHASE_BODY = """
            {"supplier_id":%d,"purchase_date":"%s","delivery_date":"2026-07-02","subtotal":2300,"tax_amount":184,
             "total_amount":2484,"remarks":"結合テスト",
             "details":[{"item_id":1,"quantity":10,"unit_price":200,"amount":2000,"tax_rate":0.08},
                        {"item_id":5,"quantity":3,"unit_price":100,"amount":300,"tax_rate":0.08}]}
            """;

    private static final String SALES_BODY = """
            {"sales_datetime":"%s","business_date":"%s","subtotal":1200,"tax_amount":120,"total_amount":1320,
             "payment_method":"CASH",
             "details":[{"menu_id":1,"quantity":2,"unit_price":600,"amount":1200,"tax_rate":0.1}]}
            """;

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
    void purchase_crud() {
        // テストデータの 2026-07-01 は 0002 まで採番済み
        Response created = api.post("/purchases", PURCHASE_BODY.formatted(1, "2026-07-01"));
        assertThat(created.statusValue()).isEqualTo(201);
        JsonNode purchase = created.body();
        String id = purchase.get("id").asText();
        assertThat(purchase.get("purchase_no").asText()).isEqualTo("PO-20260701-0003");
        assertThat(purchase.get("details")).hasSize(2);
        assertThat(purchase.get("details").get(0).get("id").asText()).matches("[0-9a-f-]{36}");
        assertThat(purchase.get("subtotal").toString()).isEqualTo("2300.00");

        // 新しい日付は 0001 から
        Response otherDay = api.post("/purchases", PURCHASE_BODY.formatted(1, "2026-08-01"));
        assertThat(otherDay.body().get("purchase_no").asText()).isEqualTo("PO-20260801-0001");

        Response list = api.get("/purchases?from=2026-07-01&to=2026-07-01");
        assertThat(list.body()).extracting(n -> n.get("purchase_no").asText())
                .containsExactly("PO-20260701-0001", "PO-20260701-0002", "PO-20260701-0003");
        assertThat(list.body().get(0).has("details")).isFalse();
        assertThat(api.get("/purchases?from=2026-07-02&to=2026-07-01").body()).isEmpty();

        // 更新: 明細は全洗い替え、日付を変えても伝票番号は変わらない、省略した任意項目は維持
        Response updated = api.put("/purchases/" + id, """
                {"supplier_id":2,"purchase_date":"2026-07-15","subtotal":1800,"tax_amount":144,"total_amount":1944,
                 "details":[{"item_id":2,"quantity":1,"unit_price":1800,"amount":1800,"tax_rate":0.08}]}
                """);
        assertThat(updated.statusValue()).isEqualTo(200);
        assertThat(updated.body().get("purchase_no").asText()).isEqualTo("PO-20260701-0003");
        assertThat(updated.body().get("purchase_date").asText()).isEqualTo("2026-07-15");
        assertThat(updated.body().get("remarks").asText()).isEqualTo("結合テスト");
        assertThat(updated.body().get("delivery_date").asText()).isEqualTo("2026-07-02");
        assertThat(updated.body().get("created_at").asText()).isEqualTo(purchase.get("created_at").asText());
        assertThat(updated.body().get("details")).hasSize(1);
        assertThat(updated.body().get("details").get(0).get("item_id").asLong()).isEqualTo(2L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_purchase_detail WHERE purchase_id = ?",
                Integer.class, id)).isEqualTo(1);

        assertThat(api.delete("/purchases/" + id).statusValue()).isEqualTo(204);
        assertThat(api.get("/purchases/" + id).statusValue()).isEqualTo(404);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_purchase_detail WHERE purchase_id = ?",
                Integer.class, id)).isZero();
    }

    @Test
    void purchase_invalidReference_returns400AndDoesNotConsumeSlipNo() {
        Response response = api.post("/purchases", PURCHASE_BODY.formatted(999, "2026-07-01")
                .replace("\"item_id\":5", "\"item_id\":999"));

        assertThat(response.statusValue()).isEqualTo(400);
        assertThat(response.body().get("message").asText())
                .isEqualTo("invalid reference: supplier_id, details[1].item_id");
        assertThat(counter("purchase_no#2026-07-01")).isEqualTo(2);
    }

    @Test
    void sales_crud() {
        Response created = api.post("/sales", SALES_BODY.formatted("2026-07-02T16:30:00Z", "2026-07-02"));
        assertThat(created.statusValue()).isEqualTo(201);
        JsonNode sales = created.body();
        String id = sales.get("id").asText();
        assertThat(sales.get("sales_no").asText()).isEqualTo("SO-20260702-0003");
        // UTC で送った日時は JST に正規化して返す(深夜の取引も営業日で採番する)
        assertThat(sales.get("sales_datetime").asText()).isEqualTo("2026-07-03T01:30:00+09:00");
        assertThat(sales.get("customer_count").asInt()).isEqualTo(1);
        assertThat(sales.get("discount_amount").toString()).isEqualTo("0.00");
        assertThat(jdbc.queryForObject("SELECT sales_datetime FROM t_sales_header WHERE id = ?", String.class, id))
                .startsWith("2026-07-03 01:30:00");

        Response list = api.get("/sales?from=2026-07-02&to=2026-07-02");
        assertThat(list.body()).extracting(n -> n.get("sales_no").asText())
                .containsExactly("SO-20260702-0001", "SO-20260702-0002", "SO-20260702-0003");

        Response updated = api.put("/sales/" + id, """
                {"sales_datetime":"2026-07-02T20:00:00+09:00","business_date":"2026-07-02","table_no":"D4",
                 "subtotal":600,"tax_amount":60,"total_amount":660,"payment_method":"QR","details":[]}
                """);
        assertThat(updated.statusValue()).isEqualTo(200);
        assertThat(updated.body().get("sales_no").asText()).isEqualTo("SO-20260702-0003");
        assertThat(updated.body().get("table_no").asText()).isEqualTo("D4");
        assertThat(updated.body().get("customer_count").asInt()).isEqualTo(1);
        assertThat(updated.body().get("details")).isEmpty();

        assertThat(api.delete("/sales/" + id).statusValue()).isEqualTo(204);
        assertThat(api.get("/sales/" + id).statusValue()).isEqualTo(404);
    }

    @Test
    void sales_invalidMenuReference_returns400() {
        Response response = api.post("/sales", SALES_BODY.formatted("2026-07-02T19:00:00+09:00", "2026-07-02")
                .replace("\"menu_id\":1", "\"menu_id\":999"));

        assertThat(response.statusValue()).isEqualTo(400);
        assertThat(response.body().get("message").asText()).isEqualTo("invalid reference: details[0].menu_id");
    }

    @Test
    void notFoundAndInvalidId() {
        assertThat(api.get("/purchases/a0000000-0000-4000-8000-000000000099").statusValue()).isEqualTo(404);
        assertThat(api.delete("/sales/c0000000-0000-4000-8000-000000000099").statusValue()).isEqualTo(404);
        Response invalid = api.get("/sales/1");
        assertThat(invalid.statusValue()).isEqualTo(400);
        assertThat(invalid.body().get("message").asText()).isEqualTo("id must be a UUID");
    }

    private Integer counter(String name) {
        return jdbc.queryForObject("SELECT value FROM t_slip_no_counter WHERE counter_name = ?", Integer.class, name);
    }
}
