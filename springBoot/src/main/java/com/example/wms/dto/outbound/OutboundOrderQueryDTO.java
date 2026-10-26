package com.example.wms.dto.outbound;

import lombok.Data;

/** 出库单分页查询条件 */
@Data
public class OutboundOrderQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String status;

    private String type;

    private String keyword;
}
