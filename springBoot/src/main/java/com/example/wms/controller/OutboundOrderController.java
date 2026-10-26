package com.example.wms.controller;

import com.example.wms.aspect.OperationLog;
import com.example.wms.common.PageResult;
import com.example.wms.common.R;
import com.example.wms.dto.inbound.AuditRequest;
import com.example.wms.dto.outbound.OutboundOrderQueryDTO;
import com.example.wms.dto.outbound.OutboundOrderSaveRequest;
import com.example.wms.service.OutboundOrderService;
import com.example.wms.vo.outbound.OutboundOrderVO;
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
import org.springframework.web.bind.annotation.RestController;

/** 出库单接口（ADMIN + USER；审核仅 ADMIN） */
@Tag(name = "出库单管理")
@RestController
@RequestMapping("/api/outbound-orders")
@PreAuthorize("hasAnyRole('ADMIN','USER')")
@RequiredArgsConstructor
public class OutboundOrderController {

    private final OutboundOrderService outboundOrderService;

    @Operation(summary = "出库单分页列表")
    @GetMapping
    public R<PageResult<OutboundOrderVO>> page(OutboundOrderQueryDTO query) {
        return R.ok(outboundOrderService.page(query));
    }

    @Operation(summary = "出库单详情")
    @GetMapping("/{id}")
    public R<OutboundOrderVO> get(@PathVariable Long id) {
        return R.ok(outboundOrderService.get(id));
    }

    @Operation(summary = "新建出库单")
    @OperationLog(module = "出库单", operation = "新建出库单")
    @PostMapping
    public R<OutboundOrderVO> create(@Valid @RequestBody OutboundOrderSaveRequest request) {
        return R.ok(outboundOrderService.create(request));
    }

    @Operation(summary = "修改出库单")
    @OperationLog(module = "出库单", operation = "修改出库单")
    @PutMapping("/{id}")
    public R<OutboundOrderVO> update(@PathVariable Long id, @Valid @RequestBody OutboundOrderSaveRequest request) {
        return R.ok(outboundOrderService.update(id, request));
    }

    @Operation(summary = "删除出库单")
    @OperationLog(module = "出库单", operation = "删除出库单")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        outboundOrderService.delete(id);
        return R.ok();
    }

    @Operation(summary = "提交审核（锁定库存）")
    @OperationLog(module = "出库单", operation = "提交审核")
    @PutMapping("/{id}/submit")
    public R<Void> submit(@PathVariable Long id) {
        outboundOrderService.submit(id);
        return R.ok();
    }

    @Operation(summary = "审核通过（扣减库存）")
    @OperationLog(module = "出库单", operation = "审核通过")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id) {
        outboundOrderService.approve(id);
        return R.ok();
    }

    @Operation(summary = "驳回（释放锁定）")
    @OperationLog(module = "出库单", operation = "驳回")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @RequestBody AuditRequest request) {
        outboundOrderService.reject(id, request);
        return R.ok();
    }

    @Operation(summary = "作废（释放锁定）")
    @OperationLog(module = "出库单", operation = "作废")
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        outboundOrderService.cancel(id);
        return R.ok();
    }
}
