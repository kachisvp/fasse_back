package com.example.fasse_back.purchase.service;

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
import com.example.fasse_back.item.repository.ItemMapper;
import com.example.fasse_back.purchase.dto.PurchaseDetailRequest;
import com.example.fasse_back.purchase.dto.PurchaseHeaderResponse;
import com.example.fasse_back.purchase.dto.PurchaseRequest;
import com.example.fasse_back.purchase.dto.PurchaseResponse;
import com.example.fasse_back.purchase.entity.PurchaseDetail;
import com.example.fasse_back.purchase.entity.PurchaseHeader;
import com.example.fasse_back.purchase.repository.PurchaseMapper;
import com.example.fasse_back.supplier.repository.SupplierMapper;

/**
 * 仕入伝票の業務ロジック(design.md 3.3 節)。
 * ヘッダと明細は 1 トランザクションで登録・更新・削除し、更新時の明細は全洗い替えとする。
 */
@Service
public class PurchaseService {

    private final PurchaseMapper purchaseMapper;
    private final SupplierMapper supplierMapper;
    private final ItemMapper itemMapper;
    private final SlipNoService slipNoService;

    public PurchaseService(PurchaseMapper purchaseMapper, SupplierMapper supplierMapper, ItemMapper itemMapper,
            SlipNoService slipNoService) {
        this.purchaseMapper = purchaseMapper;
        this.supplierMapper = supplierMapper;
        this.itemMapper = itemMapper;
        this.slipNoService = slipNoService;
    }

    /** 仕入日が範囲内(両端を含む)のヘッダ(明細なし)を返す。{@code from > to} の場合は空 */
    @Transactional(readOnly = true)
    public List<PurchaseHeaderResponse> findHeaders(DateRange range) {
        if (range.from().isAfter(range.to())) {
            return List.of();
        }
        return purchaseMapper.findHeaders(range.from(), range.to()).stream()
                .map(PurchaseHeaderResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseResponse findById(String id) {
        return load(id);
    }

    /** 伝票番号の採番 → ヘッダ登録 → 明細登録を 1 トランザクションで行う */
    @Transactional
    public PurchaseResponse create(PurchaseRequest request) {
        validateReferences(request);
        PurchaseHeader header = new PurchaseHeader();
        header.setId(UUID.randomUUID().toString());
        header.setPurchaseNo(slipNoService.next(SlipType.PURCHASE, request.purchaseDate()));
        applyHeader(header, request);
        header.setDeliveryDate(request.deliveryDate());
        header.setRemarks(request.remarks());
        purchaseMapper.insertHeader(header);
        insertDetails(header.getId(), request.details());
        return load(header.getId());
    }

    /**
     * ヘッダ更新 → 既存明細の全削除 → リクエストの明細の全登録を 1 トランザクションで行う。
     * 伝票番号は採番し直さない。ヘッダの省略した任意項目は既存の値を維持する。
     */
    @Transactional
    public PurchaseResponse update(String id, PurchaseRequest request) {
        PurchaseHeader header = getExistingHeader(id);
        validateReferences(request);
        applyHeader(header, request);
        if (request.deliveryDate() != null) {
            header.setDeliveryDate(request.deliveryDate());
        }
        if (request.remarks() != null) {
            header.setRemarks(request.remarks());
        }
        purchaseMapper.updateHeader(header);
        purchaseMapper.deleteDetailsByPurchaseId(id);
        insertDetails(id, request.details());
        return load(id);
    }

    /** 明細の全削除 → ヘッダ削除(物理削除)を 1 トランザクションで行う */
    @Transactional
    public void delete(String id) {
        getExistingHeader(id);
        purchaseMapper.deleteDetailsByPurchaseId(id);
        purchaseMapper.deleteHeader(id);
    }

    /** 必須項目をヘッダに設定する */
    private static void applyHeader(PurchaseHeader header, PurchaseRequest request) {
        header.setSupplierId(request.supplierId());
        header.setPurchaseDate(request.purchaseDate());
        header.setSubtotal(request.subtotal());
        header.setTaxAmount(request.taxAmount());
        header.setTotalAmount(request.totalAmount());
    }

    private void insertDetails(String purchaseId, List<PurchaseDetailRequest> requests) {
        if (requests.isEmpty()) {
            return;
        }
        List<PurchaseDetail> details = requests.stream().map(request -> {
            PurchaseDetail detail = new PurchaseDetail();
            detail.setId(UUID.randomUUID().toString());
            detail.setPurchaseId(purchaseId);
            detail.setItemId(request.itemId());
            detail.setQuantity(request.quantity());
            detail.setUnitPrice(request.unitPrice());
            detail.setAmount(request.amount());
            detail.setTaxRate(request.taxRate());
            return detail;
        }).toList();
        purchaseMapper.insertDetails(details);
    }

    /** 参照先マスタ(仕入先・品目)の存在を確認する。論理削除済みのマスタは参照してよい */
    private void validateReferences(PurchaseRequest request) {
        List<String> invalid = new ArrayList<>();
        if (!supplierMapper.existsById(request.supplierId())) {
            invalid.add("supplier_id");
        }
        Set<Long> itemIds = new HashSet<>();
        request.details().forEach(detail -> itemIds.add(detail.itemId()));
        Set<Long> existing = itemIds.isEmpty() ? Set.of() : new HashSet<>(itemMapper.findExistingIds(itemIds));
        for (int i = 0; i < request.details().size(); i++) {
            if (!existing.contains(request.details().get(i).itemId())) {
                invalid.add("details[" + i + "].item_id");
            }
        }
        if (!invalid.isEmpty()) {
            throw BadRequestException.invalidReference(invalid);
        }
    }

    private PurchaseResponse load(String id) {
        PurchaseHeader header = getExistingHeader(id);
        return PurchaseResponse.from(header, purchaseMapper.findDetailsByPurchaseId(id));
    }

    private PurchaseHeader getExistingHeader(String id) {
        PurchaseHeader header = purchaseMapper.findHeaderById(id);
        if (header == null) {
            throw new NotFoundException("purchase not found: id=" + id);
        }
        return header;
    }
}
