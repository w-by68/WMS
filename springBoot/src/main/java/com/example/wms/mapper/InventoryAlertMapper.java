package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.InventoryAlert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 库存预警记录表 Mapper */
@Mapper
public interface InventoryAlertMapper extends BaseMapper<InventoryAlert> {

    /** 统计今日预警记录数，用于生成 AL + yyyyMMdd + 3位序号 */
    @Select("SELECT COUNT(*) FROM inventory_alert WHERE alert_no LIKE CONCAT('AL', #{dateStr}, '%')")
    int countTodayAlerts(@Param("dateStr") String dateStr);
}
