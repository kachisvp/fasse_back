package com.example.fasse_back.purchase.service;

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
import com.example.fasse_back.item.repository.ItemMapper;
import com.example.fasse_back.purchase.dto.PurchaseDetailRequest;
import com.example.fasse_back.purchase.dto.PurchaseRequest;
import com.example.fasse_back.purchase.dto.PurchaseResponse;
import com.example.fasse_back.purchase.entity.PurchaseDetail;
import com.example.fasse_back.purchase.entity.PurchaseHeader;
import com.example.fasse_back.purchase.repository.PurchaseMapper;
import com.example.fasse_back.supplier.repository.SupplierMapper;

@ExtendWith(MockitoExtension.class)
class PurchaseServiceTest {

    private static final String ID = "a0000000-0000-4000-8000-000000000001";
    private static final LocalDate DATE = LocalDate.of(2026, 7, 12);

    @Mock
    PurchaseMapper purchaseMapper;

    @Mock
    SupplierMapper supplierMapper;

    @Mock
    ItemMapper itemMapper;

    @Mock
    SlipNoService slipNoService;

    @InjectMocks
    PurchaseService purchaseService;

    private static PurchaseDetailRequest detailRequest(long itemId) {
        return new PurchaseDetailRequest(itemId, new BigDecimal("2"), new BigDecimal("100"), new BigDecimal("200"),
                new BigDecimal("0.08"));
    }

    private static PurchaseRequest request(LocalDate deliveryDate, String remarks,
            List<PurchaseDetailRequest> details) {
        return new PurchaseRequest(1L, DATE, deliveryDate, new BigDecimal("200"), new BigDecimal("16"),
                new BigDecimal("216"), remarks, details);
    }

    private static PurchaseHeader existingHeader() {
        PurchaseHeader header = new PurchaseHeader();
        header.setId(ID);
        header.setPurchaseNo("PO-20260701-0001");
        header.setSupplierId(2L);
        header.setPurchaseDate(LocalDate.of(2026, 7, 1));
        header.setDeliveryDate(LocalDate.of(2026, 7, 2));
        header.setSubtotal(new BigDecimal("1000"));
        header.setTaxAmount(new BigDecimal("80"));
        header.setTotalAmount(new BigDecimal("1080"));
        header.setRemarks("既存");
        header.setCreatedAt(Instant.parse("2026-07-01T00:00:00Z"));
        return header;
    }

    private void referencesExist(Long... itemIds) {
        given(supplierMapper.existsById(1L)).willReturn(true);
        given(itemMapper.findExistingIds(anyCollection())).willReturn(List.of(itemIds));
    }

    @Test
    void findHeaders_fromAfterTo_returnsEmptyWithoutQuery() {
        assertThat(purchaseService.findHeaders(new DateRange(DATE, DATE.minusDays(1)))).isEmpty();
        then(purchaseMapper).should(never()).findHeaders(any(), any());
    }

    @Test
    void findHeaders_queriesRange() {
        given(purchaseMapper.findHeaders(DATE, DATE)).willReturn(List.of(existingHeader()));

        assertThat(purchaseService.findHeaders(new DateRange(DATE, DATE))).hasSize(1);
    }

    @Test
    void findById_returnsHeaderAndDetails() {
        given(purchaseMapper.findHeaderById(ID)).willReturn(existingHeader());
        PurchaseDetail detail = new PurchaseDetail();
        detail.setId("d1");
        detail.setItemId(1L);
        given(purchaseMapper.findDetailsByPurchaseId(ID)).willReturn(List.of(detail));

        PurchaseResponse response = purchaseService.findById(ID);

        assertThat(response.header().purchaseNo()).isEqualTo("PO-20260701-0001");
        assertThat(response.details()).extracting(d -> d.itemId()).containsExactly(1L);
    }

