package com.example.fasse_back.purchase.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.example.fasse_back.purchase.entity.PurchaseHeader;

/** 仕入伝票ヘッダのレスポンス(openapi.yaml: PurchaseHeader。一覧で使う) */
public record PurchaseHeaderResponse(
        String id,
        String purchaseNo,
        Long supplierId,
        LocalDate purchaseDate,
        LocalDate deliveryDate,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String remarks,
        Instant createdAt,
        Instant updatedAt) {

    public static PurchaseHeaderResponse from(PurchaseHeader header) {
        return new PurchaseHeaderResponse(header.getId(), header.getPurchaseNo(), header.getSupplierId(),
                header.getPurchaseDate(), header.getDeliveryDate(), header.getSubtotal(), header.getTaxAmount(),
                header.getTotalAmount(), header.getRemarks(), header.getCreatedAt(), header.getUpdatedAt());
    }
}
