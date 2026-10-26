package com.example.wms.dto.log;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** 操作日志分页查询条件 */
@Data
public class OperationLogQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /** 操作人账号，模糊匹配 */
    private String username;

    /** 模块名，模糊匹配（如"用户管理"） */
    private String module;

    /** 是否成功：1-成功 0-失败，不传则不过滤 */
    private Integer success;

    /** 操作时间范围起 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /** 操作时间范围止 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
