package com.example.wms.vo.dashboard;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 数据看板 VO：首页一屏展示的核心指标 */
@Data
public class DashboardVO {

    /** SKU 种类数（库存表中去重商品数） */
    private Long totalSku;

    /** 全仓库账面库存总量 */
    private Integer totalQuantity;

    /** 库存总值（数量 × 成本价） */
    private BigDecimal totalValue;

    /** 按仓库分布 */
    private List<WarehouseInventoryVO> warehouseInventory;

    /** 预警统计 */
    private AlertStatsVO alertStats;

    /** 入库单统计 */
    private OrderStatsVO inboundStats;

    /** 出库单统计 */
    private OrderStatsVO outboundStats;

    /** 最近 10 条库存流水 */
    private List<RecentTransactionVO> recentTransactions;
}
