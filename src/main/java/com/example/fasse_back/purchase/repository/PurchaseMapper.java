package com.example.fasse_back.purchase.repository;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.fasse_back.purchase.entity.PurchaseDetail;
import com.example.fasse_back.purchase.entity.PurchaseHeader;

/** 仕入伝票({@code t_purchase_header} / {@code t_purchase_detail})。SQL は PurchaseMapper.xml に記述する */
@Mapper
public interface PurchaseMapper {

    /** 仕入日が {@code from} 以上 {@code to} 以下のヘッダを、仕入日・伝票番号の昇順で返す */
    List<PurchaseHeader> findHeaders(@Param("from") LocalDate from, @Param("to") LocalDate to);

    PurchaseHeader findHeaderById(@Param("id") String id);

    List<PurchaseDetail> findDetailsByPurchaseId(@Param("purchaseId") String purchaseId);

    void insertHeader(PurchaseHeader header);

    int updateHeader(PurchaseHeader header);

    int deleteHeader(@Param("id") String id);

    /** 明細をまとめて登録する(空のリストは渡さないこと) */
    void insertDetails(@Param("details") List<PurchaseDetail> details);

    int deleteDetailsByPurchaseId(@Param("purchaseId") String purchaseId);
}
