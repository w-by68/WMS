package com.example.wms.dto.outbound;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 出库单明细行 */
@Data
public class OutboundOrderItemDTO {

    @NotNull(message = "商品不能为空")
    private Long productId;

    @NotNull(message = "库位不能为空")
    private Long locationId;

    @NotNull(message = "出库数量不能为空")
    private Integer quantity;

    private BigDecimal price;
}
