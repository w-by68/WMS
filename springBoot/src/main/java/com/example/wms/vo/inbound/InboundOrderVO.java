package com.example.wms.vo.inbound;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 入库单 VO（含明细） */
@Data
public class InboundOrderVO {

    private Long id;

    private String orderNo;

    private String type;

    private Long warehouseId;

    private String warehouseName;

    private Long supplierId;

    private String supplierName;

    private String status;

    private Integer totalQuantity;

    private BigDecimal totalAmount;

    private Long applicantId;

    private String applicantName;

    private Long auditorId;

    private LocalDateTime auditTime;

    private String auditRemark;

    private String remark;

    private LocalDateTime createTime;

    private List<InboundOrderItemVO> items;
}
