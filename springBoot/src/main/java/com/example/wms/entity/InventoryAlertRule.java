package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存预警规则表："智能"的配置来源。
 * productId + warehouseId 唯一；warehouseId 为 NULL 表示全仓库通用规则，
 * 匹配时优先取具体仓库规则，没有再 fallback 到通用规则（05 阶段实现）。
 */
@Data
@TableName("inventory_alert_rule")
public class InventoryAlertRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** 仓库 ID，NULL 表示全仓库通用 */
    private Long warehouseId;

    /** 安全库存下限：可用库存低于此值触发 SHORTAGE 缺货预警 */
    private Integer safetyStock;

    /** 库存上限：可用库存高于此值触发 OVERSTOCK 积压预警；0 表示不限制 */
    private Integer maxStock;

    /** 呆滞天数：超过该天数无出库记录触发 DEAD_STOCK 呆滞预警 */
    private Integer deadStockDays;

    /** 是否启用：0-停用 1-启用（停用的规则不参与扫描） */
    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
