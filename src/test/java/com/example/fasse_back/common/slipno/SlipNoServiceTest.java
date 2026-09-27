package com.example.fasse_back.common.slipno;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlipNoServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 7, 12);

    @Mock
    SlipNoMapper slipNoMapper;

    @InjectMocks
    SlipNoService slipNoService;

    @Test
    void next_purchase_incrementsCounterForDateThenFormats() {
        given(slipNoMapper.selectLastInsertId()).willReturn(1L);

        assertThat(slipNoService.next(SlipType.PURCHASE, DATE)).isEqualTo("PO-20260712-0001");

        InOrder inOrder = Mockito.inOrder(slipNoMapper);
        inOrder.verify(slipNoMapper).increment("purchase_no#2026-07-12");
        inOrder.verify(slipNoMapper).selectLastInsertId();
    }

    @Test
    void next_sales_usesSalesCounter() {
        given(slipNoMapper.selectLastInsertId()).willReturn(42L);

        assertThat(slipNoService.next(SlipType.SALES, DATE)).isEqualTo("SO-20260712-0042");
        then(slipNoMapper).should().increment("sales_no#2026-07-12");
    }

    @Test
    void format_widensAfter9999() {
        assertThat(SlipNoService.format(SlipType.PURCHASE, DATE, 9999)).isEqualTo("PO-20260712-9999");
        assertThat(SlipNoService.format(SlipType.PURCHASE, DATE, 10000)).isEqualTo("PO-20260712-10000");
    }
}
