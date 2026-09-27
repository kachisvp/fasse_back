package com.example.fasse_back.taxrate.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 消費税率の更新リクエスト(openapi.yaml: TaxRateUpdateInput)。
 * キー({@code tax_category} / {@code valid_from})はパスで指定するため持たない(ボディに含まれても無視する)。
 */
public record TaxRateUpdateRequest(
        @NotNull @Size(max = 50) String description,
        @NotNull @Digits(integer = 1, fraction = Integer.MAX_VALUE) BigDecimal rate,
        LocalDate validTo) {
}
