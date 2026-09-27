package com.example.fasse_back.supplier.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fasse_back.common.exception.NotFoundException;
import com.example.fasse_back.supplier.dto.SupplierRequest;
import com.example.fasse_back.supplier.dto.SupplierResponse;
import com.example.fasse_back.supplier.entity.Supplier;
import com.example.fasse_back.supplier.repository.SupplierMapper;

/** 仕入先マスタの業務ロジック(design.md 3.1 節) */
@Service
public class SupplierService {

    private final SupplierMapper supplierMapper;

    public SupplierService(SupplierMapper supplierMapper) {
        this.supplierMapper = supplierMapper;
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> findAll() {
        return supplierMapper.findAll().stream().map(SupplierResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SupplierResponse findById(long id) {
        return SupplierResponse.from(getExisting(id));
    }

    /** 登録する。{@code is_active} 省略時は true */
    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        Supplier supplier = new Supplier();
        supplier.setSupplierName(request.supplierName());
        supplier.setPostalCode(request.postalCode());
        supplier.setAddress(request.address());
        supplier.setPhoneNumber(request.phoneNumber());
        supplier.setEmail(request.email());
        supplier.setIsActive(request.isActive() == null ? Boolean.TRUE : request.isActive());
        supplierMapper.insert(supplier);
        return SupplierResponse.from(getExisting(supplier.getId()));
    }

    /** 更新する。省略した任意項目は既存の値を維持する */
    @Transactional
    public SupplierResponse update(long id, SupplierRequest request) {
        Supplier supplier = getExisting(id);
        supplier.setSupplierName(request.supplierName());
        if (request.postalCode() != null) {
            supplier.setPostalCode(request.postalCode());
        }
        if (request.address() != null) {
            supplier.setAddress(request.address());
        }
        if (request.phoneNumber() != null) {
            supplier.setPhoneNumber(request.phoneNumber());
        }
        if (request.email() != null) {
            supplier.setEmail(request.email());
        }
        if (request.isActive() != null) {
            supplier.setIsActive(request.isActive());
        }
        supplierMapper.update(supplier);
        return SupplierResponse.from(getExisting(id));
    }

    /** 論理削除する */
    @Transactional
    public void delete(long id) {
        if (supplierMapper.deactivate(id) == 0) {
            throw notFound(id);
        }
    }

    private Supplier getExisting(long id) {
        Supplier supplier = supplierMapper.findById(id);
        if (supplier == null) {
            throw notFound(id);
        }
        return supplier;
    }

    private static NotFoundException notFound(long id) {
        return new NotFoundException("supplier not found: id=" + id);
    }
}
