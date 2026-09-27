package com.example.fasse_back.supplier.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 仕入先の登録・更新リクエスト(openapi.yaml: SupplierInput) */
public record SupplierRequest(
        @NotNull @Size(max = 100) String supplierName,
        @Size(max = 10) String postalCode,
        @Size(max = 255) String address,
        @Size(max = 30) String phoneNumber,
        @Size(max = 255) String email,
        Boolean isActive) {
}
