package com.example.fasse_back.taxrate.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.fasse_back.common.TaxCategory;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 消費税率の登録リクエスト(openapi.yaml: TaxRateInput) */
public record TaxRateRequest(
        @NotNull TaxCategory taxCategory,
        @NotNull @Size(max = 50) String description,
        @NotNull @Digits(integer = 1, fraction = Integer.MAX_VALUE) BigDecimal rate,
        @NotNull LocalDate validFrom,
        LocalDate validTo) {
}
