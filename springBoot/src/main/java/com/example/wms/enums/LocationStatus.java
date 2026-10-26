package com.example.wms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 库位状态：由库存变化自动维护（04 阶段实现），
 * 库存 > 0 -> USING，库存 = 0 -> EMPTY；DISABLED 为人工禁用，不允许再入库。
 */
@Getter
@AllArgsConstructor
public enum LocationStatus {

    EMPTY("EMPTY", "空闲"),
    USING("USING", "占用"),
    DISABLED("DISABLED", "禁用");

    private final String code;

    private final String desc;

    public static LocationStatus fromCode(String code) {
        for (LocationStatus e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
