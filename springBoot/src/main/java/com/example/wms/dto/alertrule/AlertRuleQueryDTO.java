package com.example.wms.dto.alertrule;

import lombok.Data;

/** 预警规则分页查询条件 */
@Data
public class AlertRuleQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Long productId;

    private Long warehouseId;

    /** 是否启用：0-停用 1-启用，空查全部 */
    private Integer enabled;
}
