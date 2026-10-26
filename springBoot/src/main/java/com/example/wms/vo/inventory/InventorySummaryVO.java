package com.example.wms.vo.inventory;

import lombok.Data;

/** 按商品汇总的库存 VO */
@Data
public class InventorySummaryVO {

    private Long productId;

    private String productName;

    private String sku;

    /** 总账面库存 */
    private Integer totalQuantity;

    /** 总锁定数量 */
    private Integer totalLockedQuantity;

    /** 总可用库存 */
    private Integer totalAvailableQuantity;
}
