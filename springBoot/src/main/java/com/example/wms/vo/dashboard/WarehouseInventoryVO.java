package com.example.wms.vo.dashboard;

import lombok.Data;

import java.math.BigDecimal;

/** 看板：按仓库汇总的库存 */
@Data
public class WarehouseInventoryVO {

    private Long warehouseId;

    private String warehouseName;

    /** 该仓库总账面库存 */
    private Integer totalQuantity;

    /** 该仓库库存总值（数量 × 成本价） */
    private BigDecimal totalValue;
}
