package com.example.fasse_back.sales.controller;

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
import com.example.fasse_back.sales.dto.SalesHeaderResponse;
import com.example.fasse_back.sales.dto.SalesRequest;
import com.example.fasse_back.sales.dto.SalesResponse;
import com.example.fasse_back.sales.service.SalesService;

import jakarta.validation.Valid;

/** 売上伝票 API({@code /sales}) */
@RestController
@RequestMapping("/sales")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    @GetMapping
    public List<SalesHeaderResponse> list(@RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        return salesService.findHeaders(RequestParams.dateRange(from, to));
    }

    @GetMapping("/{id}")
    public SalesResponse get(@PathVariable String id) {
        return salesService.findById(RequestParams.uuid(id));
    }

    @PostMapping
    public ResponseEntity<SalesResponse> create(@Valid @RequestBody SalesRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salesService.create(request));
    }

    @PutMapping("/{id}")
    public SalesResponse update(@PathVariable String id, @Valid @RequestBody SalesRequest request) {
        return salesService.update(RequestParams.uuid(id), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        salesService.delete(RequestParams.uuid(id));
        return ResponseEntity.noContent().build();
    }
}
