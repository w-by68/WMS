package com.example.wms.common;

import lombok.Getter;

/**
 * 业务异常：Service 层业务规则不满足时抛出（如库存不足、单据状态不允许）。
 * 由 GlobalExceptionHandler 统一捕获并转为对应业务码的返回体，
 * 保证 Controller 不需要写 try-catch，事务也能正常回滚（运行时异常默认回滚）。
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 业务错误码，来自 ResultCode */
    private final Integer code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    /** 常用场景：保留错误码，但给前端更具体的提示（如带上商品名与可用数量） */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
