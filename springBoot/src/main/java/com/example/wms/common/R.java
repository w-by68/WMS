package com.example.wms.common;

import lombok.Data;

/**
 * 统一返回体：所有接口的响应都包裹在此结构中。
 * 约定：HTTP 状态码恒为 200，业务成败由 code 区分，前端只需拆一层即可。
 *
 * @param <T> 业务数据类型
 */
@Data
public class R<T> {

    /** 业务状态码：200 成功，其余见 {@link ResultCode} */
    private Integer code;

    /** 提示信息，成功为 success，失败为可读的中文原因 */
    private String message;

    /** 业务数据，失败时一般为 null */
    private T data;

    /** 响应时间戳（毫秒），便于排查接口耗时与时序问题 */
    private Long timestamp;

    private R(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    /** 成功：无数据 */
    public static <T> R<T> ok() {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    /** 成功：携带数据 */
    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    /** 失败：指定错误码枚举 */
    public static <T> R<T> fail(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    /** 失败：指定错误码枚举，但覆盖提示信息（用于给前端更具体的文案） */
    public static <T> R<T> fail(ResultCode resultCode, String message) {
        return new R<>(resultCode.getCode(), message, null);
    }

    /** 失败：直接使用错误码与文案（兜底场景） */
    public static <T> R<T> fail(Integer code, String message) {
        return new R<>(code, message, null);
    }
}
