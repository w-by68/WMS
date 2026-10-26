package com.example.wms.controller;

import com.example.wms.aspect.OperationLog;
import com.example.wms.common.PageResult;
import com.example.wms.common.R;
import com.example.wms.dto.alert.AlertHandleRequest;
import com.example.wms.dto.alert.AlertQueryDTO;
import com.example.wms.service.AlertScanService;
import com.example.wms.service.InventoryAlertService;
import com.example.wms.vo.alert.AlertVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 库存预警记录接口（ADMIN + USER 可查看；处理/扫描仅 ADMIN） */
@Tag(name = "库存预警")
@RestController
@RequestMapping("/api/alerts")
@PreAuthorize("hasAnyRole('ADMIN','USER')")
@RequiredArgsConstructor
public class InventoryAlertController {

    private final InventoryAlertService alertService;
    private final AlertScanService alertScanService;

    @Operation(summary = "预警记录分页列表")
    @GetMapping
    public R<PageResult<AlertVO>> page(AlertQueryDTO query) {
        return R.ok(alertService.page(query));
    }

    @Operation(summary = "预警记录详情")
    @GetMapping("/{id}")
    public R<AlertVO> get(@PathVariable Long id) {
        return R.ok(alertService.get(id));
    }

    @Operation(summary = "处理预警（已处理/已忽略）")
    @OperationLog(module = "库存预警", operation = "处理预警")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/handle")
    public R<Void> handle(@PathVariable Long id, @RequestBody AlertHandleRequest request) {
        alertService.handle(id, request);
        return R.ok();
    }

    @Operation(summary = "批量处理所有未处理预警")
    @OperationLog(module = "库存预警", operation = "批量处理预警")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/batch-handle")
    public R<Integer> batchHandle(@RequestBody AlertHandleRequest request) {
        return R.ok(alertService.batchHandle(request));
    }

    @Operation(summary = "手动触发预警扫描")
    @OperationLog(module = "库存预警", operation = "手动触发预警扫描")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/scan")
    public R<Integer> scan() {
        return R.ok(alertScanService.scan());
    }
}
