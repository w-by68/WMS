package com.example.wms.dto.customer;

import lombok.Data;

/** 客户分页查询条件 */
@Data
public class CustomerQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String keyword;

    private String status;
}
