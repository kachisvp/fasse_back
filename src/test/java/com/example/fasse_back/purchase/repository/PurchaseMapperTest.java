package com.example.fasse_back.purchase.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.fasse_back.purchase.entity.PurchaseDetail;
import com.example.fasse_back.purchase.entity.PurchaseHeader;
import com.example.fasse_back.support.MapperTest;
import com.example.fasse_back.support.TestDataLoader;

@MapperTest
class PurchaseMapperTest {

    private static final String ID_1 = "a0000000-0000-4000-8000-000000000001";
    private static final String NEW_ID = "e0000000-0000-4000-8000-000000000001";

    @Autowired
    PurchaseMapper purchaseMapper;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        TestDataLoader.load(jdbc);
    }

    @Test
    void findHeaders_includesBothEndsAndOrdersByDateAndNo() {
        List<PurchaseHeader> headers = purchaseMapper.findHeaders(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2));

        assertThat(headers).extracting(PurchaseHeader::getPurchaseNo)
                .containsExactly("PO-20260701-0001", "PO-20260701-0002", "PO-20260702-0001");
    }

    @Test
    void findHeaders_singleDay() {
        assertThat(purchaseMapper.findHeaders(LocalDate.of(2026, 7, 10), LocalDate.of(2026, 7, 10)))
                .extracting(PurchaseHeader::getPurchaseNo)
                .containsExactly("PO-20260710-0001", "PO-20260710-0002");
    }

    @Test
    void findHeaders_noMatch_returnsEmpty() {
        assertThat(purchaseMapper.findHeaders(LocalDate.of(2026, 7, 3), LocalDate.of(2026, 7, 9))).isEmpty();
    }

    @Test
    void findHeaderById_mapsAllColumns() {
        PurchaseHeader header = purchaseMapper.findHeaderById("a0000000-0000-4000-8000-000000000002");

        assertThat(header.getPurchaseNo()).isEqualTo("PO-20260701-0002");
        assertThat(header.getSupplierId()).isEqualTo(2L);
        assertThat(header.getPurchaseDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(header.getDeliveryDate()).isEqualTo(LocalDate.of(2026, 7, 2));
        assertThat(header.getSubtotal()).isEqualByComparingTo("3600");
        assertThat(header.getTaxAmount()).isEqualByComparingTo("288");
        assertThat(header.getTotalAmount()).isEqualByComparingTo("3888");
        assertThat(header.getRemarks()).isEqualTo("午前中に配送");
        assertThat(header.getCreatedAt()).isNotNull();
        assertThat(purchaseMapper.findHeaderById(NEW_ID)).isNull();
    }

    @Test
    void findDetailsByPurchaseId_returnsDetails() {
        List<PurchaseDetail> details = purchaseMapper.findDetailsByPurchaseId(ID_1);

        assertThat(details).hasSize(1);
        PurchaseDetail detail = details.get(0);
        assertThat(detail.getItemId()).isEqualTo(1L);
        assertThat(detail.getQuantity()).isEqualByComparingTo("10");
        assertThat(detail.getUnitPrice()).isEqualByComparingTo("200");
        assertThat(detail.getAmount()).isEqualByComparingTo("2000");
        assertThat(detail.getTaxRate()).isEqualByComparingTo("0.08");
    }

    @Test
    void insertHeaderAndDetails() {
        PurchaseHeader header = new PurchaseHeader();
        header.setId(NEW_ID);
        header.setPurchaseNo("PO-20260801-0001");
        header.setSupplierId(1L);
        header.setPurchaseDate(LocalDate.of(2026, 8, 1));
        header.setSubtotal(new BigDecimal("300"));
        header.setTaxAmount(new BigDecimal("24"));
        header.setTotalAmount(new BigDecimal("324"));
        purchaseMapper.insertHeader(header);
        purchaseMapper.insertDetails(List.of(detail("f0000000-0000-4000-8000-000000000001", 1L),
                detail("f0000000-0000-4000-8000-000000000002", 2L)));

        assertThat(purchaseMapper.findHeaderById(NEW_ID).getPurchaseNo()).isEqualTo("PO-20260801-0001");
        assertThat(purchaseMapper.findDetailsByPurchaseId(NEW_ID)).extracting(PurchaseDetail::getItemId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void updateHeader_keepsPurchaseNo() {
        PurchaseHeader header = purchaseMapper.findHeaderById(ID_1);
        header.setPurchaseDate(LocalDate.of(2026, 7, 5));
        header.setPurchaseNo("PO-IGNORED");
        header.setRemarks("変更");

        assertThat(purchaseMapper.updateHeader(header)).isEqualTo(1);

        PurchaseHeader saved = purchaseMapper.findHeaderById(ID_1);
        assertThat(saved.getPurchaseDate()).isEqualTo(LocalDate.of(2026, 7, 5));
        assertThat(saved.getPurchaseNo()).isEqualTo("PO-20260701-0001");
        assertThat(saved.getRemarks()).isEqualTo("変更");
    }

    @Test
    void deleteDetailsAndHeader() {
        assertThat(purchaseMapper.deleteDetailsByPurchaseId(ID_1)).isEqualTo(1);
        assertThat(purchaseMapper.deleteHeader(ID_1)).isEqualTo(1);

        assertThat(purchaseMapper.findHeaderById(ID_1)).isNull();
        assertThat(purchaseMapper.findDetailsByPurchaseId(ID_1)).isEmpty();
    }

    private static PurchaseDetail detail(String id, long itemId) {
        PurchaseDetail detail = new PurchaseDetail();
        detail.setId(id);
        detail.setPurchaseId(NEW_ID);
        detail.setItemId(itemId);
        detail.setQuantity(new BigDecimal("1.5"));
        detail.setUnitPrice(new BigDecimal("100"));
        detail.setAmount(new BigDecimal("150"));
        detail.setTaxRate(new BigDecimal("0.08"));
        return detail;
    }
}
