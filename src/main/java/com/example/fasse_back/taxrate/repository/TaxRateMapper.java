package com.example.fasse_back.taxrate.repository;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.taxrate.entity.TaxRate;

/** 消費税率マスタ({@code m_tax_rate})。SQL は TaxRateMapper.xml に記述する */
@Mapper
public interface TaxRateMapper {

    /**
     * 税区分(ENUM 定義順)・適用開始日の昇順で返す
     *
     * @param taxCategory 絞り込む税区分。null の場合は全件
     */
    List<TaxRate> findAll(@Param("taxCategory") TaxCategory taxCategory);

    TaxRate findByKey(@Param("taxCategory") TaxCategory taxCategory, @Param("validFrom") LocalDate validFrom);

    /** 登録する。同じキーが既にある場合は {@code DuplicateKeyException} */
    void insert(TaxRate taxRate);

    /** キー({@code tax_category} + {@code valid_from})で更新する。更新件数を返す */
    int update(TaxRate taxRate);

    /** 物理削除する。削除件数を返す */
    int delete(@Param("taxCategory") TaxCategory taxCategory, @Param("validFrom") LocalDate validFrom);
}
