package com.example.fasse_back.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 業務上の想定内エラー(400 / 404 / 409)の基底クラス。
 * {@code GlobalExceptionHandler} がステータスとメッセージをエラーレスポンスに変換する。
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
