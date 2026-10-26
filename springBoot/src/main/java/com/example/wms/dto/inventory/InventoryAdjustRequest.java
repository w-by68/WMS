package com.example.wms.dto.inventory;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 盘点调整请求：指定商品 + 库位的实际盘点数量 */
@Data
public class InventoryAdjustRequest {

    @NotNull(message = "商品不能为空")
    private Long productId;

    @NotNull(message = "库位不能为空")
    private Long locationId;

    /** 实际盘点数量 */
    @NotNull(message = "实际盘点数量不能为空")
    private Integer actualQuantity;

    /** 盘点差异原因 */
    private String remark;
}
