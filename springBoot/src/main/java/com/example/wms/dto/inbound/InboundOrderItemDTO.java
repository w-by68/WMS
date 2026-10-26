package com.example.wms.dto.inbound;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** 入库单明细行 */
@Data
public class InboundOrderItemDTO {

    @NotNull(message = "商品不能为空")
    private Long productId;

    @NotNull(message = "库位不能为空")
    private Long locationId;

    @NotNull(message = "入库数量不能为空")
    private Integer quantity;

    private BigDecimal price;
}
