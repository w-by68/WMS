package com.example.wms.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举：与 docs 接口清单.md 末尾的错误码表严格对应，前后端共同遵守。
 * 约定：HTTP 状态码恒为 200，业务码由本枚举表达。
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    /** 成功 */
    SUCCESS(200, "success"),

    /** 参数校验失败（@Valid 校验不通过由全局异常处理器统一转为此码） */
    PARAM_ERROR(1001, "参数校验失败"),

    /** 未登录 / token 失效（JwtAuthenticationFilter 直接写响应时使用） */
    UNAUTHORIZED(1002, "未登录或登录已过期"),

    /** 账号已禁用 */
    ACCOUNT_DISABLED(1003, "账号已禁用"),

    /** 无权限（角色不足） */
    FORBIDDEN(1004, "无权限访问"),

    /** 业务冲突：用户名已存在 / SKU 重复 / 编码重复 */
    DUPLICATE_KEY(2001, "数据已存在，不可重复"),

    /** 库存不足（出库提交时可用库存不够） */
    STOCK_INSUFFICIENT(2002, "库存不足"),

    /** 单据状态不允许当前操作（如重复审核、审核后再编辑） */
    ORDER_STATE_ERROR(2003, "单据状态不允许当前操作"),

    /** 数据被引用，无法删除（如仓库下有库位、商品有库存） */
    DATA_REFERENCED(2004, "数据被引用，无法删除"),

    /** 系统异常（未预期的错误兜底） */
    SYSTEM_ERROR(500, "系统异常，请稍后重试");

    private final Integer code;

    private final String message;
}
