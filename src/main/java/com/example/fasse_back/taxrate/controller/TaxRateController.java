package com.example.fasse_back.taxrate.controller;

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

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.common.web.RequestParams;
import com.example.fasse_back.taxrate.dto.TaxRateRequest;
import com.example.fasse_back.taxrate.dto.TaxRateResponse;
import com.example.fasse_back.taxrate.dto.TaxRateUpdateRequest;
import com.example.fasse_back.taxrate.service.TaxRateService;

import jakarta.validation.Valid;

/** 消費税率マスタ API({@code /tax-rates}) */
@RestController
@RequestMapping("/tax-rates")
public class TaxRateController {

    private final TaxRateService taxRateService;

    public TaxRateController(TaxRateService taxRateService) {
        this.taxRateService = taxRateService;
    }

    @GetMapping
    public List<TaxRateResponse> list(@RequestParam(name = "tax_category", required = false) String taxCategory) {
        TaxCategory category = taxCategory == null ? null : RequestParams.taxCategory(taxCategory, "tax_category");
        return taxRateService.findAll(category);
    }

    @GetMapping("/{taxCategory}/{validFrom}")
    public TaxRateResponse get(@PathVariable String taxCategory, @PathVariable String validFrom) {
        return taxRateService.findByKey(RequestParams.taxCategory(taxCategory, "taxCategory"),
                RequestParams.validFrom(validFrom));
    }

    @PostMapping
    public ResponseEntity<TaxRateResponse> create(@Valid @RequestBody TaxRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taxRateService.create(request));
    }

    @PutMapping("/{taxCategory}/{validFrom}")
    public TaxRateResponse update(@PathVariable String taxCategory, @PathVariable String validFrom,
            @Valid @RequestBody TaxRateUpdateRequest request) {
        return taxRateService.update(RequestParams.taxCategory(taxCategory, "taxCategory"),
                RequestParams.validFrom(validFrom), request);
    }

    @DeleteMapping("/{taxCategory}/{validFrom}")
    public ResponseEntity<Void> delete(@PathVariable String taxCategory, @PathVariable String validFrom) {
        taxRateService.delete(RequestParams.taxCategory(taxCategory, "taxCategory"),
                RequestParams.validFrom(validFrom));
        return ResponseEntity.noContent().build();
    }
}
