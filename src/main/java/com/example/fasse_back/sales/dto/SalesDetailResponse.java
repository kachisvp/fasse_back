package com.example.fasse_back.sales.dto;

import java.math.BigDecimal;

import com.example.fasse_back.sales.entity.SalesDetail;

/** 売上伝票明細のレスポンス(openapi.yaml: SalesDetail) */
public record SalesDetailResponse(
        String id,
        Long menuId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        BigDecimal taxRate) {

    public static SalesDetailResponse from(SalesDetail detail) {
        return new SalesDetailResponse(detail.getId(), detail.getMenuId(), detail.getQuantity(),
                detail.getUnitPrice(), detail.getAmount(), detail.getTaxRate());
    }
}
