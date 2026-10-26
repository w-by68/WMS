package com.example.wms.dto.inventory;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/** 库存流水分页查询条件 */
@Data
public class StockTransactionQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Long productId;

    /** 流水类型：INBOUND / OUTBOUND / LOCK / UNLOCK / ADJUST */
    private String bizType;

    /** 关联业务单号 */
    private String bizNo;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
