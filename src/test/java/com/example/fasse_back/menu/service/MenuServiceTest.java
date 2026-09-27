package com.example.fasse_back.menu.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.common.exception.NotFoundException;
import com.example.fasse_back.menu.dto.MenuRequest;
import com.example.fasse_back.menu.dto.MenuResponse;
import com.example.fasse_back.menu.entity.Menu;
import com.example.fasse_back.menu.repository.MenuMapper;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    MenuMapper menuMapper;

    @InjectMocks
    MenuService menuService;

    private static Menu existing() {
        Menu menu = new Menu();
        menu.setId(1L);
        menu.setMenuName("生ビール");
        menu.setCategory("ドリンク");
        menu.setStandardPrice(new BigDecimal("600.00"));
        menu.setTaxCategory(TaxCategory.STANDARD);
        menu.setIsActive(false);
        return menu;
    }

    private static MenuRequest request(Boolean isActive) {
        return new MenuRequest("ハイボール", "ドリンク", new BigDecimal("500"), TaxCategory.STANDARD, isActive);
    }

    /** insert で id=10 を採番し、読み直しで既存行を返す */
    private void stubInsert() {
        willAnswer(inv -> {
            inv.<Menu>getArgument(0).setId(10L);
            return null;
        }).given(menuMapper).insert(any());
        given(menuMapper.findById(10L)).willReturn(existing());
    }

    @Test
    void findAll_convertsToResponses() {
        given(menuMapper.findAll()).willReturn(List.of(existing()));

        assertThat(menuService.findAll()).extracting(MenuResponse::menuName).containsExactly("生ビール");
    }

    @Test
    void findById_notFound_throws404() {
        assertThatThrownBy(() -> menuService.findById(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_defaultsIsActiveToTrue() {
        stubInsert();

        menuService.create(request(null));

        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        then(menuMapper).should().insert(captor.capture());
        assertThat(captor.getValue().getIsActive()).isTrue();
        assertThat(captor.getValue().getStandardPrice()).isEqualByComparingTo("500");
    }

    @Test
    void create_keepsSpecifiedIsActive() {
        stubInsert();

        menuService.create(request(false));

        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        then(menuMapper).should().insert(captor.capture());
        assertThat(captor.getValue().getIsActive()).isFalse();
    }

    @Test
    void update_keepsOmittedIsActive() {
        given(menuMapper.findById(1L)).willReturn(existing());

        menuService.update(1L, request(null));

        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        then(menuMapper).should().update(captor.capture());
        Menu updated = captor.getValue();
        assertThat(updated.getMenuName()).isEqualTo("ハイボール");
        assertThat(updated.getStandardPrice()).isEqualByComparingTo("500");
        assertThat(updated.getIsActive()).isFalse();
    }

    @Test
    void update_overwritesSpecifiedIsActive() {
        given(menuMapper.findById(1L)).willReturn(existing());

        menuService.update(1L, request(true));

        ArgumentCaptor<Menu> captor = ArgumentCaptor.forClass(Menu.class);
        then(menuMapper).should().update(captor.capture());
        assertThat(captor.getValue().getIsActive()).isTrue();
    }

    @Test
    void update_notFound_throws404() {
        assertThatThrownBy(() -> menuService.update(9L, request(null))).isInstanceOf(NotFoundException.class);
        then(menuMapper).should(never()).update(any());
    }

    @Test
    void delete_notFound_throws404() {
        given(menuMapper.deactivate(9L)).willReturn(0);

        assertThatThrownBy(() -> menuService.delete(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_deactivates() {
        given(menuMapper.deactivate(1L)).willReturn(1);

        menuService.delete(1L);

        then(menuMapper).should().deactivate(1L);
    }
}
