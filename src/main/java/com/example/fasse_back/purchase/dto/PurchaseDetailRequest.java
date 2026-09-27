package com.example.fasse_back.purchase.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** 仕入伝票明細の登録・更新リクエスト(openapi.yaml: PurchaseDetailInput) */
public record PurchaseDetailRequest(
        @NotNull Long itemId,
        @NotNull @Digits(integer = 8, fraction = Integer.MAX_VALUE) BigDecimal quantity,
        @NotNull @Digits(integer = 10, fraction = Integer.MAX_VALUE) BigDecimal unitPrice,
        @NotNull @Digits(integer = 10, fraction = Integer.MAX_VALUE) BigDecimal amount,
        @NotNull @Digits(integer = 1, fraction = Integer.MAX_VALUE) BigDecimal taxRate) {
}
