package com.example.fasse_back.purchase.dto;

import java.math.BigDecimal;

import com.example.fasse_back.purchase.entity.PurchaseDetail;

/** 仕入伝票明細のレスポンス(openapi.yaml: PurchaseDetail) */
public record PurchaseDetailResponse(
        String id,
        Long itemId,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        BigDecimal taxRate) {

    public static PurchaseDetailResponse from(PurchaseDetail detail) {
        return new PurchaseDetailResponse(detail.getId(), detail.getItemId(), detail.getQuantity(),
                detail.getUnitPrice(), detail.getAmount(), detail.getTaxRate());
    }
}
