package com.example.wms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户角色：系统只有两种角色，管理员负责全局管理与审核，普通用户负责制单与查询。
 */
@Getter
@AllArgsConstructor
public enum UserRole {

    ADMIN("ADMIN", "管理员"),
    USER("USER", "普通用户");

    /** 与数据库 role 字段一致的取值 */
    private final String code;

    private final String desc;

    /** 按库中的字符串值反查枚举，查不到返回 null（脏数据容忍，由调用方决定如何处理） */
    public static UserRole fromCode(String code) {
        for (UserRole e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
