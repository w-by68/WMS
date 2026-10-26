package com.example.wms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 出入库单据状态机（出入库单共用同一套状态）：
 * DRAFT -> PENDING -> APPROVED / REJECTED；PENDING 可被作废为 CANCELED。
 * 仅 DRAFT / REJECTED 允许编辑与删除；APPROVED / CANCELED 为终态只读。
 */
@Getter
@AllArgsConstructor
public enum OrderStatus {

    DRAFT("DRAFT", "草稿"),
    PENDING("PENDING", "待审核"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已驳回"),
    CANCELED("CANCELED", "已作废");

    private final String code;

    private final String desc;

    public static OrderStatus fromCode(String code) {
        for (OrderStatus e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
