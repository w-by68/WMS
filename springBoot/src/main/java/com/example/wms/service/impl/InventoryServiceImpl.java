package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.common.BusinessException;
import com.example.wms.common.PageResult;
import com.example.wms.common.ResultCode;
import com.example.wms.dto.inventory.InventoryAdjustRequest;
import com.example.wms.dto.inventory.InventoryQueryDTO;
import com.example.wms.entity.Inventory;
import com.example.wms.entity.InventoryAlertRule;
import com.example.wms.entity.Product;
import com.example.wms.entity.StockTransaction;
import com.example.wms.entity.StorageLocation;
import com.example.wms.entity.Warehouse;
import com.example.wms.mapper.InventoryAlertRuleMapper;
import com.example.wms.mapper.InventoryMapper;
import com.example.wms.mapper.ProductMapper;
import com.example.wms.mapper.StockTransactionMapper;
import com.example.wms.mapper.StorageLocationMapper;
import com.example.wms.mapper.WarehouseMapper;
import com.example.wms.security.SecurityUtils;
import com.example.wms.service.InventoryService;
import com.example.wms.vo.inventory.InventorySummaryVO;
import com.example.wms.vo.inventory.InventoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 库存服务实现。
 * 核心约束：
 * 1. 可用库存 = quantity - lockedQuantity，所有展示口径统一；
 * 2. 盘点调整走乐观锁（inventory.version），并发冲突时拒绝并提示刷新；
 * 3. 每次库存变动必须落一条 stock_transaction 流水（ADJUST）；
 * 4. 盘点后联动库位状态（有货 USING / 无货 EMPTY，禁用库位不自动改）。
 */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryMapper inventoryMapper;
    private final InventoryAlertRuleMapper alertRuleMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;
    private final StorageLocationMapper storageLocationMapper;
    private final StockTransactionMapper stockTransactionMapper;

    @Override
    public PageResult<InventoryVO> page(InventoryQueryDTO query) {
        // 走自定义 JOIN 分页：一次拿齐商品/仓库/库位名称与安全库存，
        // 并支持 onlyShortage 在数据库层过滤（见 InventoryMapper#selectVoPage）
        Page<InventoryVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<InventoryVO> result = inventoryMapper.selectVoPage(page, query);
        result.getRecords().forEach(this::markShortage);
        return PageResult.of(result);
    }

    @Override
    public InventoryVO get(Long id) {
        Inventory inventory = inventoryMapper.selectById(id);
        if (inventory == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "库存记录不存在");
        }
        InventoryVO vo = toVO(inventory);
        fillNames(List.of(vo));
        fillSafetyStock(List.of(vo));
        return vo;
    }

    @Override
    public List<InventorySummaryVO> summaryByProduct() {
        List<Map<String, Object>> rows = inventoryMapper.summaryByProduct();
        List<InventorySummaryVO> list = rows.stream().map(row -> {
            InventorySummaryVO vo = new InventorySummaryVO();
            vo.setProductId(((Number) row.get("productId")).longValue());
            vo.setTotalQuantity(((Number) row.get("totalQuantity")).intValue());
            vo.setTotalLockedQuantity(((Number) row.get("totalLockedQuantity")).intValue());
            vo.setTotalAvailableQuantity(vo.getTotalQuantity() - vo.getTotalLockedQuantity());
            return vo;
        }).toList();
        // 回填商品名称
        Set<Long> productIds = list.stream().map(InventorySummaryVO::getProductId).collect(Collectors.toSet());
        if (!productIds.isEmpty()) {
            Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));
            list.forEach(vo -> {
                Product p = productMap.get(vo.getProductId());
                if (p != null) {
                    vo.setProductName(p.getName());
                    vo.setSku(p.getSku());
                }
            });
        }
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjust(InventoryAdjustRequest request) {
        // 校验商品与库位存在
        Product product = productMapper.selectById(request.getProductId());
        if (product == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "商品不存在");
        }
        StorageLocation location = storageLocationMapper.selectById(request.getLocationId());
        if (location == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "库位不存在");
        }
        if ("DISABLED".equals(location.getStatus())) {
            throw new BusinessException(ResultCode.ORDER_STATE_ERROR, "库位已禁用，不允许盘点入库");
        }
        int actual = request.getActualQuantity();
        if (actual < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "盘点数量不能为负数");
        }

        Inventory inventory = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getProductId, request.getProductId())
                .eq(Inventory::getLocationId, request.getLocationId()));

        if (inventory == null) {
            // 无库存记录：实际数量 > 0 则新建
            if (actual > 0) {
                Inventory newInv = new Inventory();
                newInv.setProductId(request.getProductId());
                newInv.setWarehouseId(location.getWarehouseId());
                newInv.setLocationId(request.getLocationId());
                newInv.setQuantity(actual);
                newInv.setLockedQuantity(0);
                newInv.setVersion(0);
                newInv.setLastInboundTime(java.time.LocalDateTime.now());
                inventoryMapper.insert(newInv);
                writeTransaction(request.getProductId(), location.getWarehouseId(),
                        request.getLocationId(), 0, actual, actual, request.getRemark());
                updateLocationStatus(location, actual);
            }
            return;
        }

        int before = inventory.getQuantity();
        int change = actual - before;
        if (change == 0) {
            return; // 无差异，无需调整
        }

        inventory.setQuantity(actual);
        if (change > 0) {
            inventory.setLastInboundTime(java.time.LocalDateTime.now());
        } else {
            inventory.setLastOutboundTime(java.time.LocalDateTime.now());
        }
        int rows = inventoryMapper.updateById(inventory);
        if (rows == 0) {
            // 乐观锁冲突：version 不匹配
            throw new BusinessException(ResultCode.ORDER_STATE_ERROR, "库存数据已变更，请刷新后重试");
        }

        writeTransaction(request.getProductId(), location.getWarehouseId(),
                request.getLocationId(), before, actual, change, request.getRemark());
        updateLocationStatus(location, actual);
    }

    // ==================== 私有方法 ====================

    /** 写入一条 ADJUST 流水 */
    private void writeTransaction(Long productId, Long warehouseId, Long locationId,
                                  int before, int after, int change, String remark) {
        StockTransaction tx = new StockTransaction();
        tx.setTransactionNo(generateTransactionNo());
        tx.setProductId(productId);
        tx.setWarehouseId(warehouseId);
        tx.setLocationId(locationId);
        tx.setBizType("ADJUST");
        tx.setBizNo("ADJUST-" + System.currentTimeMillis());
        tx.setChangeQuantity(change);
        tx.setBeforeQuantity(before);
        tx.setAfterQuantity(after);
        tx.setOperatorId(SecurityUtils.getCurrentUserId());
        tx.setRemark(remark);
        stockTransactionMapper.insert(tx);
    }

    /** 流水号：ST + yyyyMMdd + 3 位当日序号 */
    private String generateTransactionNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int count = stockTransactionMapper.countTodayTransactions(dateStr);
        return "ST" + dateStr + String.format("%03d", count + 1);
    }

    /** 联动库位状态：有货 USING，无货 EMPTY；禁用库位不动 */
    private void updateLocationStatus(StorageLocation location, int quantity) {
        if ("DISABLED".equals(location.getStatus())) {
            return;
        }
        String target = quantity > 0 ? "USING" : "EMPTY";
        if (!target.equals(location.getStatus())) {
            storageLocationMapper.update(null, new LambdaUpdateWrapper<StorageLocation>()
                    .eq(StorageLocation::getId, location.getId())
                    .set(StorageLocation::getStatus, target));
        }
    }

    private InventoryVO toVO(Inventory inventory) {
        InventoryVO vo = new InventoryVO();
        BeanUtils.copyProperties(inventory, vo);
        int qty = inventory.getQuantity() == null ? 0 : inventory.getQuantity();
        int locked = inventory.getLockedQuantity() == null ? 0 : inventory.getLockedQuantity();
        vo.setAvailableQuantity(qty - locked);
        return vo;
    }

    /** 标记「可用库存低于安全库存」；安全库存为 0 表示未配置下限，不参与判断 */
    private void markShortage(InventoryVO vo) {
        int safety = vo.getSafetyStock() == null ? 0 : vo.getSafetyStock();
        int available = vo.getAvailableQuantity() == null ? 0 : vo.getAvailableQuantity();
        vo.setShortage(safety > 0 && available < safety);
    }

    /**
     * 回填安全库存（非分页场景用，如详情接口）。
     * 规则匹配顺序：先「商品 + 具体仓库」，再退到 warehouse_id 为 NULL 的通用规则。
     */
    private void fillSafetyStock(List<InventoryVO> vos) {
        Set<Long> productIds = vos.stream().map(InventoryVO::getProductId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return;
        }
        List<InventoryAlertRule> rules = alertRuleMapper.selectList(
                new LambdaQueryWrapper<InventoryAlertRule>()
                        .in(InventoryAlertRule::getProductId, productIds)
                        .eq(InventoryAlertRule::getEnabled, 1));
        Map<String, Integer> exactRules = new HashMap<>();
        Map<Long, Integer> globalRules = new HashMap<>();
        rules.forEach(rule -> {
            if (rule.getWarehouseId() == null) {
                globalRules.put(rule.getProductId(), rule.getSafetyStock());
            } else {
                exactRules.put(rule.getProductId() + ":" + rule.getWarehouseId(), rule.getSafetyStock());
            }
        });
        vos.forEach(vo -> {
            Integer safety = exactRules.get(vo.getProductId() + ":" + vo.getWarehouseId());
            if (safety == null) {
                safety = globalRules.get(vo.getProductId());
            }
            vo.setSafetyStock(safety == null ? 0 : safety);
            markShortage(vo);
        });
    }

    /** 批量回填商品/仓库/库位名称 */
    private void fillNames(List<InventoryVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> productIds = vos.stream().map(InventoryVO::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> warehouseIds = vos.stream().map(InventoryVO::getWarehouseId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> locationIds = vos.stream().map(InventoryVO::getLocationId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, Product> productMap = productIds.isEmpty() ? Map.of()
                : productMapper.selectBatchIds(productIds).stream().collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, String> warehouseMap = warehouseIds.isEmpty() ? Map.of()
                : warehouseMapper.selectBatchIds(warehouseIds).stream().collect(Collectors.toMap(Warehouse::getId, Warehouse::getName));
        Map<Long, StorageLocation> locationMap = locationIds.isEmpty() ? Map.of()
                : storageLocationMapper.selectBatchIds(locationIds).stream().collect(Collectors.toMap(StorageLocation::getId, l -> l));

        vos.forEach(vo -> {
            Product p = productMap.get(vo.getProductId());
            if (p != null) {
                vo.setProductName(p.getName());
                vo.setSku(p.getSku());
                vo.setSpec(p.getSpec());
            }
            vo.setWarehouseName(warehouseMap.get(vo.getWarehouseId()));
            StorageLocation loc = locationMap.get(vo.getLocationId());
            if (loc != null) {
                vo.setLocationCode(loc.getCode());
            }
        });
    }
}
