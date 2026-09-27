package com.example.fasse_back.purchase.entity;

import java.math.BigDecimal;

import lombok.Data;

/** 仕入伝票明細({@code t_purchase_detail})の行 */
@Data
public class PurchaseDetail {
    private String id;
    private String purchaseId;
    private Long itemId;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    /** 登録時点の税率のスナップショット */
    private BigDecimal taxRate;
}
