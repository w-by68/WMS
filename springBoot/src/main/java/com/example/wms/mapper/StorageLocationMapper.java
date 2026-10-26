package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.StorageLocation;
import org.apache.ibatis.annotations.Mapper;

/** 库位表 Mapper */
@Mapper
public interface StorageLocationMapper extends BaseMapper<StorageLocation> {
}
