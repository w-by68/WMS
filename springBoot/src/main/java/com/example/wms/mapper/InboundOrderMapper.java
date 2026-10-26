package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.InboundOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 入库单主表 Mapper */
@Mapper
public interface InboundOrderMapper extends BaseMapper<InboundOrder> {

    /** 统计今日入库单数量，用于生成 IN + yyyyMMdd + 3位序号 */
    @Select("SELECT COUNT(*) FROM inbound_order WHERE order_no LIKE CONCAT('IN', #{dateStr}, '%')")
    int countTodayOrders(@Param("dateStr") String dateStr);
}
