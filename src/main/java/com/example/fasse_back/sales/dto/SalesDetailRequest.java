package com.example.fasse_back.sales.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** 売上伝票明細の登録・更新リクエスト(openapi.yaml: SalesDetailInput) */
public record SalesDetailRequest(
        @NotNull Long menuId,
        @NotNull Integer quantity,
        @NotNull @Digits(integer = 10, fraction = Integer.MAX_VALUE) BigDecimal unitPrice,
        @NotNull @Digits(integer = 10, fraction = Integer.MAX_VALUE) BigDecimal amount,
        @NotNull @Digits(integer = 1, fraction = Integer.MAX_VALUE) BigDecimal taxRate) {
}
