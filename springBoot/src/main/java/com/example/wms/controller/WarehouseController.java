package com.example.wms.controller;

import com.example.wms.aspect.OperationLog;
import com.example.wms.common.PageResult;
import com.example.wms.common.R;
import com.example.wms.dto.warehouse.WarehouseCreateRequest;
import com.example.wms.dto.warehouse.WarehouseQueryDTO;
import com.example.wms.dto.warehouse.WarehouseUpdateRequest;
import com.example.wms.service.WarehouseService;
import com.example.wms.vo.warehouse.WarehouseVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 仓库管理接口（ADMIN） */
@Tag(name = "仓库管理")
@RestController
@RequestMapping("/api/warehouses")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "仓库分页列表")
    @GetMapping
    public R<PageResult<WarehouseVO>> page(WarehouseQueryDTO query) {
        return R.ok(warehouseService.page(query));
    }

    @Operation(summary = "仓库详情")
    @GetMapping("/{id}")
    public R<WarehouseVO> get(@PathVariable Long id) {
        return R.ok(warehouseService.get(id));
    }

    @Operation(summary = "启用仓库下拉列表")
    // 制单时需要，放开给普通用户；本类其余写接口仍受类级 ADMIN 限制
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/options")
    public R<List<WarehouseVO>> options() {
        return R.ok(warehouseService.listEnabled());
    }

    @Operation(summary = "创建仓库")
    @OperationLog(module = "仓库管理", operation = "创建仓库")
    @PostMapping
    public R<WarehouseVO> create(@Valid @RequestBody WarehouseCreateRequest request) {
        return R.ok(warehouseService.create(request));
    }

    @Operation(summary = "修改仓库")
    @OperationLog(module = "仓库管理", operation = "修改仓库")
    @PutMapping("/{id}")
    public R<WarehouseVO> update(@PathVariable Long id, @Valid @RequestBody WarehouseUpdateRequest request) {
        return R.ok(warehouseService.update(id, request));
    }

    @Operation(summary = "删除仓库")
    @OperationLog(module = "仓库管理", operation = "删除仓库")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return R.ok();
    }

    @Operation(summary = "启用/停用仓库")
    @OperationLog(module = "仓库管理", operation = "切换仓库状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        warehouseService.updateStatus(id, status);
        return R.ok();
    }
}
