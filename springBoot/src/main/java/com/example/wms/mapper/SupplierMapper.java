package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.Supplier;
import org.apache.ibatis.annotations.Mapper;

/** 供应商表 Mapper */
@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {
}
