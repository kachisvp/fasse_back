package com.example.fasse_back.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.example.fasse_back.common.TaxCategory;
import com.example.fasse_back.common.exception.BadRequestException;

class RequestParamsTest {

    @Test
    void masterId_acceptsPositiveInteger() {
        assertThat(RequestParams.masterId("1")).isEqualTo(1L);
        assertThat(RequestParams.masterId("9223372036854775807")).isEqualTo(Long.MAX_VALUE);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "0", "-1", "01", "+1", "1.0", "a", "9223372036854775808" })
    void masterId_rejectsOthers(String value) {
        assertThatThrownBy(() -> RequestParams.masterId(value))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("id must be a positive integer");
    }

    @Test
    void uuid_acceptsUuidInAnyCase() {
        assertThat(RequestParams.uuid("A0000000-0000-4000-8000-000000000001"))
                .isEqualTo("A0000000-0000-4000-8000-000000000001");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = { "1-1-1-1-1", "a0000000000040008000000000000001", "g0000000-0000-4000-8000-000000000001" })
    void uuid_rejectsOthers(String value) {
        assertThatThrownBy(() -> RequestParams.uuid(value)).hasMessage("id must be a UUID");
    }

    @Test
    void taxCategory_parsesAndUsesNameInMessage() {
        assertThat(RequestParams.taxCategory("EXEMPT", "taxCategory")).isEqualTo(TaxCategory.EXEMPT);
        assertThatThrownBy(() -> RequestParams.taxCategory(null, "tax_category"))
                .hasMessage("tax_category must be one of STANDARD, REDUCED, EXEMPT");
    }

    @Test
    void dateRange_requiresBothDates() {
        assertThat(RequestParams.dateRange("2026-07-01", "2026-06-30"))
                .isEqualTo(new RequestParams.DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 6, 30)));
        assertThatThrownBy(() -> RequestParams.dateRange(null, "2026-07-01"))
                .hasMessage("from and to are required in YYYY-MM-DD format");
        assertThatThrownBy(() -> RequestParams.dateRange("2026-13-01", "2026-07-01"))
                .hasMessage("from and to are required in YYYY-MM-DD format");
    }

    @Test
    void validFrom_rejectsInvalidDate() {
        assertThat(RequestParams.validFrom("2019-10-01")).isEqualTo(LocalDate.of(2019, 10, 1));
        assertThatThrownBy(() -> RequestParams.validFrom("2019-02-29"))
                .hasMessage("validFrom must be in YYYY-MM-DD format");
    }
}
