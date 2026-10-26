package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.Warehouse;
import org.apache.ibatis.annotations.Mapper;

/** 仓库表 Mapper */
@Mapper
public interface WarehouseMapper extends BaseMapper<Warehouse> {
}
