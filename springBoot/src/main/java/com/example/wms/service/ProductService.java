package com.example.wms.service;

import com.example.wms.common.PageResult;
import com.example.wms.dto.product.ProductCreateRequest;
import com.example.wms.dto.product.ProductQueryDTO;
import com.example.wms.dto.product.ProductUpdateRequest;
import com.example.wms.vo.product.ProductVO;

import java.util.List;

/** 商品管理服务 */
public interface ProductService {

    PageResult<ProductVO> page(ProductQueryDTO query);

    ProductVO get(Long id);

    ProductVO create(ProductCreateRequest request);

    ProductVO update(Long id, ProductUpdateRequest request);

    /** 删除：校验无库存引用，否则返回 2004 */
    void delete(Long id);

    void updateStatus(Long id, String status);

    /** 启用商品下拉列表 */
    List<ProductVO> listEnabled();
}
