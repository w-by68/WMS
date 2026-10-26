package com.example.wms.vo.inventory;

import lombok.Data;

import java.time.LocalDateTime;

/** 库存流水 VO（带商品/仓库/库位名称） */
@Data
public class StockTransactionVO {

    private Long id;

    private String transactionNo;

    private Long productId;

    private String productName;

    private Long warehouseId;

    private String warehouseName;

    private Long locationId;

    private String locationCode;

    private String bizType;

    private String bizNo;

    private Integer changeQuantity;

    private Integer beforeQuantity;

    private Integer afterQuantity;

    private Long operatorId;

    private String remark;

    private LocalDateTime createTime;
}
