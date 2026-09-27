package com.example.fasse_back.taxrate.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.taxrate.entity.TaxRate;

/** 消費税率のレスポンス(openapi.yaml: TaxRate)。{@code id} は公開しない */
public record TaxRateResponse(
        TaxCategory taxCategory,
        String description,
        BigDecimal rate,
        LocalDate validFrom,
        LocalDate validTo,
        Instant createdAt,
        Instant updatedAt) {

    public static TaxRateResponse from(TaxRate taxRate) {
        return new TaxRateResponse(taxRate.getTaxCategory(), taxRate.getDescription(), taxRate.getRate(),
                taxRate.getValidFrom(), taxRate.getValidTo(), taxRate.getCreatedAt(), taxRate.getUpdatedAt());
    }
}
