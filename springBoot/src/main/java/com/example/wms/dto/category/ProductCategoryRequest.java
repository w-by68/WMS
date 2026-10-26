package com.example.wms.dto.category;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 创建/修改商品分类请求 */
@Data
public class ProductCategoryRequest {

    @NotBlank(message = "分类名称不能为空")
    private String name;

    /** 父级分类 ID，0 表示一级分类 */
    private Long parentId;

    /** 同级排序号，越小越靠前 */
    private Integer sort;
}
