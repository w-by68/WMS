package com.example.wms.vo.category;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 商品分类 VO（树形） */
@Data
public class ProductCategoryVO {

    private Long id;

    private String name;

    private Long parentId;

    private Integer sort;

    private LocalDateTime createTime;

    /** 子分类列表 */
    private List<ProductCategoryVO> children;
}
