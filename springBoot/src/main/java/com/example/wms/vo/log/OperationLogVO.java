package com.example.wms.vo.log;

import lombok.Data;

import java.time.LocalDateTime;

/** 操作日志 VO：管理员审计查询用，字段与 sys_operation_log 一一对应 */
@Data
public class OperationLogVO {

    private Long id;

    /** 操作人 ID（未登录场景可能为空） */
    private Long userId;

    /** 操作人账号 */
    private String username;

    /** 业务模块名 */
    private String module;

    /** 操作描述 */
    private String operation;

    /** 请求方法（类名#方法名） */
    private String method;

    /** 请求 URL */
    private String requestUrl;

    /** 客户端 IP */
    private String ip;

    /** 耗时毫秒 */
    private Integer costMs;

    /** 是否成功：1-成功 0-失败 */
    private Integer success;

    /** 失败时的异常信息 */
    private String errorMsg;

    private LocalDateTime createTime;
}
