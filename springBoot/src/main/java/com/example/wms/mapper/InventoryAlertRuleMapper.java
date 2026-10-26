package com.example.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.wms.entity.InventoryAlertRule;
import org.apache.ibatis.annotations.Mapper;

/** 库存预警规则表 Mapper */
@Mapper
public interface InventoryAlertRuleMapper extends BaseMapper<InventoryAlertRule> {
}
