package com.example.wms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预警级别：缺货且可用库存为 0 或低于安全库存 50% -> CRITICAL，
 * 其余缺货/积压 -> WARNING，呆滞固定 INFO（05 阶段按此判定）。
 */
@Getter
@AllArgsConstructor
public enum AlertLevel {

    INFO("INFO", "提示"),
    WARNING("WARNING", "警告"),
    CRITICAL("CRITICAL", "严重");

    private final String code;

    private final String desc;

    public static AlertLevel fromCode(String code) {
        for (AlertLevel e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
