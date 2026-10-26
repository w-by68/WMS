package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.wms.entity.Inventory;
import com.example.wms.entity.InventoryAlert;
import com.example.wms.entity.InboundOrder;
import com.example.wms.entity.OutboundOrder;
import com.example.wms.entity.Product;
import com.example.wms.entity.StockTransaction;
import com.example.wms.entity.Warehouse;
import com.example.wms.mapper.InventoryAlertMapper;
import com.example.wms.mapper.InventoryMapper;
import com.example.wms.mapper.InboundOrderMapper;
import com.example.wms.mapper.OutboundOrderMapper;
import com.example.wms.mapper.ProductMapper;
import com.example.wms.mapper.StockTransactionMapper;
import com.example.wms.mapper.WarehouseMapper;
import com.example.wms.service.DashboardService;
import com.example.wms.vo.dashboard.AlertStatsVO;
import com.example.wms.vo.dashboard.CategoryStockVO;
import com.example.wms.vo.dashboard.DashboardVO;
import com.example.wms.vo.dashboard.OrderStatsVO;
import com.example.wms.vo.dashboard.RecentTransactionVO;
import com.example.wms.vo.dashboard.TrendPointVO;
import com.example.wms.vo.dashboard.WarehouseInventoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据看板服务实现：首页一屏核心指标聚合。
 * 全部为只读查询，无事务。
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final InventoryMapper inventoryMapper;
    private final InventoryAlertMapper alertMapper;
    private final InboundOrderMapper inboundOrderMapper;
    private final OutboundOrderMapper outboundOrderMapper;
    private final StockTransactionMapper stockTransactionMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;

    @Override
    public DashboardVO overview() {
        DashboardVO vo = new DashboardVO();

        // 1. 库存总量 & 总值 & SKU 数
        List<Map<String, Object>> whRows = inventoryMapper.summaryByWarehouse();
        int totalQty = 0;
        BigDecimal totalValue = BigDecimal.ZERO;
        for (Map<String, Object> row : whRows) {
            totalQty += ((Number) row.get("totalQuantity")).intValue();
            Object val = row.get("totalValue");
            if (val != null) {
                totalValue = totalValue.add(new BigDecimal(val.toString()));
            }
        }
        vo.setTotalQuantity(totalQty);
        vo.setTotalValue(totalValue);

        // SKU 种类数
        vo.setTotalSku(countDistinctProducts());

        // 2. 按仓库分布
        vo.setWarehouseInventory(buildWarehouseInventory(whRows));

        // 3. 预警统计
        vo.setAlertStats(buildAlertStats());

        // 4. 入库单统计
        vo.setInboundStats(buildInboundStats());

        // 5. 出库单统计
        vo.setOutboundStats(buildOutboundStats());

        // 6. 最近 10 条流水
        vo.setRecentTransactions(buildRecentTransactions());

        return vo;
    }

    @Override
    public List<TrendPointVO> trend(int days) {
        int span = days <= 0 ? 30 : days;
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(span - 1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 查询结果按日期聚合，避免同一天出现多条
        Map<String, int[]> agg = new HashMap<>();
        List<Map<String, Object>> rows = stockTransactionMapper.trendByDay(from.atStartOfDay());
        for (Map<String, Object> row : rows) {
            String date = String.valueOf(row.get("bizDate"));
            int in = row.get("inboundQty") == null ? 0 : ((Number) row.get("inboundQty")).intValue();
            int out = row.get("outboundQty") == null ? 0 : ((Number) row.get("outboundQty")).intValue();
            agg.put(date, new int[]{in, out});
        }

        // 逐日补零：查询缺哪天就补 0
        List<TrendPointVO> result = new ArrayList<>(span);
        for (int i = 0; i < span; i++) {
            String date = from.plusDays(i).format(fmt);
            int[] v = agg.getOrDefault(date, new int[]{0, 0});
            result.add(new TrendPointVO(date, v[0], v[1]));
        }
        return result;
    }

    @Override
    public List<CategoryStockVO> categoryStock() {
        List<Map<String, Object>> rows = inventoryMapper.categoryStock();
        if (rows.isEmpty()) {
            return List.of();
        }
        return rows.stream().map(r -> {
            String name = r.get("categoryName") == null ? "未分类" : r.get("categoryName").toString();
            int qty = r.get("quantity") == null ? 0 : ((Number) r.get("quantity")).intValue();
            return new CategoryStockVO(name, qty);
        }).collect(Collectors.toList());
    }

    private Long countDistinctProducts() {
        List<Inventory> list = inventoryMapper.selectList(null);
        return list.stream().map(Inventory::getProductId).distinct().count();
    }

    private List<WarehouseInventoryVO> buildWarehouseInventory(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        Set<Long> whIds = rows.stream()
                .map(r -> ((Number) r.get("warehouseId")).longValue())
                .collect(Collectors.toSet());
        Map<Long, String> whMap = warehouseMapper.selectBatchIds(whIds).stream()
                .collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
        return rows.stream().map(r -> {
            WarehouseInventoryVO vo = new WarehouseInventoryVO();
            Long whId = ((Number) r.get("warehouseId")).longValue();
            vo.setWarehouseId(whId);
            vo.setWarehouseName(whMap.get(whId));
            vo.setTotalQuantity(((Number) r.get("totalQuantity")).intValue());
            Object val = r.get("totalValue");
            vo.setTotalValue(val == null ? BigDecimal.ZERO : new BigDecimal(val.toString()));
            return vo;
        }).toList();
    }

    private AlertStatsVO buildAlertStats() {
        AlertStatsVO vo = new AlertStatsVO();
        vo.setUnresolved(alertMapper.selectCount(new LambdaQueryWrapper<InventoryAlert>()
                .eq(InventoryAlert::getStatus, "UNRESOLVED")));
        vo.setShortage(alertMapper.selectCount(new LambdaQueryWrapper<InventoryAlert>()
                .eq(InventoryAlert::getStatus, "UNRESOLVED")
                .eq(InventoryAlert::getAlertType, "SHORTAGE")));
        vo.setOverstock(alertMapper.selectCount(new LambdaQueryWrapper<InventoryAlert>()
                .eq(InventoryAlert::getStatus, "UNRESOLVED")
                .eq(InventoryAlert::getAlertType, "OVERSTOCK")));
        vo.setDeadStock(alertMapper.selectCount(new LambdaQueryWrapper<InventoryAlert>()
                .eq(InventoryAlert::getStatus, "UNRESOLVED")
                .eq(InventoryAlert::getAlertType, "DEAD_STOCK")));
        vo.setCritical(alertMapper.selectCount(new LambdaQueryWrapper<InventoryAlert>()
                .eq(InventoryAlert::getStatus, "UNRESOLVED")
                .eq(InventoryAlert::getLevel, "CRITICAL")));
        return vo;
    }

    private OrderStatsVO buildInboundStats() {
        OrderStatsVO vo = new OrderStatsVO();
        vo.setPending(inboundOrderMapper.selectCount(new LambdaQueryWrapper<InboundOrder>()
                .eq(InboundOrder::getStatus, "PENDING")));
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        vo.setToday((long) inboundOrderMapper.countTodayOrders(dateStr));
        return vo;
    }

    private OrderStatsVO buildOutboundStats() {
        OrderStatsVO vo = new OrderStatsVO();
        vo.setPending(outboundOrderMapper.selectCount(new LambdaQueryWrapper<OutboundOrder>()
                .eq(OutboundOrder::getStatus, "PENDING")));
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        vo.setToday((long) outboundOrderMapper.countTodayOrders(dateStr));
        return vo;
    }

    private List<RecentTransactionVO> buildRecentTransactions() {
        List<StockTransaction> txs = stockTransactionMapper.selectList(
                new LambdaQueryWrapper<StockTransaction>()
                        .orderByDesc(StockTransaction::getCreateTime)
                        .last("LIMIT 10"));
        if (txs.isEmpty()) {
            return List.of();
        }
        Set<Long> productIds = txs.stream().map(StockTransaction::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        return txs.stream().map(tx -> {
            RecentTransactionVO vo = new RecentTransactionVO();
            vo.setId(tx.getId());
            vo.setTransactionNo(tx.getTransactionNo());
            vo.setProductId(tx.getProductId());
            vo.setBizType(tx.getBizType());
            vo.setBizNo(tx.getBizNo());
            vo.setChangeQuantity(tx.getChangeQuantity());
            vo.setAfterQuantity(tx.getAfterQuantity());
            vo.setCreateTime(tx.getCreateTime());
            Product p = productMap.get(tx.getProductId());
            if (p != null) {
                vo.setProductName(p.getName());
                vo.setSku(p.getSku());
            }
            return vo;
        }).toList();
    }
}
