package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.BusinessException;
import com.example.wms.common.PageResult;
import com.example.wms.common.ResultCode;
import com.example.wms.dto.product.ProductCreateRequest;
import com.example.wms.dto.product.ProductQueryDTO;
import com.example.wms.dto.product.ProductUpdateRequest;
import com.example.wms.entity.Inventory;
import com.example.wms.entity.Product;
import com.example.wms.entity.ProductCategory;
import com.example.wms.mapper.InventoryMapper;
import com.example.wms.mapper.ProductCategoryMapper;
import com.example.wms.mapper.ProductMapper;
import com.example.wms.service.ProductService;
import com.example.wms.vo.product.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商品管理服务实现。
 * 业务约束：
 * 1. SKU 全局唯一（uk_sku）；
 * 2. 分类 ID 若指定需存在；
 * 3. 删除前校验无库存引用，否则返回 2004。
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final ProductCategoryMapper productCategoryMapper;
    private final InventoryMapper inventoryMapper;

    @Override
    public PageResult<ProductVO> page(ProductQueryDTO query) {
        Page<Product> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Product::getSku, query.getKeyword())
                    .or().like(Product::getName, query.getKeyword()));
        }
        if (query.getCategoryId() != null) {
            wrapper.eq(Product::getCategoryId, query.getCategoryId());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(Product::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(Product::getCreateTime);

        IPage<Product> result = productMapper.selectPage(page, wrapper);
        Page<ProductVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        fillCategoryName(voPage.getRecords());
        return PageResult.of(voPage);
    }

    @Override
    public ProductVO get(Long id) {
        ProductVO vo = toVO(getOrThrow(id));
        fillCategoryName(List.of(vo));
        return vo;
    }

    @Override
    public ProductVO create(ProductCreateRequest request) {
        Long count = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getSku, request.getSku()));
        if (count > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "SKU 编码已存在");
        }
        // 分类存在校验
        checkCategoryExists(request.getCategoryId());

        Product product = new Product();
        BeanUtils.copyProperties(request, product);
        if (product.getCostPrice() == null) {
            product.setCostPrice(BigDecimal.ZERO);
        }
        if (product.getSalePrice() == null) {
            product.setSalePrice(BigDecimal.ZERO);
        }
        if (!StringUtils.hasText(product.getStatus())) {
            product.setStatus("ENABLED");
        }
        productMapper.insert(product);
        return toVO(product);
    }

    @Override
    public ProductVO update(Long id, ProductUpdateRequest request) {
        Product product = getOrThrow(id);
        checkCategoryExists(request.getCategoryId());
        BeanUtils.copyProperties(request, product);
        productMapper.updateById(product);
        return toVO(product);
    }

    @Override
    public void delete(Long id) {
        getOrThrow(id);
        // 引用校验：该商品是否有库存记录
        Long invCount = inventoryMapper.selectCount(
                new LambdaQueryWrapper<Inventory>().eq(Inventory::getProductId, id));
        if (invCount > 0) {
            throw new BusinessException(ResultCode.DATA_REFERENCED, "该商品存在库存记录，无法删除");
        }
        productMapper.deleteById(id);
    }

    @Override
    public void updateStatus(Long id, String status) {
        getOrThrow(id);
        productMapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, id)
                .set(Product::getStatus, status));
    }

    @Override
    public List<ProductVO> listEnabled() {
        List<Product> list = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, "ENABLED")
                        .orderByAsc(Product::getSku));
        List<ProductVO> vos = list.stream().map(this::toVO).toList();
        fillCategoryName(vos);
        return vos;
    }

    private Product getOrThrow(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "商品不存在");
        }
        return product;
    }

    private void checkCategoryExists(Long categoryId) {
        if (categoryId != null && categoryId > 0) {
            ProductCategory category = productCategoryMapper.selectById(categoryId);
            if (category == null) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "商品分类不存在");
            }
        }
    }

    private ProductVO toVO(Product product) {
        ProductVO vo = new ProductVO();
        BeanUtils.copyProperties(product, vo);
        return vo;
    }

    private void fillCategoryName(List<ProductVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> catIds = vos.stream().map(ProductVO::getCategoryId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (catIds.isEmpty()) {
            return;
        }
        Map<Long, String> catNameMap = productCategoryMapper.selectBatchIds(catIds).stream()
                .collect(Collectors.toMap(ProductCategory::getId, ProductCategory::getName));
        vos.forEach(vo -> vo.setCategoryName(catNameMap.get(vo.getCategoryId())));
    }
}
