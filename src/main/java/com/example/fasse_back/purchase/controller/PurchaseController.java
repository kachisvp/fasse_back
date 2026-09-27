package com.example.fasse_back.purchase.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.fasse_back.common.web.RequestParams;
import com.example.fasse_back.purchase.dto.PurchaseHeaderResponse;
import com.example.fasse_back.purchase.dto.PurchaseRequest;
import com.example.fasse_back.purchase.dto.PurchaseResponse;
import com.example.fasse_back.purchase.service.PurchaseService;

import jakarta.validation.Valid;

/** 仕入伝票 API({@code /purchases}) */
@RestController
@RequestMapping("/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @GetMapping
    public List<PurchaseHeaderResponse> list(@RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return purchaseService.findHeaders(RequestParams.dateRange(from, to));
    }

    @GetMapping("/{id}")
    public PurchaseResponse get(@PathVariable String id) {
        return purchaseService.findById(RequestParams.uuid(id));
    }

    @PostMapping
    public ResponseEntity<PurchaseResponse> create(@Valid @RequestBody PurchaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseService.create(request));
    }

    @PutMapping("/{id}")
    public PurchaseResponse update(@PathVariable String id, @Valid @RequestBody PurchaseRequest request) {
        return purchaseService.update(RequestParams.uuid(id), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        purchaseService.delete(RequestParams.uuid(id));
        return ResponseEntity.noContent().build();
    }
}
