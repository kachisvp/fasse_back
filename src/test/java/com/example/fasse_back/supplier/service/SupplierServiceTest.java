package com.example.fasse_back.supplier.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.fasse_back.common.exception.NotFoundException;
import com.example.fasse_back.supplier.dto.SupplierRequest;
import com.example.fasse_back.supplier.dto.SupplierResponse;
import com.example.fasse_back.supplier.entity.Supplier;
import com.example.fasse_back.supplier.repository.SupplierMapper;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    SupplierMapper supplierMapper;

    @InjectMocks
    SupplierService supplierService;

    private static Supplier existing() {
        Supplier supplier = new Supplier();
        supplier.setId(1L);
        supplier.setSupplierName("山田青果");
        supplier.setPostalCode("100-0001");
        supplier.setAddress("東京都");
        supplier.setPhoneNumber("03-1111-1111");
        supplier.setEmail("a@example.com");
        supplier.setIsActive(true);
        return supplier;
    }

    /** insert で id=10 を採番し、読み直しで既存行を返す */
    private void stubInsert() {
        willAnswer(inv -> {
            inv.<Supplier>getArgument(0).setId(10L);
            return null;
        }).given(supplierMapper).insert(any());
        given(supplierMapper.findById(10L)).willReturn(existing());
    }

    @Test
    void findAll_convertsToResponses() {
        given(supplierMapper.findAll()).willReturn(List.of(existing()));

        assertThat(supplierService.findAll()).extracting(SupplierResponse::supplierName).containsExactly("山田青果");
    }

    @Test
    void findById_notFound_throws404() {
        assertThatThrownBy(() -> supplierService.findById(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_defaultsIsActiveToTrue() {
        stubInsert();

        supplierService.create(new SupplierRequest("新規", null, null, null, null, null));

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        then(supplierMapper).should().insert(captor.capture());
        assertThat(captor.getValue().getIsActive()).isTrue();
        assertThat(captor.getValue().getSupplierName()).isEqualTo("新規");
    }

    @Test
    void create_keepsSpecifiedIsActive() {
        stubInsert();

        supplierService.create(new SupplierRequest("新規", null, null, null, null, false));

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        then(supplierMapper).should().insert(captor.capture());
        assertThat(captor.getValue().getIsActive()).isFalse();
    }

    @Test
    void update_keepsOmittedOptionalFields() {
        given(supplierMapper.findById(1L)).willReturn(existing());

        supplierService.update(1L, new SupplierRequest("山田青果店", null, null, null, null, null));

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        then(supplierMapper).should().update(captor.capture());
        Supplier updated = captor.getValue();
        assertThat(updated.getSupplierName()).isEqualTo("山田青果店");
        assertThat(updated.getPostalCode()).isEqualTo("100-0001");
        assertThat(updated.getAddress()).isEqualTo("東京都");
        assertThat(updated.getPhoneNumber()).isEqualTo("03-1111-1111");
        assertThat(updated.getEmail()).isEqualTo("a@example.com");
        assertThat(updated.getIsActive()).isTrue();
    }

    @Test
    void update_overwritesSpecifiedOptionalFields() {
        given(supplierMapper.findById(1L)).willReturn(existing());

        supplierService.update(1L, new SupplierRequest("a", "1", "2", "3", "4", false));

        ArgumentCaptor<Supplier> captor = ArgumentCaptor.forClass(Supplier.class);
        then(supplierMapper).should().update(captor.capture());
        Supplier updated = captor.getValue();
        assertThat(updated.getPostalCode()).isEqualTo("1");
        assertThat(updated.getAddress()).isEqualTo("2");
        assertThat(updated.getPhoneNumber()).isEqualTo("3");
        assertThat(updated.getEmail()).isEqualTo("4");
        assertThat(updated.getIsActive()).isFalse();
    }

    @Test
    void update_notFound_throws404() {
        assertThatThrownBy(() -> supplierService.update(9L, new SupplierRequest("a", null, null, null, null, null)))
                .isInstanceOf(NotFoundException.class);
        then(supplierMapper).should(never()).update(any());
    }

    @Test
    void delete_notFound_throws404() {
        given(supplierMapper.deactivate(9L)).willReturn(0);

        assertThatThrownBy(() -> supplierService.delete(9L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_deactivates() {
        given(supplierMapper.deactivate(1L)).willReturn(1);

        supplierService.delete(1L);

        then(supplierMapper).should().deactivate(1L);
    }
}
