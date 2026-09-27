package com.example.fasse_back.sales.dto;

import java.util.List;

import com.example.fasse_back.sales.entity.SalesDetail;
import com.example.fasse_back.sales.entity.SalesHeader;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * 売上伝票(ヘッダ+明細)のレスポンス(openapi.yaml: Sales)。
 * ヘッダの項目は {@code details} と同じ階層に展開して出力する。
 */
public record SalesResponse(
        @JsonUnwrapped SalesHeaderResponse header,
        List<SalesDetailResponse> details) {

    public static SalesResponse from(SalesHeader header, List<SalesDetail> details) {
        return new SalesResponse(SalesHeaderResponse.from(header),
                details.stream().map(SalesDetailResponse::from).toList());
    }
}
