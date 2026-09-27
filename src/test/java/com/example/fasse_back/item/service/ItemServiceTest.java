package com.example.fasse_back.item.service;

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
import com.example.fasse_back.item.dto.ItemRequest;
import com.example.fasse_back.item.dto.ItemResponse;
import com.example.fasse_back.item.entity.Item;
import com.example.fasse_back.item.repository.ItemMapper;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    ItemMapper itemMapper;

    @InjectMocks
    ItemService itemService;

    private static Item existing() {
        Item item = new Item();
        item.setId(1L);
        item.setItemName("キャベツ");
        item.setUnit("玉");
        item.setStandardPrice(new BigDecimal("200.00"));
        item.setTaxCategory(TaxCategory.REDUCED);
        item.setIsActive(false);
        return item;
    }

    /** insert で id=10 を採番し、読み直しで既存行を返す */
    private void stubInsert() {
        willAnswer(inv -> {
            inv.<Item>getArgument(0).setId(10L);
            return null;
        }).given(itemMapper).insert(any());
        given(itemMapper.findById(10L)).willReturn(existing());
    }

    @Test
    void findAll_convertsToResponses() {
        given(itemMapper.findAll()).willReturn(List.of(existing()));

        assertThat(itemService.findAll()).extracting(ItemResponse::itemName).containsExactly("キャベツ");
    }

    @Test
    void findById_notFound_throws404() {
        assertThatThrownBy(() -> itemService.findById(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_defaultsIsActiveToTrue() {
        willAnswer(inv -> {
            inv.<Item>getArgument(0).setId(10L);
            return null;
        }).given(itemMapper).insert(any());
        given(itemMapper.findById(10L)).willAnswer(inv -> {
            Item item = existing();
            item.setId(10L);
            return item;
        });

        ItemResponse response = itemService.create(new ItemRequest("a", "b", null, TaxCategory.STANDARD, null));

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        then(itemMapper).should().insert(captor.capture());
        assertThat(captor.getValue().getIsActive()).isTrue();
        assertThat(captor.getValue().getItemName()).isEqualTo("a");
        // 登録後は DB から読み直した値を返す
        assertThat(response.id()).isEqualTo(10L);
    }

    @Test
    void create_keepsSpecifiedIsActive() {
        stubInsert();

        itemService.create(new ItemRequest("a", "b", null, TaxCategory.STANDARD, false));

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        then(itemMapper).should().insert(captor.capture());
        assertThat(captor.getValue().getIsActive()).isFalse();
    }

    @Test
    void update_keepsOmittedOptionalFields() {
        given(itemMapper.findById(1L)).willReturn(existing());

        itemService.update(1L, new ItemRequest("新", "kg", null, TaxCategory.STANDARD, null));

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        then(itemMapper).should().update(captor.capture());
        Item updated = captor.getValue();
        assertThat(updated.getItemName()).isEqualTo("新");
        assertThat(updated.getUnit()).isEqualTo("kg");
        assertThat(updated.getTaxCategory()).isEqualTo(TaxCategory.STANDARD);
        assertThat(updated.getStandardPrice()).isEqualByComparingTo("200");
        assertThat(updated.getIsActive()).isFalse();
    }

    @Test
    void update_overwritesSpecifiedOptionalFields() {
        given(itemMapper.findById(1L)).willReturn(existing());

        itemService.update(1L, new ItemRequest("新", "kg", new BigDecimal("300"), TaxCategory.STANDARD, true));

        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);
        then(itemMapper).should().update(captor.capture());
        assertThat(captor.getValue().getStandardPrice()).isEqualByComparingTo("300");
        assertThat(captor.getValue().getIsActive()).isTrue();
    }

    @Test
    void update_notFound_throws404() {
        assertThatThrownBy(() -> itemService.update(9L, new ItemRequest("a", "b", null, TaxCategory.EXEMPT, null)))
                .isInstanceOf(NotFoundException.class);
        then(itemMapper).should(never()).update(any());
    }

    @Test
    void delete_deactivates() {
        given(itemMapper.deactivate(1L)).willReturn(1);

        itemService.delete(1L);

        then(itemMapper).should().deactivate(1L);
    }

    @Test
    void delete_notFound_throws404() {
        given(itemMapper.deactivate(9L)).willReturn(0);

        assertThatThrownBy(() -> itemService.delete(9L)).isInstanceOf(NotFoundException.class);
    }
}
