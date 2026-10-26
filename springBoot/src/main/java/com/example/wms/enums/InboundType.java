package com.example.wms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 入库单类型：采购入库需关联供应商，退货入库对应客户退回，调整为盘点纠偏入库。
 */
@Getter
@AllArgsConstructor
public enum InboundType {

    PURCHASE("PURCHASE", "采购入库"),
    RETURN("RETURN", "退货入库"),
    ADJUST("ADJUST", "调整入库");

    private final String code;

    private final String desc;

    public static InboundType fromCode(String code) {
        for (InboundType e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
