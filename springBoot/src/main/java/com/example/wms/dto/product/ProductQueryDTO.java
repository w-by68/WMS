package com.example.wms.dto.product;

import lombok.Data;

import java.math.BigDecimal;

/** 商品分页查询条件 */
@Data
public class ProductQueryDTO {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    /** 模糊匹配 SKU 或名称 */
    private String keyword;

    private Long categoryId;

    private String status;
}
