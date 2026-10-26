package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;

/** 商品分类表 Mapper */
@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {
}
