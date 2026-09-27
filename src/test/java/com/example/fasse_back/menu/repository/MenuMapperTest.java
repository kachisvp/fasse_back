package com.example.fasse_back.menu.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.menu.entity.Menu;
import com.example.fasse_back.support.MapperTest;
import com.example.fasse_back.support.TestDataLoader;

@MapperTest
class MenuMapperTest {

    @Autowired
    MenuMapper menuMapper;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        TestDataLoader.load(jdbc);
    }

    @Test
    void findAll_returnsAllIncludingInactiveOrderedById() {
        List<Menu> menus = menuMapper.findAll();

        assertThat(menus).extracting(Menu::getId).containsExactly(1L, 2L, 3L, 4L, 5L);
        assertThat(menus.get(4).getIsActive()).isFalse();
    }

    @Test
    void findById_mapsAllColumns() {
        Menu menu = menuMapper.findById(5L);

        assertThat(menu.getMenuName()).isEqualTo("お持ち帰り弁当");
        assertThat(menu.getCategory()).isEqualTo("テイクアウト");
        assertThat(menu.getStandardPrice()).isEqualByComparingTo("1200");
        assertThat(menu.getTaxCategory()).isEqualTo(TaxCategory.REDUCED);
        assertThat(menu.getIsActive()).isFalse();
    }

    @Test
    void findExistingIds_returnsOnlyExisting() {
        assertThat(menuMapper.findExistingIds(List.of(2L, 5L, 999L))).containsExactlyInAnyOrder(2L, 5L);
    }

    @Test
    void insertAndUpdate() {
        Menu menu = new Menu();
        menu.setMenuName("ハイボール");
        menu.setCategory("ドリンク");
        menu.setStandardPrice(new BigDecimal("500"));
        menu.setTaxCategory(TaxCategory.STANDARD);
        menu.setIsActive(true);
        menuMapper.insert(menu);

        menu.setStandardPrice(new BigDecimal("550.5"));
        assertThat(menuMapper.update(menu)).isEqualTo(1);

        assertThat(menuMapper.findById(menu.getId()).getStandardPrice()).isEqualByComparingTo("550.50");
    }

    @Test
    void deactivate_setsInactive() {
        assertThat(menuMapper.deactivate(1L)).isEqualTo(1);
        assertThat(menuMapper.findById(1L).getIsActive()).isFalse();
        assertThat(menuMapper.deactivate(999L)).isZero();
    }
}
