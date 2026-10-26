package com.example.wms.dto.inbound;

import lombok.Data;

/** 入库单分页查询条件 */
@Data
public class InboundOrderQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String status;

    private String type;

    /** 模糊匹配单号 */
    private String keyword;
}
