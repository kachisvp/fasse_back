package com.example.fasse_back.taxrate.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.support.MapperTest;
import com.example.fasse_back.support.TestDataLoader;
import com.example.fasse_back.taxrate.entity.TaxRate;

@MapperTest
class TaxRateMapperTest {

    private static final LocalDate REFORM_2019 = LocalDate.of(2019, 10, 1);

    @Autowired
    TaxRateMapper taxRateMapper;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        TestDataLoader.load(jdbc);
    }

    @Test
    void findAll_ordersByEnumDefinitionAndValidFrom() {
        assertThat(taxRateMapper.findAll(null))
                .extracting(TaxRate::getTaxCategory, TaxRate::getValidFrom)
                .containsExactly(
                        tuple(TaxCategory.STANDARD, LocalDate.of(1997, 4, 1)),
                        tuple(TaxCategory.STANDARD, LocalDate.of(2014, 4, 1)),
                        tuple(TaxCategory.STANDARD, REFORM_2019),
                        tuple(TaxCategory.REDUCED, REFORM_2019),
                        tuple(TaxCategory.EXEMPT, REFORM_2019));
    }

    @Test
    void findAll_filtersByTaxCategory() {
        assertThat(taxRateMapper.findAll(TaxCategory.STANDARD))
                .extracting(TaxRate::getValidFrom)
                .containsExactly(LocalDate.of(1997, 4, 1), LocalDate.of(2014, 4, 1), REFORM_2019);
    }

    @Test
    void findByKey_mapsAllColumns() {
        TaxRate taxRate = taxRateMapper.findByKey(TaxCategory.STANDARD, LocalDate.of(2014, 4, 1));

        assertThat(taxRate.getDescription()).isEqualTo("標準税率");
        assertThat(taxRate.getRate()).isEqualByComparingTo("0.08");
        assertThat(taxRate.getRate().scale()).isEqualTo(4);
        assertThat(taxRate.getValidTo()).isEqualTo(LocalDate.of(2019, 9, 30));
        assertThat(taxRateMapper.findByKey(TaxCategory.REDUCED, LocalDate.of(2014, 4, 1))).isNull();
    }

    @Test
    void insert_duplicateKey_throwsDuplicateKeyException() {
        TaxRate taxRate = newTaxRate(TaxCategory.STANDARD, REFORM_2019);

        assertThatThrownBy(() -> taxRateMapper.insert(taxRate)).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void insert_newKey() {
        taxRateMapper.insert(newTaxRate(TaxCategory.REDUCED, LocalDate.of(2030, 4, 1)));

        assertThat(taxRateMapper.findByKey(TaxCategory.REDUCED, LocalDate.of(2030, 4, 1)).getRate())
                .isEqualByComparingTo("0.12");
    }

    @Test
    void update_byKey() {
        TaxRate taxRate = taxRateMapper.findByKey(TaxCategory.STANDARD, REFORM_2019);
        taxRate.setValidTo(LocalDate.of(2030, 3, 31));

        assertThat(taxRateMapper.update(taxRate)).isEqualTo(1);

        assertThat(taxRateMapper.findByKey(TaxCategory.STANDARD, REFORM_2019).getValidTo())
                .isEqualTo(LocalDate.of(2030, 3, 31));
    }

    @Test
    void delete_physicallyDeletes() {
        assertThat(taxRateMapper.delete(TaxCategory.EXEMPT, REFORM_2019)).isEqualTo(1);
        assertThat(taxRateMapper.findByKey(TaxCategory.EXEMPT, REFORM_2019)).isNull();
        assertThat(taxRateMapper.delete(TaxCategory.EXEMPT, REFORM_2019)).isZero();
    }

    private static TaxRate newTaxRate(TaxCategory category, LocalDate validFrom) {
        TaxRate taxRate = new TaxRate();
        taxRate.setTaxCategory(category);
        taxRate.setDescription("テスト");
        taxRate.setRate(new BigDecimal("0.12"));
        taxRate.setValidFrom(validFrom);
        return taxRate;
    }
}
