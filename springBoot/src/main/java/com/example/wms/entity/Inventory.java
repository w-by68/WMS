package com.example.wms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存表（核心表）：商品 x 库位 唯一（uk_product_location）。
 * 全系统库存并发安全的锚点：
 * 1. 可用库存 = quantity - lockedQuantity（所有校验与展示都以此为口径）；
 * 2. version 为乐观锁版本号 —— 任何库存更新必须先查后改，由 MP 乐观锁插件
 *    自动追加 WHERE version = ? 条件，更新失败（影响行数=0）说明发生并发冲突，
 *    上层需重试或报"库存已变更，请重试"（04 阶段实现重试模板）。
 */
@Data
@TableName("inventory")
public class Inventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** 仓库 ID（冗余存储，便于按仓库汇总） */
    private Long warehouseId;

    /** 库位 ID */
    private Long locationId;

    /** 账面库存数量 */
    private Integer quantity;

    /** 锁定数量：出库单待审核占用，可用库存 = quantity - lockedQuantity */
    private Integer lockedQuantity;

    /** 乐观锁版本号：每次更新自动 +1，并发更新冲突的裁判 */
    @Version
    private Integer version;

    /** 最近入库时间 */
    private LocalDateTime lastInboundTime;

    /** 最近出库时间：呆滞预警的判断依据 */
    private LocalDateTime lastOutboundTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
