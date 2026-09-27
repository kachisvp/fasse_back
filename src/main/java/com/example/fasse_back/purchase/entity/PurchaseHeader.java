package com.example.fasse_back.purchase.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import lombok.Data;

/** 仕入伝票ヘッダ({@code t_purchase_header})の行 */
@Data
public class PurchaseHeader {
    private String id;
    private String purchaseNo;
    private Long supplierId;
    private LocalDate purchaseDate;
    private LocalDate deliveryDate;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String remarks;
    private Instant createdAt;
    private Instant updatedAt;
}
