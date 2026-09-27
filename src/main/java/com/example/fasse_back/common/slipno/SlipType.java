package com.example.fasse_back.common.slipno;

/** 伝票の種類と、伝票番号の接頭辞・採番カウンタ名の対応(design.md 4 節) */
public enum SlipType {
    /** 仕入伝票(例: PO-20260712-0001) */
    PURCHASE("PO", "purchase_no"),
    /** 売上伝票(例: SO-20260712-0001) */
    SALES("SO", "sales_no");

    private final String prefix;
    private final String counterPrefix;

    SlipType(String prefix, String counterPrefix) {
        this.prefix = prefix;
        this.counterPrefix = counterPrefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getCounterPrefix() {
        return counterPrefix;
    }
}
