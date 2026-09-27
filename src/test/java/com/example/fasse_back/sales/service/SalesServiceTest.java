package com.example.fasse_back.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.fasse_back.common.exception.BadRequestException;
import com.example.fasse_back.common.exception.NotFoundException;
import com.example.fasse_back.common.slipno.SlipNoService;
import com.example.fasse_back.common.slipno.SlipType;
import com.example.fasse_back.common.web.RequestParams.DateRange;
import com.example.fasse_back.menu.repository.MenuMapper;
import com.example.fasse_back.sales.dto.SalesDetailRequest;
import com.example.fasse_back.sales.dto.SalesRequest;
import com.example.fasse_back.sales.dto.SalesResponse;
import com.example.fasse_back.sales.entity.SalesDetail;
import com.example.fasse_back.sales.entity.SalesHeader;
import com.example.fasse_back.sales.repository.SalesMapper;

@ExtendWith(MockitoExtension.class)
class SalesServiceTest {

    private static final String ID = "c0000000-0000-4000-8000-000000000001";
    private static final LocalDate BUSINESS_DATE = LocalDate.of(2026, 7, 12);

    @Mock
    SalesMapper salesMapper;

    @Mock
    MenuMapper menuMapper;

    @Mock
    SlipNoService slipNoService;

    @InjectMocks
    SalesService salesService;

    private static SalesDetailRequest detailRequest(long menuId) {
        return new SalesDetailRequest(menuId, 2, new BigDecimal("600"), new BigDecimal("1200"),
                new BigDecimal("0.10"));
    }

    private static SalesRequest request(OffsetDateTime salesDatetime, String tableNo, Integer customerCount,
            BigDecimal discountAmount, String remarks, List<SalesDetailRequest> details) {
        return new SalesRequest(salesDatetime, BUSINESS_DATE, tableNo, customerCount, new BigDecimal("1200"),
                new BigDecimal("120"), discountAmount, new BigDecimal("1320"), "CASH", remarks, details);
    }

    private static SalesRequest minimalRequest(List<SalesDetailRequest> details) {
        return request(OffsetDateTime.parse("2026-07-12T19:30:00+09:00"), null, null, null, null, details);
    }

    private static SalesHeader existingHeader() {
        SalesHeader header = new SalesHeader();
        header.setId(ID);
        header.setSalesNo("SO-20260701-0001");
        header.setSalesDatetime(LocalDateTime.of(2026, 7, 1, 18, 30));
        header.setBusinessDate(LocalDate.of(2026, 7, 1));
        header.setTableNo("A1");
        header.setCustomerCount(2);
        header.setSubtotal(new BigDecimal("1200"));
        header.setTaxAmount(new BigDecimal("120"));
        header.setDiscountAmount(new BigDecimal("50"));
        header.setTotalAmount(new BigDecimal("1270"));
        header.setPaymentMethod("CARD");
        header.setRemarks("既存");
        header.setCreatedAt(Instant.parse("2026-07-01T09:30:00Z"));
        return header;
    }

    private void menusExist(Long... menuIds) {
        given(menuMapper.findExistingIds(anyCollection())).willReturn(List.of(menuIds));
    }

    @Test
    void toJst_convertsOffsetToJstLocalDateTime() {
        assertThat(SalesService.toJst(OffsetDateTime.of(2026, 7, 12, 10, 30, 0, 0, ZoneOffset.UTC)))
                .isEqualTo(LocalDateTime.of(2026, 7, 12, 19, 30));
        assertThat(SalesService.toJst(OffsetDateTime.parse("2026-07-12T19:30:00+09:00")))
                .isEqualTo(LocalDateTime.of(2026, 7, 12, 19, 30));
    }

    @Test
    void findHeaders_fromAfterTo_returnsEmptyWithoutQuery() {
        assertThat(salesService.findHeaders(new DateRange(BUSINESS_DATE, BUSINESS_DATE.minusDays(1)))).isEmpty();
        then(salesMapper).should(never()).findHeaders(any(), any());
    }

    @Test
    void findHeaders_returnsSalesDatetimeInJst() {
        given(salesMapper.findHeaders(BUSINESS_DATE, BUSINESS_DATE)).willReturn(List.of(existingHeader()));

        assertThat(salesService.findHeaders(new DateRange(BUSINESS_DATE, BUSINESS_DATE)).get(0).salesDatetime())
                .isEqualTo(OffsetDateTime.parse("2026-07-01T18:30:00+09:00"));
    }

    @Test
    void findById_returnsHeaderAndDetails() {
        given(salesMapper.findHeaderById(ID)).willReturn(existingHeader());
        SalesDetail detail = new SalesDetail();
        detail.setMenuId(3L);
        given(salesMapper.findDetailsBySalesId(ID)).willReturn(List.of(detail));

        SalesResponse response = salesService.findById(ID);

        assertThat(response.header().salesNo()).isEqualTo("SO-20260701-0001");
        assertThat(response.details()).extracting(d -> d.menuId()).containsExactly(3L);
    }

