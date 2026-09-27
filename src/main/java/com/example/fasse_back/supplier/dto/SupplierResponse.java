package com.example.fasse_back.supplier.dto;

import java.time.Instant;

import com.example.fasse_back.supplier.entity.Supplier;

/** 仕入先のレスポンス(openapi.yaml: Supplier) */
public record SupplierResponse(
        Long id,
        String supplierName,
        String postalCode,
        String address,
        String phoneNumber,
        String email,
        Boolean isActive,
        Instant createdAt,
        Instant updatedAt) {

    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(supplier.getId(), supplier.getSupplierName(), supplier.getPostalCode(),
                supplier.getAddress(), supplier.getPhoneNumber(), supplier.getEmail(), supplier.getIsActive(),
                supplier.getCreatedAt(), supplier.getUpdatedAt());
    }
}
