package com.example.fasse_back.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 売上伝票の登録・更新リクエスト(openapi.yaml: SalesInput)。
 * {@code sales_datetime} はタイムゾーンのオフセットを必須とする(例: 2026-07-12T19:30:00+09:00)。
 */
public record SalesRequest(
        @NotNull OffsetDateTime salesDatetime,
        @NotNull LocalDate businessDate,
        @Size(max = 10) String tableNo,
        Integer customerCount,
        @NotNull @Digits(integer = 10, fraction = Integer.MAX_VALUE) BigDecimal subtotal,
        @NotNull @Digits(integer = 10, fraction = Integer.MAX_VALUE) BigDecimal taxAmount,
        @Digits(integer = 10, fraction = Integer.MAX_VALUE) BigDecimal discountAmount,
        @NotNull @Digits(integer = 10, fraction = Integer.MAX_VALUE) BigDecimal totalAmount,
        @NotNull @Size(max = 20) String paymentMethod,
        @Size(max = 500) String remarks,
        @NotNull @Valid List<@NotNull SalesDetailRequest> details) {
}
