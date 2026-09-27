package com.example.fasse_back.menu.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fasse_back.common.exception.NotFoundException;
import com.example.fasse_back.menu.dto.MenuRequest;
import com.example.fasse_back.menu.dto.MenuResponse;
import com.example.fasse_back.menu.entity.Menu;
import com.example.fasse_back.menu.repository.MenuMapper;

/** メニューマスタの業務ロジック(design.md 3.1 節) */
@Service
public class MenuService {

    private final MenuMapper menuMapper;

    public MenuService(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> findAll() {
        return menuMapper.findAll().stream().map(MenuResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MenuResponse findById(long id) {
        return MenuResponse.from(getExisting(id));
    }

    /** 登録する。{@code is_active} 省略時は true */
    @Transactional
    public MenuResponse create(MenuRequest request) {
        Menu menu = new Menu();
        menu.setMenuName(request.menuName());
        menu.setCategory(request.category());
        menu.setStandardPrice(request.standardPrice());
        menu.setTaxCategory(request.taxCategory());
        menu.setIsActive(request.isActive() == null ? Boolean.TRUE : request.isActive());
        menuMapper.insert(menu);
        return MenuResponse.from(getExisting(menu.getId()));
    }

    /** 更新する。省略した任意項目({@code is_active})は既存の値を維持する */
    @Transactional
    public MenuResponse update(long id, MenuRequest request) {
        Menu menu = getExisting(id);
        menu.setMenuName(request.menuName());
        menu.setCategory(request.category());
        menu.setStandardPrice(request.standardPrice());
        menu.setTaxCategory(request.taxCategory());
        if (request.isActive() != null) {
            menu.setIsActive(request.isActive());
        }
        menuMapper.update(menu);
        return MenuResponse.from(getExisting(id));
    }

    /** 論理削除する */
    @Transactional
    public void delete(long id) {
        if (menuMapper.deactivate(id) == 0) {
            throw notFound(id);
        }
    }

    private Menu getExisting(long id) {
        Menu menu = menuMapper.findById(id);
        if (menu == null) {
            throw notFound(id);
        }
        return menu;
    }

    private static NotFoundException notFound(long id) {
        return new NotFoundException("menu not found: id=" + id);
    }
}
