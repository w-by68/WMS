package com.example.wms.vo.inventory;

import lombok.Data;

import java.time.LocalDateTime;

/** 库存 VO（带商品/仓库/库位名称 + 可用库存） */
@Data
public class InventoryVO {

    private Long id;

    private Long productId;

    private String productName;

    private String sku;

    /** 商品规格 */
    private String spec;

    private Long warehouseId;

    private String warehouseName;

    private Long locationId;

    private String locationCode;

    /** 账面库存 */
    private Integer quantity;

    /** 锁定数量 */
    private Integer lockedQuantity;

    /** 可用库存 = quantity - lockedQuantity */
    private Integer availableQuantity;

    /**
     * 安全库存下限：取自 inventory_alert_rule，
     * 优先匹配「商品 + 具体仓库」的规则，没有则退到 warehouse_id 为 NULL 的通用规则；
     * 都没有配置时为 0（表示未设置下限，不参与缺货判断）。
     */
    private Integer safetyStock;

    /** 是否低于安全库存（可用库存 < 安全库存），用于列表整行标红 */
    private Boolean shortage;

    /** 乐观锁版本号 */
    private Integer version;

    private LocalDateTime lastInboundTime;

    private LocalDateTime lastOutboundTime;
}
