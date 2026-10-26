package com.example.wms.controller;

import com.example.wms.aspect.OperationLog;
import com.example.wms.common.PageResult;
import com.example.wms.common.R;
import com.example.wms.dto.location.StorageLocationCreateRequest;
import com.example.wms.dto.location.StorageLocationQueryDTO;
import com.example.wms.dto.location.StorageLocationUpdateRequest;
import com.example.wms.service.StorageLocationService;
import com.example.wms.vo.location.StorageLocationVO;
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

/** 库位管理接口（ADMIN） */
@Tag(name = "库位管理")
@RestController
@RequestMapping("/api/locations")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class StorageLocationController {

    private final StorageLocationService storageLocationService;

    @Operation(summary = "库位分页列表")
    @GetMapping
    public R<PageResult<StorageLocationVO>> page(StorageLocationQueryDTO query) {
        return R.ok(storageLocationService.page(query));
    }

    @Operation(summary = "库位详情")
    @GetMapping("/{id}")
    public R<StorageLocationVO> get(@PathVariable Long id) {
        return R.ok(storageLocationService.get(id));
    }

    @Operation(summary = "按仓库查询库位列表")
    // 制单时需要（库位随仓库联动），放开给普通用户；本类其余写接口仍受类级 ADMIN 限制
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/by-warehouse")
    public R<List<StorageLocationVO>> listByWarehouse(@RequestParam Long warehouseId) {
        return R.ok(storageLocationService.listByWarehouse(warehouseId));
    }

    @Operation(summary = "创建库位")
    @OperationLog(module = "库位管理", operation = "创建库位")
    @PostMapping
    public R<StorageLocationVO> create(@Valid @RequestBody StorageLocationCreateRequest request) {
        return R.ok(storageLocationService.create(request));
    }

    @Operation(summary = "修改库位")
    @OperationLog(module = "库位管理", operation = "修改库位")
    @PutMapping("/{id}")
    public R<StorageLocationVO> update(@PathVariable Long id, @RequestBody StorageLocationUpdateRequest request) {
        return R.ok(storageLocationService.update(id, request));
    }

    @Operation(summary = "删除库位")
    @OperationLog(module = "库位管理", operation = "删除库位")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        storageLocationService.delete(id);
        return R.ok();
    }

    @Operation(summary = "启用/禁用库位")
    @OperationLog(module = "库位管理", operation = "切换库位状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        storageLocationService.updateStatus(id, status);
        return R.ok();
    }
}
