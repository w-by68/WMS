package com.example.wms.controller;

import com.example.wms.aspect.OperationLog;
import com.example.wms.common.PageResult;
import com.example.wms.common.R;
import com.example.wms.dto.supplier.SupplierCreateRequest;
import com.example.wms.dto.supplier.SupplierQueryDTO;
import com.example.wms.dto.supplier.SupplierUpdateRequest;
import com.example.wms.service.SupplierService;
import com.example.wms.vo.supplier.SupplierVO;
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

/** 供应商管理接口（ADMIN） */
@Tag(name = "供应商管理")
@RestController
@RequestMapping("/api/suppliers")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @Operation(summary = "供应商分页列表")
    @GetMapping
    public R<PageResult<SupplierVO>> page(SupplierQueryDTO query) {
        return R.ok(supplierService.page(query));
    }

    @Operation(summary = "供应商详情")
    @GetMapping("/{id}")
    public R<SupplierVO> get(@PathVariable Long id) {
        return R.ok(supplierService.get(id));
    }

    @Operation(summary = "启用供应商下拉列表")
    // 制单时需要，放开给普通用户；本类其余写接口仍受类级 ADMIN 限制
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    @GetMapping("/options")
    public R<List<SupplierVO>> options() {
        return R.ok(supplierService.listEnabled());
    }

    @Operation(summary = "创建供应商")
    @OperationLog(module = "供应商管理", operation = "创建供应商")
    @PostMapping
    public R<SupplierVO> create(@Valid @RequestBody SupplierCreateRequest request) {
        return R.ok(supplierService.create(request));
    }

    @Operation(summary = "修改供应商")
    @OperationLog(module = "供应商管理", operation = "修改供应商")
    @PutMapping("/{id}")
    public R<SupplierVO> update(@PathVariable Long id, @Valid @RequestBody SupplierUpdateRequest request) {
        return R.ok(supplierService.update(id, request));
    }

    @Operation(summary = "删除供应商")
    @OperationLog(module = "供应商管理", operation = "删除供应商")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return R.ok();
    }

    @Operation(summary = "启用/停用供应商")
    @OperationLog(module = "供应商管理", operation = "切换供应商状态")
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        supplierService.updateStatus(id, status);
        return R.ok();
    }
}
