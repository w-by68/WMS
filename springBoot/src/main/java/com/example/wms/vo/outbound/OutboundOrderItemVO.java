package com.example.wms.vo.outbound;

import lombok.Data;

import java.math.BigDecimal;

/** 出库单明细 VO */
@Data
public class OutboundOrderItemVO {

    private Long id;

    private Long productId;

    private String productName;

    private String sku;

    private Long locationId;

    private String locationCode;

    private Integer quantity;

    private BigDecimal price;

    private BigDecimal amount;
}
