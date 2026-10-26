package com.example.wms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预警类型：缺货（低于安全库存）、积压（高于上限）、呆滞（长期无出库）。
 * 本项目的"智能"点即围绕这三种类型的自动扫描与建议量计算（05 阶段实现）。
 */
@Getter
@AllArgsConstructor
public enum AlertType {

    SHORTAGE("SHORTAGE", "缺货"),
    OVERSTOCK("OVERSTOCK", "积压"),
    DEAD_STOCK("DEAD_STOCK", "呆滞");

    private final String code;

    private final String desc;

    public static AlertType fromCode(String code) {
        for (AlertType e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
