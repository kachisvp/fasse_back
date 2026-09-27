package com.example.fasse_back.common.exception;

import org.springframework.http.HttpStatus;

/** 409 Conflict。一意キーの重複 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
