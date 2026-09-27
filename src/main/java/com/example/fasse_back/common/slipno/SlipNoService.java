package com.example.fasse_back.common.slipno;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 伝票番号の採番(design.md 4 節)。
 * 形式は {@code <PO|SO>-<基準日 YYYYMMDD>-<連番 4 桁ゼロ埋め>} で、連番は基準日単位でリセットする。
 */
@Service
public class SlipNoService {

    private final SlipNoMapper slipNoMapper;

    public SlipNoService(SlipNoMapper slipNoMapper) {
        this.slipNoMapper = slipNoMapper;
    }

    /**
     * 次の伝票番号を採番する。伝票登録と同じトランザクション内で呼ぶこと
     * ({@code LAST_INSERT_ID()} を同じ DB 接続で取得し、ロールバック時に連番も戻すため)。
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String next(SlipType type, LocalDate baseDate) {
        slipNoMapper.increment(type.getCounterPrefix() + "#" + baseDate);
        long sequence = slipNoMapper.selectLastInsertId();
        return format(type, baseDate, sequence);
    }

    /** 連番が 9999 を超えた場合は桁を増やす */
    static String format(SlipType type, LocalDate baseDate, long sequence) {
        return String.format("%s-%s-%04d", type.getPrefix(), baseDate.format(DateTimeFormatter.BASIC_ISO_DATE),
                sequence);
    }
}
