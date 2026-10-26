package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志表：由 OperationLogAspect 切面异步写入（02 阶段实现），
 * 记录管理员/用户的写操作行为，用于审计追溯。
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人 ID */
    private Long userId;

    /** 操作人账号（冗余存储，账号删除后日志仍可读） */
    private String username;

    /** 业务模块名，如"用户管理" */
    private String module;

    /** 操作描述，如"创建用户" */
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
