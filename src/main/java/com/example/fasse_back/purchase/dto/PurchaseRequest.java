package com.example.fasse_back.purchase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 仕入伝票の登録・更新リクエスト(openapi.yaml: PurchaseInput) */
public record PurchaseRequest(
        @NotNull Long supplierId,
        @NotNull LocalDate purchaseDate,
        LocalDate deliveryDate,
        @NotNull @Digits(integer = 10, fraction = Integer.MAX_VALUE) BigDecimal subtotal,
        @NotNull @Digits(integer = 10, fraction = Integer.MAX_VALUE) BigDecimal taxAmount,
        @NotNull @Digits(integer = 10, fraction = Integer.MAX_VALUE) BigDecimal totalAmount,
        @Size(max = 500) String remarks,
        @NotNull @Valid List<@NotNull PurchaseDetailRequest> details) {
}
