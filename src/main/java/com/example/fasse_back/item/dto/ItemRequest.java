package com.example.fasse_back.item.dto;

import java.math.BigDecimal;

import com.example.fasse_back.common.TaxCategory;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 品目の登録・更新リクエスト(openapi.yaml: ItemInput) */
public record ItemRequest(
        @NotNull @Size(max = 100) String itemName,
        @NotNull @Size(max = 20) String unit,
        @Digits(integer = 10, fraction = Integer.MAX_VALUE) BigDecimal standardPrice,
        @NotNull TaxCategory taxCategory,
        Boolean isActive) {
}
