package com.example.fasse_back.sales.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.fasse_back.sales.entity.SalesDetail;
import com.example.fasse_back.sales.entity.SalesHeader;
import com.example.fasse_back.support.MapperTest;
import com.example.fasse_back.support.TestDataLoader;

@MapperTest
class SalesMapperTest {

    private static final String ID_2 = "c0000000-0000-4000-8000-000000000002";
    private static final String NEW_ID = "e0000000-0000-4000-8000-000000000002";

    @Autowired
    SalesMapper salesMapper;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        TestDataLoader.load(jdbc);
    }

    @Test
    void findHeaders_filtersByBusinessDateNotSalesDatetime() {
        // SO-20260701-0003 は売上日時が 7/2 01:30 だが営業日は 7/1
        assertThat(salesMapper.findHeaders(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 1)))
                .extracting(SalesHeader::getSalesNo)
                .containsExactly("SO-20260701-0001", "SO-20260701-0002", "SO-20260701-0003");
    }

    @Test
    void findHeaders_includesBothEnds() {
        assertThat(salesMapper.findHeaders(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 2))).hasSize(5);
        assertThat(salesMapper.findHeaders(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 3)))
                .extracting(SalesHeader::getSalesNo)
                .containsExactly("SO-20260702-0001", "SO-20260702-0002");
    }

    @Test
    void findHeaderById_mapsAllColumns() {
        SalesHeader header = salesMapper.findHeaderById(ID_2);

        assertThat(header.getSalesNo()).isEqualTo("SO-20260701-0002");
        assertThat(header.getSalesDatetime()).isEqualTo(LocalDateTime.of(2026, 7, 1, 20, 15));
        assertThat(header.getBusinessDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(header.getTableNo()).isEqualTo("B2");
        assertThat(header.getCustomerCount()).isEqualTo(4);
        assertThat(header.getDiscountAmount()).isEqualByComparingTo("100");
        assertThat(header.getTotalAmount()).isEqualByComparingTo("2716");
        assertThat(header.getPaymentMethod()).isEqualTo("CARD");
        assertThat(header.getRemarks()).isEqualTo("クーポン利用");
    }

    @Test
    void findDetailsBySalesId_returnsAllDetails() {
        List<SalesDetail> details = salesMapper.findDetailsBySalesId(ID_2);

        assertThat(details).extracting(SalesDetail::getMenuId).containsExactlyInAnyOrder(2L, 4L);
        assertThat(details).allSatisfy(d -> assertThat(d.getTaxRate()).isEqualByComparingTo("0.10"));
    }

    @Test
    void insertHeaderAndDetails_storesDatetimeAsIs() {
        SalesHeader header = new SalesHeader();
        header.setId(NEW_ID);
        header.setSalesNo("SO-20260801-0001");
        header.setSalesDatetime(LocalDateTime.of(2026, 8, 1, 19, 30, 15));
        header.setBusinessDate(LocalDate.of(2026, 8, 1));
        header.setCustomerCount(1);
        header.setSubtotal(new BigDecimal("600"));
        header.setTaxAmount(new BigDecimal("60"));
        header.setDiscountAmount(BigDecimal.ZERO);
        header.setTotalAmount(new BigDecimal("660"));
        header.setPaymentMethod("CASH");
        salesMapper.insertHeader(header);

        SalesDetail detail = new SalesDetail();
        detail.setId("f0000000-0000-4000-8000-000000000003");
        detail.setSalesId(NEW_ID);
        detail.setMenuId(1L);
        detail.setQuantity(1);
        detail.setUnitPrice(new BigDecimal("600"));
        detail.setAmount(new BigDecimal("600"));
        detail.setTaxRate(new BigDecimal("0.1"));
        salesMapper.insertDetails(List.of(detail));

        SalesHeader saved = salesMapper.findHeaderById(NEW_ID);
        assertThat(saved.getSalesDatetime()).isEqualTo(LocalDateTime.of(2026, 8, 1, 19, 30, 15));
        assertThat(saved.getTableNo()).isNull();
        assertThat(salesMapper.findDetailsBySalesId(NEW_ID)).hasSize(1);
    }

    @Test
    void updateHeader_keepsSalesNo() {
        SalesHeader header = salesMapper.findHeaderById(ID_2);
        header.setBusinessDate(LocalDate.of(2026, 7, 3));
        header.setCustomerCount(5);

        assertThat(salesMapper.updateHeader(header)).isEqualTo(1);

        SalesHeader saved = salesMapper.findHeaderById(ID_2);
        assertThat(saved.getSalesNo()).isEqualTo("SO-20260701-0002");
        assertThat(saved.getBusinessDate()).isEqualTo(LocalDate.of(2026, 7, 3));
        assertThat(saved.getCustomerCount()).isEqualTo(5);
    }

    @Test
    void deleteDetailsAndHeader() {
        assertThat(salesMapper.deleteDetailsBySalesId(ID_2)).isEqualTo(2);
        assertThat(salesMapper.deleteHeader(ID_2)).isEqualTo(1);

        assertThat(salesMapper.findHeaderById(ID_2)).isNull();
        assertThat(salesMapper.findDetailsBySalesId(ID_2)).isEmpty();
    }
}
