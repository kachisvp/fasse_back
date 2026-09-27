package com.example.fasse_back.supplier.entity;

import java.time.Instant;

import lombok.Data;

/** 仕入先マスタ({@code m_supplier})の行 */
@Data
public class Supplier {
    private Long id;
    private String supplierName;
    private String postalCode;
    private String address;
    private String phoneNumber;
    private String email;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}