    @Test
    void findById_notFound_throws404() {
        assertThatThrownBy(() -> purchaseService.findById(ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void create_numbersSlipAndInsertsHeaderThenDetails() {
        referencesExist(1L, 2L);
        given(slipNoService.next(SlipType.PURCHASE, DATE)).willReturn("PO-20260712-0001");
        given(purchaseMapper.findHeaderById(anyString())).willReturn(existingHeader());

        purchaseService.create(request(null, "備考", List.of(detailRequest(1L), detailRequest(2L))));

        ArgumentCaptor<PurchaseHeader> headerCaptor = ArgumentCaptor.forClass(PurchaseHeader.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PurchaseDetail>> detailsCaptor = ArgumentCaptor.forClass(List.class);
        InOrder inOrder = Mockito.inOrder(slipNoService, purchaseMapper);
        inOrder.verify(slipNoService).next(SlipType.PURCHASE, DATE);
        inOrder.verify(purchaseMapper).insertHeader(headerCaptor.capture());
        inOrder.verify(purchaseMapper).insertDetails(detailsCaptor.capture());

        PurchaseHeader header = headerCaptor.getValue();
        assertThat(header.getId()).matches("[0-9a-f-]{36}");
        assertThat(header.getPurchaseNo()).isEqualTo("PO-20260712-0001");
        assertThat(header.getSupplierId()).isEqualTo(1L);
        assertThat(header.getRemarks()).isEqualTo("備考");
        assertThat(detailsCaptor.getValue()).hasSize(2).allSatisfy(d -> {
            assertThat(d.getPurchaseId()).isEqualTo(header.getId());
            assertThat(d.getId()).matches("[0-9a-f-]{36}");
        });
        assertThat(detailsCaptor.getValue()).extracting(PurchaseDetail::getItemId).containsExactly(1L, 2L);
    }

    @Test
    void create_withoutDetails_skipsDetailInsert() {
        given(supplierMapper.existsById(1L)).willReturn(true);
        given(purchaseMapper.findHeaderById(anyString())).willReturn(existingHeader());

        purchaseService.create(request(null, null, List.of()));

        then(purchaseMapper).should(never()).insertDetails(any());
        then(itemMapper).should(never()).findExistingIds(anyCollection());
    }

    @Test
    void create_invalidReferences_throws400WithAllFields() {
        given(supplierMapper.existsById(1L)).willReturn(false);
        given(itemMapper.findExistingIds(anyCollection())).willReturn(List.of(1L));

        assertThatThrownBy(() -> purchaseService.create(
                request(null, null, List.of(detailRequest(1L), detailRequest(99L), detailRequest(98L)))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("invalid reference: supplier_id, details[1].item_id, details[2].item_id");
        then(slipNoService).should(never()).next(any(), any());
        then(purchaseMapper).should(never()).insertHeader(any());
    }

    @Test
    void update_replacesDetailsAndKeepsSlipNoAndOptionalFields() {
        given(purchaseMapper.findHeaderById(ID)).willReturn(existingHeader());
        referencesExist(1L);

        purchaseService.update(ID, request(null, null, List.of(detailRequest(1L))));

        ArgumentCaptor<PurchaseHeader> headerCaptor = ArgumentCaptor.forClass(PurchaseHeader.class);
        InOrder inOrder = Mockito.inOrder(purchaseMapper);
        inOrder.verify(purchaseMapper).updateHeader(headerCaptor.capture());
        inOrder.verify(purchaseMapper).deleteDetailsByPurchaseId(ID);
        inOrder.verify(purchaseMapper).insertDetails(any());

        PurchaseHeader header = headerCaptor.getValue();
        // 基準日を変更しても伝票番号は採番し直さない
        assertThat(header.getPurchaseDate()).isEqualTo(DATE);
        assertThat(header.getPurchaseNo()).isEqualTo("PO-20260701-0001");
        assertThat(header.getCreatedAt()).isEqualTo(Instant.parse("2026-07-01T00:00:00Z"));
        then(slipNoService).should(never()).next(any(), any());
        // 省略した任意項目は既存の値を維持する
        assertThat(header.getDeliveryDate()).isEqualTo(LocalDate.of(2026, 7, 2));
        assertThat(header.getRemarks()).isEqualTo("既存");
        // 必須項目は上書きする
        assertThat(header.getSupplierId()).isEqualTo(1L);
        assertThat(header.getTotalAmount()).isEqualByComparingTo("216");
    }

    @Test
    void update_overwritesSpecifiedOptionalFields() {
        given(purchaseMapper.findHeaderById(ID)).willReturn(existingHeader());
        referencesExist(1L);

        purchaseService.update(ID, request(LocalDate.of(2026, 7, 20), "変更", List.of(detailRequest(1L))));

        ArgumentCaptor<PurchaseHeader> headerCaptor = ArgumentCaptor.forClass(PurchaseHeader.class);
        then(purchaseMapper).should().updateHeader(headerCaptor.capture());
        assertThat(headerCaptor.getValue().getDeliveryDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(headerCaptor.getValue().getRemarks()).isEqualTo("変更");
    }

    @Test
    void update_notFound_throws404BeforeReferenceCheck() {
        assertThatThrownBy(() -> purchaseService.update(ID, request(null, null, List.of(detailRequest(1L)))))
                .isInstanceOf(NotFoundException.class);
        then(supplierMapper).should(never()).existsById(any(Long.class));
        then(purchaseMapper).should(never()).updateHeader(any());
    }

    @Test
    void update_invalidReference_throws400() {
        given(purchaseMapper.findHeaderById(ID)).willReturn(existingHeader());
        given(supplierMapper.existsById(1L)).willReturn(true);
        given(itemMapper.findExistingIds(anyCollection())).willReturn(List.of());

        assertThatThrownBy(() -> purchaseService.update(ID, request(null, null, List.of(detailRequest(5L)))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("invalid reference: details[0].item_id");
        then(purchaseMapper).should(never()).updateHeader(any());
    }

    @Test
    void delete_deletesDetailsThenHeader() {
        given(purchaseMapper.findHeaderById(ID)).willReturn(existingHeader());

        purchaseService.delete(ID);

        InOrder inOrder = Mockito.inOrder(purchaseMapper);
        inOrder.verify(purchaseMapper).deleteDetailsByPurchaseId(ID);
        inOrder.verify(purchaseMapper).deleteHeader(ID);
    }

    @Test
    void delete_notFound_throws404() {
        assertThatThrownBy(() -> purchaseService.delete(ID)).isInstanceOf(NotFoundException.class);
        then(purchaseMapper).should(never()).deleteHeader(any());
    }
}
