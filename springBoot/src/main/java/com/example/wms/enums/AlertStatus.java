package com.example.wms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 预警处理状态：
 * UNRESOLVED 未处理（去重窗口：同商品+仓库+类型只允许一条未处理记录）；
 * RESOLVED / IGNORED 为终态，之后的扫描可重新生成新预警。
 */
@Getter
@AllArgsConstructor
public enum AlertStatus {

    UNRESOLVED("UNRESOLVED", "未处理"),
    RESOLVED("RESOLVED", "已处理"),
    IGNORED("IGNORED", "已忽略");

    private final String code;

    private final String desc;

    public static AlertStatus fromCode(String code) {
        for (AlertStatus e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
