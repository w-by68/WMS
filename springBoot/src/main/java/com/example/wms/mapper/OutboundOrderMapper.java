package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.OutboundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 出库单主表 Mapper */
@Mapper
public interface OutboundOrderMapper extends BaseMapper<OutboundOrder> {

    /** 统计今日出库单数量，用于生成 OUT + yyyyMMdd + 3位序号 */
    @Select("SELECT COUNT(*) FROM outbound_order WHERE order_no LIKE CONCAT('OUT', #{dateStr}, '%')")
    int countTodayOrders(@Param("dateStr") String dateStr);
}
