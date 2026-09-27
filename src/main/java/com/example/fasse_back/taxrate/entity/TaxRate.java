package com.example.fasse_back.taxrate.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.example.fasse_back.common.TaxCategory;

import lombok.Data;

/** 消費税率マスタ({@code m_tax_rate})の行。API 上のキーは {@code tax_category} + {@code valid_from} */
@Data
public class TaxRate {
    private Long id;
    private TaxCategory taxCategory;
    private String description;
    private BigDecimal rate;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Instant createdAt;
    private Instant updatedAt;
}
