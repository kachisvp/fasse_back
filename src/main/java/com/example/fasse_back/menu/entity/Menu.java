package com.example.fasse_back.menu.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.example.fasse_back.common.TaxCategory;

import lombok.Data;

/** メニューマスタ({@code m_menu})の行 */
@Data
public class Menu {
    private Long id;
    private String menuName;
    private String category;
    private BigDecimal standardPrice;
    private TaxCategory taxCategory;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
