package com.example.wms.dto.inventory;

import lombok.Data;

/** 库存分页查询条件 */
@Data
public class InventoryQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /** 商品 ID 过滤 */
    private Long productId;

    /** 仓库 ID 过滤 */
    private Long warehouseId;

    /** 库位 ID 过滤 */
    private Long locationId;

    /** 模糊匹配商品 SKU / 名称 */
    private String keyword;

    /** 只看低于安全库存（可用库存 < 安全库存）的记录 */
    private Boolean onlyShortage;
}
