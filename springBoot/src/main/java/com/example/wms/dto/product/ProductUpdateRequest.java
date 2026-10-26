package com.example.wms.dto.product;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/** 修改商品请求 */
@Data
public class ProductUpdateRequest {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    private Long categoryId;

    private String spec;

    private String unit;

    private String barcode;

    private BigDecimal costPrice;

    private BigDecimal salePrice;
}
