package com.example.fasse_back.item.repository;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.fasse_back.item.entity.Item;

/** 品目マスタ({@code m_item})。SQL は ItemMapper.xml に記述する */
@Mapper
public interface ItemMapper {

    /** 全件(論理削除済みを含む)を id 昇順で返す */
    List<Item> findAll();

    Item findById(@Param("id") long id);

    /** 指定した id のうち存在するもの(論理削除済みを含む)を返す */
    List<Long> findExistingIds(@Param("ids") Collection<Long> ids);

    /** 登録し、採番した id をエンティティに設定する */
    void insert(Item item);

    int update(Item item);

    /** 論理削除({@code is_active=false})。更新件数を返す */
    int deactivate(@Param("id") long id);
}
