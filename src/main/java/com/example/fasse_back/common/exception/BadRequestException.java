package com.example.fasse_back.common.exception;

import java.util.Collection;

import org.springframework.http.HttpStatus;

/** 400 Bad Request。{@code message} は利用者向けにそのまま返す */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }

    /** 必須項目の欠落・型の不一致等(例: {@code invalid or missing fields: item_name, unit}) */
    public static BadRequestException invalidFields(Collection<String> fields) {
        return new BadRequestException("invalid or missing fields: " + String.join(", ", fields));
    }

    /** 参照先マスタが存在しない(例: {@code invalid reference: supplier_id, details[1].item_id}) */
    public static BadRequestException invalidReference(Collection<String> fields) {
        return new BadRequestException("invalid reference: " + String.join(", ", fields));
    }
}
