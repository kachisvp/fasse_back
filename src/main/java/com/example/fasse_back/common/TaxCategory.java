package com.example.fasse_back.common;

/**
 * 税区分(design.md 2.3 節)。消費税法で定まる固定区分のため、独立したマスタにせず ENUM とする。
 * 定義順は DB の ENUM 定義と一致させる(一覧の並び順に使うため)。
 */
public enum TaxCategory {
    /** 標準税率 */
    STANDARD,
    /** 軽減税率 */
    REDUCED,
    /** 非課税 */
    EXEMPT
}
