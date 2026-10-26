package com.example.wms.vo.product;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 商品 VO（带分类名称） */
@Data
public class ProductVO {

    private Long id;

    private String sku;

    private String name;

    private Long categoryId;

    private String categoryName;

    private String spec;

    private String unit;

    private String barcode;

    private BigDecimal costPrice;

    private BigDecimal salePrice;

    private String status;

    private LocalDateTime createTime;
}
