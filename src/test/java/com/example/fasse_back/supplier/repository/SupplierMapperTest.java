package com.example.fasse_back.supplier.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.fasse_back.supplier.entity.Supplier;
import com.example.fasse_back.support.MapperTest;
import com.example.fasse_back.support.TestDataLoader;

@MapperTest
class SupplierMapperTest {

    @Autowired
    SupplierMapper supplierMapper;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        TestDataLoader.load(jdbc);
    }

    @Test
    void findAll_returnsAllIncludingInactiveOrderedById() {
        List<Supplier> suppliers = supplierMapper.findAll();

        assertThat(suppliers).extracting(Supplier::getId).containsExactly(1L, 2L, 3L, 4L, 5L);
        assertThat(suppliers.get(4).getIsActive()).isFalse();
    }

    @Test
    void findById_mapsAllColumns() {
        Supplier supplier = supplierMapper.findById(1L);

        assertThat(supplier.getSupplierName()).isEqualTo("山田青果");
        assertThat(supplier.getPostalCode()).isEqualTo("100-0001");
        assertThat(supplier.getAddress()).isEqualTo("東京都千代田区千代田1-1");
        assertThat(supplier.getPhoneNumber()).isEqualTo("03-1111-1111");
        assertThat(supplier.getEmail()).isEqualTo("yamada@example.com");
        assertThat(supplier.getCreatedAt()).isNotNull();
        assertThat(supplier.getUpdatedAt()).isNotNull();
    }

    @Test
    void existsById_includesInactive() {
        assertThat(supplierMapper.existsById(5L)).isTrue();
        assertThat(supplierMapper.existsById(999L)).isFalse();
    }

    @Test
    void insert_setsGeneratedId() {
        Supplier supplier = new Supplier();
        supplier.setSupplierName("新規仕入先");
        supplier.setIsActive(true);

        supplierMapper.insert(supplier);

        Supplier saved = supplierMapper.findById(supplier.getId());
        assertThat(saved.getSupplierName()).isEqualTo("新規仕入先");
        assertThat(saved.getEmail()).isNull();
    }

    @Test
    void update_overwritesColumns() {
        Supplier supplier = supplierMapper.findById(4L);
        supplier.setEmail("sato@example.com");

        assertThat(supplierMapper.update(supplier)).isEqualTo(1);

        assertThat(supplierMapper.findById(4L).getEmail()).isEqualTo("sato@example.com");
    }

    @Test
    void deactivate_setsInactive() {
        assertThat(supplierMapper.deactivate(1L)).isEqualTo(1);
        assertThat(supplierMapper.findById(1L).getIsActive()).isFalse();
        assertThat(supplierMapper.deactivate(999L)).isZero();
    }
}
