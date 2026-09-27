package com.example.fasse_back.sales.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

/** 売上伝票ヘッダ({@code t_sales_header})の行 */
@Data
public class SalesHeader {
    private String id;
    private String salesNo;
    /** 売上日時(JST) */
    private LocalDateTime salesDatetime;
    /** 営業日(クライアントが指定する) */
    private LocalDate businessDate;
    private String tableNo;
    private Integer customerCount;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String remarks;
    private Instant createdAt;
    private Instant updatedAt;
}
