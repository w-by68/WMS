package com.example.wms.service;

import com.example.wms.common.PageResult;
import com.example.wms.dto.inventory.InventoryAdjustRequest;
import com.example.wms.dto.inventory.InventoryQueryDTO;
import com.example.wms.vo.inventory.InventorySummaryVO;
import com.example.wms.vo.inventory.InventoryVO;

import java.util.List;

/** 库存服务：查询 + 盘点调整 */
public interface InventoryService {

    PageResult<InventoryVO> page(InventoryQueryDTO query);

    InventoryVO get(Long id);

    /** 按商品汇总（跨仓库） */
    List<InventorySummaryVO> summaryByProduct();

    /**
     * 盘点调整：以实际盘点数量覆盖账面库存，差额写入 ADJUST 流水。
     * 并发安全：依赖 inventory.version 乐观锁，冲突时报"库存已变更，请刷新后重试"。
     */
    void adjust(InventoryAdjustRequest request);
}
