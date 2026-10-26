package com.example.wms.dto.warehouse;

import lombok.Data;

/** 仓库分页查询条件 */
@Data
public class WarehouseQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /** 模糊匹配编码或名称 */
    private String keyword;

    /** 状态过滤：ENABLED / DISABLED */
    private String status;
}
