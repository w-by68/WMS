package com.example.wms.dto.alertrule;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 预警规则创建/修改请求 */
@Data
public class AlertRuleSaveRequest {

    @NotNull(message = "商品不能为空")
    private Long productId;

    /** 仓库 ID，为空表示全仓库通用规则 */
    private Long warehouseId;

    /** 安全库存下限：可用库存低于此值触发缺货预警 */
    @NotNull(message = "安全库存下限不能为空")
    private Integer safetyStock;

    /** 库存上限：高于此值触发积压预警；0 表示不限制 */
    @NotNull(message = "库存上限不能为空")
    private Integer maxStock;

    /** 呆滞天数：超过该天数无出库则预警 */
    @NotNull(message = "呆滞天数不能为空")
    private Integer deadStockDays;

    /** 是否启用：0-停用 1-启用 */
    private Integer enabled;
}
