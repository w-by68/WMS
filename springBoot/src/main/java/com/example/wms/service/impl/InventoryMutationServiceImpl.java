package com.example.wms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.wms.common.BusinessException;
import com.example.wms.common.ResultCode;
import com.example.wms.entity.Inventory;
import com.example.wms.entity.StockTransaction;
import com.example.wms.mapper.InventoryMapper;
import com.example.wms.mapper.StockTransactionMapper;
import com.example.wms.service.InventoryMutationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 库存变动服务实现。
 * 并发安全：所有写操作依赖 inventory.version 乐观锁，更新失败（影响行数=0）时抛出
 *  ORDER_STATE_ERROR 提示上层重试。
 */
@Service
@RequiredArgsConstructor
public class InventoryMutationServiceImpl implements InventoryMutationService {

    private final InventoryMapper inventoryMapper;
    private final StockTransactionMapper stockTransactionMapper;

    @Override
    public void inbound(Long productId, Long warehouseId, Long locationId, int quantity,
                        String bizNo, Long operatorId) {
        Inventory inv = getOrCreate(productId, warehouseId, locationId);
        int before = inv.getQuantity();
        int after = before + quantity;
        inv.setQuantity(after);
        inv.setLastInboundTime(LocalDateTime.now());
        updateWithVersion(inv);
        writeTransaction(productId, warehouseId, locationId, "INBOUND", bizNo,
                quantity, before, after, operatorId);
    }

    @Override
    public void lock(Long productId, Long warehouseId, Long locationId, int quantity,
                     String bizNo, Long operatorId) {
        Inventory inv = getOrThrow(productId, locationId);
        int available = inv.getQuantity() - inv.getLockedQuantity();
        if (available < quantity) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT,
                    "可用库存不足，当前可用 " + available);
        }
        int before = inv.getQuantity();
        inv.setLockedQuantity(inv.getLockedQuantity() + quantity);
        updateWithVersion(inv);
        // LOCK 不改变实际库存数量，流水 changeQuantity=0
        writeTransaction(productId, warehouseId, locationId, "LOCK", bizNo,
                0, before, before, operatorId);
    }

    @Override
    public void unlock(Long productId, Long warehouseId, Long locationId, int quantity,
                       String bizNo, Long operatorId) {
        Inventory inv = getOrThrow(productId, locationId);
        int before = inv.getQuantity();
        inv.setLockedQuantity(Math.max(0, inv.getLockedQuantity() - quantity));
        updateWithVersion(inv);
        writeTransaction(productId, warehouseId, locationId, "UNLOCK", bizNo,
                0, before, before, operatorId);
    }

    @Override
    public void outbound(Long productId, Long warehouseId, Long locationId, int quantity,
                         String bizNo, Long operatorId) {
        Inventory inv = getOrThrow(productId, locationId);
        int before = inv.getQuantity();
        int after = before - quantity;
        if (after < 0) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT, "库存不足，无法出库");
        }
        inv.setQuantity(after);
        inv.setLockedQuantity(Math.max(0, inv.getLockedQuantity() - quantity));
        inv.setLastOutboundTime(LocalDateTime.now());
        updateWithVersion(inv);
        writeTransaction(productId, warehouseId, locationId, "OUTBOUND", bizNo,
                -quantity, before, after, operatorId);
    }

    // ==================== 私有方法 ====================

    /** 查库存，不存在则新建（仅入库场景使用） */
    private Inventory getOrCreate(Long productId, Long warehouseId, Long locationId) {
        Inventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getProductId, productId)
                .eq(Inventory::getLocationId, locationId));
        if (inv == null) {
            inv = new Inventory();
            inv.setProductId(productId);
            inv.setWarehouseId(warehouseId);
            inv.setLocationId(locationId);
            inv.setQuantity(0);
            inv.setLockedQuantity(0);
            inv.setVersion(0);
            inventoryMapper.insert(inv);
        }
        return inv;
    }

    /** 查库存，不存在抛异常（出库/锁定场景必须有库存） */
    private Inventory getOrThrow(Long productId, Long locationId) {
        Inventory inv = inventoryMapper.selectOne(new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getProductId, productId)
                .eq(Inventory::getLocationId, locationId));
        if (inv == null) {
            throw new BusinessException(ResultCode.STOCK_INSUFFICIENT, "该库位无此商品库存");
        }
        return inv;
    }

    /** 乐观锁更新，失败抛冲突异常 */
    private void updateWithVersion(Inventory inv) {
        int rows = inventoryMapper.updateById(inv);
        if (rows == 0) {
            throw new BusinessException(ResultCode.ORDER_STATE_ERROR, "库存数据已变更，请刷新后重试");
        }
    }

    private void writeTransaction(Long productId, Long warehouseId, Long locationId,
                                  String bizType, String bizNo, int change,
                                  int before, int after, Long operatorId) {
        StockTransaction tx = new StockTransaction();
        tx.setTransactionNo(generateTransactionNo());
        tx.setProductId(productId);
        tx.setWarehouseId(warehouseId);
        tx.setLocationId(locationId);
        tx.setBizType(bizType);
        tx.setBizNo(bizNo);
        tx.setChangeQuantity(change);
        tx.setBeforeQuantity(before);
        tx.setAfterQuantity(after);
        tx.setOperatorId(operatorId);
        stockTransactionMapper.insert(tx);
    }

    private String generateTransactionNo() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int count = stockTransactionMapper.countTodayTransactions(dateStr);
        return "ST" + dateStr + String.format("%03d", count + 1);
    }
}
