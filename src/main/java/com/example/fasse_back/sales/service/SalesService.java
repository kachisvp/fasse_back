package com.example.fasse_back.sales.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fasse_back.common.exception.BadRequestException;
import com.example.fasse_back.common.exception.NotFoundException;
import com.example.fasse_back.common.slipno.SlipNoService;
import com.example.fasse_back.common.slipno.SlipType;
import com.example.fasse_back.common.web.RequestParams.DateRange;
import com.example.fasse_back.menu.repository.MenuMapper;
import com.example.fasse_back.sales.dto.SalesDetailRequest;
import com.example.fasse_back.sales.dto.SalesHeaderResponse;
import com.example.fasse_back.sales.dto.SalesRequest;
import com.example.fasse_back.sales.dto.SalesResponse;
import com.example.fasse_back.sales.entity.SalesDetail;
import com.example.fasse_back.sales.entity.SalesHeader;
import com.example.fasse_back.sales.repository.SalesMapper;

/**
 * 売上伝票の業務ロジック(design.md 3.3 節)。
 * ヘッダと明細は 1 トランザクションで登録・更新・削除し、更新時の明細は全洗い替えとする。
 */
@Service
public class SalesService {

    /** 業務日時のタイムゾーン(JST 固定) */
    public static final ZoneOffset JST = ZoneOffset.ofHours(9);

    private static final int DEFAULT_CUSTOMER_COUNT = 1;

    private final SalesMapper salesMapper;
    private final MenuMapper menuMapper;
    private final SlipNoService slipNoService;

    public SalesService(SalesMapper salesMapper, MenuMapper menuMapper, SlipNoService slipNoService) {
        this.salesMapper = salesMapper;
        this.menuMapper = menuMapper;
        this.slipNoService = slipNoService;
    }

    /** 営業日が範囲内(両端を含む)のヘッダ(明細なし)を返す。{@code from > to} の場合は空 */
    @Transactional(readOnly = true)
    public List<SalesHeaderResponse> findHeaders(DateRange range) {
        if (range.from().isAfter(range.to())) {
            return List.of();
        }
        return salesMapper.findHeaders(range.from(), range.to()).stream()
                .map(SalesHeaderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SalesResponse findById(String id) {
        return load(id);
    }

    /**
     * 伝票番号の採番 → ヘッダ登録 → 明細登録を 1 トランザクションで行う。
     * {@code customer_count} 省略時は 1、{@code discount_amount} 省略時は 0 とする。
     */
    @Transactional
    public SalesResponse create(SalesRequest request) {
        validateReferences(request);
        SalesHeader header = new SalesHeader();
        header.setId(UUID.randomUUID().toString());
        header.setSalesNo(slipNoService.next(SlipType.SALES, request.businessDate()));
        applyHeader(header, request);
        header.setTableNo(request.tableNo());
        header.setCustomerCount(request.customerCount() == null ? DEFAULT_CUSTOMER_COUNT : request.customerCount());
        header.setDiscountAmount(request.discountAmount() == null ? BigDecimal.ZERO : request.discountAmount());
        header.setRemarks(request.remarks());
        salesMapper.insertHeader(header);
        insertDetails(header.getId(), request.details());
        return load(header.getId());
    }

    /**
     * ヘッダ更新 → 既存明細の全削除 → リクエストの明細の全登録を 1 トランザクションで行う。
     * 伝票番号は採番し直さない。ヘッダの省略した任意項目は既存の値を維持する。
     */
    @Transactional
    public SalesResponse update(String id, SalesRequest request) {
        SalesHeader header = getExistingHeader(id);
        validateReferences(request);
        applyHeader(header, request);
        if (request.tableNo() != null) {
            header.setTableNo(request.tableNo());
        }
        if (request.customerCount() != null) {
            header.setCustomerCount(request.customerCount());
        }
        if (request.discountAmount() != null) {
            header.setDiscountAmount(request.discountAmount());
        }
        if (request.remarks() != null) {
            header.setRemarks(request.remarks());
        }
        salesMapper.updateHeader(header);
        salesMapper.deleteDetailsBySalesId(id);
        insertDetails(id, request.details());
        return load(id);
    }

    /** 明細の全削除 → ヘッダ削除(物理削除)を 1 トランザクションで行う */
    @Transactional
    public void delete(String id) {
        getExistingHeader(id);
        salesMapper.deleteDetailsBySalesId(id);
        salesMapper.deleteHeader(id);
    }

    /** 売上日時を JST の日時にする(DB の DATETIME に JST で保存するため) */
    static LocalDateTime toJst(OffsetDateTime dateTime) {
        return dateTime.withOffsetSameInstant(JST).toLocalDateTime();
    }

    /** 必須項目をヘッダに設定する */
    private static void applyHeader(SalesHeader header, SalesRequest request) {
        header.setSalesDatetime(toJst(request.salesDatetime()));
        header.setBusinessDate(request.businessDate());
        header.setSubtotal(request.subtotal());
        header.setTaxAmount(request.taxAmount());
        header.setTotalAmount(request.totalAmount());
        header.setPaymentMethod(request.paymentMethod());
    }

    private void insertDetails(String salesId, List<SalesDetailRequest> requests) {
        if (requests.isEmpty()) {
            return;
        }
        List<SalesDetail> details = requests.stream().map(request -> {
            SalesDetail detail = new SalesDetail();
            detail.setId(UUID.randomUUID().toString());
            detail.setSalesId(salesId);
            detail.setMenuId(request.menuId());
            detail.setQuantity(request.quantity());
            detail.setUnitPrice(request.unitPrice());
            detail.setAmount(request.amount());
            detail.setTaxRate(request.taxRate());
            return detail;
        }).toList();
        salesMapper.insertDetails(details);
    }

    /** 参照先マスタ(メニュー)の存在を確認する。論理削除済みのマスタは参照してよい */
    private void validateReferences(SalesRequest request) {
        Set<Long> menuIds = new HashSet<>();
        request.details().forEach(detail -> menuIds.add(detail.menuId()));
        if (menuIds.isEmpty()) {
            return;
        }
        Set<Long> existing = new HashSet<>(menuMapper.findExistingIds(menuIds));
        List<String> invalid = new ArrayList<>();
        for (int i = 0; i < request.details().size(); i++) {
            if (!existing.contains(request.details().get(i).menuId())) {
                invalid.add("details[" + i + "].menu_id");
            }
        }
        if (!invalid.isEmpty()) {
            throw BadRequestException.invalidReference(invalid);
        }
    }

    private SalesResponse load(String id) {
        SalesHeader header = getExistingHeader(id);
        return SalesResponse.from(header, salesMapper.findDetailsBySalesId(id));
    }

    private SalesHeader getExistingHeader(String id) {
        SalesHeader header = salesMapper.findHeaderById(id);
        if (header == null) {
            throw new NotFoundException("sales not found: id=" + id);
        }
        return header;
    }
}
