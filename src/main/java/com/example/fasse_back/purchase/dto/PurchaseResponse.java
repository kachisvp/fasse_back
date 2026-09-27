package com.example.fasse_back.purchase.dto;

import java.util.List;

import com.example.fasse_back.purchase.entity.PurchaseDetail;
import com.example.fasse_back.purchase.entity.PurchaseHeader;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * 仕入伝票(ヘッダ+明細)のレスポンス(openapi.yaml: Purchase)。
 * ヘッダの項目は {@code details} と同じ階層に展開して出力する。
 */
public record PurchaseResponse(
        @JsonUnwrapped PurchaseHeaderResponse header,
        List<PurchaseDetailResponse> details) {

    public static PurchaseResponse from(PurchaseHeader header, List<PurchaseDetail> details) {
        return new PurchaseResponse(PurchaseHeaderResponse.from(header),
                details.stream().map(PurchaseDetailResponse::from).toList());
    }
}
