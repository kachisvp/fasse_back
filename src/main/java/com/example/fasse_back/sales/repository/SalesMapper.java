package com.example.fasse_back.sales.repository;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.fasse_back.sales.entity.SalesDetail;
import com.example.fasse_back.sales.entity.SalesHeader;

/** 売上伝票({@code t_sales_header} / {@code t_sales_detail})。SQL は SalesMapper.xml に記述する */
@Mapper
public interface SalesMapper {

    /** 営業日が {@code from} 以上 {@code to} 以下のヘッダを、営業日・伝票番号の昇順で返す */
    List<SalesHeader> findHeaders(@Param("from") LocalDate from, @Param("to") LocalDate to);

    SalesHeader findHeaderById(@Param("id") String id);

    List<SalesDetail> findDetailsBySalesId(@Param("salesId") String salesId);

    void insertHeader(SalesHeader header);

    int updateHeader(SalesHeader header);

    int deleteHeader(@Param("id") String id);

    /** 明細をまとめて登録する(空のリストは渡さないこと) */
    void insertDetails(@Param("details") List<SalesDetail> details);

    int deleteDetailsBySalesId(@Param("salesId") String salesId);
}
