package com.example.fasse_back.item.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.item.entity.Item;
import com.example.fasse_back.support.MapperTest;
import com.example.fasse_back.support.TestDataLoader;

@MapperTest
class ItemMapperTest {

    @Autowired
    ItemMapper itemMapper;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        TestDataLoader.load(jdbc);
    }

    @Test
    void findAll_returnsAllIncludingInactiveOrderedById() {
        List<Item> items = itemMapper.findAll();

        assertThat(items).extracting(Item::getId).containsExactly(1L, 2L, 3L, 4L, 5L);
        assertThat(items.get(4).getIsActive()).isFalse();
    }

    @Test
    void findById_mapsAllColumns() {
        Item item = itemMapper.findById(1L);

        assertThat(item.getItemName()).isEqualTo("キャベツ");
        assertThat(item.getUnit()).isEqualTo("玉");
        assertThat(item.getStandardPrice()).isEqualByComparingTo("200");
        assertThat(item.getStandardPrice().scale()).isEqualTo(2);
        assertThat(item.getTaxCategory()).isEqualTo(TaxCategory.REDUCED);
        assertThat(item.getIsActive()).isTrue();
        assertThat(item.getCreatedAt()).isCloseTo(Instant.now(), org.assertj.core.api.Assertions.within(
                5, ChronoUnit.MINUTES));
    }

    @Test
    void findById_notFound_returnsNull() {
        assertThat(itemMapper.findById(999L)).isNull();
    }

    @Test
    void findExistingIds_returnsOnlyExistingIncludingInactive() {
        assertThat(itemMapper.findExistingIds(List.of(1L, 5L, 999L))).containsExactlyInAnyOrder(1L, 5L);
    }

    @Test
    void insert_setsGeneratedId() {
        Item item = new Item();
        item.setItemName("玉ねぎ");
        item.setUnit("kg");
        item.setTaxCategory(TaxCategory.REDUCED);
        item.setIsActive(true);

        itemMapper.insert(item);

        assertThat(item.getId()).isGreaterThan(5L);
        Item saved = itemMapper.findById(item.getId());
        assertThat(saved.getItemName()).isEqualTo("玉ねぎ");
        assertThat(saved.getStandardPrice()).isNull();
    }

    @Test
    void update_overwritesColumns() {
        Item item = itemMapper.findById(1L);
        item.setItemName("春キャベツ");
        item.setStandardPrice(new BigDecimal("250"));
        item.setIsActive(false);

        assertThat(itemMapper.update(item)).isEqualTo(1);

        Item saved = itemMapper.findById(1L);
        assertThat(saved.getItemName()).isEqualTo("春キャベツ");
        assertThat(saved.getStandardPrice()).isEqualByComparingTo("250");
        assertThat(saved.getIsActive()).isFalse();
    }

    @Test
    void deactivate_setsInactive() {
        assertThat(itemMapper.deactivate(1L)).isEqualTo(1);
        assertThat(itemMapper.findById(1L).getIsActive()).isFalse();
    }

    @Test
    void deactivate_alreadyInactive_stillCountsRow() {
        assertThat(itemMapper.deactivate(5L)).isEqualTo(1);
    }

    @Test
    void deactivate_notFound_returnsZero() {
        assertThat(itemMapper.deactivate(999L)).isZero();
    }
}
