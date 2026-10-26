package com.example.wms.dto.alert;

import lombok.Data;

/** 预警记录分页查询条件 */
@Data
public class AlertQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /** 预警类型：SHORTAGE / OVERSTOCK / DEAD_STOCK */
    private String alertType;

    /** 级别：INFO / WARNING / CRITICAL */
    private String level;

    /** 状态：UNRESOLVED / RESOLVED / IGNORED */
    private String status;

    private Long productId;

    private Long warehouseId;
}
