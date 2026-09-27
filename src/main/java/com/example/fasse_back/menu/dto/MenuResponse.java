package com.example.fasse_back.menu.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.menu.entity.Menu;

/** メニューのレスポンス(openapi.yaml: Menu) */
public record MenuResponse(
        Long id,
        String menuName,
        String category,
        BigDecimal standardPrice,
        TaxCategory taxCategory,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt) {

    public static MenuResponse from(Menu menu) {
        return new MenuResponse(menu.getId(), menu.getMenuName(), menu.getCategory(), menu.getStandardPrice(),
                menu.getTaxCategory(), menu.getIsActive(), menu.getCreatedAt(), menu.getUpdatedAt());
    }
}
