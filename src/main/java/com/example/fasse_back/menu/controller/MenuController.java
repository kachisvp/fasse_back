package com.example.fasse_back.menu.controller;

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
import com.example.fasse_back.menu.dto.MenuRequest;
import com.example.fasse_back.menu.dto.MenuResponse;
import com.example.fasse_back.menu.service.MenuService;

import jakarta.validation.Valid;

/** メニューマスタ API({@code /menus}) */
@RestController
@RequestMapping("/menus")
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public List<MenuResponse> list() {
        return menuService.findAll();
    }

    @GetMapping("/{id}")
    public MenuResponse get(@PathVariable String id) {
        return menuService.findById(RequestParams.masterId(id));
    }

    @PostMapping
    public ResponseEntity<MenuResponse> create(@Valid @RequestBody MenuRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(menuService.create(request));
    }

    @PutMapping("/{id}")
    public MenuResponse update(@PathVariable String id, @Valid @RequestBody MenuRequest request) {
        return menuService.update(RequestParams.masterId(id), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        menuService.delete(RequestParams.masterId(id));
        return ResponseEntity.noContent().build();
    }
}
