package com.example.fasse_back.item.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.item.entity.Item;

/** 品目のレスポンス(openapi.yaml: Item) */
public record ItemResponse(
        Long id,
        String itemName,
        String unit,
        BigDecimal standardPrice,
        TaxCategory taxCategory,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt) {

    public static ItemResponse from(Item item) {
        return new ItemResponse(item.getId(), item.getItemName(), item.getUnit(), item.getStandardPrice(),
                item.getTaxCategory(), item.getIsActive(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
