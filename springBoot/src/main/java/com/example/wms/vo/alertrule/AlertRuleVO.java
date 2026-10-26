package com.example.wms.vo.alertrule;

import lombok.Data;

import java.time.LocalDateTime;

/** 预警规则 VO */
@Data
public class AlertRuleVO {

    private Long id;

    private Long productId;

    private String productName;

    private String sku;

    private Long warehouseId;

    private String warehouseName;

    /** 是否全仓库通用（warehouseId 为空时为 true） */
    private Boolean global;

    private Integer safetyStock;

    private Integer maxStock;

    private Integer deadStockDays;

    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
