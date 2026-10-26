package com.example.wms.vo.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类库存占比的一项：前端饼图的每个扇区。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryStockVO {

    /** 分类名称；商品未挂分类时归为"未分类" */
    private String categoryName;

    /** 该分类下所有商品的账面库存合计 */
    private Integer quantity;
}
