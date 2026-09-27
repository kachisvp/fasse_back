package com.example.fasse_back.common.web;

import org.slf4j.MDC;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 全エラーレスポンスの形式({@code { "message", "requestId" }})。
 * {@code requestId} は仕様どおりキャメルケースで出力する(全体のスネークケース変換の対象外)。
 *
 * @param message   利用者向けのメッセージ
 * @param requestId 問い合わせ・ログ調査用のリクエスト ID
 */
public record ErrorResponse(String message, @JsonProperty("requestId") String requestId) {

    /** MDC のリクエスト ID を付けてエラーレスポンスを作る */
    public static ErrorResponse of(String message) {
        return new ErrorResponse(message, MDC.get(RequestIdFilter.MDC_REQUEST_ID));
    }
}
