package com.example.fasse_back.common.slipno;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 伝票番号の採番カウンタ({@code t_slip_no_counter})。SQL は SlipNoMapper.xml に記述する */
@Mapper
public interface SlipNoMapper {

    /**
     * カウンタを 1 増やす(無ければ 1 で作成する)。増やした値は {@link #selectLastInsertId()} で取得する。
     * 行ロックはトランザクション終了まで保持される。
     */
    void increment(@Param("counterName") String counterName);

    /** 同じ DB 接続で直前の {@link #increment(String)} が設定した値を返す */
    long selectLastInsertId();
}
