package com.example.wms.vo.alert;

import lombok.Data;

import java.time.LocalDateTime;

/** 库存预警记录 VO */
@Data
public class AlertVO {

    private Long id;

    private String alertNo;

    private Long ruleId;

    private Long productId;

    private String productName;

    private String sku;

    private Long warehouseId;

    private String warehouseName;

    /** SHORTAGE / OVERSTOCK / DEAD_STOCK */
    private String alertType;

    /** INFO / WARNING / CRITICAL */
    private String level;

    /** 触发时可用库存 */
    private Integer currentQuantity;

    /** 阈值 */
    private Integer thresholdValue;

    /** 建议量 */
    private Integer suggestQuantity;

    /** UNRESOLVED / RESOLVED / IGNORED */
    private String status;

    private Long handlerId;

    private String handlerName;

    private LocalDateTime handleTime;

    private String handleRemark;

    private LocalDateTime createTime;
}
