package com.example.wms.dto.product;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/** 创建商品请求 */
@Data
public class ProductCreateRequest {

    @NotBlank(message = "SKU 编码不能为空")
    private String sku;

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private Long categoryId;

    private String spec;

    private String unit;

    private String barcode;

    private BigDecimal costPrice;

    private BigDecimal salePrice;

    /** ENABLED / DISABLED，默认 ENABLED */
    private String status;
}
