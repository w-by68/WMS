package com.example.wms.service;

import com.example.wms.dto.category.ProductCategoryRequest;
import com.example.wms.vo.category.ProductCategoryVO;

import java.util.List;

/** 商品分类服务 */
public interface ProductCategoryService {

    /** 全部分类树 */
    List<ProductCategoryVO> tree();

    ProductCategoryVO get(Long id);

    ProductCategoryVO create(ProductCategoryRequest request);

    ProductCategoryVO update(Long id, ProductCategoryRequest request);

    /** 删除：校验无子分类且无商品引用，否则返回 2004 */
    void delete(Long id);
}
