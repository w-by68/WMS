package com.example.wms.dto.inbound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 入库单创建/修改请求 */
@Data
public class InboundOrderSaveRequest {

    /** PURCHASE / RETURN / ADJUST */
    @NotNull(message = "入库类型不能为空")
    private String type;

    @NotNull(message = "入库仓库不能为空")
    private Long warehouseId;

    /** 采购入库时必填 */
    private Long supplierId;

    private String remark;

    @NotEmpty(message = "入库明细不能为空")
    @Valid
    private List<InboundOrderItemDTO> items;
}
