package com.example.wms.service;

/**
 * 库存变动服务：被出入库单审核流程调用。
 * 所有方法都走 inventory.version 乐观锁，并发冲突时抛出业务异常由上层感知。
 */
public interface InventoryMutationService {

    /**
     * 入库：增加库存，写 INBOUND 流水。
     * 若该商品+库位无库存记录，则新建（适用于首次入库）。
     */
    void inbound(Long productId, Long warehouseId, Long locationId, int quantity,
                 String bizNo, Long operatorId);

    /**
     * 锁定库存：lockedQuantity += quantity。
     * 校验可用库存（quantity - lockedQuantity）>= quantity，否则抛 STOCK_INSUFFICIENT。
     * 写 LOCK 流水（changeQuantity=0，仅锁定数量变化）。
     */
    void lock(Long productId, Long warehouseId, Long locationId, int quantity,
              String bizNo, Long operatorId);

    /**
     * 解锁库存：lockedQuantity -= quantity，写 UNLOCK 流水。
     */
    void unlock(Long productId, Long warehouseId, Long locationId, int quantity,
                String bizNo, Long operatorId);

    /**
     * 出库扣减：quantity -= quantity 且 lockedQuantity -= quantity，写 OUTBOUND 流水。
     */
    void outbound(Long productId, Long warehouseId, Long locationId, int quantity,
                  String bizNo, Long operatorId);
}
