package com.example.wms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 出库单类型：销售出库需关联客户，领用对应内部部门领用，报损为损耗出库。
 */
@Getter
@AllArgsConstructor
public enum OutboundType {

    SALE("SALE", "销售出库"),
    PICK("PICK", "领用出库"),
    SCRAP("SCRAP", "报损出库");

    private final String code;

    private final String desc;

    public static OutboundType fromCode(String code) {
        for (OutboundType e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
