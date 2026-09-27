package com.example.fasse_back.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.example.fasse_back.purchase.repository.PurchaseMapper;
import com.example.fasse_back.sales.repository.SalesMapper;
import com.example.fasse_back.support.ApiClient;
import com.example.fasse_back.support.ApiClient.Response;
import com.example.fasse_back.support.IntegrationTest;
import com.example.fasse_back.support.TestDataLoader;

/**
 * 明細の登録に失敗した場合に、ヘッダ・明細・採番が残らないことの結合テスト(design.md 10.1 節)。
 * 伝票 Mapper を Spy にし、明細の登録メソッドだけが例外を投げるようにする。
 */
@IntegrationTest
class TransactionRollbackIntegrationTest {

    private static final String PURCHASE_ID = "a0000000-0000-4000-8000-000000000001";
    private static final String SALES_ID = "c0000000-0000-4000-8000-000000000002";

    @Autowired
    TestRestTemplate rest;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoSpyBean
    PurchaseMapper purchaseMapper;

    @MockitoSpyBean
    SalesMapper salesMapper;

    ApiClient api;

    @BeforeEach
    void setUp() {
        TestDataLoader.load(jdbc);
        api = ApiClient.withJwt(rest);
        willThrow(new IllegalStateException("simulated detail insert failure")).given(purchaseMapper)
                .insertDetails(anyList());
        willThrow(new IllegalStateException("simulated detail insert failure")).given(salesMapper)
                .insertDetails(anyList());
    }

    @Test
    void purchasePost_detailFailure_rollsBackHeaderDetailsAndSlipNo() {
        Response response = api.post("/purchases", """
                {"supplier_id":1,"purchase_date":"2026-07-01","subtotal":200,"tax_amount":16,"total_amount":216,
                 "details":[{"item_id":1,"quantity":1,"unit_price":200,"amount":200,"tax_rate":0.08}]}
                """);

        assertInternalServerError(response);
        assertThat(count("t_purchase_header")).isEqualTo(5);
        assertThat(count("t_purchase_detail")).isEqualTo(5);
        assertThat(counter("purchase_no#2026-07-01")).isEqualTo(2);
    }

    @Test
    void purchasePut_detailFailure_keepsPreviousState() {
        Response response = api.put("/purchases/" + PURCHASE_ID, """
                {"supplier_id":2,"purchase_date":"2026-07-05","subtotal":1,"tax_amount":0,"total_amount":1,
                 "remarks":"変更","details":[{"item_id":2,"quantity":1,"unit_price":1,"amount":1,"tax_rate":0.08}]}
                """);

        assertInternalServerError(response);
        assertThat(jdbc.queryForMap("SELECT supplier_id, purchase_date, remarks FROM t_purchase_header WHERE id = ?",
                PURCHASE_ID))
                .containsEntry("supplier_id", 1L)
                .containsEntry("remarks", null);
        assertThat(jdbc.queryForObject("SELECT item_id FROM t_purchase_detail WHERE purchase_id = ?", Long.class,
                PURCHASE_ID)).isEqualTo(1L);
    }

    @Test
    void salesPost_detailFailure_rollsBackHeaderDetailsAndSlipNo() {
        Response response = api.post("/sales", """
                {"sales_datetime":"2026-07-01T19:00:00+09:00","business_date":"2026-07-01","subtotal":600,
                 "tax_amount":60,"total_amount":660,"payment_method":"CASH",
                 "details":[{"menu_id":1,"quantity":1,"unit_price":600,"amount":600,"tax_rate":0.1}]}
                """);

        assertInternalServerError(response);
        assertThat(count("t_sales_header")).isEqualTo(5);
        assertThat(count("t_sales_detail")).isEqualTo(5);
        assertThat(counter("sales_no#2026-07-01")).isEqualTo(3);
    }

    @Test
    void salesPut_detailFailure_keepsPreviousDetails() {
        Response response = api.put("/sales/" + SALES_ID, """
                {"sales_datetime":"2026-07-01T20:15:00+09:00","business_date":"2026-07-01","subtotal":600,
                 "tax_amount":60,"total_amount":660,"payment_method":"QR",
                 "details":[{"menu_id":1,"quantity":1,"unit_price":600,"amount":600,"tax_rate":0.1}]}
                """);

        assertInternalServerError(response);
        assertThat(jdbc.queryForObject("SELECT payment_method FROM t_sales_header WHERE id = ?", String.class,
                SALES_ID)).isEqualTo("CARD");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM t_sales_detail WHERE sales_id = ?", Integer.class,
                SALES_ID)).isEqualTo(2);
    }

    /** 500 は固定文言で返し、内部の例外メッセージを含めない */
    private static void assertInternalServerError(Response response) {
        assertThat(response.statusValue()).isEqualTo(500);
        assertThat(response.body().get("message").asText()).isEqualTo("Internal Server Error");
        assertThat(response.body().get("requestId").asText()).isEqualTo(response.headers().getFirst("X-Request-Id"));
        assertThat(response.body().toString()).doesNotContain("simulated");
    }

    private Integer count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private Integer counter(String name) {
        return jdbc.queryForObject("SELECT value FROM t_slip_no_counter WHERE counter_name = ?", Integer.class, name);
    }
}
