package com.example.fasse_back.common.exception;

import org.springframework.http.HttpStatus;

/** 404 Not Found。利用者向けのメッセージは固定文言 {@code Not Found} とし、詳細はログにのみ出す */
public class NotFoundException extends ApiException {

    public static final String MESSAGE = "Not Found";

    private final String detail;

    /**
     * @param detail ログ用の詳細(例: {@code item not found: id=1})
     */
    public NotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, MESSAGE);
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }
}
