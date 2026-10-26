package com.example.wms.vo.dashboard;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 看板：最近库存流水（简化版） */
@Data
public class RecentTransactionVO {

    private Long id;

    private String transactionNo;

    private Long productId;

    private String productName;

    private String sku;

    /** INBOUND / OUTBOUND / LOCK / UNLOCK / ADJUST */
    private String bizType;

    private String bizNo;

    private Integer changeQuantity;

    private Integer afterQuantity;

    private LocalDateTime createTime;
}
