package com.example.fasse_back.item.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fasse_back.common.exception.NotFoundException;
import com.example.fasse_back.item.dto.ItemRequest;
import com.example.fasse_back.item.dto.ItemResponse;
import com.example.fasse_back.item.entity.Item;
import com.example.fasse_back.item.repository.ItemMapper;

/** 品目マスタの業務ロジック(design.md 3.1 節) */
@Service
public class ItemService {

    private final ItemMapper itemMapper;

    public ItemService(ItemMapper itemMapper) {
        this.itemMapper = itemMapper;
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> findAll() {
        return itemMapper.findAll().stream().map(ItemResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse findById(long id) {
        return ItemResponse.from(getExisting(id));
    }

    /** 登録する。{@code is_active} 省略時は true */
    @Transactional
    public ItemResponse create(ItemRequest request) {
        Item item = new Item();
        item.setItemName(request.itemName());
        item.setUnit(request.unit());
        item.setStandardPrice(request.standardPrice());
        item.setTaxCategory(request.taxCategory());
        item.setIsActive(request.isActive() == null ? Boolean.TRUE : request.isActive());
        itemMapper.insert(item);
        return ItemResponse.from(getExisting(item.getId()));
    }

    /** 更新する。省略した任意項目は既存の値を維持する */
    @Transactional
    public ItemResponse update(long id, ItemRequest request) {
        Item item = getExisting(id);
        item.setItemName(request.itemName());
        item.setUnit(request.unit());
        item.setTaxCategory(request.taxCategory());
        if (request.standardPrice() != null) {
            item.setStandardPrice(request.standardPrice());
        }
        if (request.isActive() != null) {
            item.setIsActive(request.isActive());
        }
        itemMapper.update(item);
        return ItemResponse.from(getExisting(id));
    }

    /** 論理削除する */
    @Transactional
    public void delete(long id) {
        if (itemMapper.deactivate(id) == 0) {
            throw notFound(id);
        }
    }

    private Item getExisting(long id) {
        Item item = itemMapper.findById(id);
        if (item == null) {
            throw notFound(id);
        }
        return item;
    }

    private static NotFoundException notFound(long id) {
        return new NotFoundException("item not found: id=" + id);
    }
}
