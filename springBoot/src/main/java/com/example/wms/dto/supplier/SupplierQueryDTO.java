package com.example.wms.dto.supplier;

import lombok.Data;

/** 供应商分页查询条件 */
@Data
public class SupplierQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String keyword;

    private String status;
}