    @Test
    void findById_notFound_throws404() {
        assertThatThrownBy(() -> salesService.findById(ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_appliesDefaultsAndNumbersByBusinessDate() {
        menusExist(1L);
        given(slipNoService.next(SlipType.SALES, BUSINESS_DATE)).willReturn("SO-20260712-0001");
        given(salesMapper.findHeaderById(anyString())).willReturn(existingHeader());

        salesService.create(request(OffsetDateTime.parse("2026-07-12T10:30:00Z"), null, null, null, null,
                List.of(detailRequest(1L))));

        ArgumentCaptor<SalesHeader> headerCaptor = ArgumentCaptor.forClass(SalesHeader.class);
        InOrder inOrder = Mockito.inOrder(slipNoService, salesMapper);
        inOrder.verify(slipNoService).next(SlipType.SALES, BUSINESS_DATE);
        inOrder.verify(salesMapper).insertHeader(headerCaptor.capture());
        inOrder.verify(salesMapper).insertDetails(any());

        SalesHeader header = headerCaptor.getValue();
        assertThat(header.getSalesNo()).isEqualTo("SO-20260712-0001");
        assertThat(header.getSalesDatetime()).isEqualTo(LocalDateTime.of(2026, 7, 12, 19, 30));
        assertThat(header.getCustomerCount()).isEqualTo(1);
        assertThat(header.getDiscountAmount()).isEqualByComparingTo("0");
    }

    @Test
    void create_keepsSpecifiedOptionalFields() {
        menusExist(1L);
        given(salesMapper.findHeaderById(anyString())).willReturn(existingHeader());

        salesService.create(request(OffsetDateTime.parse("2026-07-12T19:30:00+09:00"), "B1", 3,
                new BigDecimal("100"), "メモ", List.of(detailRequest(1L))));

        ArgumentCaptor<SalesHeader> headerCaptor = ArgumentCaptor.forClass(SalesHeader.class);
        then(salesMapper).should().insertHeader(headerCaptor.capture());
        assertThat(headerCaptor.getValue().getTableNo()).isEqualTo("B1");
        assertThat(headerCaptor.getValue().getCustomerCount()).isEqualTo(3);
        assertThat(headerCaptor.getValue().getDiscountAmount()).isEqualByComparingTo("100");
        assertThat(headerCaptor.getValue().getRemarks()).isEqualTo("メモ");
    }

    @Test
    void create_withoutDetails_skipsReferenceCheckAndDetailInsert() {
        given(salesMapper.findHeaderById(anyString())).willReturn(existingHeader());

        salesService.create(minimalRequest(List.of()));

        then(menuMapper).should(never()).findExistingIds(anyCollection());
        then(salesMapper).should(never()).insertDetails(any());
    }

    @Test
    void create_invalidReference_throws400() {
        menusExist(1L);

        assertThatThrownBy(() -> salesService.create(minimalRequest(List.of(detailRequest(1L), detailRequest(9L)))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("invalid reference: details[1].menu_id");
        then(salesMapper).should(never()).insertHeader(any());
    }

    @Test
    void update_keepsOmittedOptionalFieldsAndSlipNo() {
        given(salesMapper.findHeaderById(ID)).willReturn(existingHeader());
        menusExist(1L);

        salesService.update(ID, minimalRequest(List.of(detailRequest(1L))));

        ArgumentCaptor<SalesHeader> headerCaptor = ArgumentCaptor.forClass(SalesHeader.class);
        InOrder inOrder = Mockito.inOrder(salesMapper);
        inOrder.verify(salesMapper).updateHeader(headerCaptor.capture());
        inOrder.verify(salesMapper).deleteDetailsBySalesId(ID);
        inOrder.verify(salesMapper).insertDetails(any());

        SalesHeader header = headerCaptor.getValue();
        assertThat(header.getSalesNo()).isEqualTo("SO-20260701-0001");
        assertThat(header.getBusinessDate()).isEqualTo(BUSINESS_DATE);
        assertThat(header.getCreatedAt()).isEqualTo(Instant.parse("2026-07-01T09:30:00Z"));
        assertThat(header.getTableNo()).isEqualTo("A1");
        assertThat(header.getCustomerCount()).isEqualTo(2);
        assertThat(header.getDiscountAmount()).isEqualByComparingTo("50");
        assertThat(header.getRemarks()).isEqualTo("既存");
        assertThat(header.getPaymentMethod()).isEqualTo("CASH");
        then(slipNoService).should(never()).next(any(), any());
    }

    @Test
    void update_overwritesSpecifiedOptionalFields() {
        given(salesMapper.findHeaderById(ID)).willReturn(existingHeader());
        menusExist(1L);

        salesService.update(ID, request(OffsetDateTime.parse("2026-07-12T19:30:00+09:00"), "C9", 6,
                BigDecimal.ZERO, "変更", List.of(detailRequest(1L))));

        ArgumentCaptor<SalesHeader> headerCaptor = ArgumentCaptor.forClass(SalesHeader.class);
        then(salesMapper).should().updateHeader(headerCaptor.capture());
        SalesHeader header = headerCaptor.getValue();
        assertThat(header.getTableNo()).isEqualTo("C9");
        assertThat(header.getCustomerCount()).isEqualTo(6);
        assertThat(header.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(header.getRemarks()).isEqualTo("変更");
    }

    @Test
    void update_notFound_throws404() {
        assertThatThrownBy(() -> salesService.update(ID, minimalRequest(List.of())))
                .isInstanceOf(NotFoundException.class);
        then(salesMapper).should(never()).updateHeader(any());
    }

    @Test
    void delete_deletesDetailsThenHeader() {
        given(salesMapper.findHeaderById(ID)).willReturn(existingHeader());

        salesService.delete(ID);

        InOrder inOrder = Mockito.inOrder(salesMapper);
        inOrder.verify(salesMapper).deleteDetailsBySalesId(ID);
        inOrder.verify(salesMapper).deleteHeader(ID);
    }

    @Test
    void delete_notFound_throws404() {
        assertThatThrownBy(() -> salesService.delete(ID)).isInstanceOf(NotFoundException.class);
        then(salesMapper).should(never()).deleteHeader(any());
    }
}
