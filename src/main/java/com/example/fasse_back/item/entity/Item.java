package com.example.fasse_back.item.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.fasse_back.common.TaxCategory;

import lombok.Data;

/** 品目マスタ({@code m_item})の行 */
@Data
public class Item {
    private Long id;
    private String itemName;
    private String unit;
    private BigDecimal standardPrice;
    private TaxCategory taxCategory;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
