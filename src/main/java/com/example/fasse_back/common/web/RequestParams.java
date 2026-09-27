package com.example.fasse_back.common.web;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.common.exception.BadRequestException;

/**
 * パス・クエリパラメータの検証と変換(design.md 6.1 節)。
 * Spring の型変換に任せず、仕様どおりの {@code message} で 400 を返すために文字列で受け取って変換する。
 */
public final class RequestParams {

    private static final Pattern POSITIVE_INTEGER = Pattern.compile("[1-9][0-9]{0,18}");
    private static final Pattern UUID_FORMAT =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final String TAX_CATEGORIES = Arrays.stream(TaxCategory.values())
            .map(Enum::name)
            .collect(Collectors.joining(", "));

    private RequestParams() {
    }

    /** マスタのパス {@code id}(正の整数) */
    public static long masterId(String value) {
        if (value == null || !POSITIVE_INTEGER.matcher(value).matches()) {
            throw new BadRequestException("id must be a positive integer");
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            // 19 桁で Long の最大値を超える場合
            throw new BadRequestException("id must be a positive integer");
        }
    }

    /** 伝票のパス {@code id}(UUID 形式) */
    public static String uuid(String value) {
        if (value == null || !UUID_FORMAT.matcher(value).matches()) {
            throw new BadRequestException("id must be a UUID");
        }
        return value;
    }

    /**
     * 税区分(パスの {@code taxCategory}、クエリの {@code tax_category})
     *
     * @param name エラーメッセージに示すパラメータ名
     */
    public static TaxCategory taxCategory(String value, String name) {
        try {
            return TaxCategory.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadRequestException(name + " must be one of " + TAX_CATEGORIES);
        }
    }

    /** パスの {@code validFrom}(YYYY-MM-DD) */
    public static LocalDate validFrom(String value) {
        LocalDate date = parseDate(value);
        if (date == null) {
            throw new BadRequestException("validFrom must be in YYYY-MM-DD format");
        }
        return date;
    }

    /** 一覧の日付範囲({@code from} / {@code to}、両方必須、YYYY-MM-DD) */
    public static DateRange dateRange(String from, String to) {
        LocalDate fromDate = parseDate(from);
        LocalDate toDate = parseDate(to);
        if (fromDate == null || toDate == null) {
            throw new BadRequestException("from and to are required in YYYY-MM-DD format");
        }
        return new DateRange(fromDate, toDate);
    }

    /** 形式不正・未指定の場合は null */
    private static LocalDate parseDate(String value) {
        if (value == null || value.length() != 10) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * 日付範囲(両端を含む)
     *
     * @param from 開始日
     * @param to   終了日
     */
    public record DateRange(LocalDate from, LocalDate to) {
    }
}
