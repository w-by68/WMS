package com.example.wms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户/仓库/商品等通用启停状态。
 * DISABLED 的账号无法登录；DISABLED 的仓库/商品不再出现在下拉选项中。
 */
@Getter
@AllArgsConstructor
public enum UserStatus {

    ENABLED("ENABLED", "启用"),
    DISABLED("DISABLED", "禁用");

    private final String code;

    private final String desc;

    public static UserStatus fromCode(String code) {
        for (UserStatus e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }
}
