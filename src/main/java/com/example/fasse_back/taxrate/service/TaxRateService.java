package com.example.fasse_back.taxrate.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.common.exception.ConflictException;
import com.example.fasse_back.common.exception.NotFoundException;
import com.example.fasse_back.taxrate.dto.TaxRateRequest;
import com.example.fasse_back.taxrate.dto.TaxRateResponse;
import com.example.fasse_back.taxrate.dto.TaxRateUpdateRequest;
import com.example.fasse_back.taxrate.entity.TaxRate;
import com.example.fasse_back.taxrate.repository.TaxRateMapper;

/** 消費税率マスタの業務ロジック(design.md 3.2 節)。キーは {@code tax_category} + {@code valid_from} */
@Service
public class TaxRateService {

    private final TaxRateMapper taxRateMapper;

    public TaxRateService(TaxRateMapper taxRateMapper) {
        this.taxRateMapper = taxRateMapper;
    }

    /** @param taxCategory 絞り込む税区分。null の場合は全件 */
    @Transactional(readOnly = true)
    public List<TaxRateResponse> findAll(TaxCategory taxCategory) {
        return taxRateMapper.findAll(taxCategory).stream().map(TaxRateResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaxRateResponse findByKey(TaxCategory taxCategory, LocalDate validFrom) {
        return TaxRateResponse.from(getExisting(taxCategory, validFrom));
    }

    /** 登録する。同じキーが既にあれば 409 */
    @Transactional
    public TaxRateResponse create(TaxRateRequest request) {
        TaxRate taxRate = new TaxRate();
        taxRate.setTaxCategory(request.taxCategory());
        taxRate.setDescription(request.description());
        taxRate.setRate(request.rate());
        taxRate.setValidFrom(request.validFrom());
        taxRate.setValidTo(request.validTo());
        try {
            taxRateMapper.insert(taxRate);
        } catch (DuplicateKeyException e) {
            throw new ConflictException(
                    "tax rate already exists: " + request.taxCategory() + "/" + request.validFrom());
        }
        return TaxRateResponse.from(getExisting(request.taxCategory(), request.validFrom()));
    }

    /** 更新する。{@code valid_to} を省略した場合は既存の値を維持する */
    @Transactional
    public TaxRateResponse update(TaxCategory taxCategory, LocalDate validFrom, TaxRateUpdateRequest request) {
        TaxRate taxRate = getExisting(taxCategory, validFrom);
        taxRate.setDescription(request.description());
        taxRate.setRate(request.rate());
        if (request.validTo() != null) {
            taxRate.setValidTo(request.validTo());
        }
        taxRateMapper.update(taxRate);
        return TaxRateResponse.from(getExisting(taxCategory, validFrom));
    }

    /** 物理削除する */
    @Transactional
    public void delete(TaxCategory taxCategory, LocalDate validFrom) {
        if (taxRateMapper.delete(taxCategory, validFrom) == 0) {
            throw notFound(taxCategory, validFrom);
        }
    }

    private TaxRate getExisting(TaxCategory taxCategory, LocalDate validFrom) {
        TaxRate taxRate = taxRateMapper.findByKey(taxCategory, validFrom);
        if (taxRate == null) {
            throw notFound(taxCategory, validFrom);
        }
        return taxRate;
    }

    private static NotFoundException notFound(TaxCategory taxCategory, LocalDate validFrom) {
        return new NotFoundException("tax rate not found: " + taxCategory + "/" + validFrom);
    }
}
