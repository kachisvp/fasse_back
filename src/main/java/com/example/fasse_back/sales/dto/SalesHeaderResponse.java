package com.example.fasse_back.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.example.fasse_back.sales.entity.SalesHeader;
import com.example.fasse_back.sales.service.SalesService;

/** 売上伝票ヘッダのレスポンス(openapi.yaml: SalesHeader。一覧で使う) */
public record SalesHeaderResponse(
        String id,
        String salesNo,
        OffsetDateTime salesDatetime,
        LocalDate businessDate,
        String tableNo,
        Integer customerCount,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String paymentMethod,
        String remarks,
        Instant createdAt,
        Instant updatedAt) {

    /** {@code sales_datetime} は JST({@code +09:00})で返す */
    public static SalesHeaderResponse from(SalesHeader header) {
        return new SalesHeaderResponse(header.getId(), header.getSalesNo(),
                header.getSalesDatetime().atOffset(SalesService.JST), header.getBusinessDate(), header.getTableNo(),
                header.getCustomerCount(), header.getSubtotal(), header.getTaxAmount(), header.getDiscountAmount(),
                header.getTotalAmount(), header.getPaymentMethod(), header.getRemarks(), header.getCreatedAt(),
                header.getUpdatedAt());
    }
}
