package com.example.fasse_back.supplier.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fasse_back.common.web.RequestParams;
import com.example.fasse_back.supplier.dto.SupplierRequest;
import com.example.fasse_back.supplier.dto.SupplierResponse;
import com.example.fasse_back.supplier.service.SupplierService;

import jakarta.validation.Valid;

/** 仕入先マスタ API({@code /suppliers}) */
@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    public List<SupplierResponse> list() {
        return supplierService.findAll();
    }

    @GetMapping("/{id}")
    public SupplierResponse get(@PathVariable String id) {
        return supplierService.findById(RequestParams.masterId(id));
    }

    @PostMapping
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.create(request));
    }

    @PutMapping("/{id}")
    public SupplierResponse update(@PathVariable String id, @Valid @RequestBody SupplierRequest request) {
        return supplierService.update(RequestParams.masterId(id), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        supplierService.delete(RequestParams.masterId(id));
        return ResponseEntity.noContent().build();
    }
}
