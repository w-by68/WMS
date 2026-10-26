package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.wms.common.BusinessException;
import com.example.wms.common.ResultCode;
import com.example.wms.dto.category.ProductCategoryRequest;
import com.example.wms.entity.Product;
import com.example.wms.entity.ProductCategory;
import com.example.wms.mapper.ProductCategoryMapper;
import com.example.wms.mapper.ProductMapper;
import com.example.wms.service.ProductCategoryService;
import com.example.wms.vo.category.ProductCategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品分类服务实现。
 * 业务约束：
 * 1. 删除前校验无下级分类；
 * 2. 删除前校验无商品引用该分类，否则返回 2004。
 */
@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryMapper productCategoryMapper;
    private final ProductMapper productMapper;

    @Override
    public List<ProductCategoryVO> tree() {
        List<ProductCategory> all = productCategoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>().orderByAsc(ProductCategory::getSort));
        Map<Long, ProductCategoryVO> voMap = all.stream().collect(Collectors.toMap(
                ProductCategory::getId, this::toVO));
        List<ProductCategoryVO> roots = new ArrayList<>();
        for (ProductCategoryVO vo : voMap.values()) {
            Long parentId = vo.getParentId();
            if (parentId == null || parentId == 0L) {
                roots.add(vo);
            } else {
                ProductCategoryVO parent = voMap.get(parentId);
                if (parent != null) {
                    if (parent.getChildren() == null) {
                        parent.setChildren(new ArrayList<>());
                    }
                    parent.getChildren().add(vo);
                } else {
                    // 父级不存在（数据异常），挂到根级避免丢失
                    roots.add(vo);
                }
            }
        }
        return roots;
    }

    @Override
    public ProductCategoryVO get(Long id) {
        return toVO(getOrThrow(id));
    }

    @Override
    public ProductCategoryVO create(ProductCategoryRequest request) {
        ProductCategory category = new ProductCategory();
        BeanUtils.copyProperties(request, category);
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getSort() == null) {
            category.setSort(0);
        }
        productCategoryMapper.insert(category);
        return toVO(category);
    }

    @Override
    public ProductCategoryVO update(Long id, ProductCategoryRequest request) {
        ProductCategory category = getOrThrow(id);
        BeanUtils.copyProperties(request, category);
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        productCategoryMapper.updateById(category);
        return toVO(category);
    }

    @Override
    public void delete(Long id) {
        getOrThrow(id);
        // 校验无下级分类
        Long childCount = productCategoryMapper.selectCount(
                new LambdaQueryWrapper<ProductCategory>().eq(ProductCategory::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException(ResultCode.DATA_REFERENCED, "该分类下存在子分类，无法删除");
        }
        // 校验无商品引用
        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id));
        if (productCount > 0) {
            throw new BusinessException(ResultCode.DATA_REFERENCED, "该分类下存在商品，无法删除");
        }
        productCategoryMapper.deleteById(id);
    }

    private ProductCategory getOrThrow(Long id) {
        ProductCategory category = productCategoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "商品分类不存在");
        }
        return category;
    }

    private ProductCategoryVO toVO(ProductCategory category) {
        ProductCategoryVO vo = new ProductCategoryVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }
}
