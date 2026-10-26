package com.example.wms.controller;

import com.example.wms.aspect.OperationLog;
import com.example.wms.common.PageResult;
import com.example.wms.common.R;
import com.example.wms.dto.inventory.InventoryAdjustRequest;
import com.example.wms.dto.inventory.InventoryQueryDTO;
import com.example.wms.service.InventoryService;
import com.example.wms.vo.inventory.InventorySummaryVO;
import com.example.wms.vo.inventory.InventoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 库存查询与盘点调整接口（ADMIN + USER） */
@Tag(name = "库存管理")
@RestController
@RequestMapping("/api/inventory")
@PreAuthorize("hasAnyRole('ADMIN','USER')")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @Operation(summary = "库存分页列表")
    @GetMapping
    public R<PageResult<InventoryVO>> page(InventoryQueryDTO query) {
        return R.ok(inventoryService.page(query));
    }

    @Operation(summary = "库存详情")
    @GetMapping("/{id}")
    public R<InventoryVO> get(@PathVariable Long id) {
        return R.ok(inventoryService.get(id));
    }

    @Operation(summary = "按商品汇总库存（跨仓库）")
    @GetMapping("/summary")
    public R<List<InventorySummaryVO>> summary() {
        return R.ok(inventoryService.summaryByProduct());
    }

    @Operation(summary = "盘点调整")
    @OperationLog(module = "库存管理", operation = "盘点调整")
    @PostMapping("/adjust")
    public R<Void> adjust(@Valid @RequestBody InventoryAdjustRequest request) {
        inventoryService.adjust(request);
        return R.ok();
    }
}
