package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存预警记录表：定时任务 / 库存变动事件触发后落库。
 * 去重规则：同一 productId + warehouseId + alertType 在 UNRESOLVED 状态下不重复生成，
 * 只更新数量、级别与建议量（05 阶段实现）。
 */
@Data
@TableName("inventory_alert")
public class InventoryAlert {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 预警单号：AL + yyyyMMdd + 3 位当日序号 */
    private String alertNo;

    /** 触发所依据的规则 ID，可空 */
    private Long ruleId;

    /** 商品 ID */
    private Long productId;

    /** 仓库 ID */
    private Long warehouseId;

    /** 预警类型：SHORTAGE-缺货 OVERSTOCK-积压 DEAD_STOCK-呆滞 */
    private String alertType;

    /** 级别：INFO-提示 WARNING-警告 CRITICAL-严重 */
    private String level;

    /** 预警触发时的可用库存快照 */
    private Integer currentQuantity;

    /** 触发阈值（缺货=安全库存，积压=上限，呆滞=天数） */
    private Integer thresholdValue;

    /** 智能建议量：缺货=建议补货量，积压=建议消化量，呆滞=建议清理量（即当前可用） */
    private Integer suggestQuantity;

    /** 状态：UNRESOLVED-未处理 RESOLVED-已处理 IGNORED-已忽略 */
    private String status;

    /** 处理人（管理员） */
    private Long handlerId;

    /** 处理时间 */
    private LocalDateTime handleTime;

    /** 处理备注 */
    private String handleRemark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
