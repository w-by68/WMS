package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.OutboundOrderItem;
import org.apache.ibatis.annotations.Mapper;

/** 出库单明细表 Mapper */
@Mapper
public interface OutboundOrderItemMapper extends BaseMapper<OutboundOrderItem> {
}
