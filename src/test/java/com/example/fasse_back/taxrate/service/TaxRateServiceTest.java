package com.example.fasse_back.taxrate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.common.exception.ConflictException;
import com.example.fasse_back.common.exception.NotFoundException;
import com.example.fasse_back.taxrate.dto.TaxRateRequest;
import com.example.fasse_back.taxrate.dto.TaxRateResponse;
import com.example.fasse_back.taxrate.dto.TaxRateUpdateRequest;
import com.example.fasse_back.taxrate.entity.TaxRate;
import com.example.fasse_back.taxrate.repository.TaxRateMapper;

@ExtendWith(MockitoExtension.class)
class TaxRateServiceTest {

    private static final LocalDate VALID_FROM = LocalDate.of(2019, 10, 1);

    @Mock
    TaxRateMapper taxRateMapper;

    @InjectMocks
    TaxRateService taxRateService;

    private static TaxRate existing() {
        TaxRate taxRate = new TaxRate();
        taxRate.setId(3L);
        taxRate.setTaxCategory(TaxCategory.STANDARD);
        taxRate.setDescription("標準税率");
        taxRate.setRate(new BigDecimal("0.1000"));
        taxRate.setValidFrom(VALID_FROM);
        taxRate.setValidTo(LocalDate.of(2030, 3, 31));
        return taxRate;
    }

    @Test
    void findAll_passesFilter() {
        given(taxRateMapper.findAll(TaxCategory.REDUCED)).willReturn(List.of(existing()));

        assertThat(taxRateService.findAll(TaxCategory.REDUCED)).extracting(TaxRateResponse::description)
                .containsExactly("標準税率");
    }

    @Test
    void findByKey_notFound_throws404() {
        assertThatThrownBy(() -> taxRateService.findByKey(TaxCategory.STANDARD, VALID_FROM))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_insertsAndReadsBack() {
        given(taxRateMapper.findByKey(TaxCategory.STANDARD, VALID_FROM)).willReturn(existing());

        TaxRateResponse response = taxRateService.create(
                new TaxRateRequest(TaxCategory.STANDARD, "標準税率", new BigDecimal("0.10"), VALID_FROM, null));

        ArgumentCaptor<TaxRate> captor = ArgumentCaptor.forClass(TaxRate.class);
        then(taxRateMapper).should().insert(captor.capture());
        assertThat(captor.getValue().getRate()).isEqualByComparingTo("0.10");
        assertThat(captor.getValue().getValidTo()).isNull();
        assertThat(response.validTo()).isEqualTo(LocalDate.of(2030, 3, 31));
    }

    @Test
    void create_duplicateKey_throws409() {
        willThrow(new DuplicateKeyException("duplicate")).given(taxRateMapper).insert(any());

        assertThatThrownBy(() -> taxRateService.create(
                new TaxRateRequest(TaxCategory.STANDARD, "標準税率", new BigDecimal("0.10"), VALID_FROM, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("tax rate already exists: STANDARD/2019-10-01");
    }

    @Test
    void update_keepsValidToWhenOmitted() {
        given(taxRateMapper.findByKey(TaxCategory.STANDARD, VALID_FROM)).willReturn(existing());

        taxRateService.update(TaxCategory.STANDARD, VALID_FROM,
                new TaxRateUpdateRequest("標準", new BigDecimal("0.11"), null));

        ArgumentCaptor<TaxRate> captor = ArgumentCaptor.forClass(TaxRate.class);
        then(taxRateMapper).should().update(captor.capture());
        TaxRate updated = captor.getValue();
        assertThat(updated.getDescription()).isEqualTo("標準");
        assertThat(updated.getRate()).isEqualByComparingTo("0.11");
        assertThat(updated.getValidTo()).isEqualTo(LocalDate.of(2030, 3, 31));
        // キーはパスの値のまま
        assertThat(updated.getTaxCategory()).isEqualTo(TaxCategory.STANDARD);
        assertThat(updated.getValidFrom()).isEqualTo(VALID_FROM);
    }

    @Test
    void update_overwritesValidToWhenSpecified() {
        given(taxRateMapper.findByKey(TaxCategory.STANDARD, VALID_FROM)).willReturn(existing());

        taxRateService.update(TaxCategory.STANDARD, VALID_FROM,
                new TaxRateUpdateRequest("標準", new BigDecimal("0.10"), LocalDate.of(2031, 3, 31)));

        ArgumentCaptor<TaxRate> captor = ArgumentCaptor.forClass(TaxRate.class);
        then(taxRateMapper).should().update(captor.capture());
        assertThat(captor.getValue().getValidTo()).isEqualTo(LocalDate.of(2031, 3, 31));
    }

    @Test
    void update_notFound_throws404() {
        assertThatThrownBy(() -> taxRateService.update(TaxCategory.EXEMPT, VALID_FROM,
                new TaxRateUpdateRequest("非課税", BigDecimal.ZERO, null)))
                .isInstanceOf(NotFoundException.class);
        then(taxRateMapper).should(never()).update(any());
    }

    @Test
    void delete_physicallyDeletes() {
        given(taxRateMapper.delete(TaxCategory.EXEMPT, VALID_FROM)).willReturn(1);

        taxRateService.delete(TaxCategory.EXEMPT, VALID_FROM);

        then(taxRateMapper).should().delete(TaxCategory.EXEMPT, VALID_FROM);
    }

    @Test
    void delete_notFound_throws404() {
        given(taxRateMapper.delete(TaxCategory.EXEMPT, VALID_FROM)).willReturn(0);

        assertThatThrownBy(() -> taxRateService.delete(TaxCategory.EXEMPT, VALID_FROM))
                .isInstanceOf(NotFoundException.class);
    }
}
