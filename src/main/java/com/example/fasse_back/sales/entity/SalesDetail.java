package com.example.fasse_back.sales.entity;

import java.math.BigDecimal;

import lombok.Data;

/** 売上伝票明細({@code t_sales_detail})の行 */
@Data
public class SalesDetail {
    private String id;
    private String salesId;
    private Long menuId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    /** 登録時点の税率のスナップショット */
    private BigDecimal taxRate;
}
