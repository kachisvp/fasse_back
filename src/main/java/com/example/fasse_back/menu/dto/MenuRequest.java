package com.example.fasse_back.menu.dto;

import java.math.BigDecimal;

import com.example.fasse_back.common.TaxCategory;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** メニューの登録・更新リクエスト(openapi.yaml: MenuInput) */
public record MenuRequest(
        @NotNull @Size(max = 100) String menuName,
        @NotNull @Size(max = 50) String category,
        @NotNull @Digits(integer = 10, fraction = Integer.MAX_VALUE) BigDecimal standardPrice,
        @NotNull TaxCategory taxCategory,
        Boolean isActive) {
}
