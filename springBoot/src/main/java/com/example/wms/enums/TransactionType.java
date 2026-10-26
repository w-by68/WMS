package com.example.wms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库存流水类型（stock_transaction.biz_type）：
 * 每一次库存变动都必须落一条流水，LOCK/UNLOCK 只动 locked_quantity 不动 quantity。
 */
@Getter
@AllArgsConstructor
public enum TransactionType {

    INBOUND("INBOUND", "入库"),
    OUTBOUND("OUTBOUND", "出库"),
    LOCK("LOCK", "锁定（出库单待审核占用）"),
    UNLOCK("UNLOCK", "解锁（驳回/作废释放）"),
    ADJUST("ADJUST", "盘点调整");

    private final String code;

    private final String desc;

    public static TransactionType fromCode(String code) {
        for (TransactionType e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
