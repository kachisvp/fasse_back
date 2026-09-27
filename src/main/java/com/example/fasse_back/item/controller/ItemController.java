package com.example.fasse_back.item.controller;

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
import com.example.fasse_back.item.dto.ItemRequest;
import com.example.fasse_back.item.dto.ItemResponse;
import com.example.fasse_back.item.service.ItemService;

import jakarta.validation.Valid;

/** 品目マスタ API({@code /items}) */
@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public List<ItemResponse> list() {
        return itemService.findAll();
    }

    @GetMapping("/{id}")
    public ItemResponse get(@PathVariable String id) {
        return itemService.findById(RequestParams.masterId(id));
    }

    @PostMapping
    public ResponseEntity<ItemResponse> create(@Valid @RequestBody ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.create(request));
    }

    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable String id, @Valid @RequestBody ItemRequest request) {
        return itemService.update(RequestParams.masterId(id), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        itemService.delete(RequestParams.masterId(id));
        return ResponseEntity.noContent().build();
    }
}
