package com.example.wms.dto.outbound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 出库单创建/修改请求 */
@Data
public class OutboundOrderSaveRequest {

    /** SALE / PICK / SCRAP */
    @NotNull(message = "出库类型不能为空")
    private String type;

    @NotNull(message = "出库仓库不能为空")
    private Long warehouseId;

    /** 销售出库时必填 */
    private Long customerId;

    private String remark;

    @NotEmpty(message = "出库明细不能为空")
    @Valid
    private List<OutboundOrderItemDTO> items;
}
