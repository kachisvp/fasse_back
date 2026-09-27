package com.example.fasse_back.supplier.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.fasse_back.supplier.entity.Supplier;

/** 仕入先マスタ({@code m_supplier})。SQL は SupplierMapper.xml に記述する */
@Mapper
public interface SupplierMapper {

    /** 全件(論理削除済みを含む)を id 昇順で返す */
    List<Supplier> findAll();

    Supplier findById(@Param("id") long id);

    /** 存在するか(論理削除済みを含む) */
    boolean existsById(@Param("id") long id);

    /** 登録し、採番した id をエンティティに設定する */
    void insert(Supplier supplier);

    int update(Supplier supplier);

    /** 論理削除({@code is_active=false})。更新件数を返す */
    int deactivate(@Param("id") long id);
}
